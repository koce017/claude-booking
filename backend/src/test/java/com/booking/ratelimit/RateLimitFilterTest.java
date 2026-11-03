package com.booking.ratelimit;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.booking.company.Company;
import com.booking.support.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "booking.rate-limit.enabled=true",
        "booking.rate-limit.login-link.requests=2",
        "booking.rate-limit.login-link.window=15m",
        "booking.rate-limit.booking.requests=1",
        "booking.rate-limit.booking.window=1h"})
class RateLimitFilterTest extends IntegrationTest {

    @Test
    void tooManyLoginLinkRequestsGet429() throws Exception {
        String body = json(Map.of("email", "someone@example.com"));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/admin/auth/request-link").contentType(MediaType.APPLICATION_JSON).content(body)
                            .with(r -> { r.setRemoteAddr("10.0.0.1"); return r; }))
                    .andExpect(status().isAccepted());
        }
        mvc.perform(post("/api/admin/auth/request-link").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(r -> { r.setRemoteAddr("10.0.0.1"); return r; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        // Other clients are not affected.
        mvc.perform(post("/api/admin/auth/request-link").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(r -> { r.setRemoteAddr("10.0.0.2"); return r; }))
                .andExpect(status().isAccepted());
    }

    @Test
    void bookingsAreLimitedButReadingIsNot() throws Exception {
        Company company = createCompany("Test Company");
        mvc.perform(post("/api/companies/{id}/appointments", company.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(at(MONDAY, 10, 0), "Ana"))
                        .with(r -> { r.setRemoteAddr("10.0.0.3"); return r; }))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/companies/{id}/appointments", company.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(at(MONDAY, 11, 0), "Ana"))
                        .with(r -> { r.setRemoteAddr("10.0.0.3"); return r; }))
                .andExpect(status().isTooManyRequests());
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/companies/{id}/availability", company.getId()).param("date", MONDAY.toString())
                            .with(r -> { r.setRemoteAddr("10.0.0.3"); return r; }))
                    .andExpect(status().isOk());
        }
    }
}
