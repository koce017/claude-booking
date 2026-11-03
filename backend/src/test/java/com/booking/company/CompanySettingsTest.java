package com.booking.company;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.booking.support.IntegrationTest;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanySettingsTest extends IntegrationTest {

    private String token;
    private Company company;

    @BeforeEach
    void setUp() throws Exception {
        token = adminToken();
        company = createCompany("Frizer Ana");
    }

    private Map<String, Object> settings() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Frizer Ana");
        return body;
    }

    private ResultActions update(Map<String, Object> body) throws Exception {
        return mvc.perform(put("/api/admin/companies/{id}", company.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    @Test
    void updatesSettingsAndReportsEffectiveValues() throws Exception {
        Map<String, Object> body = settings();
        body.put("name", "  Frizer Ana Novi Sad ");
        body.put("description", "Šišanje i feniranje");
        body.put("address", "Zmaj Jovina 1, Novi Sad");
        body.put("fulfillmentEmail", "termini@frizer-ana.rs");
        body.put("timezone", "Europe/London");
        body.put("appointmentDurationMinutes", 30);
        body.put("maxConcurrentAppointments", 2);

        update(body).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Frizer Ana Novi Sad"))
                .andExpect(jsonPath("$.fulfillmentEmail").value("termini@frizer-ana.rs"))
                .andExpect(jsonPath("$.slotIntervalMinutes").doesNotExist())
                .andExpect(jsonPath("$.effective.timezone").value("Europe/London"))
                .andExpect(jsonPath("$.effective.appointmentDurationMinutes").value(30))
                .andExpect(jsonPath("$.effective.slotIntervalMinutes").value(30))
                .andExpect(jsonPath("$.effective.maxConcurrentAppointments").value(2))
                .andExpect(jsonPath("$.effective.bookingWindowMonths").value(12));
    }

    @Test
    void blankValuesClearSettingsBackToDefaults() throws Exception {
        Map<String, Object> body = settings();
        body.put("timezone", "Europe/London");
        body.put("appointmentDurationMinutes", 30);
        update(body).andExpect(status().isOk());

        Map<String, Object> cleared = settings();
        cleared.put("timezone", " ");
        update(cleared).andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").doesNotExist())
                .andExpect(jsonPath("$.effective.timezone").value("Europe/Belgrade"))
                .andExpect(jsonPath("$.effective.appointmentDurationMinutes").value(60));
    }

    @Test
    void rejectsInvalidSettings() throws Exception {
        Map<String, Object> badZone = settings();
        badZone.put("timezone", "Mars/Olympus");
        update(badZone).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.timezone").exists());

        Map<String, Object> badDuration = settings();
        badDuration.put("appointmentDurationMinutes", 0);
        update(badDuration).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.appointmentDurationMinutes").exists());

        Map<String, Object> badEmail = settings();
        badEmail.put("fulfillmentEmail", "not-an-email");
        update(badEmail).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fulfillmentEmail").exists());

        Map<String, Object> noName = settings();
        noName.put("name", "");
        update(noName).andExpect(status().isBadRequest());
    }

    @Test
    void fulfillmentEmailIsNeverPublic() throws Exception {
        Map<String, Object> body = settings();
        body.put("email", "info@frizer-ana.rs");
        body.put("fulfillmentEmail", "tajno@frizer-ana.rs");
        update(body).andExpect(status().isOk());

        mvc.perform(get("/api/companies/{id}", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("info@frizer-ana.rs"))
                .andExpect(content().string(not(containsString("tajno@frizer-ana.rs"))));
        mvc.perform(get("/api/companies"))
                .andExpect(content().string(not(containsString("tajno@frizer-ana.rs"))));
    }

    @Test
    void customerFormIsPublishedAndQuestionKeysAreGenerated() throws Exception {
        Map<String, Object> body = settings();
        body.put("customerForm", Map.of(
                "phone", "OPTIONAL",
                "email", "REQUIRED",
                "address", "REQUIRED",
                "questions", List.of(
                        Map.of("label", "Dužina kose?", "required", true),
                        Map.of("key", "napomena", "label", "Napomena", "required", false))));
        update(body).andExpect(status().isOk())
                .andExpect(jsonPath("$.customerForm.questions[0].key").value("q1"))
                .andExpect(jsonPath("$.customerForm.questions[1].key").value("napomena"));

        mvc.perform(get("/api/companies/{id}", company.getId()))
                .andExpect(jsonPath("$.customerForm.phone").value("OPTIONAL"))
                .andExpect(jsonPath("$.customerForm.address").value("REQUIRED"))
                .andExpect(jsonPath("$.customerForm.questions[*].label").value(hasItems("Dužina kose?", "Napomena")));
    }

    @Test
    void duplicateQuestionKeysAreRejected() throws Exception {
        Map<String, Object> body = settings();
        body.put("customerForm", Map.of("questions", List.of(
                Map.of("key", "a", "label", "Prvo", "required", false),
                Map.of("key", "a", "label", "Drugo", "required", false))));
        update(body).andExpect(status().isBadRequest());
    }

    @Test
    void slotIntervalShorterThanDurationGivesOverlappingSlots() throws Exception {
        Map<String, Object> body = settings();
        body.put("appointmentDurationMinutes", 60);
        body.put("slotIntervalMinutes", 30);
        update(body).andExpect(status().isOk());

        mvc.perform(get("/api/companies/{id}/availability", company.getId()).param("date", MONDAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotIntervalMinutes").value(30))
                .andExpect(jsonPath("$.days[0].slots[?(@.status != 'NON_WORKING')].localStart")
                        .value(hasItems("09:00", "09:30", "10:00", "15:30", "16:00")))
                .andExpect(jsonPath("$.days[0].slots[?(@.localStart == '16:30')]").isEmpty());
    }

    @Test
    void overlappingSlotsShareCapacity() throws Exception {
        Map<String, Object> body = settings();
        body.put("slotIntervalMinutes", 30);
        update(body).andExpect(status().isOk());

        bookViaApi(company.getId(), at(MONDAY, 10, 0), "Ana");

        // 09:30-10:30 and 10:30-11:30 overlap the 10:00-11:00 booking; capacity is 1.
        mvc.perform(get("/api/companies/{id}/availability", company.getId()).param("date", MONDAY.toString()))
                .andExpect(jsonPath("$.days[0].slots[?(@.localStart == '09:30')].status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.days[0].slots[?(@.localStart == '10:30')].status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.days[0].slots[?(@.localStart == '11:00')].status").value("AVAILABLE"));
    }
}
