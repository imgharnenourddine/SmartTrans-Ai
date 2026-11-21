package com.smarttrans.ai.assistant;

import com.smarttrans.ai.ServiceTestSupport;
import com.smarttrans.ai.assistant.AssistantService.AskRequest;
import com.smarttrans.ai.config.ExternalConfig;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantServiceTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final ExternalConfig config = mock(ExternalConfig.class);
    private final HttpClient httpClient = mock(HttpClient.class);
    private AssistantService assistant;

    @BeforeEach
    void setUp() {
        assistant = new AssistantService();
        ServiceTestSupport.inject(assistant, entityManager, config, httpClient);
        ServiceTestSupport.stubQueries(entityManager, Map.of("select count(m) from Mission m", 7L));
        when(config.mistralApiKey()).thenReturn(Optional.empty());
        when(config.mistralBaseUrl()).thenReturn("http://llm.test");
        when(config.mistralModel()).thenReturn("mistral-small-latest");
        when(config.mistralRequestTimeoutMs()).thenReturn(12_000L);
    }

    @Test
    void countQuestionIsAnsweredFromTheDatabaseWhenNoApiKeyIsConfigured() throws Exception {
        var response = assistant.ask(new AskRequest("Combien de missions ?"));

        assertEquals("7 missions", response.answer());
        assertEquals("GROUNDED", response.mode());
        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }

    @Test
    void questionWithoutOperationalDataReturnsTheGroundedFallback() {
        ServiceTestSupport.stubQueries(entityManager, Map.of());

        var response = assistant.ask(new AskRequest("xyzabc"));

        assertEquals(
                "Je n’ai pas trouvé d’information dans les missions, chauffeurs, véhicules ou clients pour cette question.",
                response.answer());
        assertEquals("GROUNDED", response.mode());
    }

    @Test
    void languageModelAnswerReplacesTheGroundedAnswer() throws Exception {
        when(config.mistralApiKey()).thenReturn(Optional.of("test-key"));
        HttpResponse<String> httpResponse = response(200, """
                {"choices":[{"message":{"content":"  Le parc compte 7 missions.  "}}]}
                """);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        var response = assistant.ask(new AskRequest("Combien de missions ?"));

        assertEquals("Le parc compte 7 missions.", response.answer());
        assertEquals("LLM", response.mode());
    }

    @Test
    void languageModelFailureFallsBackToTheGroundedAnswer() throws Exception {
        when(config.mistralApiKey()).thenReturn(Optional.of("test-key"));
        doThrow(new IOException("unavailable")).when(httpClient).send(any(HttpRequest.class), any());

        var response = assistant.ask(new AskRequest("Combien de missions ?"));

        assertEquals("7 missions", response.answer());
        assertEquals("GROUNDED", response.mode());
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
