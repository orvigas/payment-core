package com.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Extracts JWT tokens from Authorization headers and populates the security context.
 * Runs once per HTTP request to validate and apply token-based authentication.
 *
 * @author orvigas@gmail.com
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String AUTHORIZATION_HEADER = "Authorization";

  /**
   * Processes a single HTTP request to extract and validate JWT authentication.
   */
  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String jwt = getJwtFromRequest(request);

      if (StringUtils.hasText(jwt) && tokenProvider.isTokenValid(jwt)) {
        String userId = tokenProvider.getUserIdFromToken(jwt);

        if (userId != null) {
          // Create authentication token
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              userId,
              null,
              new ArrayList<>() // authorities (roles could go here)
          );

          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);

          log.debug("JWT authentication set for userId: {}", userId);
        }
      }
    } catch (Exception ex) {
      log.error("Could not set user authentication in security context", ex);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Extracts the JWT token from the Authorization header.
   * Expects format: "Bearer &lt;token&gt;"
   *
   * @param request the HTTP request
   * @return the JWT token or null if not present or incorrectly formatted
   */
  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }

    return null;
  }
}