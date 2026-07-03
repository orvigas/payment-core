package com.payment.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class RateLimitingConfig {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    /**
     * Rate limiter for payment creation: 100 requests per hour per user
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
     * Rate limiter for login: 10 attempts per minute
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