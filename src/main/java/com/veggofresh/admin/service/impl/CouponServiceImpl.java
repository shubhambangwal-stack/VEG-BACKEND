package com.veggofresh.admin.service.impl;

import com.veggofresh.admin.service.CouponService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CouponServiceImpl implements CouponService {

    @Override
    public BigDecimal validateCoupon(String code, BigDecimal orderTotal) {
        if (code == null || code.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Match mock/example coupons: SAVE10 gives 10% off, SAVE20 gives 20% off
        String upperCode = code.toUpperCase();
        if (upperCode.equals("SAVE10")) {
            return orderTotal.multiply(BigDecimal.valueOf(0.10));
        } else if (upperCode.equals("SAVE20")) {
            return orderTotal.multiply(BigDecimal.valueOf(0.20));
        } else if (upperCode.equals("FRESH50")) {
            return BigDecimal.valueOf(50.00).min(orderTotal);
        }
        
        return BigDecimal.ZERO;
    }
}
