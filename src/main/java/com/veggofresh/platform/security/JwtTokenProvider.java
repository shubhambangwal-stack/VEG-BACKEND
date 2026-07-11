package com.veggofresh.platform.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token provider for the VegGo Fresh platform.
 *
 * <p>Generates and validates HS256-signed JWT access and refresh tokens.
 * Token expiry and the signing secret are externalized to {@code application.yml}
 * and can be overridden via environment variables in production.
 *
 * <h3>Token structure</h3>
 * <ul>
 *   <li>{@code sub} — user ID (UUID string)</li>
 *   <li>{@code email} — user's email address</li>
 *   <li>{@code role} — user's role, e.g. {@code CUSTOMER}, {@code VENDOR}, {@code ADMIN}</li>
 *   <li>{@code type} — {@code ACCESS} or {@code REFRESH}</li>
 *   <li>{@code iat} — issued-at timestamp</li>
 *   <li>{@code exp} — expiration timestamp</li>
 *   <li>{@code jti} — unique token ID (UUID) for revocation support</li>
 * </ul>
 *
 * <h3>Configuration (application.yml)</h3>
 * <pre>
 * veggofresh:
 *   jwt:
 *     secret: ${JWT_SECRET}          # Base64-encoded 256-bit key
 *     access-token-expiry-ms: 900000  # 15 minutes
 *     refresh-token-expiry-ms: 604800000  # 7 days
 * </pre>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE  = "role";
    private static final String CLAIM_TYPE  = "type";
    private static final String TYPE_ACCESS  = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtTokenProvider(
            @Value("${veggofresh.jwt.secret}") String base64Secret,
            @Value("${veggofresh.jwt.access-token-expiry-ms:900000}") long accessTokenExpiryMs,
            @Value("${veggofresh.jwt.refresh-token-expiry-ms:604800000}") long refreshTokenExpiryMs) {

        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generates a short-lived JWT access token.
     *
     * @param userId user's UUID
     * @param email  user's email
     * @param role   user's platform role (e.g. CUSTOMER, VENDOR, ADMIN)
     * @return signed JWT access token string
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, TYPE_ACCESS, accessTokenExpiryMs);
    }

    /**
     * Generates a long-lived JWT refresh token.
     *
     * @param userId user's UUID
     * @param email  user's email
     * @param role   user's platform role
     * @return signed JWT refresh token string
     */
    public String generateRefreshToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, TYPE_REFRESH, refreshTokenExpiryMs);
    }

    private String buildToken(UUID userId, String email, String role,
                               String tokenType, long expiryMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())            // jti — unique per token
                .subject(userId.toString())                   // sub
                .claims(Map.of(
                        CLAIM_EMAIL, email,
                        CLAIM_ROLE,  role,
                        CLAIM_TYPE,  tokenType
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMs)))
                .signWith(signingKey)                         // HS256 by key type
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token validation & claims extraction
    // -------------------------------------------------------------------------

    /**
     * Validates the given token signature and expiry.
     *
     * @param token raw JWT string (without "Bearer " prefix)
     * @return {@code true} if the token is valid and not expired; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts the user ID ({@code sub} claim) from a valid token.
     *
     * @param token raw JWT string
     * @return user UUID
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    /**
     * Extracts the user's email from the token claims.
     *
     * @param token raw JWT string
     * @return email address
     */
    public String extractEmail(String token) {
        return parseClaims(token).get(CLAIM_EMAIL, String.class);
    }

    /**
     * Extracts the user's role from the token claims.
     *
     * @param token raw JWT string
     * @return role string, e.g. {@code CUSTOMER}
     */
    public String extractRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * Checks whether the given token is an access token (not a refresh token).
     *
     * @param token raw JWT string
     * @return {@code true} if {@code type} claim is {@code ACCESS}
     */
    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    /**
     * Returns the expiration {@link Instant} of the given token.
     *
     * @param token raw JWT string
     * @return expiration instant
     */
    public Instant extractExpiry(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
