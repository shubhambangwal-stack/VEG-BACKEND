package com.veggofresh.platform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Domain-level exception for all VegGo Fresh business rule violations.
 *
 * <p>Throw this exception when a business invariant is broken (e.g., vendor not found,
 * insufficient stock, order in non-cancellable state). It carries both a machine-readable
 * {@code errorCode} and an HTTP status, allowing {@link GlobalExceptionHandler} to map it
 * to the correct HTTP response automatically.
 *
 * <h3>Error code convention</h3>
 * Use the format {@code MODULE_ENTITY_REASON}, for example:
 * <ul>
 *   <li>{@code VENDOR_STORE_NOT_FOUND}</li>
 *   <li>{@code AUTH_TOKEN_EXPIRED}</li>
 *   <li>{@code ORDER_ALREADY_CANCELLED}</li>
 *   <li>{@code PAYMENT_INSUFFICIENT_BALANCE}</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // 404 — resource not found
 * throw new BusinessException("VENDOR_NOT_FOUND", "Vendor does not exist", HttpStatus.NOT_FOUND);
 *
 * // 400 — bad state (default HTTP status)
 * throw new BusinessException("ORDER_ALREADY_CANCELLED", "Order has already been cancelled");
 *
 * // 409 — conflict
 * throw new BusinessException("EMAIL_ALREADY_TAKEN", "Email is already registered", HttpStatus.CONFLICT);
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** Machine-readable error code in the format {@code MODULE_ENTITY_REASON}. */
    private final String errorCode;

    /** HTTP status to return in the response. Defaults to {@code 400 Bad Request}. */
    private final HttpStatus status;

    /**
     * Creates a {@code BusinessException} with a custom HTTP status.
     *
     * @param errorCode module-scoped error code
     * @param message   human-readable error description
     * @param status    HTTP status for the response
     */
    public BusinessException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    /**
     * Creates a {@code BusinessException} with default HTTP status {@code 400 Bad Request}.
     *
     * @param errorCode module-scoped error code
     * @param message   human-readable error description
     */
    public BusinessException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
