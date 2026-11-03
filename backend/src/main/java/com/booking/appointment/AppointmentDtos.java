package com.booking.appointment;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;

import com.booking.appointment.CustomerFieldPolicy.SubmittedCustomer;

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

    /**
     * Format checks only; which fields are asked for and required is decided by
     * {@link CustomerFieldPolicy} from the company's settings.
     *
     * @param answers answers to the company's own questions, by question key
     */
    public record CustomerRequest(
            @Size(max = 200) String name,
            @Size(max = 50) @Pattern(regexp = "^[0-9+()\\-./ ]*$", message = "must be a valid phone number") String phone,
            @Size(max = 320) @Email String email,
            @Size(max = 500) String address,
            @Size(max = 20) Map<String, String> answers) {

        public SubmittedCustomer toSubmitted() {
            return new SubmittedCustomer(name, phone, email, address, answers);
        }
    }

    /** An appointment entered by an administrator. */
    public record AdminBookingRequest(
            @NotNull Long companyId,
            @NotNull OffsetDateTime start,
            @NotNull @Valid CustomerRequest customer,
            @Size(max = 1000) String adminNote) {
    }

    public record RescheduleRequest(@NotNull OffsetDateTime start) {
    }

    public record NoteRequest(@Size(max = 1000) String adminNote) {
    }

    /** Returned to the person who booked. Deliberately excludes customer data. */
    public record BookingResponse(Long id, Long companyId, Instant start, Instant end, AppointmentStatus status) {

        public static BookingResponse from(Appointment a) {
            return new BookingResponse(a.getId(), a.getCompany().getId(), a.getStartAt(), a.getEndAt(), a.getStatus());
        }
    }

    public record CustomerResponse(String name, String phone, String email, String address,
                                   List<CustomerInfo.Answer> answers) {

        static CustomerResponse from(CustomerInfo c) {
            return new CustomerResponse(c.name(), c.phone(), c.email(), c.address(), c.answers());
        }
    }

    /**
     * @param companyTimezone the company's time zone; admin pages show times in it
     * @param fulfilledAt     when the appointment was last emailed to the company;
     *                        null for an approved appointment means it must be handed over manually
     */
    public record AdminAppointmentResponse(
            Long id,
            Long companyId,
            String companyName,
            String companyTimezone,
            Instant start,
            Instant end,
            AppointmentStatus status,
            AppointmentSource source,
            CustomerResponse customer,
            String adminNote,
            Instant fulfilledAt,
            Instant createdAt,
            Instant updatedAt) {

        public static AdminAppointmentResponse from(Appointment a, String companyTimezone) {
            return new AdminAppointmentResponse(a.getId(), a.getCompany().getId(), a.getCompany().getName(),
                    companyTimezone, a.getStartAt(), a.getEndAt(), a.getStatus(), a.getSource(),
                    CustomerResponse.from(a.getCustomer()), a.getAdminNote(), a.getFulfilledAt(),
                    a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }
}
