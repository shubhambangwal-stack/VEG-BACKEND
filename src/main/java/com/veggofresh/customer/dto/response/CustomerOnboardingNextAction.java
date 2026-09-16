package com.veggofresh.customer.dto.response;

/**
 * Tells the frontend exactly which screen to route to next, so routing logic
 * lives server-side once instead of being re-derived (and potentially
 * duplicated/drifted) in the app. Mirrors Delivery/Vendor's
 * OnboardingNextAction pattern -- Customer has no KYC/verification steps,
 * so this is deliberately just two states.
 */
public enum CustomerOnboardingNextAction {
    BASIC_INFO,
    HOME
}
