package com.booking.schedule;

import java.time.LocalTime;

/** A half-open local time range [start, end) within one day. */
public record TimeRange(LocalTime start, LocalTime end) {

    public TimeRange {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
