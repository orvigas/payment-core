package com.payment.config;

import com.payment.security.JwtAuthenticationFilter;
import com.payment.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Configures Spring Security with stateless JWT-based authentication.
 * Enables CORS, disables session creation, and applies JWT filter for token validation.
 *
 * @author orvigas@gmail.com
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider tokenProvider;

  /**
   * Builds the HTTP security filter chain with JWT authentication and CORS support.
   *
   * @param http the HttpSecurity builder
   * @return configured SecurityFilterChain
   * @throws Exception if security configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless JWT auth
        .sessionManagement(management -> management
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests
            // Public endpoints (no auth required)
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/actuator/prometheus").permitAll()
            .requestMatchers("/swagger-ui/**").permitAll()
            .requestMatchers("/v3/api-docs/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()

            // Protected endpoints (auth required)
            .requestMatchers(HttpMethod.POST, "/api/v1/payments").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").authenticated()
            .requestMatchers(HttpMethod.PUT, "/api/v1/payments/**").authenticated()

            // Default: require authentication
            .anyRequest().authenticated())
        .cors(withDefaults())
        .addFilterBefore(
            new JwtAuthenticationFilter(tokenProvider),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Provides CORS configuration for cross-origin requests.
   * Allows requests from localhost development environments.
   *
   * @return CorsConfigurationSource configured for payment service
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:3000",
        "http://localhost:8080"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}