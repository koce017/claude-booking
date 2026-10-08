package com.booking.availability;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.booking.schedule.TimeRange;

/**
 * Generates slots inside working periods. For now the slot interval equals the
 * appointment duration; slots always start at the period start and must fit
 * entirely inside the period (09:00-17:00 with 60 min gives 09:00 ... 16:00).
 *
 * <p>Daylight-saving transitions: slots whose local start does not exist (spring
 * forward gap) or whose real length differs from the duration are skipped.
 */
@Component
public class SlotGenerator {

    public List<Slot> generate(LocalDate date, List<TimeRange> periods, Duration duration, ZoneId zone) {
        List<Slot> slots = new ArrayList<>();
        for (TimeRange period : periods) {
            LocalDateTime limit = date.atTime(period.end());
            for (LocalDateTime cursor = date.atTime(period.start());
                 !cursor.plus(duration).isAfter(limit);
                 cursor = cursor.plus(duration)) {
                LocalDateTime localEnd = cursor.plus(duration);
                ZonedDateTime start = cursor.atZone(zone);
                ZonedDateTime end = localEnd.atZone(zone);
                if (!start.toLocalDateTime().equals(cursor)
                        || !Duration.between(start.toInstant(), end.toInstant()).equals(duration)) {
                    continue;
                }
                slots.add(new Slot(cursor, localEnd, start.toInstant(), end.toInstant()));
            }
        }
        return slots;
    }
}
