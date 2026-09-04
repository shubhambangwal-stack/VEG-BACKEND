package com.veggofresh.customer.controller;

import com.veggofresh.customer.dto.request.CustomerBasicInfoRequestDto;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.service.CustomerProfileService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer Onboarding — the one-time, required step shown right after OTP verification.
 *
 * <pre>
 * PUT /api/customer/onboarding/basic-info   — submit fullName (required)
 * </pre>
 *
 * <p>Client-side flow (mirrors Delivery/Vendor exactly):
 * <ol>
 *   <li>Phone → OTP verified → {@code User} exists, {@code CustomerProfile} auto-created empty.</li>
 *   <li>Client calls {@code GET /api/customer/profile}. If {@code fullName} is null/blank,
 *       show the basic-info screen; otherwise go straight to home.</li>
 *   <li>User submits their name here. On success, go to home.</li>
 *   <li>If the app is closed before submitting and reopened, step 2 repeats — the
 *       basic-info screen keeps showing until {@code fullName} is actually saved.</li>
 *   <li>After that, home is always the landing screen; name/email/avatar remain editable
 *       any time via {@code PUT /api/customer/profile}.</li>
 * </ol>
 *
 * <p>Deliberately just this one field — email and avatar are NOT part of onboarding and
 * stay fully optional, set later from the profile screen if the customer chooses to.
 */
@RestController
@RequestMapping("/api/customer/onboarding")
@RequiredArgsConstructor
public class CustomerOnboardingController {

    private final CustomerProfileService customerProfileService;

    @PutMapping("/basic-info")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> submitBasicInfo(
            @Valid @RequestBody CustomerBasicInfoRequestDto request) {
        CustomerProfileResponseDto profile = customerProfileService.submitBasicInfo(
                SecurityUtils.getCurrentUserId(), request.getFullName());
        return ResponseEntity.ok(ApiResponse.success(profile, "Basic info saved successfully"));
    }
}
