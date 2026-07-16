package com.veggofresh.admin.controller;

import com.veggofresh.auth.repository.UserRepository;
import com.veggofresh.customer.repository.CustomerProfileRepository;
import com.veggofresh.platform.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalCustomers = customerProfileRepository.count();

        Map<String, Object> stats = Map.of(
                "totalUsers", totalUsers,
                "totalCustomers", totalCustomers,
                "systemStatus", "healthy",
                "environment", "production"
        );

        return ResponseEntity.ok(ApiResponse.success(stats, "Admin dashboard stats retrieved successfully"));
    }
}
