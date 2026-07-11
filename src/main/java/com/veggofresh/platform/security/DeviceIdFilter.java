package com.veggofresh.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggofresh.platform.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that enforces the presence of the {@code X-Device-Id} header on all
 * {@code /api/public/**} routes.
 *
 * <h3>Rationale</h3>
 * Public routes are accessible without authentication but still require a device identifier
 * for anonymous session tracking, rate limiting, fraud detection, and analytics.
 * This filter short-circuits the request with a {@code 400 Bad Request} if the header is absent.
 *
 * <h3>Header format</h3>
 * The {@code X-Device-Id} header should contain a stable, unique identifier for the device
 * (e.g., a UUID generated once on app install and stored in secure local storage).
 * <pre>
 *   X-Device-Id: 550e8400-e29b-41d4-a716-446655440000
 * </pre>
 *
 * <h3>Filter scope</h3>
 * Only {@code /api/public/**} paths are subject to this validation.
 * Auth routes ({@code /api/auth/**}) and protected routes are not affected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceIdFilter extends OncePerRequestFilter {

    private static final String DEVICE_ID_HEADER = "X-Device-Id";
    private static final String PUBLIC_API_PREFIX = "/api/public/";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith(PUBLIC_API_PREFIX)) {
            String deviceId = request.getHeader(DEVICE_ID_HEADER);

            if (!StringUtils.hasText(deviceId)) {
                log.warn("Missing X-Device-Id header on public route: {}", path);
                rejectRequest(response);
                return;
            }

            log.debug("X-Device-Id [{}] on public route [{}]", deviceId, path);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Writes a 400 Bad Request response using the standard {@link ApiResponse} error shape.
     */
    private void rejectRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(
                "The X-Device-Id header is required for public API access.",
                "DEVICE_ID_MISSING"
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
