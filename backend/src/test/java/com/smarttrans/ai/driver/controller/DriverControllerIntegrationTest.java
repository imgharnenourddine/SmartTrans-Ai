package com.smarttrans.ai.driver.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.role.entity.Role;
import com.smarttrans.ai.role.repository.RoleRepository;
import com.smarttrans.ai.support.AbstractPostgresIntegrationTest;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Transactional
class DriverControllerIntegrationTest extends AbstractPostgresIntegrationTest {

	private static final String BASE = "/api/v1/drivers";
	private static final String LOGIN = "/api/v1/auth/login";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	@WithMockUser(roles = "ADMIN")
	void createSearchUpdateDeactivateDriver_happyPath() throws Exception {
		MvcResult created = mockMvc.perform(post(BASE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"firstName", "Youssef",
								"lastName", "Benali",
								"phone", "+212600000001",
								"licenseNumber", "MA-" + UUID.randomUUID().toString().substring(0, 8),
								"licenseExpiresAt", LocalDate.now().plusYears(2).toString(),
								"experienceYears", 5,
								"status", "AVAILABLE"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.firstName").value("Youssef"))
				.andExpect(jsonPath("$.licenseExpired").value(false))
				.andExpect(jsonPath("$.availableForMission").value(true))
				.andReturn();

		UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

		mockMvc.perform(get(BASE + "?search=youssef&status=AVAILABLE&availableOnly=true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(id.toString()));

		mockMvc.perform(patch(BASE + "/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("phone", "+212600000002"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phone").value("+212600000002"));

		mockMvc.perform(patch(BASE + "/" + id + "/availability")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("status", "UNAVAILABLE"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNAVAILABLE"))
				.andExpect(jsonPath("$.availableForMission").value(false));

		mockMvc.perform(patch(BASE + "/" + id + "/deactivate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUSPENDED"));

		mockMvc.perform(get(BASE + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUSPENDED"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createDriver_withExpiredLicense_flagsComputedFields() throws Exception {
		mockMvc.perform(post(BASE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"firstName", "Expired",
								"lastName", "Driver",
								"phone", "+212600000003",
								"licenseNumber", "EXP-" + UUID.randomUUID().toString().substring(0, 8),
								"licenseExpiresAt", LocalDate.now(ZoneOffset.UTC).minusDays(1).toString(),
								"status", "UNAVAILABLE"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.licenseExpired").value(true))
				.andExpect(jsonPath("$.availableForMission").value(false));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void updateAvailability_withExpiredLicense_returns422() throws Exception {
		MvcResult created = mockMvc.perform(post(BASE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"firstName", "Blocked",
								"lastName", "Availability",
								"phone", "+212600000004",
								"licenseNumber", "BLK-" + UUID.randomUUID().toString().substring(0, 8),
								"licenseExpiresAt", LocalDate.now(ZoneOffset.UTC).minusDays(2).toString(),
								"status", "UNAVAILABLE"))))
				.andExpect(status().isCreated())
				.andReturn();

		UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

		mockMvc.perform(patch(BASE + "/" + id + "/availability")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("status", "AVAILABLE"))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.message", containsString("expired")));
	}

	@Test
	@WithMockUser(roles = "DRIVER")
	void driverRole_cannotAccessAdminEndpoints() throws Exception {
		mockMvc.perform(get(BASE))
				.andExpect(status().isForbidden());

		mockMvc.perform(get(BASE + "/expiring-licenses"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void getDriver_notFound_returns404() throws Exception {
		mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", containsString("Driver")));
	}

	@Test
	void driverJwt_canReadOwnProfile_andIsForbiddenOnOthers() throws Exception {
		String password = "Password1!";
		User driverUser = seedUser("driver+" + UUID.randomUUID() + "@smarttrans.test", password, RoleCode.DRIVER);
		UUID driverId = createDriverAsAdmin(driverUser.getId(), "Karim", "Idrissi");
		UUID otherDriverId = createDriverAsAdmin(null, "Other", "Driver");

		String token = loginAndGetToken(driverUser.getEmail(), password);

		mockMvc.perform(get(BASE + "/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(driverId.toString()))
				.andExpect(jsonPath("$.firstName").value("Karim"));

		mockMvc.perform(get(BASE + "/" + driverId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(driverId.toString()));

		mockMvc.perform(get(BASE + "/" + otherDriverId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden());

		mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	private UUID createDriverAsAdmin(UUID userId, String firstName, String lastName) throws Exception {
		Map<String, Object> payload = new java.util.HashMap<>();
		payload.put("firstName", firstName);
		payload.put("lastName", lastName);
		payload.put("phone", "+2126000000" + UUID.randomUUID().toString().substring(0, 2));
		payload.put("licenseNumber", "DRV-" + UUID.randomUUID().toString().substring(0, 8));
		payload.put("licenseExpiresAt", LocalDate.now().plusYears(1).toString());
		if (userId != null) {
			payload.put("userId", userId.toString());
		}

		MvcResult created = mockMvc.perform(post(BASE)
						.with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isCreated())
				.andReturn();

		return UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());
	}

	private String loginAndGetToken(String email, String password) throws Exception {
		MvcResult loginResult = mockMvc.perform(post(LOGIN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"email", email,
								"password", password))))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		return body.get("accessToken").asText();
	}

	private User seedUser(String email, String password, RoleCode roleCode) {
		Role role = roleRepository.findByCode(roleCode)
				.orElseThrow(() -> new IllegalStateException("Missing seeded role: " + roleCode));

		User user = User.builder()
				.email(email.trim().toLowerCase())
				.passwordHash(passwordEncoder.encode(password))
				.firstName("Test")
				.lastName("User")
				.enabled(true)
				.roles(Set.of(role))
				.build();
		return userRepository.save(user);
	}
}
