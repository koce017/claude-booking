package com.booking.admin;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuthTest extends IntegrationTest {

    @Autowired
    AdminLoginTokenRepository loginTokenRepository;

    private void requestLink(String email) throws Exception {
        mvc.perform(post("/api/admin/auth/request-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isAccepted());
    }

    private String verify(String token) throws Exception {
        String body = mvc.perform(post("/api/admin/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    void magicLinkLoginFlow() throws Exception {
        administratorRepository.save(new Administrator("admin@example.com"));

        requestLink("Admin@Example.com");
        assertThat(magicLinks.sent()).hasSize(1);
        assertThat(magicLinks.last().link()).startsWith("http://localhost:5173/admin/verify?token=");
        String session = verify(magicLinks.last().token());

        mvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + session))
                .andExpect(status().isOk());
    }

    @Test
    void tokensAreStoredHashed() throws Exception {
        administratorRepository.save(new Administrator("admin@example.com"));
        requestLink("admin@example.com");
        String token = magicLinks.last().token();

        String stored = jdbc.queryForObject("SELECT token_hash FROM admin_login_token", String.class);
        assertThat(stored).isNotEqualTo(token).hasSize(64);
        assertThat(token.length()).isGreaterThanOrEqualTo(43);
    }

    @Test
    void unknownEmailGetsSameResponseButNoLink() throws Exception {
        requestLink("nobody@example.com");
        assertThat(magicLinks.sent()).isEmpty();
        assertThat(loginTokenRepository.count()).isZero();
    }

    @Test
    void tokenIsSingleUse() throws Exception {
        administratorRepository.save(new Administrator("admin@example.com"));
        requestLink("admin@example.com");
        String token = magicLinks.last().token();
        verify(token);

        mvc.perform(post("/api/admin/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_LOGIN_TOKEN"));
    }

    @Test
    void tokenExpires() throws Exception {
        administratorRepository.save(new Administrator("admin@example.com"));
        requestLink("admin@example.com");
        clock.advance(Duration.ofMinutes(16));

        mvc.perform(post("/api/admin/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", magicLinks.last().token()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mvc.perform(post("/api/admin/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", "made-up"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionExpiresAndCanBeRevoked() throws Exception {
        String session = adminToken();
        mvc.perform(post("/api/admin/auth/logout").header("Authorization", "Bearer " + session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + session))
                .andExpect(status().isUnauthorized());

        String second = adminToken();
        clock.advance(Duration.ofHours(13));
        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + second))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsRequireValidBearerToken() throws Exception {
        mvc.perform(get("/api/admin/companies")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/appointments").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
        // public endpoints need nothing
        mvc.perform(get("/api/companies")).andExpect(status().isOk());
    }
}
