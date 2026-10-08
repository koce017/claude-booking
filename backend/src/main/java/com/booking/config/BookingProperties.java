package com.booking.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("booking")
public record BookingProperties(
        @DefaultValue("Europe/Belgrade") ZoneId defaultTimezone,
        @DefaultValue("http://localhost:5173") String frontendUrl,
        @DefaultValue("62") int maxAvailabilityRangeDays,
        @DefaultValue Admin admin,
        @DefaultValue Mail mail) {

    public record Admin(
            @DefaultValue List<String> bootstrapEmails,
            @DefaultValue("15m") Duration magicLinkTtl,
            @DefaultValue("12h") Duration sessionTtl) {
    }

    public record Mail(
            @DefaultValue("log") String mode,
            @DefaultValue("no-reply@booking.local") String from) {
    }
}
