package com.payment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Controls how Flyway applies the migrations in db/migration on startup.
 *
 * @author orvigas@gmail.com
 */
@Configuration
@Slf4j
public class FlywayConfig {

    /**
     * Builds the startup migration strategy.
     *
     * <p>When {@code app.database.reset-on-startup} is true, the schema is dropped via
     * Flyway's clean before migrations reapply it, giving every local run a schema that
     * matches the checked-in migrations rather than whatever accumulated from prior runs.
     * Set the property to false to skip the clean step and only apply pending migrations,
     * which keeps existing data intact across restarts.
     *
     * @param resetOnStartup whether to clean the schema before migrating
     * @return the migration strategy applied by the Flyway autoconfiguration
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
        @Value("${app.database.reset-on-startup:true}") boolean resetOnStartup
    ) {
        return flyway -> {
            if (resetOnStartup) {
                log.warn("app.database.reset-on-startup=true - dropping and recreating the schema");
                flyway.clean();
            }
            flyway.migrate();
        };
    }
}
