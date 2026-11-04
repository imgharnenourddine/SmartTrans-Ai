package com.smarttrans.ai.assistant.controller;

import static org.hamcrest.Matchers.containsString;
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
class AssistantControllerIntegrationTest extends AbstractPostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void answersFromMissionData() throws Exception {
		MvcResult clientCreated = mockMvc.perform(post("/api/v1/clients")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"companyName", "Atlas Freight",
								"address", "Rabat",
								"phone", "+212537000111"))))
				.andExpect(status().isCreated())
				.andReturn();
		UUID clientId = UUID.fromString(
				objectMapper.readTree(clientCreated.getResponse().getContentAsString()).get("id").asText());
		mockMvc.perform(post("/api/v1/missions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"reference", "MIS-AI-42",
								"clientId", clientId.toString(),
								"departureLocation", "Casablanca",
								"destination", "Fes",
								"scheduledAt", "2026-09-01T08:00:00Z"))))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/assistant/ask")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("message", "Où va la mission MIS-AI-42 ?"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mode").value("GROUNDED"))
				.andExpect(jsonPath("$.answer", containsString("Fes")))
				.andExpect(jsonPath("$.citations[0].kind").value("MISSION"));

		mockMvc.perform(post("/api/v1/assistant/ask")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("message", "Combien de missions"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.answer").value("1 mission"));
	}

	@Test
	void suggestsAvailableDriverAndVehicle() throws Exception {
		mockMvc.perform(post("/api/v1/drivers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"firstName", "Nadia",
								"lastName", "ElFassi",
								"phone", "+212600000099",
								"licenseNumber", "MA-" + UUID.randomUUID().toString().substring(0, 8),
								"licenseExpiresAt", "2028-01-01",
								"experienceYears", 4,
								"status", "AVAILABLE"))))
				.andExpect(status().isCreated());

		String registration = "AI-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
		mockMvc.perform(post("/api/v1/vehicles")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"registrationNumber", registration,
								"type", "TRUCK",
								"mileageKm", 1200,
								"status", "AVAILABLE",
								"insuranceExpiresAt", "2028-06-01",
								"technicalVisitExpiresAt", "2028-06-01"))))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/assistant/ask")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"message", "Quel chauffeur et véhicule puis-je affecter ?"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mode").value("GROUNDED"))
				.andExpect(jsonPath("$.answer", containsString("Nadia")))
				.andExpect(jsonPath("$.answer", containsString(registration.toUpperCase())));
	}

	@Test
	@WithMockUser(roles = "DRIVER")
	void driverCannotAskAssistant() throws Exception {
		mockMvc.perform(post("/api/v1/assistant/ask")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("message", "Combien de missions ?"))))
				.andExpect(status().isForbidden());
	}
}
