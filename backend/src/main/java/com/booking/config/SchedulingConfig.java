package com.booking.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Background jobs (e.g. cleanup of expired admin logins). Disabled in tests. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "booking.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
