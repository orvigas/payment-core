package com.payment.config;

import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
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

    /**
     * Binds rate limiter permit/wait/rejection metrics to the meter registry.
     *
     * <p>The registry above is created manually rather than through Resilience4j's Spring
     * Boot autoconfiguration, so metrics have to be bound explicitly too - autoconfiguration
     * only wires metrics for the registry beans it creates itself.
     *
     * @param registry the rate limiter registry
     * @param meterRegistry the meter registry metrics are exported through (defined in
     *        {@link ResilienceConfig})
     * @return the bound metrics binder
     */
    @Bean
    public TaggedRateLimiterMetrics rateLimiterMetricsBinder(
            RateLimiterRegistry registry, MeterRegistry meterRegistry) {
        TaggedRateLimiterMetrics metrics = TaggedRateLimiterMetrics.ofRateLimiterRegistry(registry);
        metrics.bindTo(meterRegistry);
        return metrics;
    }
}