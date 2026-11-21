package com.smarttrans.ai.assistant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.smarttrans.ai.ServiceTestSupport;
import com.smarttrans.ai.assistant.AssistantService.AskRequest;
import com.smarttrans.ai.config.ExternalConfig;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MistralChatWireMockTest {

    private static final String COMPLETION = "/v1/chat/completions";
    private static final String TEST_KEY = "test-key";

    private WireMockServer mistral;
    private ExternalConfig config;
    private AssistantService assistant;

    @BeforeEach
    void setUp() {
        mistral = new WireMockServer(wireMockConfig().dynamicPort().notifier(new ConsoleNotifier(false)));
        mistral.start();
        EntityManager entityManager = org.mockito.Mockito.mock(EntityManager.class);
        config = org.mockito.Mockito.mock(ExternalConfig.class);
        assistant = new AssistantService();
        ServiceTestSupport.inject(assistant, entityManager, config);
        ServiceTestSupport.stubQueries(entityManager, Map.of("select count(m) from Mission m", 7L));
        org.mockito.Mockito.when(config.mistralApiKey()).thenReturn(Optional.of(TEST_KEY));
        org.mockito.Mockito.when(config.mistralBaseUrl()).thenReturn("http://127.0.0.1:" + mistral.port() + "/v1");
        org.mockito.Mockito.when(config.mistralModel()).thenReturn("mistral-small-latest");
        org.mockito.Mockito.when(config.mistralRequestTimeoutMs()).thenReturn(12_000L);
    }

    @AfterEach
    void tearDown() {
        mistral.stop();
    }

    @Test
    void successfulCompletionIsReturnedAsLlmMode() {
        mistral.stubFor(post(urlEqualTo(COMPLETION)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"choices":[{"message":{"role":"assistant","content":"  Le parc compte 7 missions.  "}}]}
                        """)));

        var response = assistant.ask(new AskRequest("Combien de missions ?"));

        assertEquals("Le parc compte 7 missions.", response.answer());
        assertEquals("LLM", response.mode());
        mistral.verify(postRequestedFor(urlEqualTo(COMPLETION))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(containing("\"model\":\"mistral-small-latest\""))
                .withRequestBody(containing("\"role\":\"system\""))
                .withRequestBody(containing("\"role\":\"user\"")));
    }

    @Test
    void httpErrorFallsBackToGrounded() {
        mistral.stubFor(post(urlEqualTo(COMPLETION)).willReturn(aResponse().withStatus(503).withBody("unavailable")));

        var response = assistant.ask(new AskRequest("Combien de missions ?"));

        assertEquals("7 missions", response.answer());
        assertEquals("GROUNDED", response.mode());
    }

    @Test
    void requestTimeoutFallsBackToGrounded() {
        org.mockito.Mockito.when(config.mistralRequestTimeoutMs()).thenReturn(300L);
        mistral.stubFor(post(urlEqualTo(COMPLETION)).willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5_000)
                .withBody("{\"choices\":[{\"message\":{\"content\":\"trop tard\"}}]}")));

        var response = assistant.ask(new AskRequest("Combien de missions ?"));

        assertEquals("7 missions", response.answer());
        assertEquals("GROUNDED", response.mode());
    }

    @Test
    void invalidResponseFallsBackToGrounded() {
        mistral.stubFor(post(urlEqualTo(COMPLETION)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{not-json")));

        var malformed = assistant.ask(new AskRequest("Combien de missions ?"));
        assertEquals("7 missions", malformed.answer());
        assertEquals("GROUNDED", malformed.mode());

        mistral.resetAll();
        mistral.stubFor(post(urlEqualTo(COMPLETION)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"choices\":[{\"message\":{\"content\":{\"parts\":[\"x\"]}}}]}")));

        var unexpected = assistant.ask(new AskRequest("Combien de missions ?"));
        assertEquals("7 missions", unexpected.answer());
        assertEquals("GROUNDED", unexpected.mode());
    }
}
