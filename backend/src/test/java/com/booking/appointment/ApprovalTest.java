package com.booking.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

import com.booking.company.Company;
import com.booking.fulfillment.AppointmentFulfillmentService;
import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApprovalTest extends IntegrationTest {

    @MockitoSpyBean
    AppointmentFulfillmentService fulfillmentService;

    @Autowired
    AppointmentRepository appointmentRepository;

    private String token;

    private ResultActions action(long id, String action) throws Exception {
        if (token == null) {
            token = adminToken();
        }
        return mvc.perform(post("/api/admin/appointments/{id}/" + action, id).header("Authorization", "Bearer " + token));
    }

    private AppointmentStatus statusOf(long id) {
        return appointmentRepository.findById(id).orElseThrow().getStatus();
    }

    @Test
    void pendingToApprovedInvokesFulfillment() throws Exception {
        reset(fulfillmentService);
        Company company = createCompany("Test Company");
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");

        action(id, "approve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.customer.name").value("Ana"));

        assertThat(statusOf(id)).isEqualTo(AppointmentStatus.APPROVED);
        verify(fulfillmentService).fulfill(argThat(a -> a.getId() == id));
    }

    @Test
    void fulfillmentFailureDoesNotUndoApproval() throws Exception {
        reset(fulfillmentService);
        doThrow(new RuntimeException("downstream down")).when(fulfillmentService).fulfill(any());
        Company company = createCompany("Test Company");
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");

        action(id, "approve").andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo(AppointmentStatus.APPROVED);
        reset(fulfillmentService);
    }

    @Test
    void pendingToDeniedFreesCapacity() throws Exception {
        reset(fulfillmentService);
        Company company = createCompany("Test Company");
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");

        action(id, "deny").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DENIED"));

        assertThat(statusOf(id)).isEqualTo(AppointmentStatus.DENIED);
        verify(fulfillmentService, never()).fulfill(any());
        // the slot can be requested again
        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Bob");
    }

    @Test
    void approvedCannotBeApprovedOrDeniedAgain() throws Exception {
        Company company = createCompany("Test Company");
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        action(id, "approve").andExpect(status().isOk());

        action(id, "approve").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        action(id, "deny").andExpect(status().isConflict());
        assertThat(statusOf(id)).isEqualTo(AppointmentStatus.APPROVED);
    }

    @Test
    void deniedCannotBeApproved() throws Exception {
        Company company = createCompany("Test Company");
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        action(id, "deny").andExpect(status().isOk());

        action(id, "approve").andExpect(status().isConflict());
        assertThat(statusOf(id)).isEqualTo(AppointmentStatus.DENIED);
    }

    @Test
    void approvalCannotExceedCapacity() throws Exception {
        Company company = createCompany("Test Company", 2);
        long first = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        long second = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Bob");
        // capacity is later reduced to 1, leaving two pending requests for one place
        company.setMaxConcurrentAppointments(1);
        companyRepository.save(company);

        action(first, "approve").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));
        action(second, "deny").andExpect(status().isOk());
        action(first, "approve").andExpect(status().isOk());
    }

    @Test
    void pastAppointmentCannotBeApproved() throws Exception {
        Company company = createCompany("Test Company");
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        clock.set(at(MONDAY, 10, 0));

        action(id, "approve").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_IN_PAST"));
        action(id, "deny").andExpect(status().isOk());
    }

    @Test
    void unknownAppointmentIs404() throws Exception {
        action(12345, "approve").andExpect(status().isNotFound());
    }

    @Test
    void adminCanListAndFilterAppointments() throws Exception {
        Company a = createCompany("A");
        Company b = createCompany("B");
        long a1 = bookViaApi(a.getId(), at(MONDAY, 10, 0), "Ana");
        bookViaApi(a.getId(), at(MONDAY, 11, 0), "Bob");
        bookViaApi(b.getId(), at(MONDAY, 10, 0), "Cid");
        action(a1, "approve").andExpect(status().isOk());

        mvc.perform(get("/api/admin/appointments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/admin/appointments").param("companyId", a.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].customer.email").value("ana@example.com"));
        mvc.perform(get("/api/admin/appointments").param("companyId", a.getId().toString())
                        .param("status", "PENDING").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].customer.name").value("Bob"));
        mvc.perform(get("/api/admin/appointments/{id}", a1).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.companyName").value("A"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/api/admin/appointments").param("status", "NOPE").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/appointments"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/appointments/{id}/approve", a1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
