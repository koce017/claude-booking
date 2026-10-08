package com.booking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.booking.appointment.Appointment;

/** Placeholder until notification channels are decided: logs events, sends nothing. */
@Service
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void appointmentRequested(Appointment appointment) {
        log.info("Notification: appointment {} requested", appointment.getId());
    }

    @Override
    public void appointmentApproved(Appointment appointment) {
        log.info("Notification: appointment {} approved", appointment.getId());
    }

    @Override
    public void appointmentDenied(Appointment appointment) {
        log.info("Notification: appointment {} denied", appointment.getId());
    }
}
