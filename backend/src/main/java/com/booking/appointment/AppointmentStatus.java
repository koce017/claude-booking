package com.booking.appointment;

import java.util.EnumSet;
import java.util.Set;

public enum AppointmentStatus {
    /** Requested by a customer, waiting for an administrator. */
    PENDING,
    /** Accepted by an administrator (or entered by one). */
    APPROVED,
    /** Rejected by an administrator. */
    DENIED,
    /** Called off after being requested or approved. */
    CANCELLED,
    /** The appointment took place. */
    COMPLETED,
    /** The customer did not show up. */
    NO_SHOW;

    /**
     * Statuses that occupy a capacity unit. COMPLETED and NO_SHOW are kept so that a
     * slot's history stays consistent; they only ever apply to past slots anyway.
     */
    public static final Set<AppointmentStatus> CAPACITY_CONSUMING = EnumSet.of(PENDING, APPROVED, COMPLETED, NO_SHOW);

    public boolean consumesCapacity() {
        return CAPACITY_CONSUMING.contains(this);
    }
}
