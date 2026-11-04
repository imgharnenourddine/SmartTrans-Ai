package com.smarttrans.ai.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class OpenAiChatClient {

	private final AssistantProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public OpenAiChatClient(AssistantProperties properties) {
		this.properties = properties;
		this.objectMapper = new ObjectMapper();
		String baseUrl = properties.openaiBaseUrl() == null || properties.openaiBaseUrl().isBlank()
				? "https://api.openai.com/v1"
				: properties.openaiBaseUrl();
		this.restClient = RestClient.builder().baseUrl(baseUrl).build();
	}

	public boolean enabled() {
		return properties.openaiApiKey() != null && !properties.openaiApiKey().isBlank();
	}

	public Optional<String> complete(String systemPrompt, String userPrompt) {
		if (!enabled()) {
			return Optional.empty();
		}
		try {
			String model = properties.openaiModel() == null || properties.openaiModel().isBlank()
					? "gpt-4o-mini"
					: properties.openaiModel();
			Map<String, Object> body = Map.of(
					"model", model,
					"temperature", 0.2,
					"messages", List.of(
							Map.of("role", "system", "content", systemPrompt),
							Map.of("role", "user", "content", userPrompt)));
			String raw = restClient.post()
					.uri("/chat/completions")
					.contentType(MediaType.APPLICATION_JSON)
					.headers(headers -> headers.setBearerAuth(properties.openaiApiKey()))
					.body(body)
					.retrieve()
					.body(String.class);
			if (raw == null || raw.isBlank()) {
				return Optional.empty();
			}
			JsonNode content = objectMapper.readTree(raw).path("choices").path(0).path("message").path("content");
			if (!content.isTextual() || content.asText().isBlank()) {
				return Optional.empty();
			}
			return Optional.of(content.asText().trim());
		} catch (Exception ex) {
			log.warn("OpenAI assistant call failed: {}", ex.getMessage());
			return Optional.empty();
		}
	}
}
