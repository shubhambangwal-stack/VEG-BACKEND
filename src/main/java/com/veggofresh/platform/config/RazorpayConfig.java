package com.veggofresh.platform.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Razorpay configuration properties and client bean.
 *
 * <p>Properties are bound from the {@code veggofresh.razorpay.*} block in
 * {@code application.yml}. Override via environment variables in all environments:
 * <ul>
 *   <li>{@code RAZORPAY_KEY_ID}     — API key ID from Razorpay dashboard</li>
 *   <li>{@code RAZORPAY_KEY_SECRET} — API key secret (NOT the webhook secret)</li>
 *   <li>{@code RAZORPAY_WEBHOOK_SECRET} — separate secret set in Razorpay webhook config</li>
 * </ul>
 *
 * <p><b>Never commit real key values</b> — use env vars or a secrets manager.
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "veggofresh.razorpay")
@Getter
@Setter
public class RazorpayConfig {

    /** API key ID (starts with {@code rzp_test_} in test mode, {@code rzp_live_} in prod). */
    private String keyId;

    /** API key secret. Used for signature verification in {@code verifyPayment()}. */
    private String keySecret;

    /**
     * Webhook secret. Set separately in the Razorpay dashboard under Webhooks.
     * Used only for {@code X-Razorpay-Signature} webhook verification — different
     * from keySecret.
     */
    private String webhookSecret;

    /** ISO 4217 currency code. Defaults to {@code INR}. */
    private String currency = "INR";

    /**
     * Razorpay client bean. Initialized once at startup.
     *
     * <p>If keys are placeholders (not yet configured), logs a warning but does NOT
     * fail startup — this allows the app to boot in CI/CD or test environments
     * without real Razorpay credentials. Calls to Razorpay API will fail at
     * runtime if placeholders are used.
     */
    @Bean
    public RazorpayClient razorpayClient() {
        if (keyId.contains("PLACEHOLDER") || keySecret.contains("PLACEHOLDER")) {
            log.warn("⚠️  Razorpay keys are not configured — payment endpoints will fail at runtime. " +
                    "Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables.");
        }
        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            throw new IllegalStateException("Failed to initialize RazorpayClient: " + e.getMessage(), e);
        }
    }
}
