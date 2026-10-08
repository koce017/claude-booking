package com.booking.appointment;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;

public final class AppointmentDtos {

    private AppointmentDtos() {
    }

    /**
     * Public booking request. Only the start is given; the end is derived from the
     * company's appointment duration.
     */
    public record BookingRequest(
            @NotNull OffsetDateTime start,
            @NotNull @Valid CustomerRequest customer) {
    }

    /** Format checks only; which fields are required is decided by CustomerFieldPolicy. */
    public record CustomerRequest(
            @Size(max = 200) String name,
            @Size(max = 50) @Pattern(regexp = "^[0-9+()\\-./ ]*$", message = "must be a valid phone number") String phone,
            @Size(max = 320) @Email String email) {

        public CustomerInfo toCustomerInfo() {
            return new CustomerInfo(trim(name), trim(phone), trim(email));
        }

        private static String trim(String s) {
            return s == null ? null : s.trim();
        }
    }

    /** Returned to the person who booked. Deliberately excludes customer data. */
    public record BookingResponse(Long id, Long companyId, Instant start, Instant end, AppointmentStatus status) {

        public static BookingResponse from(Appointment a) {
            return new BookingResponse(a.getId(), a.getCompany().getId(), a.getStartAt(), a.getEndAt(), a.getStatus());
        }
    }

    public record CustomerResponse(String name, String phone, String email) {
    }

    public record AdminAppointmentResponse(
            Long id,
            Long companyId,
            String companyName,
            Instant start,
            Instant end,
            AppointmentStatus status,
            CustomerResponse customer,
            Instant createdAt,
            Instant updatedAt) {

        public static AdminAppointmentResponse from(Appointment a) {
            CustomerInfo c = a.getCustomer();
            return new AdminAppointmentResponse(a.getId(), a.getCompany().getId(), a.getCompany().getName(),
                    a.getStartAt(), a.getEndAt(), a.getStatus(),
                    new CustomerResponse(c.name(), c.phone(), c.email()), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }
}
