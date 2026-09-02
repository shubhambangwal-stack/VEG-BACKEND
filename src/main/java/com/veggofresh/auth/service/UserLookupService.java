package com.veggofresh.auth.service;

import com.veggofresh.auth.dto.UserSummaryDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public interface for cross-module user lookups.
 * Other modules must use this service instead of importing User entity directly.
 */
public interface UserLookupService {
    Optional<UserSummaryDto> findById(UUID userId);
    Optional<UserSummaryDto> findByPhone(String phone);
    Optional<UserSummaryDto> findByEmail(String email);

    /**
     * All user IDs holding the given role, e.g. "CUSTOMER".
     * Used by the Notification module for role-filtered admin broadcasts.
     * Accepts a String so callers stay decoupled from the auth {@code UserRole} enum.
     */
    List<UUID> findUserIdsByRole(String role);
}
