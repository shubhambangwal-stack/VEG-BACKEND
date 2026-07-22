package com.veggofresh.platform.security;

import lombok.RequiredArgsConstructor;
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

/**
 * Spring Security configuration for the VegGo Fresh platform.
 *
 * <h3>Security model</h3>
 * <ul>
 *   <li><b>Stateless</b> — no HTTP sessions; all authentication is token-based (JWT).</li>
 *   <li><b>Method security</b> — {@code @EnableMethodSecurity} is active so later modules
 *       can annotate service/controller methods with {@code @PreAuthorize("hasRole('VENDOR')")} etc.</li>
 *   <li><b>Public routes</b> — {@code /api/auth/**} and {@code /api/public/**} are open
 *       to unauthenticated callers. {@code /swagger-ui/**} and {@code /v3/api-docs/**}
 *       are also permitted for developer convenience (lock these down in production via
 *       the {@code prod} profile if required).</li>
 *   <li><b>Protected routes</b> — all other routes require a valid Bearer token in the
 *       {@code Authorization} header.</li>
 * </ul>
 *
 * <h3>Adding role-based rules</h3>
 * <pre>{@code
 * // In a module's @Service or @RestController:
 * @PreAuthorize("hasRole('VENDOR')")
 * public VendorDto getVendorProfile(...) { ... }
 *
 * @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
 * public void approveVendor(...) { ... }
 * }</pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // Enables @PreAuthorize, @PostAuthorize, @Secured on methods
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
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless JWT APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session — Spring Security will never create an HttpSession
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .anyRequest().authenticated()
            )

            // Add filters before the standard username/password filter
            // DeviceIdFilter runs first, then JWT auth filter
            .addFilterBefore(deviceIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder bean. Used by the auth module for hashing user passwords.
     * Strength 12 is the recommended balance of security and performance.
     *
     * @return BCrypt encoder with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
