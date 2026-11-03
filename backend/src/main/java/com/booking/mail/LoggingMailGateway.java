package com.booking.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development gateway: prints emails to the application log instead of sending
 * them. Never use in production; admin login links would end up in the log.
 */
@Component
@ConditionalOnProperty(name = "booking.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingMailGateway implements MailGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailGateway.class);

    @Override
    public void send(OutgoingMail mail) {
        log.warn("[DEV MAIL] To: {}\nSubject: {}\n\n{}", mail.to(), mail.subject(), mail.body());
    }
}
