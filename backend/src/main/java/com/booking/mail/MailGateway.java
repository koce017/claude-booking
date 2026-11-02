package com.booking.mail;

/**
 * Sends emails. Selected with booking.mail.mode: "log" prints them to the
 * application log (local development), "smtp" sends them through spring.mail.*.
 * Callers run after their transaction has committed, so a failed send never
 * affects bookings.
 */
public interface MailGateway {

    void send(OutgoingMail mail);
}
