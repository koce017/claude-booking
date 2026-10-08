package com.booking.availability;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AvailabilityDtos {

    private AvailabilityDtos() {
    }

    public record AvailabilityResponse(
            Long companyId,
            String timezone,
            int appointmentDurationMinutes,
            BookingWindow bookingWindow,
            List<DayAvailability> days) {
    }

    /**
     * One day. {@code slots} covers the full day in order: bookable-or-not
     * appointment slots inside working periods, and NON_WORKING blocks between them.
     */
    public record DayAvailability(LocalDate date, boolean working, List<SlotView> slots) {
    }

    /**
     * @param localStart company-local "HH:mm"
     * @param localEnd   company-local "HH:mm" ("24:00" for end of day)
     * @param reason     set only when status is UNAVAILABLE
     */
    public record SlotView(
            Instant start,
            Instant end,
            String localStart,
            String localEnd,
            SlotStatus status,
            boolean pendingRequests,
            boolean bookable,
            UnavailableReason reason) {
    }
}
