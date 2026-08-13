package com.veggofresh.payment.service.impl;

import com.razorpay.Order;
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
import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.payment.dto.request.CreatePaymentOrderRequest;
import com.veggofresh.payment.dto.request.VerifyPaymentRequest;
import com.veggofresh.payment.dto.response.PaymentOrderResponse;
import com.veggofresh.payment.dto.response.PaymentVerifyResponse;
import com.veggofresh.payment.entity.Payment;
import com.veggofresh.payment.entity.PaymentStatus;
import com.veggofresh.payment.repository.PaymentRepository;
import com.veggofresh.payment.service.PaymentService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Razorpay payment service implementation.
 *
 * <p><b>Key design constraints:</b>
 * <ul>
 *   <li>Signature verification uses Razorpay SDK's {@code Utils.verifyPaymentSignature} and
 *       {@code Utils.verifyWebhookSignature} — do NOT reimplement the HMAC logic.</li>
 *   <li>Amounts are stored in INR (BigDecimal) throughout the app. Razorpay requires paise
 *       (integer × 100). Conversion happens exactly at the point of API call.</li>
 *   <li>Webhook handler is idempotent: if payment is already CAPTURED, duplicate events are ignored.</li>
 *   <li>Cart is cleared ONLY after payment capture — not at order creation time.</li>
 * </ul>
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

    // ── createPaymentOrder ────────────────────────────────────

    @Override
    public PaymentOrderResponse createPaymentOrder(UUID userId, CreatePaymentOrderRequest request) {
        // 1. Validate cart is non-empty
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND",
                        "Cart not found — add items before initiating payment", HttpStatus.NOT_FOUND));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("CART_EMPTY",
                    "Cannot initiate payment for an empty cart", HttpStatus.BAD_REQUEST);
        }

        // 2. Compute order total (subtotal + delivery fee + tax)
        BigDecimal subtotal = computeSubtotal(cart);
        BigDecimal deliveryFee = BigDecimal.valueOf(5.00);
        BigDecimal estimatedTax = subtotal.multiply(BigDecimal.valueOf(0.05));
        BigDecimal total = subtotal.add(deliveryFee).add(estimatedTax);

        // 3. Create a pending VegGoFresh order (PAYMENT_PENDING)
        com.veggofresh.customer.entity.Order pendingOrder = buildPendingOrder(userId, request, cart, subtotal, deliveryFee, estimatedTax, total);
        com.veggofresh.customer.entity.Order savedOrder = orderRepository.save(pendingOrder);

        // 4. Create Razorpay order (amount in paise)
        long amountInPaise = total.multiply(BigDecimal.valueOf(100)).longValue();
        String razorpayOrderId = createRazorpayOrder(amountInPaise, savedOrder.getId().toString());

        // 5. Persist Payment(CREATED)
        Payment payment = new Payment();
        payment.setOrderId(savedOrder.getId());
        payment.setUserId(userId);
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAmount(total);
        payment.setCurrency(razorpayConfig.getCurrency());
        payment.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(payment);

        // 6. Update order with razorpayOrderId for tracking
        savedOrder.setRazorpayOrderId(razorpayOrderId);
        savedOrder.setPaymentStatus(PaymentStatus.CREATED.name());
        orderRepository.save(savedOrder);

        log.info("Payment order created: razorpayOrderId={}, orderId={}, amount={}",
                razorpayOrderId, savedOrder.getId(), total);

        return PaymentOrderResponse.builder()
                .veggoOrderId(savedOrder.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(total)
                .amountInPaise(amountInPaise)
                .currency(razorpayConfig.getCurrency())
                .keyId(razorpayConfig.getKeyId())
                .build();
    }

    // ── verifyPayment ─────────────────────────────────────────

    @Override
    public PaymentVerifyResponse verifyPayment(UUID userId, VerifyPaymentRequest request) {
        // 1. Load the Payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                        "No payment found for Razorpay order ID: " + request.getRazorpayOrderId(),
                        HttpStatus.NOT_FOUND));

        // 2. Guard: already captured (idempotency — don't double process)
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            log.warn("verifyPayment called for already-captured payment: razorpayOrderId={}",
                    request.getRazorpayOrderId());
            com.veggofresh.customer.entity.Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found"));
            return PaymentVerifyResponse.builder()
                    .success(true)
                    .razorpayPaymentId(payment.getRazorpayPaymentId())
                    .order(orderService.getOrderDetails(userId, payment.getOrderId()))
                    .build();
        }

        // 3. HMAC-SHA256 signature verification
        verifyRazorpaySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        // 4. Mark Payment as CAPTURED
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        paymentRepository.save(payment);

        // 5. Move VegGoFresh order from PAYMENT_PENDING → PLACED and clear cart
        com.veggofresh.customer.entity.Order order = orderRepository.findById(request.getVeggoOrderId())
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND",
                        "Pending order not found: " + request.getVeggoOrderId(), HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException("ORDER_INVALID_STATE",
                    "Order is not in PAYMENT_PENDING state — current status: " + order.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.CAPTURED.name());
        order.setPaymentMethodId(request.getRazorpayPaymentId());
        orderRepository.save(order);

        // 6. Clear cart — only now, after successful payment
        cartService.clearCart(userId);

        log.info("Payment verified and order placed: paymentId={}, orderId={}",
                request.getRazorpayPaymentId(), order.getId());

        OrderResponseDto orderDto = orderService.getOrderDetails(userId, order.getId());
        return PaymentVerifyResponse.builder()
                .success(true)
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .order(orderDto)
                .build();
    }

    // ── handleWebhook ─────────────────────────────────────────

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        // 1. Verify webhook signature using the webhook secret (different from key-secret)
        verifyWebhookSignature(payload, signature);

        // 2. Parse event type
        JSONObject body = new JSONObject(payload);
        String event = body.optString("event", "");
        log.info("Razorpay webhook received: event={}", event);

        switch (event) {
            case "payment.captured" -> handlePaymentCaptured(body);
            case "payment.failed"   -> handlePaymentFailed(body);
            default -> log.debug("Razorpay webhook event ignored (not handled): {}", event);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrder(UUID orderId, UUID userId) {
        // Security: verify the order belongs to this user before returning payment data
        orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND",
                        "Order not found for user", HttpStatus.NOT_FOUND));
        return paymentRepository.findByOrderId(orderId);
    }

    // ── Private helpers ───────────────────────────────────────

    private String createRazorpayOrder(long amountInPaise, String receipt) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", razorpayConfig.getCurrency());
            orderRequest.put("receipt", receipt);

            Order rzpOrder = razorpayClient.orders.create(orderRequest);
            return rzpOrder.get("id");
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            throw new BusinessException("PAYMENT_ORDER_CREATION_FAILED",
                    "Failed to create payment order. Please try again.", HttpStatus.BAD_GATEWAY);
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
            log.warn("Payment signature verification failed for orderId={}: {}", razorpayOrderId, e.getMessage());
            throw new BusinessException("PAYMENT_SIGNATURE_INVALID",
                    "Payment signature verification failed. This may indicate a tampered request.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void verifyWebhookSignature(String payload, String signature) {
        try {
            Utils.verifyWebhookSignature(payload, signature, razorpayConfig.getWebhookSecret());
        } catch (RazorpayException e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            throw new BusinessException("WEBHOOK_SIGNATURE_INVALID",
                    "Webhook signature verification failed", HttpStatus.UNAUTHORIZED);
        }
    }

    private void handlePaymentCaptured(JSONObject body) {
        String razorpayPaymentId = extractPaymentId(body);
        String razorpayOrderId = extractOrderId(body);

        paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
            // Idempotency: skip if already captured
            if (payment.getStatus() == PaymentStatus.CAPTURED) {
                log.info("Webhook payment.captured: already captured, skipping. razorpayOrderId={}", razorpayOrderId);
                return;
            }

            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setWebhookEvent("payment.captured");
            paymentRepository.save(payment);

            // Also promote the VegGoFresh order if still PAYMENT_PENDING
            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                    order.setStatus(OrderStatus.PLACED);
                    order.setPaymentStatus(PaymentStatus.CAPTURED.name());
                    order.setPaymentMethodId(razorpayPaymentId);
                    orderRepository.save(order);
                    // Clear cart via CartRepository directly (avoid service circular dep)
                    cartRepository.findByUserId(order.getUserId()).ifPresent(cart -> {
                        cart.getItems().clear();
                        cartRepository.save(cart);
                    });
                    log.info("Webhook payment.captured: order promoted to PLACED. orderId={}", order.getId());
                }
            });
        });
    }

    private void handlePaymentFailed(JSONObject body) {
        String razorpayOrderId = extractOrderId(body);
        String reason = body.optJSONObject("payload") != null
                ? body.optJSONObject("payload").optJSONObject("payment") != null
                    ? body.optJSONObject("payload").optJSONObject("payment")
                          .optJSONObject("entity") != null
                        ? body.optJSONObject("payload").optJSONObject("payment")
                              .optJSONObject("entity").optString("error_description", "Payment failed")
                        : "Payment failed"
                    : "Payment failed"
                : "Payment failed";

        paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.FAILED) {
                log.info("Webhook payment.failed: already marked failed, skipping. razorpayOrderId={}", razorpayOrderId);
                return;
            }
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            payment.setWebhookEvent("payment.failed");
            paymentRepository.save(payment);

            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.FAILED.name());
                orderRepository.save(order);
            });

            log.info("Webhook payment.failed: payment marked failed. razorpayOrderId={}, reason={}", razorpayOrderId, reason);
        });
    }

    private String extractPaymentId(JSONObject body) {
        try {
            return body.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity")
                    .getString("id");
        } catch (Exception e) {
            log.warn("Could not extract payment ID from webhook payload");
            return null;
        }
    }

    private String extractOrderId(JSONObject body) {
        try {
            return body.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity")
                    .getString("order_id");
        } catch (Exception e) {
            log.warn("Could not extract order ID from webhook payload");
            return null;
        }
    }

    private BigDecimal computeSubtotal(Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId());
            if (product == null) {
                throw new BusinessException("PRODUCT_NOT_FOUND",
                        "One or more products in your cart are no longer available", HttpStatus.BAD_REQUEST);
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

        // Build order items from cart
        List<com.veggofresh.customer.entity.OrderItem> orderItems = new java.util.ArrayList<>();
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

        // Delivery slot
        if (request.getDeliverySlotId() != null) {
            DeliverySlot slot = deliverySlotRepository.findById(request.getDeliverySlotId())
                    .orElseThrow(() -> new BusinessException("DELIVERY_SLOT_NOT_FOUND",
                            "Selected delivery slot is invalid", HttpStatus.BAD_REQUEST));
            order.setDeliveryTimeSlot(slot.getLabel());
            if (request.getScheduledDate() != null) {
                order.setScheduledDate(LocalDate.parse(request.getScheduledDate()));
            } else {
                order.setScheduledDate(slot.getDate());
            }
        }

        return order;
    }
}
