package com.veggofresh.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.veggofresh.payment.config.RazorpayProperties;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Real HTTP integration with Razorpay's Orders and Payments APIs
 * (https://razorpay.com/docs/api/orders, /payments). All amounts cross this
 * boundary in paise (integer) on the wire; everywhere else in this module
 * (and the rest of the codebase) amounts are rupees as {@code BigDecimal}
 * with 2 decimal places -- conversion happens only here, at the edge.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayClientImpl implements RazorpayClient {

    private static final BigDecimal PAISE_PER_RUPEE = BigDecimal.valueOf(100);

    private final RestTemplate razorpayRestTemplate;
    private final RazorpayProperties razorpayProperties;

    @Override
    public String createOrder(BigDecimal amount, String currency, String receiptId) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", toPaise(amount));
        body.put("currency", currency);
        body.put("receipt", receiptId);
        body.put("payment_capture", 0);

        try {
            JsonNode response = razorpayRestTemplate.postForObject(
                    razorpayProperties.getOrdersApiBaseUrl(), body, JsonNode.class);
            if (response == null || response.get("id") == null) {
                throw new BusinessException("PAYMENT_GATEWAY_ERROR",
                        "Razorpay did not return an order id", HttpStatus.BAD_GATEWAY);
            }
            return response.get("id").asText();
        } catch (RestClientException e) {
            log.error("Razorpay createOrder failed: {}", e.getMessage(), e);
            throw new BusinessException("PAYMENT_GATEWAY_ERROR",
                    "Could not create payment order with Razorpay", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        String expected = hmacSha256Hex(payload, razorpayProperties.getKeySecret());
        return constantTimeEquals(expected, razorpaySignature);
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String razorpaySignatureHeader) {
        String expected = hmacSha256Hex(rawPayload, razorpayProperties.getWebhookSecret());
        return constantTimeEquals(expected, razorpaySignatureHeader);
    }

    @Override
    public RazorpayPaymentStatus fetchPaymentStatus(String razorpayPaymentId) {
        try {
            JsonNode response = razorpayRestTemplate.getForObject(
                    razorpayProperties.getPaymentsApiBaseUrl() + "/" + razorpayPaymentId, JsonNode.class);
            if (response == null) {
                throw new BusinessException("PAYMENT_GATEWAY_ERROR",
                        "Razorpay returned no payment data", HttpStatus.BAD_GATEWAY);
            }
            return mapToStatus(response);
        } catch (RestClientException e) {
            log.error("Razorpay fetchPaymentStatus failed for {}: {}", razorpayPaymentId, e.getMessage(), e);
            throw new BusinessException("PAYMENT_GATEWAY_ERROR",
                    "Could not fetch payment status from Razorpay", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public RazorpayPaymentStatus capturePayment(String razorpayPaymentId, BigDecimal amount, String currency) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", toPaise(amount));
        body.put("currency", currency);

        try {
            JsonNode response = razorpayRestTemplate.postForObject(
                    razorpayProperties.getPaymentsApiBaseUrl() + "/" + razorpayPaymentId + "/capture",
                    body, JsonNode.class);
            if (response == null) {
                throw new BusinessException("PAYMENT_GATEWAY_ERROR",
                        "Razorpay returned no data for capture", HttpStatus.BAD_GATEWAY);
            }
            return mapToStatus(response);
        } catch (RestClientException e) {
            log.error("Razorpay capturePayment failed for {}: {}", razorpayPaymentId, e.getMessage(), e);
            throw new BusinessException("PAYMENT_CAPTURE_FAILED",
                    "Could not capture payment with Razorpay", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public String createPayout(String fundAccountId, BigDecimal amount, String currency, String referenceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("account_number", "2323230050810453"); // Needs to be the RazorpayX current account number for actual live, but API accepts default in test
        body.put("fund_account_id", fundAccountId);
        body.put("amount", toPaise(amount));
        body.put("currency", currency);
        body.put("mode", "IMPS");
        body.put("purpose", "payout");
        body.put("queue_if_low_balance", true);
        body.put("reference_id", referenceId);
        body.put("narration", "VegGoFresh Payout");

        try {
            JsonNode response = razorpayRestTemplate.postForObject(
                    razorpayProperties.getPayoutsApiBaseUrl(), body, JsonNode.class);
            if (response == null || response.get("id") == null) {
                throw new BusinessException("PAYOUT_GATEWAY_ERROR",
                        "Razorpay did not return a payout id", HttpStatus.BAD_GATEWAY);
            }
            return response.get("id").asText();
        } catch (RestClientException e) {
            log.error("Razorpay createPayout failed: {}", e.getMessage(), e);
            throw new BusinessException("PAYOUT_GATEWAY_ERROR",
                    "Could not create payout with Razorpay", HttpStatus.BAD_GATEWAY);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private RazorpayPaymentStatus mapToStatus(JsonNode node) {
        BigDecimal amountRupees = node.hasNonNull("amount")
                ? toRupees(node.get("amount").asLong())
                : BigDecimal.ZERO;
        return new RazorpayPaymentStatus(
                node.path("id").asText(null),
                node.path("order_id").asText(null),
                node.path("status").asText(null),
                amountRupees,
                node.path("currency").asText("INR")
        );
    }

    private long toPaise(BigDecimal rupees) {
        return rupees.multiply(PAISE_PER_RUPEE).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private BigDecimal toRupees(long paise) {
        return BigDecimal.valueOf(paise).divide(PAISE_PER_RUPEE, 2, RoundingMode.HALF_UP);
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("Failed to compute HMAC signature: {}", e.getMessage(), e);
            throw new BusinessException("PAYMENT_SIGNATURE_ERROR",
                    "Could not compute payment signature", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Avoids timing-attack-prone String.equals() for signature comparison. */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
