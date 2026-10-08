package com.booking.appointment;

/** Published inside the transaction that changed an appointment; handled after commit. */
public record AppointmentEvent(Long appointmentId, Type type) {

    public enum Type {
        REQUESTED,
        APPROVED,
        DENIED
    }
}
