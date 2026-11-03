package com.booking.notification;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.booking.config.BookingProperties;
import com.booking.mail.MailGateway;
import com.booking.mail.MailTemplates;

/**
 * Emails admin login links using the admin-login-link template. With
 * booking.mail.mode=log the email, including the link, is printed to the log.
 */
@Component
@ConditionalOnExpression("'${booking.mail.mode:log}' == 'log' or '${booking.mail.mode:log}' == 'smtp'")
public class MailMagicLinkSender implements MagicLinkSender {

    private final MailGateway mailGateway;
    private final MailTemplates templates;
    private final BookingProperties properties;

    public MailMagicLinkSender(MailGateway mailGateway, MailTemplates templates, BookingProperties properties) {
        this.mailGateway = mailGateway;
        this.templates = templates;
        this.properties = properties;
    }

    @Override
    public void send(String email, String link) {
        mailGateway.send(templates.render("admin-login-link", email, Map.of(
                "link", link,
                "minutes", String.valueOf(properties.admin().magicLinkTtl().toMinutes()))));
    }
}
