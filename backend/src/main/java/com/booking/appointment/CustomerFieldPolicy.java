package com.booking.appointment;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.booking.common.error.FieldValidationException;
import com.booking.company.Company;

/**
 * Decides which customer fields a company requires and validates a booking's
 * customer info against them. Today every company requires name, phone and email;
 * per-company configuration plugs in here.
 */
@Component
public class CustomerFieldPolicy {

    private static final Set<CustomerField> DEFAULT_REQUIRED = EnumSet.allOf(CustomerField.class);

    public Set<CustomerField> requiredFields(Company company) {
        return DEFAULT_REQUIRED;
    }

    public void validate(Company company, CustomerInfo customer) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (CustomerField field : requiredFields(company)) {
            String value = customer.value(field);
            if (value == null || value.isBlank()) {
                errors.put("customer." + field.jsonName(), "is required");
            }
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }
}
