package com.booking.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.booking.company.Company;
import com.booking.support.IntegrationTest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeeklyScheduleTest extends IntegrationTest {

    private static final LocalDate SATURDAY = MONDAY.plusDays(5);

    private String token;
    private Company company;

    @BeforeEach
    void setUp() throws Exception {
        token = adminToken();
        company = createCompany("Test Company");
    }

    private ResultActions putWeekly(Object days) throws Exception {
        return mvc.perform(put("/api/admin/companies/{id}/schedule/weekly", company.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("days", days))));
    }

    private ResultActions availability(LocalDate date) throws Exception {
        return mvc.perform(get("/api/companies/{id}/availability", company.getId()).param("date", date.toString()));
    }

    @Test
    void returnsDefaultScheduleUntilOneIsSaved() throws Exception {
        mvc.perform(get("/api/admin/companies/{id}/schedule/weekly", company.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(false))
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[0].day").value("MONDAY"))
                .andExpect(jsonPath("$.days[0].periods[0].start").value("09:00:00"))
                .andExpect(jsonPath("$.days[5].periods", hasSize(0)));
    }

    @Test
    void savedScheduleReplacesDefault() throws Exception {
        putWeekly(List.of(
                Map.of("day", "MONDAY", "periods", List.of(
                        Map.of("start", "08:00", "end", "12:00"),
                        Map.of("start", "13:00", "end", "15:00"))),
                Map.of("day", "SATURDAY", "periods", List.of(Map.of("start", "10:00", "end", "12:00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(true))
                .andExpect(jsonPath("$.days[0].periods", hasSize(2)))
                .andExpect(jsonPath("$.days[1].periods", hasSize(0)));

        availability(MONDAY)
                .andExpect(jsonPath("$.days[0].slots[?(@.status != 'NON_WORKING')].localStart")
                        .value(org.hamcrest.Matchers.contains("08:00", "09:00", "10:00", "11:00", "13:00", "14:00")));
        availability(SATURDAY).andExpect(jsonPath("$.days[0].working").value(true));
        availability(MONDAY.plusDays(1)).andExpect(jsonPath("$.days[0].working").value(false));
    }

    @Test
    void scheduleWithNoPeriodsClosesEveryDay() throws Exception {
        putWeekly(List.of()).andExpect(status().isOk()).andExpect(jsonPath("$.custom").value(true));
        availability(MONDAY).andExpect(jsonPath("$.days[0].working").value(false));
    }

    @Test
    void resetRestoresDefault() throws Exception {
        putWeekly(List.of()).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/companies/{id}/schedule/weekly", company.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(false));
        availability(MONDAY).andExpect(jsonPath("$.days[0].working").value(true));
    }

    @Test
    void rejectsOverlappingOrInvalidPeriods() throws Exception {
        putWeekly(List.of(Map.of("day", "MONDAY", "periods", List.of(
                Map.of("start", "08:00", "end", "12:00"),
                Map.of("start", "11:00", "end", "13:00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OVERLAPPING_PERIODS"));
        putWeekly(List.of(Map.of("day", "MONDAY", "periods", List.of(Map.of("start", "12:00", "end", "08:00")))))
                .andExpect(status().isBadRequest());
        putWeekly(List.of(Map.of("day", "MONDAY", "periods", List.of()), Map.of("day", "MONDAY", "periods", List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DAY"));
    }

    @Test
    void requiresAdmin() throws Exception {
        mvc.perform(get("/api/admin/companies/{id}/schedule/weekly", company.getId()))
                .andExpect(status().isUnauthorized());
    }
}
