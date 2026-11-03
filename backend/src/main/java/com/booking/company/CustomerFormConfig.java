package com.booking.company;

import java.util.List;

/**
 * Which customer fields a company's booking form asks for. The name is always
 * required; phone, email and address can each be required, optional or hidden, and
 * a company can add its own questions. Stored as JSON on the company row.
 */
public record CustomerFormConfig(
        FieldRequirement phone,
        FieldRequirement email,
        FieldRequirement address,
        List<CustomQuestion> questions) {

    public enum FieldRequirement {
        REQUIRED,
        OPTIONAL,
        HIDDEN
    }

    /**
     * @param key   stable identifier the answers are submitted under
     * @param label question text shown to the customer
     */
    public record CustomQuestion(String key, String label, boolean required) {
    }

    /** Name, phone and email required; no address, no extra questions. */
    public static final CustomerFormConfig DEFAULT =
            new CustomerFormConfig(FieldRequirement.REQUIRED, FieldRequirement.REQUIRED, FieldRequirement.HIDDEN, List.of());

    public CustomerFormConfig {
        phone = phone == null ? FieldRequirement.REQUIRED : phone;
        email = email == null ? FieldRequirement.REQUIRED : email;
        address = address == null ? FieldRequirement.HIDDEN : address;
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
