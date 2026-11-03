package com.booking.company;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.booking.appointment.AppointmentRepository;
import com.booking.appointment.AppointmentStatus;
import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanyApiTest extends IntegrationTest {

    @Autowired
    AppointmentRepository appointmentRepository;

    @Test
    void adminCreatesCompanyWithNameOnly() throws Exception {
        String token = adminToken();

        mvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "  Test Company "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Test Company"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.appointmentDurationMinutes").doesNotExist());

        mvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Test Company"));
    }

    @Test
    void blankNameIsRejected() throws Exception {
        mvc.perform(post("/api/admin/companies")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void creatingRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/admin/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "X"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        assertThat(companyRepository.count()).isZero();
    }

    @Test
    void publicCanRetrieveCompany() throws Exception {
        Company company = createCompany("Acme");

        mvc.perform(get("/api/companies/{id}", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
        mvc.perform(get("/api/companies/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void removedCompanyDisappearsFromPublicButHistoryRemains() throws Exception {
        Company company = createCompany("Acme");
        long pendingId = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        long approvedId = bookViaApi(company.getId(), at(MONDAY, 11, 0), "Bob");
        String token = adminToken();
        mvc.perform(post("/api/admin/appointments/{id}/approve", approvedId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/companies/{id}", company.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/companies")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/companies/{id}", company.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/companies/{id}/availability", company.getId()).param("date", MONDAY.toString()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/companies/{id}/appointments", company.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(at(MONDAY, 12, 0), "Cid")))
                .andExpect(status().isNotFound());

        // Soft delete: the row and its appointments remain; pending requests are denied.
        assertThat(companyRepository.findById(company.getId())).get()
                .extracting(Company::isActive).isEqualTo(false);
        assertThat(appointmentRepository.findById(pendingId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.DENIED);
        assertThat(appointmentRepository.findById(approvedId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.APPROVED);

        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/admin/companies").param("includeInactive", "true").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void removingUnknownCompanyIs404() throws Exception {
        mvc.perform(delete("/api/admin/companies/{id}", 42).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }
}
