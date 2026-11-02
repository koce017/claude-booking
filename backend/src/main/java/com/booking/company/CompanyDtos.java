package com.booking.company;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.booking.company.CustomerFormConfig.FieldRequirement;

public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CreateCompanyRequest(
            @NotBlank(message = "must not be blank") @Size(max = 200) String name) {
    }

    /**
     * Replaces every editable setting of a company. A null or blank value clears the
     * setting, so the application default applies again.
     */
    public record UpdateCompanyRequest(
            @NotBlank(message = "must not be blank") @Size(max = 200) String name,
            @Size(max = 5000) String description,
            @Size(max = 1000) String logoUrl,
            @Size(max = 500) String address,
            @Size(max = 50) String phone,
            @Size(max = 320) @Email String email,
            @Size(max = 1000) String website,
            @Size(max = 320) @Email String fulfillmentEmail,
            @Size(max = 64) String timezone,
            @Min(5) @Max(1440) Integer appointmentDurationMinutes,
            @Min(5) @Max(1440) Integer slotIntervalMinutes,
            @Min(1) @Max(1000) Integer maxConcurrentAppointments,
            @Min(1) @Max(60) Integer bookingWindowMonths,
            @Valid CustomerFormRequest customerForm) {
    }

    public record CustomerFormRequest(
            FieldRequirement phone,
            FieldRequirement email,
            FieldRequirement address,
            @Valid @Size(max = 20) List<QuestionRequest> questions) {
    }

    /** {@code key} may be left empty for a new question; one is generated. */
    public record QuestionRequest(
            @Size(max = 40) @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "may contain only letters, digits, - and _")
            String key,
            @NotBlank(message = "must not be blank") @Size(max = 200) String label,
            boolean required) {
    }

    /** Public view of a company. Contains only information meant for customers. */
    public record PublicCompanyResponse(
            Long id,
            String name,
            String description,
            String logoUrl,
            String address,
            String phone,
            String email,
            String website,
            String timezone,
            CustomerFormConfig customerForm) {

        public static PublicCompanyResponse from(Company c, EffectiveCompanySettings settings) {
            return new PublicCompanyResponse(c.getId(), c.getName(), c.getDescription(), c.getLogoUrl(),
                    c.getAddress(), c.getPhone(), c.getEmail(), c.getWebsite(), settings.zone().getId(),
                    settings.customerForm());
        }
    }

    /** Settings in effect after defaults are applied. */
    public record EffectiveSettingsResponse(
            String timezone,
            int appointmentDurationMinutes,
            int slotIntervalMinutes,
            int maxConcurrentAppointments,
            int bookingWindowMonths) {

        static EffectiveSettingsResponse from(EffectiveCompanySettings s) {
            return new EffectiveSettingsResponse(s.zone().getId(), (int) s.appointmentDuration().toMinutes(),
                    (int) s.slotInterval().toMinutes(), s.maxConcurrentAppointments(), s.bookingWindowMonths());
        }
    }

    /** Admin view of a company: raw (possibly unset) settings plus the values in effect. */
    public record AdminCompanyResponse(
            Long id,
            String name,
            boolean active,
            String description,
            String logoUrl,
            String address,
            String phone,
            String email,
            String website,
            String fulfillmentEmail,
            String timezone,
            Integer appointmentDurationMinutes,
            Integer slotIntervalMinutes,
            Integer maxConcurrentAppointments,
            Integer bookingWindowMonths,
            CustomerFormConfig customerForm,
            boolean customWeeklySchedule,
            EffectiveSettingsResponse effective,
            Instant createdAt,
            Instant updatedAt) {

        public static AdminCompanyResponse from(Company c, EffectiveCompanySettings settings) {
            return new AdminCompanyResponse(c.getId(), c.getName(), c.isActive(), c.getDescription(), c.getLogoUrl(),
                    c.getAddress(), c.getPhone(), c.getEmail(), c.getWebsite(), c.getFulfillmentEmail(),
                    c.getTimezone(), c.getAppointmentDurationMinutes(), c.getSlotIntervalMinutes(),
                    c.getMaxConcurrentAppointments(), c.getBookingWindowMonths(), settings.customerForm(),
                    c.hasCustomWeeklySchedule(), EffectiveSettingsResponse.from(settings),
                    c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
