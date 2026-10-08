package com.booking.company;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.booking.config.BookingProperties;

/**
 * Single place where defaults for unset company settings are decided.
 */
@Component
public class CompanySettingsResolver {

    public static final Duration DEFAULT_APPOINTMENT_DURATION = Duration.ofMinutes(60);
    public static final int DEFAULT_MAX_CONCURRENT_APPOINTMENTS = 1;
    public static final int DEFAULT_BOOKING_WINDOW_MONTHS = 12;

    private static final Logger log = LoggerFactory.getLogger(CompanySettingsResolver.class);

    private final ZoneId defaultZone;

    public CompanySettingsResolver(BookingProperties properties) {
        this.defaultZone = properties.defaultTimezone();
    }

    public EffectiveCompanySettings resolve(Company company) {
        return new EffectiveCompanySettings(
                resolveZone(company),
                company.getAppointmentDurationMinutes() != null
                        ? Duration.ofMinutes(company.getAppointmentDurationMinutes())
                        : DEFAULT_APPOINTMENT_DURATION,
                company.getMaxConcurrentAppointments() != null
                        ? company.getMaxConcurrentAppointments()
                        : DEFAULT_MAX_CONCURRENT_APPOINTMENTS,
                company.getBookingWindowMonths() != null
                        ? company.getBookingWindowMonths()
                        : DEFAULT_BOOKING_WINDOW_MONTHS);
    }

    private ZoneId resolveZone(Company company) {
        if (company.getTimezone() == null || company.getTimezone().isBlank()) {
            return defaultZone;
        }
        try {
            return ZoneId.of(company.getTimezone());
        } catch (DateTimeException e) {
            log.warn("Company {} has invalid timezone '{}', using default {}",
                    company.getId(), company.getTimezone(), defaultZone);
            return defaultZone;
        }
    }
}
