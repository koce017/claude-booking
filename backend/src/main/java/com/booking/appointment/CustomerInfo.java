package com.booking.appointment;

import java.util.List;

/**
 * Contact details supplied with a booking. Name, phone and email have their own
 * columns; the address and answers to company-specific questions are stored as JSON.
 * Which fields are asked for and required is decided by {@link CustomerFieldPolicy}.
 */
public record CustomerInfo(String name, String phone, String email, String address, List<Answer> answers) {

    /** Answer to a company question; the question text is kept so later edits don't change history. */
    public record Answer(String key, String question, String answer) {
    }

    public CustomerInfo {
        answers = answers == null ? List.of() : List.copyOf(answers);
    }

    public CustomerInfo(String name, String phone, String email) {
        this(name, phone, email, null, List.of());
    }

    public String value(CustomerField field) {
        return switch (field) {
            case NAME -> name;
            case PHONE -> phone;
            case EMAIL -> email;
            case ADDRESS -> address;
        };
    }
}
