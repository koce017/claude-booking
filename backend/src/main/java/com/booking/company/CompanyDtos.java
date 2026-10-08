package com.booking.company;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CreateCompanyRequest(
            @NotBlank(message = "must not be blank") @Size(max = 200) String name) {
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
            String website) {

        public static PublicCompanyResponse from(Company c) {
            return new PublicCompanyResponse(c.getId(), c.getName(), c.getDescription(), c.getLogoUrl(),
                    c.getAddress(), c.getPhone(), c.getEmail(), c.getWebsite());
        }
    }

    /** Admin view of a company, including raw (possibly unset) settings. */
    public record AdminCompanyResponse(
            Long id,
            String name,
            boolean active,
            String timezone,
            Integer appointmentDurationMinutes,
            Integer maxConcurrentAppointments,
            Integer bookingWindowMonths,
            Instant createdAt,
            Instant updatedAt) {

        public static AdminCompanyResponse from(Company c) {
            return new AdminCompanyResponse(c.getId(), c.getName(), c.isActive(), c.getTimezone(),
                    c.getAppointmentDurationMinutes(), c.getMaxConcurrentAppointments(),
                    c.getBookingWindowMonths(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
