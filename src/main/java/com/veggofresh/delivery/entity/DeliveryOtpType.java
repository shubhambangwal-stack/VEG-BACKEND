package com.veggofresh.delivery.entity;

/**
 * NEW (this round). Distinguishes the vendor-issued pickup OTP (delivery partner enters
 * it at the store, confirming handoff from vendor) from the existing customer-facing
 * drop OTP (delivery partner enters it at the customer's door, confirming drop-off).
 * One DeliveryOtp row per (assignmentId, type) pair -- an assignment can have both.
 */
public enum DeliveryOtpType {
    PICKUP,
    DROP
}
