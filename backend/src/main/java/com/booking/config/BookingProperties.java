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
        /** Extra browser origins allowed by CORS, in addition to frontendUrl. */
        @DefaultValue List<String> corsAllowedOrigins,
        @DefaultValue("62") int maxAvailabilityRangeDays,
        @DefaultValue Admin admin,
        @DefaultValue Mail mail,
        @DefaultValue Notifications notifications,
        @DefaultValue Fulfillment fulfillment,
        @DefaultValue RateLimit rateLimit) {

    public record Admin(
            @DefaultValue List<String> bootstrapEmails,
            @DefaultValue("15m") Duration magicLinkTtl,
            @DefaultValue("12h") Duration sessionTtl) {
    }

    /**
     * @param templatesDir optional folder with edited email templates (see MailTemplates)
     */
    public record Mail(
            @DefaultValue("log") String mode,
            @DefaultValue("no-reply@booking.local") String from,
            String templatesDir) {
    }

    /**
     * @param customerEmails  email customers about their requests (received, approved, ...)
     * @param adminNewRequest email all administrators when a new request arrives
     */
    public record Notifications(
            @DefaultValue("true") boolean customerEmails,
            @DefaultValue("true") boolean adminNewRequest) {
    }

    /**
     * @param mode "email" sends approved appointments to the company's fulfillment
     *             email; "manual" sends nothing and administrators hand them over
     */
    public record Fulfillment(@DefaultValue("email") String mode) {
    }

    /**
     * Per-client-IP request limits for endpoints that can be abused without logging in.
     * Behind a reverse proxy, set server.forward-headers-strategy so the real client IP is used.
     * A rule that is not configured means no limit for that endpoint.
     */
    public record RateLimit(
            @DefaultValue("true") boolean enabled,
            Rule loginLink,
            Rule loginVerify,
            Rule booking) {
    }

    /** At most {@code requests} requests per {@code window}. */
    public record Rule(int requests, Duration window) {
    }
}
