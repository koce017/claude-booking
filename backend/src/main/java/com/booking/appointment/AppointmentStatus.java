package com.booking.appointment;

import java.util.EnumSet;
import java.util.Set;

public enum AppointmentStatus {
    PENDING,
    APPROVED,
    DENIED;

    /** Statuses that occupy a capacity unit. New statuses must decide here. */
    public static final Set<AppointmentStatus> CAPACITY_CONSUMING = EnumSet.of(PENDING, APPROVED);

    public boolean consumesCapacity() {
        return CAPACITY_CONSUMING.contains(this);
    }
}
