package com.smarttrans.ai.user.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttrans.ai.support.AbstractPostgresIntegrationTest;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class UserControllerIntegrationTest extends AbstractPostgresIntegrationTest {

	private static final String BASE = "/api/v1/users";

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void createGetUpdateDeleteUser_happyPath() throws Exception {
		String email = "alice.manager+" + UUID.randomUUID() + "@smarttrans.test";

		MvcResult created = mockMvc.perform(post(BASE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"email", email,
								"password", "Password1!",
								"firstName", "Alice",
								"lastName", "Manager",
								"phone", "+212600000001",
								"roleCodes", Set.of("TRANSPORT_MANAGER")))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.email").value(email.toLowerCase()))
				.andExpect(jsonPath("$.firstName").value("Alice"))
				.andExpect(jsonPath("$.roleCodes", hasItem("TRANSPORT_MANAGER")))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();

		UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

		mockMvc.perform(get(BASE + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.email").value(email.toLowerCase()));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + id + "')]").exists());

		mockMvc.perform(patch(BASE + "/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"firstName", "Alicia",
								"enabled", false))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Alicia"))
				.andExpect(jsonPath("$.enabled").value(false));

		mockMvc.perform(delete(BASE + "/" + id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(BASE + "/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message", containsString("User")));
	}

	@Test
	void createUser_withInvalidPayload_returns400() throws Exception {
		mockMvc.perform(post(BASE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"email", "not-an-email",
								"password", "short",
								"firstName", "",
								"lastName", "Doe",
								"roleCodes", Set.of()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.fieldErrors").isMap());
	}

	@Test
	void createUser_withDuplicateEmail_returns409() throws Exception {
		String email = "dup+" + UUID.randomUUID() + "@smarttrans.test";
		String payload = objectMapper.writeValueAsString(Map.of(
				"email", email,
				"password", "Password1!",
				"firstName", "Dup",
				"lastName", "User",
				"roleCodes", Set.of("ADMIN")));

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isCreated());

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message", containsString("Email")));
	}

	@Test
	void getUser_whenMissing_returns404() throws Exception {
		mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void openApiDocs_areAvailable() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("SmartTrans AI API"))
				.andExpect(jsonPath("$.paths./api/v1/users").exists());
	}
}
