package com.payment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles JWT issuance and validation for authenticated requests.
 *
 * @author orvigas@gmail.com
 */
@Component
@Slf4j
public class JwtTokenProvider {

  @Value("${app.jwt.secret:your-super-secret-key-change-in-production-at-least-32-characters}")
  private String jwtSecret;

  @Value("${app.jwt.expiration:3600000}") // 1 hour in milliseconds
  private long jwtExpirationMs;

  /**
   * Generates an access token for the given user.
   *
   * @param userId the authenticated user's ID
   * @return signed JWT token
   */
  public String generateToken(String userId) {
    log.debug("Generating JWT token for userId: {}", userId);

    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("type", "access_token");

    return createToken(claims, userId);
  }

  /**
   * Generates a refresh token with extended expiration for the given user.
   *
   * @param userId the authenticated user's ID
   * @return signed JWT refresh token (7x longer expiration than access token)
   */
  public String generateRefreshToken(String userId) {
    log.debug("Generating refresh token for userId: {}", userId);

    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("type", "refresh_token");

    return Jwts.builder()
        .claims(claims)
        .subject(userId)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + (jwtExpirationMs * 7)))
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Creates a JWT token with the provided claims.
   *
   * @param claims token claims (including userId, type)
   * @param subject the token subject (typically userId)
   * @return signed JWT token
   */
  private String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Extracts the user ID from a signed JWT token.
   *
   * @param token the JWT token string
   * @return the user ID (subject), or null if token is invalid or parsing fails
   */
  public String getUserIdFromToken(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();

      return claims.getSubject();
    } catch (Exception e) {
      log.warn("Failed to get userId from token: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Validates a JWT token's signature and expiration.
   *
   * @param token the JWT token string
   * @return true if the token is valid, false otherwise
   */
  public boolean isTokenValid(String token) {
    try {
      Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      log.warn("Invalid token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Checks if a JWT token has expired.
   *
   * @param token the JWT token string
   * @return true if the token is expired or cannot be parsed, false if valid and not expired
   */
  public boolean isTokenExpired(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();

      return claims.getExpiration().before(new Date());
    } catch (Exception e) {
      log.warn("Error checking token expiration: {}", e.getMessage());
      return true;
    }
  }

  /**
   * Extracts all claims from a JWT token.
   *
   * @param token the JWT token string
   * @return token claims, or null if parsing fails
   */
  public Claims getClaimsFromToken(String token) {
    try {
      return Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (Exception e) {
      log.warn("Failed to get claims from token: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Derives the HMAC signing key from the configured secret.
   * The secret must be at least 256 bits (32 bytes) for HS256.
   *
   * @return the derived SecretKey for HMAC-SHA256 signing
   */
  private SecretKey getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}