package com.booking.appointment;

/**
 * Contact details supplied with a booking. Fixed fields for the MVP; which of them
 * are required is decided by {@link CustomerFieldPolicy}, so configurable or extra
 * fields can be added without touching the booking flow.
 */
public record CustomerInfo(String name, String phone, String email) {

    public String value(CustomerField field) {
        return switch (field) {
            case NAME -> name;
            case PHONE -> phone;
            case EMAIL -> email;
        };
    }
}
