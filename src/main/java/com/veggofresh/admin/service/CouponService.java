package com.veggofresh.admin.service;

import java.math.BigDecimal;

public interface CouponService {
    /**
     * Validates a promo code against order total.
     * Returns discount amount if valid, otherwise returns 0.
     */
    BigDecimal validateCoupon(String code, BigDecimal orderTotal);
}
