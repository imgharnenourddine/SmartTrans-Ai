package com.smarttrans.ai.mission.controller;

import static org.hamcrest.Matchers.containsString;
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
class MissionControllerIntegrationTest extends AbstractPostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void createMission_requiresClient_andSupportsStatusUpdate() throws Exception {
		MvcResult clientCreated = mockMvc.perform(post("/api/v1/clients")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"companyName", "Mission Client",
								"address", "Rabat",
								"phone", "+212537000000"))))
				.andExpect(status().isCreated())
				.andReturn();

		UUID clientId = UUID.fromString(
				objectMapper.readTree(clientCreated.getResponse().getContentAsString()).get("id").asText());

		String reference = "MIS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

		MvcResult missionCreated = mockMvc.perform(post("/api/v1/missions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"reference", reference,
								"clientId", clientId.toString(),
								"departureLocation", "Casablanca",
								"destination", "Fes",
								"scheduledAt", "2026-09-01T08:00:00Z"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reference").value(reference))
				.andExpect(jsonPath("$.status").value("PLANIFIED"))
				.andReturn();

		UUID missionId = UUID.fromString(
				objectMapper.readTree(missionCreated.getResponse().getContentAsString()).get("id").asText());

		mockMvc.perform(patch("/api/v1/missions/" + missionId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(post("/api/v1/missions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"reference", "MIS-X",
								"clientId", UUID.randomUUID().toString(),
								"departureLocation", "A",
								"destination", "B",
								"scheduledAt", "2026-09-01T08:00:00Z"))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.message", containsString("Client")));

		mockMvc.perform(get("/api/v1/missions/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}
}
