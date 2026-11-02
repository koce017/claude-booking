package com.booking.appointment;

import java.time.Instant;

/**
 * Published inside the transaction that changed an appointment; handled after commit.
 *
 * @param previousStart for {@link Type#RESCHEDULED}, the start time before the move; otherwise null
 */
public record AppointmentEvent(Long appointmentId, Type type, Instant previousStart) {

    public AppointmentEvent(Long appointmentId, Type type) {
        this(appointmentId, type, null);
    }

    public enum Type {
        /** A customer requested an appointment. */
        REQUESTED,
        /** An administrator entered an appointment; it is approved straight away. */
        CREATED_BY_ADMIN,
        APPROVED,
        DENIED,
        CANCELLED,
        RESCHEDULED
    }
}
