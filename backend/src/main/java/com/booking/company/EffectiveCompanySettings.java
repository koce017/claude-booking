package com.booking.company;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Company settings with every unset value replaced by its default. Business logic
 * works with this record rather than reading nullable entity fields directly.
 */
public record EffectiveCompanySettings(
        ZoneId zone,
        Duration appointmentDuration,
        int maxConcurrentAppointments,
        int bookingWindowMonths) {
}
