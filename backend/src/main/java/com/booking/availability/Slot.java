package com.booking.availability;

import java.time.Instant;
import java.time.LocalDateTime;

/** A generated appointment slot, both as company-local time and as instants. */
public record Slot(LocalDateTime localStart, LocalDateTime localEnd, Instant start, Instant end) {
}
