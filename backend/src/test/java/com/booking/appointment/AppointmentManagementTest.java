package com.booking.appointment;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.booking.company.Company;
import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentManagementTest extends IntegrationTest {

    @Autowired
    AppointmentRepository appointmentRepository;

    private String token;
    private Company company;

    @BeforeEach
    void setUp() throws Exception {
        token = adminToken();
        company = createCompany("Test Company");
    }

    private ResultActions action(long id, String action) throws Exception {
        return mvc.perform(post("/api/admin/appointments/{id}/" + action, id).header("Authorization", "Bearer " + token));
    }

    private ResultActions reschedule(long id, Instant start) throws Exception {
        return mvc.perform(post("/api/admin/appointments/{id}/reschedule", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("start", start.toString()))));
    }

    private ResultActions createByAdmin(Instant start, Map<String, Object> customer) throws Exception {
        return mvc.perform(post("/api/admin/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("companyId", company.getId(), "start", start.toString(),
                        "customer", customer, "adminNote", "Zakazano telefonom"))));
    }

    /** Moves the clock and signs in again, since the old session may have expired. */
    private void moveClockTo(Instant now) throws Exception {
        clock.set(now);
        token = adminToken();
    }

    private AppointmentStatus statusOf(long id) {
        return appointmentRepository.findById(id).orElseThrow().getStatus();
    }

    @Test
    void cancellingFreesCapacity() throws Exception {
        long approved = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        action(approved, "approve").andExpect(status().isOk());

        action(approved, "cancel").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Bojan");

        long denied = bookViaApi(company.getId(), at(MONDAY, 11, 0), "Ceca");
        action(denied, "deny").andExpect(status().isOk());
        action(denied, "cancel").andExpect(status().isConflict());
    }

    @Test
    void outcomeCanOnlyBeRecordedOnceTheAppointmentStarted() throws Exception {
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        action(id, "approve").andExpect(status().isOk());

        action(id, "complete").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_STARTED"));

        moveClockTo(at(MONDAY, 10, 30));
        action(id, "complete").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        action(id, "no-show").andExpect(status().isConflict());
    }

    @Test
    void noShowOnlyForApproved() throws Exception {
        long pending = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        moveClockTo(at(MONDAY, 11, 0));
        action(pending, "no-show").andExpect(status().isConflict());

        moveClockTo(DEFAULT_NOW);
        long approved = bookViaApi(company.getId(), at(MONDAY, 12, 0), "Bojan");
        action(approved, "approve").andExpect(status().isOk());
        moveClockTo(at(MONDAY, 13, 0));
        action(approved, "no-show").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    @Test
    void rescheduleMovesAppointmentAndChecksCapacity() throws Exception {
        long ana = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        action(ana, "approve").andExpect(status().isOk());
        bookViaApi(company.getId(), at(MONDAY, 12, 0), "Bojan");

        reschedule(ana, at(MONDAY, 12, 0)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        reschedule(ana, at(MONDAY, 12, 30)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_A_WORKING_SLOT"));

        reschedule(ana, at(MONDAY, 14, 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.start").value(at(MONDAY, 14, 0).toString()));

        // The old slot is free again.
        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ceca");
    }

    @Test
    void rescheduleToOwnOverlappingSlotIsAllowed() throws Exception {
        Company c = createCompany("Interval", 1);
        c.setSlotIntervalMinutes(30);
        companyRepository.save(c);
        long id = bookViaApi(c.getId(), at(MONDAY, 10, 0), "Ana");

        reschedule(id, at(MONDAY, 10, 30)).andExpect(status().isOk());
    }

    @Test
    void adminCreatesApprovedAppointmentWithOnlyAName() throws Exception {
        createByAdmin(at(MONDAY, 10, 0), Map.of("name", "Telefonski klijent"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.source").value("ADMIN"))
                .andExpect(jsonPath("$.adminNote").value("Zakazano telefonom"))
                .andExpect(jsonPath("$.companyTimezone").value("Europe/Belgrade"));

        createByAdmin(at(MONDAY, 10, 0), Map.of("name", "Drugi")).andExpect(status().isConflict());
        createByAdmin(at(MONDAY, 11, 0), Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['customer.name']").exists());
        createByAdmin(at(MONDAY, 10, 0).minus(Duration.ofDays(7)), Map.of("name", "Prošlost"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/companies/{id}/availability", company.getId()).param("date", MONDAY.toString()))
                .andExpect(jsonPath("$.days[0].slots[?(@.localStart == '10:00')].status").value(contains("UNAVAILABLE")));
    }

    @Test
    void noteCanBeChanged() throws Exception {
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        mvc.perform(put("/api/admin/appointments/{id}/note", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "  Zvati pre dolaska "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").value("Zvati pre dolaska"));
        assertThat(appointmentRepository.findById(id).orElseThrow().getAdminNote()).isEqualTo("Zvati pre dolaska");
    }

    @Test
    void listFiltersByTimeRangeForCalendar() throws Exception {
        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        bookViaApi(company.getId(), at(MONDAY.plusDays(1), 10, 0), "Bojan");
        bookViaApi(company.getId(), at(MONDAY.plusDays(7), 10, 0), "Ceca");

        mvc.perform(get("/api/admin/appointments")
                        .header("Authorization", "Bearer " + token)
                        .param("from", at(MONDAY, 0, 0).toString())
                        .param("to", at(MONDAY.plusDays(7), 0, 0).toString())
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].customer.name").value(contains("Ana", "Bojan")));
        assertThat(statusOf(1)).isEqualTo(AppointmentStatus.PENDING);
    }
}
