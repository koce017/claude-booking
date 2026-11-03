package com.booking.appointment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.booking.common.error.FieldValidationException;
import com.booking.company.Company;
import com.booking.company.CompanySettingsResolver;
import com.booking.company.CustomerFormConfig;
import com.booking.company.CustomerFormConfig.CustomQuestion;
import com.booking.company.CustomerFormConfig.FieldRequirement;

/**
 * Applies a company's customer form settings to submitted customer data: hidden
 * fields are dropped, answers are matched to the company's questions, and required
 * fields are enforced. The name is always required.
 */
@Component
public class CustomerFieldPolicy {

    private final CompanySettingsResolver settingsResolver;

    public CustomerFieldPolicy(CompanySettingsResolver settingsResolver) {
        this.settingsResolver = settingsResolver;
    }

    /** Raw customer data as submitted, before the company's form settings are applied. */
    public record SubmittedCustomer(String name, String phone, String email, String address,
                                    Map<String, String> answers) {

        public SubmittedCustomer(String name, String phone, String email) {
            this(name, phone, email, null, Map.of());
        }
    }

    /** Validates a customer's own booking: every required field must be filled in. */
    public CustomerInfo validate(Company company, SubmittedCustomer submitted) {
        return apply(company, submitted, true);
    }

    /** For bookings entered by an administrator: only the name is required. */
    public CustomerInfo validateLenient(Company company, SubmittedCustomer submitted) {
        return apply(company, submitted, false);
    }

    private CustomerInfo apply(Company company, SubmittedCustomer submitted, boolean enforceRequired) {
        CustomerFormConfig form = settingsResolver.resolve(company).customerForm();
        Map<String, String> errors = new LinkedHashMap<>();

        String name = clean(submitted.name());
        if (name == null) {
            errors.put("customer.name", "is required");
        }
        String phone = field(form.phone(), submitted.phone(), "customer.phone", enforceRequired, errors);
        String email = field(form.email(), submitted.email(), "customer.email", enforceRequired, errors);
        String address = field(form.address(), submitted.address(), "customer.address", enforceRequired, errors);

        Map<String, String> given = submitted.answers() == null ? Map.of() : submitted.answers();
        List<CustomerInfo.Answer> answers = new ArrayList<>();
        for (CustomQuestion q : form.questions()) {
            String answer = clean(given.get(q.key()));
            if (answer == null) {
                if (q.required() && enforceRequired) {
                    errors.put("customer.answers." + q.key(), "is required");
                }
                continue;
            }
            if (answer.length() > 1000) {
                errors.put("customer.answers." + q.key(), "must be at most 1000 characters");
                continue;
            }
            answers.add(new CustomerInfo.Answer(q.key(), q.label(), answer));
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return new CustomerInfo(name, phone, email, address, answers);
    }

    private static String field(FieldRequirement requirement, String value, String errorKey,
                                boolean enforceRequired, Map<String, String> errors) {
        if (requirement == FieldRequirement.HIDDEN) {
            return null;
        }
        String cleaned = clean(value);
        if (cleaned == null && requirement == FieldRequirement.REQUIRED && enforceRequired) {
            errors.put(errorKey, "is required");
        }
        return cleaned;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
