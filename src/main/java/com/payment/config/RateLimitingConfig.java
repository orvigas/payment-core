package com.payment.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures Resilience4j rate limiters for payment and authentication endpoints.
 * Provides protection against brute-force attacks and resource exhaustion.
 *
 * @author orvigas@gmail.com
 */
@Configuration
@Slf4j
public class RateLimitingConfig {

    /**
     * Provides the Resilience4j RateLimiterRegistry for managing rate limiters.
     *
     * @return RateLimiterRegistry with default configuration
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    /**
     * Configures rate limiter for payment creation endpoints.
     * Allows 100 requests per hour per user to prevent API abuse.
     *
     * @param registry the RateLimiterRegistry
     * @return RateLimiter for payment creation
     */
    @Bean
    public RateLimiter paymentCreationRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofHours(1))  // Reset every hour
            .limitForPeriod(100)                       // 100 requests per period
            .timeoutDuration(Duration.ofSeconds(1))   // Wait max 1s for permission
            .build();

        return registry.rateLimiter("payment-creation", config);
    }

    /**
     * Configures rate limiter for login attempts.
     * Allows 10 login attempts per minute to prevent brute-force attacks.
     *
     * @param registry the RateLimiterRegistry
     * @return RateLimiter for login endpoints
     */
    @Bean
    public RateLimiter loginRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))  // Reset every minute
            .limitForPeriod(10)                         // 10 login attempts per minute
            .timeoutDuration(Duration.ofSeconds(1))
            .build();

        return registry.rateLimiter("login", config);
    }
}