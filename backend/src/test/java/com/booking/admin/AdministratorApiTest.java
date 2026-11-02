package com.booking.admin;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.booking.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdministratorApiTest extends IntegrationTest {

    @Test
    void addListAndRemoveAdministrators() throws Exception {
        String token = adminToken();
        String body = mvc.perform(post("/api/admin/administrators").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email", "Marko@Example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("marko@example.com"))
                .andReturn().getResponse().getContentAsString();
        long marko = objectMapper.readTree(body).get("id").asLong();

        mvc.perform(post("/api/admin/administrators").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email", "marko@example.com"))))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/admin/administrators").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[*].email").value(contains("admin@example.com", "marko@example.com")));

        // The new administrator can sign in.
        adminAuthService.requestLink("marko@example.com");
        String markoToken = adminAuthService.verify(magicLinks.last().token()).token();

        mvc.perform(delete("/api/admin/administrators/{id}", marko).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        // Their session ends immediately.
        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + markoToken))
                .andExpect(status().isUnauthorized());
        assertThat(administratorRepository.findById(marko).orElseThrow().isActive()).isFalse();
    }

    @Test
    void cannotRemoveYourself() throws Exception {
        String token = adminToken();
        long self = administratorRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow().getId();
        mvc.perform(delete("/api/admin/administrators/{id}", self).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_REMOVE_SELF"));
    }
}
