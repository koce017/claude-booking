package com.booking.mail;

/** A plain-text email ready to send. */
public record OutgoingMail(String to, String subject, String body) {
}
