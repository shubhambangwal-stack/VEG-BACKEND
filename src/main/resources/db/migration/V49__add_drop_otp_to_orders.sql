-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V49
-- Customer Module: Add drop_otp to orders
--
-- Real drop-off OTP, pushed in by Delivery's CustomerOrderService.setDropOtpAvailable()
-- the moment the delivery partner marks "arrived at drop". Null before that -- the
-- customer-facing track screen (GET /api/customer/orders/{id}/track) only reveals it
-- once non-null, i.e. once the delivery partner is actually at the door. Replaces the
-- old fake CustomerOrderService.getDeliveryOtp() hashCode-derived stand-in (now dead
-- code, no longer called by anything).
-- ============================================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS drop_otp VARCHAR(10) NULL;
