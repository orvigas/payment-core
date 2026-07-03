package com.payment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

  @Value("${app.jwt.secret:your-super-secret-key-change-in-production-at-least-32-characters}")
  private String jwtSecret;

  @Value("${app.jwt.expiration:3600000}") // 1 hour in milliseconds
  private long jwtExpirationMs;

  /**
   * Generate JWT token for authenticated user
   */
  public String generateToken(String userId) {
    log.debug("Generating JWT token for userId: {}", userId);

    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("type", "access_token");

    return createToken(claims, userId);
  }

  /**
   * Generate refresh token (longer expiration)
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
        .expiration(new Date(System.currentTimeMillis() + (jwtExpirationMs * 7))) // 7x longer
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Create JWT token with claims
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
   * Validate JWT token and extract userId
   */
  public String getUserIdFromToken(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseEncryptedClaims(token)
          .getPayload();

      return claims.getSubject();
    } catch (Exception e) {
      log.warn("Failed to get userId from token: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Validate token signature and expiration
   */
  public boolean isTokenValid(String token) {
    try {
      Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseEncryptedClaims(token);
      return true;
    } catch (Exception e) {
      log.warn("Invalid token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Check if token is expired
   */
  public boolean isTokenExpired(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseEncryptedClaims(token)
          .getPayload();

      return claims.getExpiration().before(new Date());
    } catch (Exception e) {
      log.warn("Error checking token expiration: {}", e.getMessage());
      return true; // Treat as expired if error
    }
  }

  /**
   * Extract claims from token
   */
  public Claims getClaimsFromToken(String token) {
    try {
      return Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseEncryptedClaims(token)
          .getPayload();
    } catch (Exception e) {
      log.warn("Failed to get claims from token: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Get signing key (must be at least 256 bits for HS256)
   */
  private SecretKey getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}