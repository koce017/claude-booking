package com.booking.appointment;

import com.booking.common.error.ConflictException;

public class InvalidStatusTransitionException extends ConflictException {

    public InvalidStatusTransitionException(AppointmentStatus from, AppointmentStatus to) {
        super("INVALID_STATUS_TRANSITION", "Cannot change appointment from " + from + " to " + to);
    }
}
