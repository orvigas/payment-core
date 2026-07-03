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
}