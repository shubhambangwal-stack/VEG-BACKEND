package com.veggofresh.platform.exception;

import com.veggofresh.platform.common.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralized exception handler for all VegGo Fresh REST endpoints.
 *
 * <p>All exceptions handled here are converted into the standard {@link ApiResponse} error
 * shape so clients always receive a consistent JSON structure regardless of the error type.
 *
 * <h3>Handled exceptions</h3>
 * <table>
 *   <tr><th>Exception</th><th>HTTP Status</th><th>Notes</th></tr>
 *   <tr><td>MethodArgumentNotValidException</td><td>400</td><td>Bean validation failures; field errors concatenated</td></tr>
 *   <tr><td>BusinessException</td><td>varies</td><td>Status determined by the exception itself</td></tr>
 *   <tr><td>EntityNotFoundException</td><td>404</td><td>JPA entity not found</td></tr>
 *   <tr><td>AccessDeniedException</td><td>403</td><td>Spring Security authorization failure</td></tr>
 *   <tr><td>Exception</td><td>500</td><td>Catch-all; details hidden from client in prod</td></tr>
 * </table>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // 400 — Validation errors (Bean Validation / @Valid)
    // -------------------------------------------------------------------------

    /**
     * Handles {@code @Valid} and {@code @Validated} constraint violations.
     * All field-level errors are concatenated into a single message string.
     *
     * @param ex the validation exception thrown by Spring MVC
     * @return 400 Bad Request with a comma-separated list of field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errors, "VALIDATION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // Business rule violations
    // -------------------------------------------------------------------------

    /**
     * Handles domain-level business exceptions thrown by any service.
     * The HTTP status is determined by the exception itself (defaults to 400).
     *
     * @param ex the business exception
     * @return response with the exception's HTTP status and error code
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    // -------------------------------------------------------------------------
    // 404 — Entity not found
    // -------------------------------------------------------------------------

    /**
     * Handles JPA {@link EntityNotFoundException} (e.g., from {@code em.getReference()}).
     *
     * @param ex the entity-not-found exception
     * @return 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(
            EntityNotFoundException ex) {

        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "ENTITY_NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // 403 — Access denied
    // -------------------------------------------------------------------------

    /**
     * Handles Spring Security {@link AccessDeniedException} raised when a user attempts
     * to access a resource they are not authorized for (e.g., wrong role).
     *
     * @param ex the access denied exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to perform this action.",
                        "ACCESS_DENIED"));
    }

    // -------------------------------------------------------------------------
    // 500 — Catch-all
    // -------------------------------------------------------------------------

    /**
     * Catch-all handler for any unhandled exception.
     * The real error is logged server-side; the client only receives a generic message
     * to avoid leaking implementation details.
     *
     * @param ex the unhandled exception
     * @return 500 Internal Server Error with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later.",
                        "INTERNAL_SERVER_ERROR"));
    }
}
