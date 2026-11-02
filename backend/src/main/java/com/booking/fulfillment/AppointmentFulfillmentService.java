package com.booking.fulfillment;

import java.time.Instant;

import com.booking.appointment.Appointment;

/**
 * Delivers approved appointments to the company or a downstream system.
 *
 * <p>This is the single extension point for what happens after approval (email
 * to the company, webhook, company API, manual hand-off, ...). The booking,
 * availability and approval logic do not know which implementation is active.
 * Exactly one implementation bean is active, chosen with booking.fulfillment.mode.
 *
 * <p>Methods are called after the transaction has committed; a failure here never
 * undoes the appointment change. Each returns true if the company was informed
 * automatically, so the appointment can be marked as delivered.
 */
public interface AppointmentFulfillmentService {

    /** An appointment was approved or entered by an administrator. */
    boolean fulfill(Appointment appointment);

    /** An appointment the company was already informed about was cancelled. */
    default boolean cancelled(Appointment appointment) {
        return false;
    }

    /** An approved appointment was moved; {@code previousStart} is its old start time. */
    default boolean rescheduled(Appointment appointment, Instant previousStart) {
        return fulfill(appointment);
    }
}
