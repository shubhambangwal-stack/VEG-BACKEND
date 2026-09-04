package com.veggofresh.platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the VegGo Fresh platform.
 *
 * <h3>Security model</h3>
 * <ul>
 * <li><b>Stateless</b> — no HTTP sessions; all authentication is token-based
 * (JWT).</li>
 * <li><b>Method security</b> — {@code @EnableMethodSecurity} is active so later
 * modules
 * can annotate service/controller methods with
 * {@code @PreAuthorize("hasRole('VENDOR')")} etc.</li>
 * <li><b>Public routes</b> — {@code /api/auth/**} and {@code /api/public/**}
 * are open
 * to unauthenticated callers. {@code /swagger-ui/**} and
 * {@code /v3/api-docs/**}
 * are also permitted for developer convenience (lock these down in production
 * via
 * the {@code prod} profile if required).</li>
 * <li><b>Protected routes</b> — all other routes require a valid Bearer token
 * in the
 * {@code Authorization} header.</li>
 * <li><b>CORS</b> — configured via {@code veggofresh.cors.allowed-origins}
 * property.
 * Defaults to localhost for local dev. Set {@code CORS_ALLOWED_ORIGINS} env var
 * on the production server to include {@code http://veggofresh.in} and any
 * other
 * front-end origins.</li>
 * </ul>
 *
 * <h3>Adding role-based rules</h3>
 * 
 * <pre>{@code
 * // In a module's @Service or @RestController:
 * &#64;PreAuthorize("hasRole('VENDOR')")
 * public VendorDto getVendorProfile(...) { ... }
 *
 * &#64;PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
 * public void approveVendor(...) { ... }
 * }</pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize, @PostAuthorize, @Secured on methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DeviceIdFilter deviceIdFilter;

    /**
     * Public URL patterns that do not require a valid JWT.
     * These are also excluded from the {@link DeviceIdFilter}.
     */
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/api/vendor/auth/**",
            "/api/public/**",
            // WebSocket handshake is public at the HTTP-filter layer — real
            // auth happens inside WebSocketAuthInterceptor (JWT) because
            // SockJS clients cannot send an Authorization header.
            "/ws/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CORS — disabled so web server (Nginx/Apache) manages CORS ─────────
                .cors(AbstractHttpConfigurer::disable)

                // ── CSRF — disabled for stateless JWT APIs ────────────────────────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── Stateless session ─────────────────────────────────────────────────
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Authorization rules ───────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Preflight OPTIONS requests must always be allowed without auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated())

                // ── Filters ───────────────────────────────────────────────────────────
                // DeviceIdFilter runs first, then JWT auth filter
                .addFilterBefore(deviceIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder bean. Used by the auth module for hashing user
     * passwords.
     * Strength 12 is the recommended balance of security and performance.
     *
     * @return BCrypt encoder with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
