package com.booking.notification;

import java.time.Instant;

import com.booking.appointment.Appointment;

/**
 * Communicates with customers/admins about appointment events. Kept separate from
 * fulfillment (delivery to the company). Implementations are called after the
 * relevant transaction commits and must not affect booking outcomes.
 */
public interface NotificationService {

    /** A customer submitted a request. */
    void appointmentRequested(Appointment appointment);

    /** A request was approved, or an administrator entered an appointment. */
    void appointmentApproved(Appointment appointment);

    void appointmentDenied(Appointment appointment);

    void appointmentCancelled(Appointment appointment);

    void appointmentRescheduled(Appointment appointment, Instant previousStart);
}
