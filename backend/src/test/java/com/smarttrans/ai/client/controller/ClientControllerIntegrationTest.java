package com.smarttrans.ai.client.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttrans.ai.support.AbstractPostgresIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Transactional
@WithMockUser(roles = "ADMIN")
class ClientControllerIntegrationTest extends AbstractPostgresIntegrationTest {

	private static final String BASE = "/api/v1/clients";

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void createGetUpdateDeleteClient_happyPathAndErrors() throws Exception {
		MvcResult created = mockMvc.perform(post(BASE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"companyName", "Atlas Logistics",
								"address", "Casablanca",
								"phone", "+212522000000",
								"email", "contact@atlas.test"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.companyName").value("Atlas Logistics"))
				.andReturn();

		UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

		mockMvc.perform(get(BASE + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));

		mockMvc.perform(patch(BASE + "/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("phone", "+212522000001"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phone").value("+212522000001"));

		mockMvc.perform(post(BASE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"companyName", "",
								"address", "x",
								"phone", "y"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));

		mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", containsString("Client")));

		mockMvc.perform(delete(BASE + "/" + id))
				.andExpect(status().isNoContent());
	}
}
