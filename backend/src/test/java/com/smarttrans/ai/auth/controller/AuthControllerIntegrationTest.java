package com.smarttrans.ai.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttrans.ai.activity.ActivityEntityType;
import com.smarttrans.ai.activity.entity.Activity;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.repository.ActivityRepository;
import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.role.entity.Role;
import com.smarttrans.ai.role.repository.RoleRepository;
import com.smarttrans.ai.support.AbstractPostgresIntegrationTest;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Transactional
class AuthControllerIntegrationTest extends AbstractPostgresIntegrationTest {

	private static final String LOGIN = "/api/v1/auth/login";
	private static final String USERS = "/api/v1/users";
	private static final String VEHICLES = "/api/v1/vehicles";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ActivityRepository activityRepository;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void login_thenAccessProtectedEndpoint_withJwt() throws Exception {
		String email = "auth.admin+" + UUID.randomUUID() + "@smarttrans.test";
		String password = "Password1!";
		seedUser(email, password, RoleCode.ADMIN);

		MvcResult loginResult = mockMvc.perform(post(LOGIN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"email", email,
								"password", password))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInMs").isNumber())
				.andExpect(jsonPath("$.email").value(email.toLowerCase()))
				.andExpect(jsonPath("$.firstName").value("Auth"))
				.andExpect(jsonPath("$.roleCodes", hasItem("ADMIN")))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();

		JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String token = body.get("accessToken").asText();
		UUID userId = UUID.fromString(body.get("userId").asText());

		activityRepository.flush();
		assertThat(activityRepository.findByActor_IdOrderByCreatedAtDesc(userId))
				.anySatisfy(activity -> {
					assertThat(activity.getAction()).isEqualTo(ActivityAction.LOGIN);
					assertThat(activity.getEntityType()).isEqualTo(ActivityEntityType.USER);
					assertThat(activity.getEntityId()).isEqualTo(userId);
					assertThat(activity.getMetadata()).containsEntry("email", email.toLowerCase());
				});

		String registration = "JWT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
		MvcResult vehicleCreated = mockMvc.perform(post(VEHICLES)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"registrationNumber", registration,
								"type", "VAN",
								"mileageKm", 10,
								"status", "AVAILABLE"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.registrationNumber").value(registration))
				.andReturn();

		UUID vehicleId = UUID.fromString(
				objectMapper.readTree(vehicleCreated.getResponse().getContentAsString()).get("id").asText());
		activityRepository.flush();
		assertThat(activityRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
						ActivityEntityType.VEHICLE, vehicleId))
				.extracting(Activity::getAction)
				.contains(ActivityAction.CREATE);

		mockMvc.perform(get(VEHICLES).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void login_withInvalidCredentials_returns401() throws Exception {
		mockMvc.perform(post(LOGIN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"email", "missing+" + UUID.randomUUID() + "@smarttrans.test",
								"password", "Password1!"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message", containsString("Invalid")));
	}

	@Test
	void protectedEndpoint_withoutToken_returns401() throws Exception {
		mockMvc.perform(get(VEHICLES))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	@WithMockUser(roles = "DRIVER")
	void adminEndpoint_withDriverRole_returns403() throws Exception {
		mockMvc.perform(get(USERS))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	private void seedUser(String email, String password, RoleCode roleCode) {
		Role role = roleRepository.findByCode(roleCode)
				.orElseThrow(() -> new IllegalStateException("Missing seeded role: " + roleCode));

		User user = User.builder()
				.email(email.trim().toLowerCase())
				.passwordHash(passwordEncoder.encode(password))
				.firstName("Auth")
				.lastName("Admin")
				.enabled(true)
				.roles(Set.of(role))
				.build();
		userRepository.save(user);
	}
}
