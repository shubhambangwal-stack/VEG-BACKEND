package com.veggofresh.delivery.entity;

/**
 * Thresholds are arbitrary placeholders based on total completed deliveries -- no
 * tier design was specified anywhere. Flag if a different rule (e.g. rating-weighted,
 * time-windowed) is wanted instead.
 */
public enum PartnerTier {
    BRONZE,
    SILVER,
    GOLD;

    public static PartnerTier fromDeliveryCount(long totalDeliveries) {
        if (totalDeliveries >= 200) return GOLD;
        if (totalDeliveries >= 50) return SILVER;
        return BRONZE;
    }
}
