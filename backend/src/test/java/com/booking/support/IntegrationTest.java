package com.booking.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.booking.admin.AdminAuthService;
import com.booking.admin.Administrator;
import com.booking.admin.AdministratorRepository;
import com.booking.company.Company;
import com.booking.company.CompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for tests that run against a real PostgreSQL database (booking_test by
 * default, see application-test.yml). Every test starts with empty tables and the
 * clock set to {@link #DEFAULT_NOW}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public abstract class IntegrationTest {

    public static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");

    /** Thursday 2026-10-08 10:00 in Belgrade. */
    public static final Instant DEFAULT_NOW = Instant.parse("2026-10-08T08:00:00Z");

    /** A future Monday (working day under the default schedule). */
    public static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected CapturingMagicLinkSender magicLinks;

    @Autowired
    protected CompanyRepository companyRepository;

    @Autowired
    protected AdministratorRepository administratorRepository;

    @Autowired
    protected AdminAuthService adminAuthService;

    @BeforeEach
    void resetState() {
        jdbc.execute("""
                TRUNCATE appointment, schedule_override_period, schedule_override, weekly_schedule_period,
                         company, admin_session, admin_login_token, administrator RESTART IDENTITY CASCADE""");
        clock.set(DEFAULT_NOW);
        magicLinks.clear();
    }

    protected Company createCompany(String name) {
        return companyRepository.save(new Company(name));
    }

    protected Company createCompany(String name, int capacity) {
        Company company = new Company(name);
        company.setMaxConcurrentAppointments(capacity);
        return companyRepository.save(company);
    }

    /** Company-local date and time in Europe/Belgrade as an instant. */
    protected static Instant at(LocalDate date, int hour, int minute) {
        return date.atTime(LocalTime.of(hour, minute)).atZone(ZONE).toInstant();
    }

    /** Creates an administrator and logs in through the real magic-link flow. */
    protected String adminToken() throws Exception {
        String email = "admin@example.com";
        if (administratorRepository.findByEmailIgnoreCase(email).isEmpty()) {
            administratorRepository.save(new Administrator(email));
        }
        adminAuthService.requestLink(email);
        return adminAuthService.verify(magicLinks.last().token()).token();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected String bookingJson(Instant start, String name) throws Exception {
        return json(Map.of(
                "start", start.toString(),
                "customer", Map.of("name", name, "phone", "+381 60 1234567", "email", name.toLowerCase() + "@example.com")));
    }

    protected long bookViaApi(Long companyId, Instant start, String name) throws Exception {
        String body = mvc.perform(post("/api/companies/{id}/appointments", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(start, name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
