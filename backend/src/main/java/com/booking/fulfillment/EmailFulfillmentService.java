package com.booking.fulfillment;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.booking.appointment.Appointment;
import com.booking.mail.AppointmentMailData;
import com.booking.mail.MailGateway;
import com.booking.mail.MailTemplates;

/**
 * Emails approved appointments, with the customer's details, to the company's
 * fulfillment email address. That address is set by administrators and never shown
 * to customers. Companies without one are skipped: the appointment stays marked as
 * not delivered, and administrators hand it over themselves.
 *
 * <p>Active with booking.fulfillment.mode=email (the default).
 */
@Service
@ConditionalOnProperty(name = "booking.fulfillment.mode", havingValue = "email", matchIfMissing = true)
public class EmailFulfillmentService implements AppointmentFulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(EmailFulfillmentService.class);

    private final MailGateway mailGateway;
    private final MailTemplates templates;
    private final AppointmentMailData mailData;

    public EmailFulfillmentService(MailGateway mailGateway, MailTemplates templates, AppointmentMailData mailData) {
        this.mailGateway = mailGateway;
        this.templates = templates;
        this.mailData = mailData;
    }

    @Override
    public boolean fulfill(Appointment appointment) {
        return send(appointment, "company-new-appointment", mailData.of(appointment));
    }

    @Override
    public boolean cancelled(Appointment appointment) {
        return send(appointment, "company-appointment-cancelled", mailData.of(appointment));
    }

    @Override
    public boolean rescheduled(Appointment appointment, Instant previousStart) {
        return send(appointment, "company-appointment-rescheduled", mailData.of(appointment, previousStart));
    }

    private boolean send(Appointment appointment, String template, Map<String, String> values) {
        String to = appointment.getCompany().getFulfillmentEmail();
        if (to == null || to.isBlank()) {
            log.warn("Company {} has no fulfillment email; appointment {} must be handed over manually",
                    appointment.getCompany().getId(), appointment.getId());
            return false;
        }
        mailGateway.send(templates.render(template, to, values));
        return true;
    }
}
