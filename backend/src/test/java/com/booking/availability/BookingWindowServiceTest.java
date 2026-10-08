package com.booking.availability;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.booking.company.EffectiveCompanySettings;

import static org.assertj.core.api.Assertions.assertThat;

class BookingWindowServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");

    private static BookingWindow windowAt(String instant, int months) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new BookingWindowService(clock)
                .windowFor(new EffectiveCompanySettings(ZONE, Duration.ofHours(1), 1, months));
    }

    @Test
    void extendsToEndOfFinalMonth() {
        BookingWindow window = windowAt("2026-10-08T08:00:00Z", 12);

        assertThat(window.firstDate()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(window.lastDate()).isEqualTo(LocalDate.of(2027, 10, 31));
        assertThat(window.contains(LocalDate.of(2027, 10, 31))).isTrue();
        assertThat(window.contains(LocalDate.of(2027, 11, 1))).isFalse();
        assertThat(window.contains(LocalDate.of(2026, 10, 7))).isFalse();
    }

    @Test
    void usesCompanyLocalDate() {
        // 23:30 UTC on Jan 31 is already Feb 1 in Belgrade
        BookingWindow window = windowAt("2027-01-31T23:30:00Z", 12);

        assertThat(window.firstDate()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(window.lastDate()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void windowLengthIsConfigurable() {
        assertThat(windowAt("2026-10-08T08:00:00Z", 3).lastDate()).isEqualTo(LocalDate.of(2027, 1, 31));
    }
}
