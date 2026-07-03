package com.payment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

  @Autowired
  private JwtTokenProvider tokenProvider;

  private String testUserId = "test_user_123";

  @BeforeEach
  void setUp() {
    assertNotNull(tokenProvider);
  }

  @Test
  void testGenerateToken() {
    String token = tokenProvider.generateToken(testUserId);
    assertNotNull(token);
  }

  @Test
  void testValidateToken() {
    String token = tokenProvider.generateToken(testUserId);
    assertTrue(tokenProvider.isTokenValid(token));
  }

  @Test
  void testGetUserIdFromToken() {
    String token = tokenProvider.generateToken(testUserId);
    String userId = tokenProvider.getUserIdFromToken(token);
    assertEquals(testUserId, userId);
  }

  @Test
  void testInvalidTokenReturnsNull() {
    String userId = tokenProvider.getUserIdFromToken("invalid.token.here");
    assertNull(userId);
  }

  @Test
  void testRefreshToken() {
    String refreshToken = tokenProvider.generateRefreshToken(testUserId);
    assertNotNull(refreshToken);
    assertTrue(tokenProvider.isTokenValid(refreshToken));
    assertEquals(testUserId, tokenProvider.getUserIdFromToken(refreshToken));
  }

  @Test
  void testInvalidTokenStructure() {
    String userId = tokenProvider.getUserIdFromToken("not.a.valid.jwt");
    assertNull(userId);
  }

  @Test
  void testEmptyTokenString() {
    String userId = tokenProvider.getUserIdFromToken("");
    assertNull(userId);
  }

  @Test
  void testNullToken() {
    String userId = tokenProvider.getUserIdFromToken(null);
    assertNull(userId);
  }

  @Test
  void testMalformedJwt() {
    boolean isValid = tokenProvider.isTokenValid("malformed");
    assertFalse(isValid);
  }

  @Test
  void testGenerateTokenProducesValidToken() {
    String token = tokenProvider.generateToken(testUserId);
    assertNotNull(token);
    assertTrue(tokenProvider.isTokenValid(token));
  }

  @Test
  void testGenerateRefreshTokenProducesValidToken() {
    String refreshToken = tokenProvider.generateRefreshToken(testUserId);
    assertNotNull(refreshToken);
    assertTrue(tokenProvider.isTokenValid(refreshToken));
  }

  @Test
  void testTokensAreDifferent() {
    String accessToken = tokenProvider.generateToken(testUserId);
    String refreshToken = tokenProvider.generateRefreshToken(testUserId);
    assertNotEquals(accessToken, refreshToken);
  }

  @Test
  void testGetUserIdFromValidAccessToken() {
    String token = tokenProvider.generateToken(testUserId);
    String userId = tokenProvider.getUserIdFromToken(token);
    assertEquals(testUserId, userId);
  }

  @Test
  void testGetUserIdFromValidRefreshToken() {
    String refreshToken = tokenProvider.generateRefreshToken(testUserId);
    String userId = tokenProvider.getUserIdFromToken(refreshToken);
    assertEquals(testUserId, userId);
  }

  @Test
  void testValidateTokenWithMultipleTokens() {
    String token1 = tokenProvider.generateToken("user1");
    String token2 = tokenProvider.generateToken("user2");
    assertTrue(tokenProvider.isTokenValid(token1));
    assertTrue(tokenProvider.isTokenValid(token2));
    assertEquals("user1", tokenProvider.getUserIdFromToken(token1));
    assertEquals("user2", tokenProvider.getUserIdFromToken(token2));
  }

  @Test
  void testInvalidTokenReturnsNullForUserId() {
    assertNull(tokenProvider.getUserIdFromToken("invalid.jwt.format"));
  }

  @Test
  void testIsTokenValidReturnsFalseForMalformed() {
    assertFalse(tokenProvider.isTokenValid("invalid"));
  }

  @Test
  void testTokenWithSpecialCharacters() {
    String userId = "user@example.com";
    String token = tokenProvider.generateToken(userId);
    assertTrue(tokenProvider.isTokenValid(token));
    assertEquals(userId, tokenProvider.getUserIdFromToken(token));
  }

  @Test
  void testIsTokenExpiredReturnsFalseForValidToken() {
    String token = tokenProvider.generateToken(testUserId);
    assertFalse(tokenProvider.isTokenExpired(token));
  }

  @Test
  void testIsTokenExpiredReturnsTrueForInvalidToken() {
    assertTrue(tokenProvider.isTokenExpired("invalid.token"));
  }

  @Test
  void testGetClaimsFromValidToken() {
    String token = tokenProvider.generateToken(testUserId);
    var claims = tokenProvider.getClaimsFromToken(token);
    assertNotNull(claims);
    assertEquals(testUserId, claims.getSubject());
  }

  @Test
  void testGetClaimsFromInvalidTokenReturnsNull() {
    var claims = tokenProvider.getClaimsFromToken("invalid.token");
    assertNull(claims);
  }

  @Test
  void testGetClaimsContainsUserId() {
    String token = tokenProvider.generateToken(testUserId);
    var claims = tokenProvider.getClaimsFromToken(token);
    assertNotNull(claims);
    assertEquals(testUserId, claims.get("userId"));
  }

  @Test
  void testAccessTokenHasCorrectType() {
    String token = tokenProvider.generateToken(testUserId);
    var claims = tokenProvider.getClaimsFromToken(token);
    assertNotNull(claims);
    assertEquals("access_token", claims.get("type"));
  }

  @Test
  void testRefreshTokenHasCorrectType() {
    String token = tokenProvider.generateRefreshToken(testUserId);
    var claims = tokenProvider.getClaimsFromToken(token);
    assertNotNull(claims);
    assertEquals("refresh_token", claims.get("type"));
  }

  @Test
  void testRefreshTokenHasLongerExpiration() {
    String accessToken = tokenProvider.generateToken(testUserId);
    String refreshToken = tokenProvider.generateRefreshToken(testUserId);

    var accessClaims = tokenProvider.getClaimsFromToken(accessToken);
    var refreshClaims = tokenProvider.getClaimsFromToken(refreshToken);

    assertNotNull(accessClaims);
    assertNotNull(refreshClaims);
    assertTrue(refreshClaims.getExpiration().after(accessClaims.getExpiration()));
  }

  @Test
  void testGenerateTokenCreatesValidToken() {
    String token = tokenProvider.generateToken("test_user");
    assertTrue(tokenProvider.isTokenValid(token));
  }

  @Test
  void testGenerateRefreshTokenCreatesValidToken() {
    String refreshToken = tokenProvider.generateRefreshToken("test_user");
    assertTrue(tokenProvider.isTokenValid(refreshToken));
  }

  @Test
  void testTokenClaimsArePreserved() {
    String token = tokenProvider.generateToken(testUserId);
    var claims = tokenProvider.getClaimsFromToken(token);

    assertNotNull(claims);
    assertEquals(testUserId, claims.getSubject());
    assertEquals(testUserId, claims.get("userId"));
  }

  @Test
  void testMultipleTokensAreIndependent() {
    String token1 = tokenProvider.generateToken("user1");
    String token2 = tokenProvider.generateToken("user2");

    assertEquals("user1", tokenProvider.getUserIdFromToken(token1));
    assertEquals("user2", tokenProvider.getUserIdFromToken(token2));
    assertTrue(tokenProvider.isTokenValid(token1));
    assertTrue(tokenProvider.isTokenValid(token2));
  }

  @Test
  void testGetClaimsFromNullTokenReturnsNull() {
    var claims = tokenProvider.getClaimsFromToken(null);
    assertNull(claims);
  }

  @Test
  void testGetClaimsFromEmptyTokenReturnsNull() {
    var claims = tokenProvider.getClaimsFromToken("");
    assertNull(claims);
  }
}