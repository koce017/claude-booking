package com.booking.appointment;

public enum CustomerField {
    NAME("name"),
    PHONE("phone"),
    EMAIL("email");

    private final String jsonName;

    CustomerField(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }
}
