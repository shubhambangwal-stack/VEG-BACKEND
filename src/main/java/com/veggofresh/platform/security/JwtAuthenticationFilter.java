package com.veggofresh.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter — runs once per request.
 *
 * <p>Extracts the {@code Authorization: Bearer <token>} header, validates the token
 * using {@link JwtTokenProvider}, and populates the Spring Security {@link SecurityContextHolder}
 * so that downstream filters and controllers can access the authenticated principal.
 *
 * <p>If no token is present or validation fails, the request continues unauthenticated
 * (the Security configuration decides whether to reject it based on the URL).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            try {
                // Only accept access tokens in the Authorization header
                if (!jwtTokenProvider.isAccessToken(token)) {
                    log.warn("Refresh token presented as access token — rejected");
                    filterChain.doFilter(request, response);
                    return;
                }

                UUID userId = jwtTokenProvider.extractUserId(token);
                String role  = jwtTokenProvider.extractRole(token);
                String email = jwtTokenProvider.extractEmail(token);

                // Build granted authorities from the role claim (Spring Security convention: ROLE_ prefix)
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId.toString(),  // principal — downstream code can cast to String
                                null,               // credentials — not needed after auth
                                authorities
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated user [{}] with role [{}] for request [{}]",
                        email, role, request.getRequestURI());

            } catch (Exception e) {
                log.error("Failed to set security context from JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT string from the {@code Authorization} header,
     * stripping the {@code Bearer } prefix.
     *
     * @param request incoming HTTP request
     * @return raw token string, or {@code null} if header is absent or malformed
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
