package com.booking.appointment;

/** Built-in customer fields. Company-specific questions are handled separately as answers. */
public enum CustomerField {
    NAME("name"),
    PHONE("phone"),
    EMAIL("email"),
    ADDRESS("address");

    private final String jsonName;

    CustomerField(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }
}
