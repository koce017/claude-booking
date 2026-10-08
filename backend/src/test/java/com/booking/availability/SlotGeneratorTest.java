package com.booking.availability;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.booking.schedule.TimeRange;

import static org.assertj.core.api.Assertions.assertThat;

class SlotGeneratorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");
    private final SlotGenerator generator = new SlotGenerator();

    private static TimeRange range(int fromH, int fromM, int toH, int toM) {
        return new TimeRange(LocalTime.of(fromH, fromM), LocalTime.of(toH, toM));
    }

    private static List<String> starts(List<Slot> slots) {
        return slots.stream().map(s -> s.localStart().toLocalTime().toString()).toList();
    }

    @Test
    void hourlySlotsFillWorkingDay() {
        List<Slot> slots = generator.generate(LocalDate.of(2026, 11, 12), List.of(range(9, 0, 17, 0)),
                Duration.ofHours(1), ZONE);

        assertThat(starts(slots)).containsExactly("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00");
        assertThat(slots.get(7).localEnd().toLocalTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(slots.get(5).start().toString()).isEqualTo("2026-11-12T13:00:00Z");
        assertThat(slots.get(5).end().toString()).isEqualTo("2026-11-12T14:00:00Z");
    }

    @Test
    void slotsMustFitEntirelyInsidePeriod() {
        List<Slot> slots = generator.generate(LocalDate.of(2026, 11, 12), List.of(range(9, 30, 17, 0)),
                Duration.ofHours(1), ZONE);

        // 16:30-17:30 would overflow the period end and is not generated
        assertThat(starts(slots)).containsExactly("09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30");
    }

    @Test
    void respectsAppointmentDuration() {
        List<Slot> slots = generator.generate(LocalDate.of(2026, 11, 12), List.of(range(9, 0, 11, 0)),
                Duration.ofMinutes(30), ZONE);

        assertThat(starts(slots)).containsExactly("09:00", "09:30", "10:00", "10:30");
        assertThat(Duration.between(slots.get(0).start(), slots.get(0).end())).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void multiplePeriodsPerDay() {
        List<Slot> slots = generator.generate(LocalDate.of(2026, 11, 12),
                List.of(range(8, 0, 10, 0), range(14, 0, 16, 0)), Duration.ofHours(1), ZONE);

        assertThat(starts(slots)).containsExactly("08:00", "09:00", "14:00", "15:00");
    }

    @Test
    void noPeriodsNoSlots() {
        assertThat(generator.generate(LocalDate.of(2026, 11, 14), List.of(), Duration.ofHours(1), ZONE)).isEmpty();
    }

    @Test
    void skipsSlotsBrokenByDaylightSavingTransition() {
        // Europe/Belgrade springs forward 2027-03-28 02:00 -> 03:00
        List<Slot> slots = generator.generate(LocalDate.of(2027, 3, 28), List.of(range(1, 0, 5, 0)),
                Duration.ofHours(1), ZONE);

        // 01:00-02:00 lasts 1h; 02:00 does not exist; 03:00 and 04:00 are fine
        assertThat(starts(slots)).containsExactly("01:00", "03:00", "04:00");
        slots.forEach(s -> assertThat(Duration.between(s.start(), s.end())).isEqualTo(Duration.ofHours(1)));
    }
}
