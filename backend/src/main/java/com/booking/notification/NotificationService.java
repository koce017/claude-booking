package com.booking.notification;

import com.booking.appointment.Appointment;

/**
 * Communicates with customers/admins about appointment events. Kept separate from
 * fulfillment (delivery to the company). Implementations are called after the
 * relevant transaction commits and must not affect booking outcomes.
 */
public interface NotificationService {

    void appointmentRequested(Appointment appointment);

    void appointmentApproved(Appointment appointment);

    void appointmentDenied(Appointment appointment);
}
