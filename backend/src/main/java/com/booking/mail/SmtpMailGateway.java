package com.booking.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.booking.config.BookingProperties;

/** Sends emails through the configured SMTP server (spring.mail.*). */
@Component
@ConditionalOnProperty(name = "booking.mail.mode", havingValue = "smtp")
public class SmtpMailGateway implements MailGateway {

    private final JavaMailSender mailSender;
    private final BookingProperties properties;

    public SmtpMailGateway(JavaMailSender mailSender, BookingProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(OutgoingMail mail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mail().from());
        message.setTo(mail.to());
        message.setSubject(mail.subject());
        message.setText(mail.body());
        mailSender.send(message);
    }
}
