package com.booking.appointment;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.booking.company.Company;
import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingTest extends IntegrationTest {

    @Autowired
    AppointmentRepository appointmentRepository;

    private ResultActions book(Long companyId, Object body) throws Exception {
        return mvc.perform(post("/api/companies/{id}/appointments", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body instanceof String s ? s : json(body)));
    }

    @Test
    void validBookingCreatesPendingAppointmentWithDerivedEnd() throws Exception {
        Company company = createCompany("Test Company");

        book(company.getId(), Map.of(
                "start", "2026-10-12T10:00:00+02:00",
                // a client-supplied end is ignored; the company duration decides
                "end", "2026-10-12T13:00:00+02:00",
                "customer", Map.of("name", "Ana", "phone", "+381 60 1234567", "email", "ana@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.start").value("2026-10-12T08:00:00Z"))
                .andExpect(jsonPath("$.end").value("2026-10-12T09:00:00Z"))
                .andExpect(jsonPath("$.customer").doesNotExist());

        Appointment saved = appointmentRepository.findAll().get(0);
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(Duration.between(saved.getStartAt(), saved.getEndAt())).isEqualTo(Duration.ofHours(1));
        assertThat(saved.getCustomer()).isEqualTo(new CustomerInfo("Ana", "+381 60 1234567", "ana@example.com"));
    }

    @Test
    void bookingOutsideWorkingHoursFails() throws Exception {
        Company company = createCompany("Test Company");

        book(company.getId(), bookingJson(at(MONDAY, 8, 0), "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_A_WORKING_SLOT"));
        // 16:30 would end after 17:00 (and is misaligned)
        book(company.getId(), bookingJson(at(MONDAY, 16, 30), "Ana"))
                .andExpect(status().isBadRequest());
        // Saturday
        book(company.getId(), bookingJson(at(LocalDate.of(2026, 10, 17), 10, 0), "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_A_WORKING_SLOT"));
        assertThat(appointmentRepository.count()).isZero();
    }

    @Test
    void bookingMisalignedSlotFails() throws Exception {
        Company company = createCompany("Test Company");
        book(company.getId(), bookingJson(at(MONDAY, 9, 30), "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_A_WORKING_SLOT"));
    }

    @Test
    void bookingOnSpecialClosedDateFails() throws Exception {
        Company company = createCompany("Test Company");
        jdbc.update("INSERT INTO schedule_override (company_id, override_date) VALUES (?, ?)",
                company.getId(), MONDAY);
        book(company.getId(), bookingJson(at(MONDAY, 10, 0), "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_A_WORKING_SLOT"));
    }

    @Test
    void bookingInThePastFails() throws Exception {
        Company company = createCompany("Test Company");
        clock.set(at(MONDAY, 10, 30));

        book(company.getId(), bookingJson(at(MONDAY, 10, 0), "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SLOT_IN_PAST"));
        book(company.getId(), bookingJson(at(MONDAY, 11, 0), "Ana"))
                .andExpect(status().isCreated());
    }

    @Test
    void bookingBeyondBookingWindowFails() throws Exception {
        Company company = createCompany("Test Company");

        book(company.getId(), bookingJson(at(LocalDate.of(2027, 10, 29), 10, 0), "Ana"))
                .andExpect(status().isCreated());
        book(company.getId(), bookingJson(at(LocalDate.of(2027, 11, 1), 10, 0), "Ana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OUTSIDE_BOOKING_WINDOW"));
    }

    @Test
    void bookingUnavailableSlotFailsWithConflict() throws Exception {
        Company company = createCompany("Test Company");
        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");

        book(company.getId(), bookingJson(at(MONDAY, 10, 0), "Bob"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        // another time is still bookable
        book(company.getId(), bookingJson(at(MONDAY, 11, 0), "Bob"))
                .andExpect(status().isCreated());
    }

    @Test
    void bookingInactiveCompanyFails() throws Exception {
        Company company = createCompany("Test Company");
        company.deactivate();
        companyRepository.save(company);

        book(company.getId(), bookingJson(at(MONDAY, 10, 0), "Ana"))
                .andExpect(status().isNotFound());
        book(999L, bookingJson(at(MONDAY, 10, 0), "Ana"))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerInformationIsRequiredAndValidated() throws Exception {
        Company company = createCompany("Test Company");

        book(company.getId(), Map.of("start", at(MONDAY, 10, 0).toString(),
                "customer", Map.of("name", " ", "email", "ana@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['customer.name']").exists())
                .andExpect(jsonPath("$.errors['customer.phone']").exists());
        book(company.getId(), Map.of("start", at(MONDAY, 10, 0).toString(),
                "customer", Map.of("name", "Ana", "phone", "123", "email", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['customer.email']").exists());
        book(company.getId(), Map.of("start", at(MONDAY, 10, 0).toString()))
                .andExpect(status().isBadRequest());
        book(company.getId(), "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        assertThat(appointmentRepository.count()).isZero();
    }
}
