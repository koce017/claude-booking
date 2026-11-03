package com.booking.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.booking.company.Company;
import com.booking.company.CustomerFormConfig;
import com.booking.company.CustomerFormConfig.CustomQuestion;
import com.booking.company.CustomerFormConfig.FieldRequirement;
import com.booking.support.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerFieldsTest extends IntegrationTest {

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company("Servis");
        company.setCustomerForm(new CustomerFormConfig(FieldRequirement.HIDDEN, FieldRequirement.OPTIONAL,
                FieldRequirement.REQUIRED, List.of(
                        new CustomQuestion("model", "Model uređaja", true),
                        new CustomQuestion("opis", "Opis kvara", false))));
        company = companyRepository.save(company);
    }

    private ResultActions book(Instant start, Map<String, Object> customer) throws Exception {
        return mvc.perform(post("/api/companies/{id}/appointments", company.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("start", start.toString(), "customer", customer))));
    }

    @Test
    void requiredFieldsAndQuestionsAreEnforced() throws Exception {
        book(at(MONDAY, 10, 0), Map.of("name", "Marko"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['customer.address']").value("is required"))
                .andExpect(jsonPath("$.errors['customer.answers.model']").value("is required"))
                .andExpect(jsonPath("$.errors['customer.email']").doesNotExist())
                .andExpect(jsonPath("$.errors['customer.phone']").doesNotExist())
                .andExpect(jsonPath("$.errors['customer.answers.opis']").doesNotExist());
    }

    @Test
    void storesAddressAndAnswersAndDropsHiddenFields() throws Exception {
        String body = book(at(MONDAY, 10, 0), Map.of(
                        "name", "Marko",
                        "phone", "060 111 222",
                        "address", "Bulevar 5",
                        "answers", Map.of("model", "Bosch X1", "nepoznato", "ignored")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mvc.perform(get("/api/admin/appointments/{id}", id).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("Marko"))
                .andExpect(jsonPath("$.customer.phone").doesNotExist())
                .andExpect(jsonPath("$.customer.address").value("Bulevar 5"))
                .andExpect(jsonPath("$.customer.answers.length()").value(1))
                .andExpect(jsonPath("$.customer.answers[0].question").value("Model uređaja"))
                .andExpect(jsonPath("$.customer.answers[0].answer").value("Bosch X1"));
    }

    @Test
    void defaultFormStillRequiresNamePhoneAndEmail() throws Exception {
        Company plain = createCompany("Plain");
        mvc.perform(post("/api/companies/{id}/appointments", plain.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("start", at(MONDAY, 10, 0).toString(), "customer", Map.of("name", "A")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['customer.phone']").value("is required"))
                .andExpect(jsonPath("$.errors['customer.email']").value("is required"));
    }
}
