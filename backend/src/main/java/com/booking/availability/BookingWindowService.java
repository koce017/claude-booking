package com.booking.availability;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.springframework.stereotype.Service;

import com.booking.company.EffectiveCompanySettings;

/**
 * The booking window runs from today (company-local) to the end of the calendar
 * month containing today + N months. E.g. on 2026-10-08 with a 12-month window,
 * the last bookable date is 2027-10-31.
 */
@Service
public class BookingWindowService {

    private final Clock clock;

    public BookingWindowService(Clock clock) {
        this.clock = clock;
    }

    public BookingWindow windowFor(EffectiveCompanySettings settings) {
        LocalDate today = LocalDate.now(clock.withZone(settings.zone()));
        LocalDate lastDate = today.plusMonths(settings.bookingWindowMonths())
                .with(TemporalAdjusters.lastDayOfMonth());
        return new BookingWindow(today, lastDate);
    }
}
