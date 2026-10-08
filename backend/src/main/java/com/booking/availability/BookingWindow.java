package com.booking.availability;

import java.time.LocalDate;

/** Inclusive range of company-local dates on which appointments can be booked. */
public record BookingWindow(LocalDate firstDate, LocalDate lastDate) {

    public boolean contains(LocalDate date) {
        return !date.isBefore(firstDate) && !date.isAfter(lastDate);
    }
}
