package com.veggofresh.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds to the {@code razorpay.*} properties. Add these to
 * {@code application.yml} / {@code application-{profile}.yml} (not present
 * in the uploaded zips, so not touched here):
 *
 * <pre>{@code
 * razorpay:
 *   key-id: ${RAZORPAY_KEY_ID}
 *   key-secret: ${RAZORPAY_KEY_SECRET}
 *   webhook-secret: ${RAZORPAY_WEBHOOK_SECRET}
 *   payouts-enabled: false
 * }</pre>
 *
 * {@code keyId} is safe to expose to the frontend (it's the publishable
 * key Checkout.js needs). {@code keySecret} and {@code webhookSecret} are
 * server-side only -- never returned in any DTO.
 */
@Component
@ConfigurationProperties(prefix = "veggofresh.razorpay")
@Getter
@Setter
public class RazorpayProperties {

    private String keyId;
    private String keySecret;
    private String webhookSecret;

    /** Gates the payout client (Phase 3) -- vendor/delivery payouts require Razorpay Route/Payout account activation (KYC), which is a manual Razorpay dashboard step, not something this app can turn on by itself. */
    private boolean payoutsEnabled = false;

    /** Platform commission percentage (e.g. 10 = 10%). Used in settlement splits. */
    private int platformCommissionPercent = 10;

    /** RazorpayX Virtual Business Account Number for payouts. */
    private String accountNumber = "2323230073112811";

    private String ordersApiBaseUrl = "https://api.razorpay.com/v1/orders";
    private String paymentsApiBaseUrl = "https://api.razorpay.com/v1/payments";
    private String contactsApiBaseUrl = "https://api.razorpay.com/v1/contacts";
    private String fundAccountsApiBaseUrl = "https://api.razorpay.com/v1/fund_accounts";
    private String payoutsApiBaseUrl = "https://api.razorpay.com/v1/payouts";
}
