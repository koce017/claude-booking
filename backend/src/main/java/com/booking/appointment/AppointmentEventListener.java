package com.booking.appointment;

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
 * never rolls back or blocks the appointment change.
 */
@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentFulfillmentService fulfillmentService;
    private final NotificationService notificationService;

    public AppointmentEventListener(AppointmentRepository appointmentRepository,
                                    AppointmentFulfillmentService fulfillmentService,
                                    NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.fulfillmentService = fulfillmentService;
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onAppointmentEvent(AppointmentEvent event) {
        Appointment appointment = appointmentRepository.findWithCompanyById(event.appointmentId()).orElse(null);
        if (appointment == null) {
            return;
        }
        switch (event.type()) {
            case REQUESTED -> safely("request notification", () -> notificationService.appointmentRequested(appointment));
            case APPROVED -> {
                safely("fulfillment", () -> fulfillmentService.fulfill(appointment));
                safely("approval notification", () -> notificationService.appointmentApproved(appointment));
            }
            case DENIED -> safely("denial notification", () -> notificationService.appointmentDenied(appointment));
        }
    }

    private void safely(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Appointment {} failed", what, e);
        }
    }
}
