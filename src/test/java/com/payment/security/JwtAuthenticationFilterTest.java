package com.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * @author orvigas@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(tokenProvider);
    SecurityContextHolder.clearContext();
  }

  @Test
  void testValidTokenSetsAuthentication() throws ServletException, IOException {
    String userId = "test_user_123";
    String token = "valid.jwt.token";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenProvider.isTokenValid(token)).thenReturn(true);
    when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);

    filter.doFilterInternal(request, response, filterChain);

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testMissingAuthorizationHeaderPassesThrough() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testEmptyAuthorizationHeaderPassesThrough() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("");

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testMalformedBearerTokenIgnored() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("NotBearer token");

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testInvalidTokenNotProcessed() throws ServletException, IOException {
    String token = "invalid.jwt.token";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenProvider.isTokenValid(token)).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testValidTokenButNullUserIdDoesNotSetAuthentication() throws ServletException, IOException {
    String token = "valid.jwt.token";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenProvider.isTokenValid(token)).thenReturn(true);
    when(tokenProvider.getUserIdFromToken(token)).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testExceptionHandlingContinuesFilterChain() throws ServletException, IOException {
    String token = "token";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenProvider.isTokenValid(token)).thenThrow(new RuntimeException("Token parsing error"));

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testBearerTokenExtractionCorrect() throws ServletException, IOException {
    String userId = "user456";
    String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenProvider.isTokenValid(token)).thenReturn(true);
    when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);

    filter.doFilterInternal(request, response, filterChain);

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
  }

  @Test
  void testWhitespaceInAuthorizationHeaderHandled() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("   ");

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testMultipleBearerPrefixesHandledCorrectly() throws ServletException, IOException {
    String token = "token";

    when(request.getHeader("Authorization")).thenReturn("Bearer Bearer " + token);
    when(tokenProvider.isTokenValid("Bearer " + token)).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
