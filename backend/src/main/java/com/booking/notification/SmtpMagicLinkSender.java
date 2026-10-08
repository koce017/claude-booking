package com.booking.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.booking.config.BookingProperties;

/** Sends magic links by email through the configured SMTP server (spring.mail.*). */
@Component
@ConditionalOnProperty(name = "booking.mail.mode", havingValue = "smtp")
public class SmtpMagicLinkSender implements MagicLinkSender {

    private final JavaMailSender mailSender;
    private final BookingProperties properties;

    public SmtpMagicLinkSender(JavaMailSender mailSender, BookingProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(String email, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mail().from());
        message.setTo(email);
        message.setSubject("Your admin login link");
        message.setText("""
                Use this link to sign in to the booking administration:

                %s

                The link expires in %d minutes and can be used once.
                If you did not request it, you can ignore this email.
                """.formatted(link, properties.admin().magicLinkTtl().toMinutes()));
        mailSender.send(message);
    }
}
