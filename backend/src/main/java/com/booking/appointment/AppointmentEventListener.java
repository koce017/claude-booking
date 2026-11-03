package com.booking.appointment;

import java.time.Clock;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.booking.fulfillment.AppointmentFulfillmentService;
import com.booking.notification.NotificationService;

/**
 * Runs side effects only once the state change is committed. Fulfillment and
 * notifications are invoked independently; a failure in either is logged and
 * never rolls back or blocks the appointment change. When fulfillment reports
 * that the company was informed, the appointment is marked as delivered.
 */
@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentFulfillmentService fulfillmentService;
    private final NotificationService notificationService;
    private final Clock clock;

    public AppointmentEventListener(AppointmentRepository appointmentRepository,
                                    AppointmentFulfillmentService fulfillmentService,
                                    NotificationService notificationService,
                                    Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.fulfillmentService = fulfillmentService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentEvent(AppointmentEvent event) {
        Appointment appointment = appointmentRepository.findWithCompanyById(event.appointmentId()).orElse(null);
        if (appointment == null) {
            return;
        }
        switch (event.type()) {
            case REQUESTED -> safely("request notification", () -> notificationService.appointmentRequested(appointment));
            case APPROVED, CREATED_BY_ADMIN -> {
                fulfill(appointment, () -> fulfillmentService.fulfill(appointment));
                safely("approval notification", () -> notificationService.appointmentApproved(appointment));
            }
            case DENIED -> safely("denial notification", () -> notificationService.appointmentDenied(appointment));
            case CANCELLED -> {
                // Only tell the company about cancellations of appointments it was sent.
                if (appointment.getFulfilledAt() != null) {
                    fulfill(appointment, () -> fulfillmentService.cancelled(appointment));
                }
                safely("cancellation notification", () -> notificationService.appointmentCancelled(appointment));
            }
            case RESCHEDULED -> {
                if (appointment.getStatus() == AppointmentStatus.APPROVED) {
                    fulfill(appointment, () -> fulfillmentService.rescheduled(appointment, event.previousStart()));
                }
                safely("reschedule notification",
                        () -> notificationService.appointmentRescheduled(appointment, event.previousStart()));
            }
        }
    }

    private void fulfill(Appointment appointment, BooleanSupplier action) {
        safely("fulfillment", () -> {
            if (action.getAsBoolean()) {
                appointmentRepository.markFulfilled(appointment.getId(), clock.instant());
            }
        });
    }

    private void safely(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Appointment {} failed", what, e);
        }
    }
}
