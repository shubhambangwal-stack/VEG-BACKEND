package com.veggofresh.platform.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Standard API response envelope for all VegGo Fresh REST endpoints.
 *
 * <p>Every controller response MUST be wrapped in this class. This ensures a consistent
 * JSON shape across all modules:
 * <pre>
 * {
 *   "success":   true | false,
 *   "message":   "Human-readable result message",
 *   "data":      { ... } | null,
 *   "timestamp": "2025-01-01T00:00:00Z",
 *   "errorCode": "VENDOR_NOT_FOUND"  // only present on errors
 * }
 * </pre>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Success response
 * return ResponseEntity.ok(ApiResponse.success(vendor, "Vendor retrieved successfully"));
 *
 * // Error response (typically thrown via BusinessException, not manually)
 * return ResponseEntity.badRequest().body(ApiResponse.error("Vendor not found", "VENDOR_NOT_FOUND"));
 * }</pre>
 *
 * @param <T> the type of the {@code data} payload
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Whether the request succeeded. */
    private final boolean success;

    /** Human-readable description of the outcome. */
    private final String message;

    /** Response payload; {@code null} on error responses. */
    private final T data;

    /** ISO-8601 UTC timestamp of when this response was generated. */
    private final Instant timestamp;

    /**
     * Machine-readable error code; only present when {@code success} is {@code false}.
     * Follows the convention: {@code MODULE_ENTITY_REASON}, e.g. {@code AUTH_TOKEN_EXPIRED}.
     */
    private final String errorCode;

    private ApiResponse(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Static factory helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a successful response with a data payload and a descriptive message.
     *
     * @param data    the response payload (may be {@code null} for operations that return nothing)
     * @param message a human-readable success message
     * @param <T>     the type of the data payload
     * @return a new {@link ApiResponse} with {@code success = true}
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    /**
     * Creates a successful response with no payload (e.g., for void operations like DELETE).
     *
     * @param message a human-readable success message
     * @param <T>     the data type (Void in practice)
     * @return a new {@link ApiResponse} with {@code success = true} and {@code data = null}
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * Creates an error response with a message and a machine-readable error code.
     *
     * @param message   a human-readable error description
     * @param errorCode a module-scoped error code, e.g. {@code AUTH_TOKEN_EXPIRED}
     * @param <T>       the data type (always {@code null} on errors)
     * @return a new {@link ApiResponse} with {@code success = false}
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }
}
