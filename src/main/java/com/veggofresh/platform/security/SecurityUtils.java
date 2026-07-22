package com.veggofresh.platform.security;

import com.veggofresh.platform.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility class for security operations.
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Retrieves the current authenticated user's ID from the SecurityContext.
     *
     * @return UUID of the authenticated user
     * @throws BusinessException if no valid user is authenticated
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("UNAUTHORIZED", "User is not authenticated", HttpStatus.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalStr) {
            try {
                return UUID.fromString(principalStr);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_TOKEN_PRINCIPAL", "Invalid user ID in authentication token", HttpStatus.UNAUTHORIZED);
            }
        }

        throw new BusinessException("UNAUTHORIZED", "User identity cannot be resolved from token", HttpStatus.UNAUTHORIZED);
    }
}
