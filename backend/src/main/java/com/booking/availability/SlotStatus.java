package com.booking.availability;

/** Public, aggregate state of a time slot. Never reveals who booked it. */
public enum SlotStatus {
    /** Capacity remains and nobody has requested the slot yet. */
    AVAILABLE,
    /** Capacity remains but there are pending requests for the slot. Still bookable. */
    PENDING,
    /** Not bookable: fully booked, in the past or outside the booking window. */
    UNAVAILABLE,
    /** Outside the company's working schedule. */
    NON_WORKING
}
