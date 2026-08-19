package com.veggofresh.payment.service.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.customer.entity.DeliverySlot;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.AddressRepository;
import com.veggofresh.customer.repository.CartRepository;
import com.veggofresh.customer.repository.DeliverySlotRepository;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.CartService;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.payment.dto.request.CreatePaymentOrderRequest;
import com.veggofresh.payment.dto.request.VerifyPaymentRequest;
import com.veggofresh.payment.dto.response.PaymentOrderResponse;
import com.veggofresh.payment.dto.response.PaymentVerifyResponse;
import com.veggofresh.payment.entity.Payment;
import com.veggofresh.payment.entity.PaymentStatus;
import com.veggofresh.payment.repository.PaymentRepository;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.platform.config.RazorpayConfig;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Razorpay payment service � Authorize/Hold/Capture/Void model.
 *
 * <p><b>Money flow:</b>
 * <ol>
 *   <li>createPaymentOrder()  ? Razorpay order created with payment_capture=0 (hold mode)</li>
 *   <li>verifyPayment()       ? Signature verified; Payment -> AUTHORIZED</li>
 *   <li>capturePayment()      ? Vendor accepts; Razorpay Capture + wallet finalization</li>
 *   <li>voidPayment()         ? Timeout; Razorpay Void + wallet reservation released</li>
 *   <li>settleOrderToWallets()? Delivery complete; Vendor/Delivery/Admin wallets credited</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final DeliverySlotRepository deliverySlotRepository;
    private final ProductCatalogService productCatalogService;
    private final CartService cartService;
    private final AddressRepository addressRepository;
    private final OrderService orderService;
    private final WalletService walletService;

    // -- 1. CREATE PAYMENT ORDER (Hold/Authorize) --------------

    @Override
    public PaymentOrderResponse createPaymentOrder(UUID userId, CreatePaymentOrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND",
                        "Cart not found � add items before initiating payment", HttpStatus.NOT_FOUND));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("CART_EMPTY",
                    "Cannot initiate payment for an empty cart", HttpStatus.BAD_REQUEST);
        }

        BigDecimal subtotal = computeSubtotal(cart);
        BigDecimal deliveryFee = BigDecimal.valueOf(5.00);
        BigDecimal estimatedTax = subtotal.multiply(BigDecimal.valueOf(0.05));
        BigDecimal total = subtotal.add(deliveryFee).add(estimatedTax);

        // Wallet deduction (if requested)
        BigDecimal walletAmount = BigDecimal.ZERO;
        BigDecimal razorpayAmount = total;
        if (Boolean.TRUE.equals(request.getUseWallet())) {
            com.veggofresh.payment.entity.Wallet wallet =
                    walletService.getOrCreateWallet(userId, "CUSTOMER");
            BigDecimal available = wallet.getAvailableBalance();
            walletAmount = available.min(total);
            razorpayAmount = total.subtract(walletAmount);
            if (walletAmount.compareTo(BigDecimal.ZERO) > 0) {
                walletService.reserve(userId, walletAmount, null,
                        "Wallet hold for checkout");
            }
        }

        // Build VegGoFresh PAYMENT_PENDING order
        com.veggofresh.customer.entity.Order pendingOrder =
                buildPendingOrder(userId, request, cart, subtotal, deliveryFee, estimatedTax, total);
        com.veggofresh.customer.entity.Order savedOrder = orderRepository.save(pendingOrder);

        // If full wallet-covered, skip Razorpay
        String razorpayOrderId = null;
        long amountInPaise = 0L;
        if (razorpayAmount.compareTo(BigDecimal.ZERO) > 0) {
            amountInPaise = razorpayAmount.multiply(BigDecimal.valueOf(100)).longValue();
            razorpayOrderId = createRazorpayOrder(amountInPaise, savedOrder.getId().toString());
        }

        // Persist Payment record
        Payment payment = new Payment();
        payment.setOrderId(savedOrder.getId());
        payment.setUserId(userId);
        payment.setRazorpayOrderId(razorpayOrderId != null ? razorpayOrderId : "WALLET_ONLY_" + savedOrder.getId());
        payment.setAmount(total);
        payment.setCurrency(razorpayConfig.getCurrency());
        payment.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(payment);

        // Mirror on order
        savedOrder.setRazorpayOrderId(razorpayOrderId);
        savedOrder.setPaymentStatus(PaymentStatus.CREATED.name());
        orderRepository.save(savedOrder);

        log.info("Payment order created: razorpayOrderId={}, orderId={}, total={}, walletPortion={}, razorpayPortion={}",
                razorpayOrderId, savedOrder.getId(), total, walletAmount, razorpayAmount);

        return PaymentOrderResponse.builder()
                .veggoOrderId(savedOrder.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(total)
                .amountInPaise(amountInPaise)
                .walletAmountUsed(walletAmount)
                .razorpayAmountToPay(razorpayAmount)
                .currency(razorpayConfig.getCurrency())
                .keyId(razorpayConfig.getKeyId())
                .build();
    }

    // -- 2. VERIFY PAYMENT (AUTHORIZE state) -------------------

    @Override
    public PaymentVerifyResponse verifyPayment(UUID userId, VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                        "No payment found for Razorpay order ID: " + request.getRazorpayOrderId(),
                        HttpStatus.NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.AUTHORIZED
                || payment.getStatus() == PaymentStatus.CAPTURED) {
            log.warn("verifyPayment called for already-verified payment: {}", request.getRazorpayOrderId());
            return buildVerifyResponse(payment, userId);
        }

        verifyRazorpaySignature(request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(), request.getRazorpaySignature());

        // Move to AUTHORIZED (hold confirmed, not captured yet)
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        paymentRepository.save(payment);

        com.veggofresh.customer.entity.Order order = orderRepository.findById(request.getVeggoOrderId())
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND",
                        "Order not found", HttpStatus.NOT_FOUND));
        order.setPaymentStatus(PaymentStatus.AUTHORIZED.name());
        orderRepository.save(order);

        log.info("Payment AUTHORIZED (hold confirmed). Awaiting vendor accept to capture. orderId={}",
                request.getVeggoOrderId());

        return buildVerifyResponse(payment, userId);
    }

    // -- 3. CAPTURE (Vendor Accepts) ---------------------------

    @Override
    @Transactional
    public void capturePayment(UUID orderId, UUID vendorUserId, BigDecimal deliveryFee, BigDecimal commissionPercent) {
        com.veggofresh.customer.entity.Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        Payment payment = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.AUTHORIZED || p.getStatus() == PaymentStatus.CREATED)
                .findFirst()
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                        "No authorized payment found for order", HttpStatus.NOT_FOUND));

        // Call Razorpay Capture API (if a real Razorpay order exists)
        if (payment.getRazorpayPaymentId() != null
                && !payment.getRazorpayOrderId().startsWith("WALLET_ONLY_")) {
            try {
                long amountInPaise = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
                JSONObject captureRequest = new JSONObject();
                captureRequest.put("amount", amountInPaise);
                captureRequest.put("currency", razorpayConfig.getCurrency());
                razorpayClient.payments.capture(payment.getRazorpayPaymentId(), captureRequest);
                log.info("Razorpay Capture SUCCESS: paymentId={}, amount={}", payment.getRazorpayPaymentId(), payment.getAmount());
            } catch (RazorpayException e) {
                log.error("Razorpay Capture FAILED: {}. Reversing vendor accept.", e.getMessage());
                throw new BusinessException("PAYMENT_CAPTURE_FAILED",
                        "Payment capture failed. Vendor accept reversed.", HttpStatus.BAD_GATEWAY);
            }
        }

        // Finalize wallet reservation if any
        walletService.finalizeReservation(order.getUserId(),
                computeWalletPortion(payment), orderId, payment.getRazorpayPaymentId());

        // Update payment + order status
        payment.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.CAPTURED.name());
        order.setPaymentMethodId(payment.getRazorpayPaymentId());
        orderRepository.save(order);

        // Clear cart
        cartRepository.findByUserId(order.getUserId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });

        log.info("Payment CAPTURED. Order PLACED. orderId={}", orderId);
    }

    // -- 4. VOID (Timeout � nobody accepted) -------------------

    @Override
    @Transactional
    public void voidPayment(UUID orderId) {
        com.veggofresh.customer.entity.Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        Payment payment = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.AUTHORIZED || p.getStatus() == PaymentStatus.CREATED)
                .findFirst().orElse(null);

        if (payment == null) {
            log.warn("voidPayment called but no active payment found for orderId={}", orderId);
            return;
        }

        // Call Razorpay Void (only if real Razorpay payment exists)
        if (payment.getRazorpayPaymentId() != null
                && !payment.getRazorpayOrderId().startsWith("WALLET_ONLY_")) {
            try {
                JSONObject voidRequest = new JSONObject();
                voidRequest.put("amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
                // Razorpay SDK: refund with amount=0 effectively releases an uncaptured auth
                // Real void: call refunds.create on an AUTHORIZED payment_id
                razorpayClient.payments.refund(payment.getRazorpayPaymentId(), voidRequest);
                log.info("Razorpay Void (refund of hold) called for paymentId={}", payment.getRazorpayPaymentId());
            } catch (RazorpayException e) {
                log.error("Razorpay Void call failed: {}. Continuing with local void.", e.getMessage());
            }
        }

        // Release wallet reservation
        walletService.releaseReservation(order.getUserId(),
                computeWalletPortion(payment), orderId,
                "Order cancelled � vendor not found. Hold released.");

        payment.setStatus(PaymentStatus.VOIDED);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.VOIDED.name());
        orderRepository.save(order);

        log.info("Payment VOIDED. Order CANCELLED. orderId={}", orderId);
    }

    // -- 5. SETTLE TO WALLETS (Delivery Complete) --------------

    @Override
    @Transactional
    public void settleOrderToWallets(UUID orderId, UUID vendorUserId, UUID deliveryUserId,
                                     UUID adminUserId, BigDecimal vendorEarnings,
                                     BigDecimal deliveryEarnings, BigDecimal adminCommission) {
        walletService.credit(vendorUserId, "VENDOR", vendorEarnings, orderId,
                "Earnings for order #" + orderId);
        walletService.credit(deliveryUserId, "DELIVERY", deliveryEarnings, orderId,
                "Delivery fare for order #" + orderId);
        walletService.credit(adminUserId, "ADMIN", adminCommission, orderId,
                "Platform commission for order #" + orderId);
        log.info("Wallet settlement complete. orderId={}, vendor=+{}, delivery=+{}, admin=+{}",
                orderId, vendorEarnings, deliveryEarnings, adminCommission);
    }

    // -- 6. WEBHOOK --------------------------------------------

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        verifyWebhookSignature(payload, signature);

        JSONObject body = new JSONObject(payload);
        String event = body.optString("event", "");
        log.info("Razorpay webhook received: event={}", event);

        switch (event) {
            case "payment.authorized" -> handlePaymentAuthorized(body);
            case "payment.captured"   -> handlePaymentCaptured(body);
            case "payment.failed"     -> handlePaymentFailed(body);
            default -> log.debug("Razorpay webhook event ignored: {}", event);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrder(UUID orderId, UUID userId) {
        orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND",
                        "Order not found for user", HttpStatus.NOT_FOUND));
        return paymentRepository.findByOrderId(orderId);
    }

    // -- Webhook Handlers --------------------------------------

    private void handlePaymentAuthorized(JSONObject body) {
        String razorpayPaymentId = extractPaymentId(body);
        String razorpayOrderId = extractOrderId(body);
        paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.CREATED) {
                payment.setStatus(PaymentStatus.AUTHORIZED);
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setWebhookEvent("payment.authorized");
                paymentRepository.save(payment);
                log.info("Webhook payment.authorized processed: orderId={}", razorpayOrderId);
            }
        });
    }

    private void handlePaymentCaptured(JSONObject body) {
        String razorpayOrderId = extractOrderId(body);
        paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.CAPTURED) {
                log.info("Webhook payment.captured: already captured, idempotent skip.");
                return;
            }
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setWebhookEvent("payment.captured");
            paymentRepository.save(payment);
            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                    order.setStatus(OrderStatus.PLACED);
                    order.setPaymentStatus(PaymentStatus.CAPTURED.name());
                    orderRepository.save(order);
                }
            });
            log.info("Webhook payment.captured processed.");
        });
    }

    private void handlePaymentFailed(JSONObject body) {
        String razorpayOrderId = extractOrderId(body);
        String reason = extractFailureReason(body);
        paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            payment.setWebhookEvent("payment.failed");
            paymentRepository.save(payment);
            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.FAILED.name());
                orderRepository.save(order);
            });
            log.info("Webhook payment.failed: {}", reason);
        });
    }

    // -- Private Helpers ---------------------------------------

    private String createRazorpayOrder(long amountInPaise, String receipt) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", razorpayConfig.getCurrency());
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 0); // Authorize/Hold mode � NOT auto-capture
            com.razorpay.Order rzpOrder = razorpayClient.orders.create(orderRequest);
            return rzpOrder.get("id");
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            throw new BusinessException("PAYMENT_ORDER_CREATION_FAILED",
                    "Failed to create payment order.", HttpStatus.BAD_GATEWAY);
        }
    }

    private void verifyRazorpaySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("razorpay_order_id", razorpayOrderId);
            params.put("razorpay_payment_id", razorpayPaymentId);
            params.put("razorpay_signature", signature);
            Utils.verifyPaymentSignature(new JSONObject(params), razorpayConfig.getKeySecret());
        } catch (RazorpayException e) {
            throw new BusinessException("PAYMENT_SIGNATURE_INVALID",
                    "Payment signature verification failed.", HttpStatus.BAD_REQUEST);
        }
    }

    private void verifyWebhookSignature(String payload, String signature) {
        try {
            Utils.verifyWebhookSignature(payload, signature, razorpayConfig.getWebhookSecret());
        } catch (RazorpayException e) {
            throw new BusinessException("WEBHOOK_SIGNATURE_INVALID",
                    "Webhook signature verification failed", HttpStatus.UNAUTHORIZED);
        }
    }

    /** Computes how much of this payment was covered by wallet vs Razorpay. */
    private BigDecimal computeWalletPortion(Payment payment) {
        // TODO: store wallet portion on the Payment entity for precision.
        // For now, returns ZERO unless we can infer it.
        return BigDecimal.ZERO;
    }

    private PaymentVerifyResponse buildVerifyResponse(Payment payment, UUID userId) {
        OrderResponseDto orderDto = orderService.getOrderDetails(userId, payment.getOrderId());
        return PaymentVerifyResponse.builder()
                .success(true)
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .order(orderDto)
                .build();
    }

    private BigDecimal computeSubtotal(Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId());
            if (product == null) {
                throw new BusinessException("PRODUCT_NOT_FOUND",
                        "Product no longer available", HttpStatus.BAD_REQUEST);
            }
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subtotal;
    }

    private com.veggofresh.customer.entity.Order buildPendingOrder(
            UUID userId, CreatePaymentOrderRequest request, Cart cart,
            BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal estimatedTax, BigDecimal total) {

        com.veggofresh.customer.entity.Address resolvedAddress =
                addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                        .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND",
                                "Invalid address selected", HttpStatus.BAD_REQUEST));

        com.veggofresh.customer.entity.Order order = new com.veggofresh.customer.entity.Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setDeliveryAddress(resolvedAddress.getAddressLine1() + ", " +
                resolvedAddress.getCity() + ", " +
                resolvedAddress.getState() + " - " +
                resolvedAddress.getPostalCode());
        order.setLatitude(resolvedAddress.getLatitude());
        order.setLongitude(resolvedAddress.getLongitude());
        order.setOrderNumber("#VG-" + (100000 + new Random().nextInt(900000)));
        order.setDeliveryFee(deliveryFee);
        order.setEstimatedTax(estimatedTax);
        order.setPromoDiscount(BigDecimal.ZERO);
        order.setTotalAmount(total);

        List<com.veggofresh.customer.entity.OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId());
            com.veggofresh.customer.entity.OrderItem orderItem = new com.veggofresh.customer.entity.OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        if (request.getDeliverySlotId() != null) {
            DeliverySlot slot = deliverySlotRepository.findById(request.getDeliverySlotId())
                    .orElseThrow(() -> new BusinessException("DELIVERY_SLOT_NOT_FOUND",
                            "Selected delivery slot is invalid", HttpStatus.BAD_REQUEST));
            order.setDeliveryTimeSlot(slot.getLabel());
            order.setScheduledDate(request.getScheduledDate() != null
                    ? LocalDate.parse(request.getScheduledDate())
                    : slot.getDate());
        }

        return order;
    }

    private String extractPaymentId(JSONObject body) {
        try { return body.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity").getString("id"); }
        catch (Exception e) { return null; }
    }

    private String extractOrderId(JSONObject body) {
        try { return body.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity").getString("order_id"); }
        catch (Exception e) { return null; }
    }

    private String extractFailureReason(JSONObject body) {
        try { return body.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity").optString("error_description", "Payment failed"); }
        catch (Exception e) { return "Payment failed"; }
    }
}
