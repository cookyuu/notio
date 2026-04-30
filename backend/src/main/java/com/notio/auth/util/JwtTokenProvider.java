package com.notio.auth.util;

import com.notio.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * JWT Access Token 생성
     */
    public String generateAccessToken(final String userId, final String email) {
        final OffsetDateTime now = OffsetDateTime.now();
        final Date issuedAt = Date.from(now.toInstant());
        final Date expiresAt = Date.from(now.plusSeconds(jwtProperties.getExpiration() / 1000).toInstant());

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * JWT Refresh Token 생성 (만료 시간: 7일)
     */
    public String generateRefreshToken(final String userId) {
        final OffsetDateTime now = OffsetDateTime.now();
        final Date issuedAt = Date.from(now.toInstant());
        final Date expiresAt = Date.from(now.plusDays(7).toInstant());

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public String getUserId(final String token) {
        return getClaims(token).getSubject();
    }

    /**
     * JWT 토큰에서 이메일 추출
     */
    public String getEmail(final String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * JWT 토큰 유효성 검증
     */
    public boolean validateToken(final String token) {
        return validateTokenWithReason(token).isValid();
    }

    public JwtValidationResult validateTokenWithReason(final String token) {
        try {
            getClaims(token);
            return JwtValidationResult.success();
        } catch (final SignatureException e) {
            return JwtValidationResult.invalid("invalid_signature");
        } catch (final MalformedJwtException e) {
            return JwtValidationResult.invalid("malformed_token");
        } catch (final ExpiredJwtException e) {
            return JwtValidationResult.invalid("expired_token");
        } catch (final UnsupportedJwtException e) {
            return JwtValidationResult.invalid("unsupported_token");
        } catch (final IllegalArgumentException e) {
            return JwtValidationResult.invalid("empty_claims");
        }
    }

    /**
     * JWT 토큰 만료 시간 조회 (Refresh Token용)
     */
    public OffsetDateTime getExpirationTime(final String token) {
        final Date expiration = getClaims(token).getExpiration();
        return OffsetDateTime.ofInstant(expiration.toInstant(), java.time.ZoneOffset.UTC);
    }

    /**
     * JWT Claims 추출
     */
    private Claims getClaims(final String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 서명 키 생성
     */
    private SecretKey getSigningKey() {
        final byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public record JwtValidationResult(boolean isValid, String reason) {

        public static JwtValidationResult success() {
            return new JwtValidationResult(true, "valid");
        }

        public static JwtValidationResult invalid(final String reason) {
            return new JwtValidationResult(false, reason);
        }
    }
}
