package com.veggofresh.delivery.dto.response;

/**
 * Tells the frontend exactly which screen to route to next, so routing logic
 * lives server-side once instead of being re-derived (and potentially
 * duplicated/drifted) in the app.
 */
public enum OnboardingNextAction {
    BASIC_INFO,
    VERIFICATION_STEP_1,
    VERIFICATION_STEP_2,
    VERIFICATION_STEP_3,
    UNDER_REVIEW,
    REJECTED,
    DASHBOARD
}
