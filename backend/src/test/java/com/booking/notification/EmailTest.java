package com.booking.notification;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.booking.appointment.AppointmentRepository;
import com.booking.company.Company;
import com.booking.mail.OutgoingMail;
import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Customer and admin notifications and fulfillment emails to companies. */
class EmailTest extends IntegrationTest {

    private static final String COMPANY_MAIL = "termini@frizer.rs";
    private static final String CUSTOMER_MAIL = "ana@example.com";
    private static final String ADMIN_MAIL = "admin@example.com";

    @Autowired
    AppointmentRepository appointmentRepository;

    private String token;
    private Company company;

    @BeforeEach
    void setUp() throws Exception {
        token = adminToken();
        company = new Company("Frizer Ana");
        company.setFulfillmentEmail(COMPANY_MAIL);
        company.setAddress("Zmaj Jovina 1");
        company = companyRepository.save(company);
        mails.clear();
    }

    private ResultActions action(long id, String action) throws Exception {
        return mvc.perform(post("/api/admin/appointments/{id}/" + action, id).header("Authorization", "Bearer " + token));
    }

    private OutgoingMail only(String to) {
        assertThat(mails.sentTo(to)).hasSize(1);
        return mails.sentTo(to).get(0);
    }

    @Test
    void requestNotifiesCustomerAndAdministrators() throws Exception {
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");

        OutgoingMail customer = only(CUSTOMER_MAIL);
        assertThat(customer.subject()).isEqualTo("Primili smo vaš zahtev za termin – Frizer Ana");
        assertThat(customer.body())
                .contains("Poštovani/a Ana,")
                .contains("ponedeljak, 12.10.2026.")
                .contains("10:00–11:00")
                .contains("Broj zahteva: " + id);

        OutgoingMail admin = only(ADMIN_MAIL);
        assertThat(admin.subject()).startsWith("Novi zahtev za termin – Frizer Ana");
        assertThat(admin.body()).contains("+381 60 1234567").contains("/admin/appointments?id=" + id);

        assertThat(mails.sentTo(COMPANY_MAIL)).isEmpty();
    }

    @Test
    void approvalEmailsCompanyAndCustomerAndMarksDelivered() throws Exception {
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        mails.clear();

        action(id, "approve").andExpect(status().isOk());

        OutgoingMail toCompany = only(COMPANY_MAIL);
        assertThat(toCompany.subject()).isEqualTo("Novi potvrđeni termin – ponedeljak, 12.10.2026. 10:00");
        assertThat(toCompany.body()).contains("Klijent: Ana").contains(CUSTOMER_MAIL);
        assertThat(only(CUSTOMER_MAIL).subject()).isEqualTo("Vaš termin je potvrđen – Frizer Ana");
        assertThat(appointmentRepository.findById(id).orElseThrow().getFulfilledAt()).isEqualTo(DEFAULT_NOW);
    }

    @Test
    void withoutFulfillmentEmailTheAppointmentStaysUndelivered() throws Exception {
        Company plain = createCompany("Bez emaila");
        long id = bookViaApi(plain.getId(), at(MONDAY, 10, 0), "Ana");

        action(id, "approve").andExpect(status().isOk()).andExpect(jsonPath("$.fulfilledAt").doesNotExist());
        assertThat(appointmentRepository.findById(id).orElseThrow().getFulfilledAt()).isNull();
        assertThat(mails.sentTo(CUSTOMER_MAIL)).extracting(OutgoingMail::subject)
                .contains("Vaš termin je potvrđen – Bez emaila");
    }

    @Test
    void denialAndCompanyRemovalNotifyCustomer() throws Exception {
        long denied = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        mails.clear();
        action(denied, "deny").andExpect(status().isOk());
        assertThat(only(CUSTOMER_MAIL).subject()).isEqualTo("Zahtev za termin nije prihvaćen – Frizer Ana");

        bookViaApi(company.getId(), at(MONDAY, 11, 0), "Ana");
        mails.clear();
        mvc.perform(delete("/api/admin/companies/{id}", company.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(only(CUSTOMER_MAIL).subject()).isEqualTo("Zahtev za termin nije prihvaćen – Frizer Ana");
        assertThat(mails.sentTo(COMPANY_MAIL)).isEmpty();
    }

    @Test
    void cancellingADeliveredAppointmentTellsTheCompany() throws Exception {
        long approved = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        action(approved, "approve").andExpect(status().isOk());
        long pending = bookViaApi(company.getId(), at(MONDAY, 11, 0), "Ana");
        mails.clear();

        action(pending, "cancel").andExpect(status().isOk());
        assertThat(mails.sentTo(COMPANY_MAIL)).isEmpty();
        assertThat(only(CUSTOMER_MAIL).subject()).isEqualTo("Termin je otkazan – Frizer Ana");

        mails.clear();
        action(approved, "cancel").andExpect(status().isOk());
        assertThat(only(COMPANY_MAIL).subject()).isEqualTo("Otkazan termin – ponedeljak, 12.10.2026. 10:00");
    }

    @Test
    void reschedulingNotifiesBothWithOldAndNewTime() throws Exception {
        long id = bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");
        action(id, "approve").andExpect(status().isOk());
        mails.clear();

        Instant newStart = at(MONDAY.plusDays(1), 14, 0);
        mvc.perform(post("/api/admin/appointments/{id}/reschedule", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("start", newStart.toString()))))
                .andExpect(status().isOk());

        assertThat(only(COMPANY_MAIL).body())
                .contains("Prethodni termin: ponedeljak, 12.10.2026. u 10:00")
                .contains("Novi termin: utorak, 13.10.2026., 14:00–15:00");
        assertThat(only(CUSTOMER_MAIL).subject()).isEqualTo("Termin je pomeren – Frizer Ana");
    }

    @Test
    void adminCreatedAppointmentIsDeliveredRightAway() throws Exception {
        mvc.perform(post("/api/admin/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("companyId", company.getId(), "start", at(MONDAY, 9, 0).toString(),
                                "customer", Map.of("name", "Telefonski klijent"), "adminNote", "Doneti dokumenta"))))
                .andExpect(status().isCreated());

        assertThat(only(COMPANY_MAIL).body()).contains("Telefonski klijent").contains("Napomena: Doneti dokumenta");
        assertThat(mails.sentTo(ADMIN_MAIL)).isEmpty();
    }
}
