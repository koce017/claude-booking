package com.booking.notification;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.booking.admin.Administrator;
import com.booking.admin.AdministratorRepository;
import com.booking.appointment.Appointment;
import com.booking.config.BookingProperties;
import com.booking.mail.AppointmentMailData;
import com.booking.mail.MailGateway;
import com.booking.mail.MailTemplates;
import com.booking.mail.OutgoingMail;

/**
 * Emails customers about their appointments and administrators about new requests.
 * Customers without an email address are skipped. Each recipient is sent to
 * separately, so one failing address does not stop the others.
 */
@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final MailGateway mailGateway;
    private final MailTemplates templates;
    private final AppointmentMailData mailData;
    private final AdministratorRepository administratorRepository;
    private final BookingProperties.Notifications settings;

    public EmailNotificationService(MailGateway mailGateway,
                                    MailTemplates templates,
                                    AppointmentMailData mailData,
                                    AdministratorRepository administratorRepository,
                                    BookingProperties properties) {
        this.mailGateway = mailGateway;
        this.templates = templates;
        this.mailData = mailData;
        this.administratorRepository = administratorRepository;
        this.settings = properties.notifications();
    }

    @Override
    public void appointmentRequested(Appointment appointment) {
        Map<String, String> values = mailData.of(appointment);
        toCustomer(appointment, "customer-request-received", values);
        if (settings.adminNewRequest()) {
            for (Administrator admin : administratorRepository.findByActiveTrueOrderByIdAsc()) {
                send(templates.render("admin-new-request", admin.getEmail(), values), appointment);
            }
        }
    }

    @Override
    public void appointmentApproved(Appointment appointment) {
        toCustomer(appointment, "customer-approved", mailData.of(appointment));
    }

    @Override
    public void appointmentDenied(Appointment appointment) {
        toCustomer(appointment, "customer-denied", mailData.of(appointment));
    }

    @Override
    public void appointmentCancelled(Appointment appointment) {
        toCustomer(appointment, "customer-cancelled", mailData.of(appointment));
    }

    @Override
    public void appointmentRescheduled(Appointment appointment, Instant previousStart) {
        toCustomer(appointment, "customer-rescheduled", mailData.of(appointment, previousStart));
    }

    private void toCustomer(Appointment appointment, String template, Map<String, String> values) {
        String email = appointment.getCustomer().email();
        if (!settings.customerEmails() || email == null) {
            return;
        }
        send(templates.render(template, email, values), appointment);
    }

    private void send(OutgoingMail mail, Appointment appointment) {
        try {
            mailGateway.send(mail);
        } catch (RuntimeException e) {
            log.error("Could not send '{}' for appointment {} to {}", mail.subject(), appointment.getId(), mail.to(), e);
        }
    }
}
