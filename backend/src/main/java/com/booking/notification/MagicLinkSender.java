package com.booking.notification;

/** Delivers admin magic login links. Selected with booking.mail.mode (log | smtp). */
public interface MagicLinkSender {

    void send(String email, String link);
}
