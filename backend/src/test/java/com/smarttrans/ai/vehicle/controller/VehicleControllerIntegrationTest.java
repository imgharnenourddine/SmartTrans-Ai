package com.smarttrans.ai.vehicle.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttrans.ai.support.AbstractPostgresIntegrationTest;
import java.time.LocalDate;
import java.util.HashMap;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Transactional
@WithMockUser(roles = "ADMIN")
class VehicleControllerIntegrationTest extends AbstractPostgresIntegrationTest {

	private static final String BASE = "/api/v1/vehicles";

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void createVehicle_happyPathConflictAndNotFound() throws Exception {
		String registration = uniqueRegistration();
		String payload = objectMapper.writeValueAsString(Map.of(
				"registrationNumber", registration,
				"type", "TRUCK",
				"mileageKm", 1000,
				"status", "AVAILABLE"));

		MvcResult created = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.registrationNumber").value(registration.toUpperCase()))
				.andReturn();

		UUID id = extractId(created);

		mockMvc.perform(get(BASE + "/" + id))
				.andExpect(status().isOk());

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", containsString("Registration")));

		mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete(BASE + "/" + id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(BASE + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
	}

	@Test
	void searchVehicles_withFilters() throws Exception {
		LocalDate today = LocalDate.now();
		UUID availableId = extractId(createVehicle(uniqueRegistration(), "AVAILABLE",
				today.plusYears(1), today.plusYears(1)));
		String maintenanceRegistration = uniqueRegistration();
		UUID maintenanceId = extractId(createVehicle(maintenanceRegistration, "MAINTENANCE",
				today.plusYears(1), today.plusYears(1)));
		UUID soonInsuranceId = extractId(createVehicle(uniqueRegistration(), "AVAILABLE",
				today.plusDays(10), today.plusYears(1)));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));

		mockMvc.perform(get(BASE).param("status", "AVAILABLE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get(BASE).param("availableOnly", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get(BASE).param("insuranceExpiringWithinDays", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(soonInsuranceId.toString()));

		mockMvc.perform(get(BASE).param("search", maintenanceRegistration.substring(6, 12)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(maintenanceId.toString()));

		mockMvc.perform(get(BASE).param("availableOnly", "true").param("status", "AVAILABLE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get(BASE).param("technicalVisitExpiringWithinDays", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void expiringDocuments_returnsSoonOrAlreadyExpired() throws Exception {
		LocalDate today = LocalDate.now();
		createVehicle(uniqueRegistration(), "AVAILABLE", today.plusDays(15), today.plusYears(1));
		createVehicle(uniqueRegistration(), "AVAILABLE", today.plusYears(1), today.minusDays(2));
		createVehicle(uniqueRegistration(), "AVAILABLE", today.plusYears(1), today.plusYears(1));

		mockMvc.perform(get(BASE + "/expiring-documents").param("withinDays", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get(BASE + "/expiring-documents").param("withinDays", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(get(BASE + "/expiring-documents").param("withinDays", "-1"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void updateVehicle_partialFieldsAndConflictWhenInUse() throws Exception {
		UUID id = extractId(createVehicle(uniqueRegistration(), "IN_USE",
				LocalDate.now().plusYears(1), LocalDate.now().plusYears(1)));

		mockMvc.perform(patch(BASE + "/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("mileageKm", 150000))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mileageKm").value(150000));

		mockMvc.perform(patch(BASE + "/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("status", "OUT_OF_SERVICE"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", containsString("in use")));

		mockMvc.perform(patch(BASE + "/" + id + "/deactivate"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", containsString("in use")));
	}

	@Test
	@WithMockUser(roles = "DRIVER")
	void driverRole_canReadButCannotWrite() throws Exception {
		UUID id = extractId(createVehicleAsAdmin(uniqueRegistration(), "AVAILABLE",
				LocalDate.now().plusYears(1), LocalDate.now().plusYears(1)));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk());

		mockMvc.perform(get(BASE + "/" + id))
				.andExpect(status().isOk());

		mockMvc.perform(get(BASE + "/expiring-documents"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"registrationNumber", uniqueRegistration(),
								"type", "TRUCK"))))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("mileageKm", 999))))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch(BASE + "/" + id + "/deactivate"))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete(BASE + "/" + id))
				.andExpect(status().isForbidden());
	}

	private MvcResult createVehicle(String registration, String status,
			LocalDate insuranceExpiresAt, LocalDate technicalVisitExpiresAt) throws Exception {
		return performCreate(registration, status, insuranceExpiresAt, technicalVisitExpiresAt, null);
	}

	private MvcResult createVehicleAsAdmin(String registration, String status,
			LocalDate insuranceExpiresAt, LocalDate technicalVisitExpiresAt) throws Exception {
		return performCreate(registration, status, insuranceExpiresAt, technicalVisitExpiresAt,
				user("admin").roles("ADMIN"));
	}

	private MvcResult performCreate(String registration, String status,
			LocalDate insuranceExpiresAt, LocalDate technicalVisitExpiresAt,
			RequestPostProcessor postProcessor) throws Exception {
		Map<String, Object> payload = new HashMap<>();
		payload.put("registrationNumber", registration);
		payload.put("type", "TRUCK");
		payload.put("status", status);
		payload.put("insuranceExpiresAt", insuranceExpiresAt.toString());
		payload.put("technicalVisitExpiresAt", technicalVisitExpiresAt.toString());

		var request = post(BASE).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload));
		if (postProcessor != null) {
			request = request.with(postProcessor);
		}
		return mockMvc.perform(request)
				.andExpect(status().isCreated())
				.andReturn();
	}

	private UUID extractId(MvcResult result) throws Exception {
		return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
	}

	private String uniqueRegistration() {
		return "12345-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}
}
