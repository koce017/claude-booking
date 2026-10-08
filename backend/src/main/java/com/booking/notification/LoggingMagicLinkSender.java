package com.booking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development sender: prints the magic link to the application log instead of
 * emailing it. Never use in production; the link grants admin access.
 */
@Component
@ConditionalOnProperty(name = "booking.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingMagicLinkSender implements MagicLinkSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMagicLinkSender.class);

    @Override
    public void send(String email, String link) {
        log.warn("[DEV MAIL] Magic login link for {}: {}", email, link);
    }
}
