package com.booking.fulfillment;

import com.booking.appointment.Appointment;

/**
 * Delivers an approved appointment to the company or a downstream system.
 *
 * <p>This is the single extension point for what happens after approval (email
 * to the company, webhook, company API, manual hand-off, ...). The booking,
 * availability and approval logic do not know which implementation is active.
 * Exactly one implementation bean should be present.
 *
 * <p>Called after the approval transaction has committed; a failure here never
 * undoes the approval.
 */
public interface AppointmentFulfillmentService {

    void fulfill(Appointment appointment);
}
