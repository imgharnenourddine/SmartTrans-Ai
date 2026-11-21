package com.smarttrans.ai.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ExternalConfig {

    @Inject
    @ConfigProperty(name = "JWT_SECRET")
    private Optional<String> jwtSecret;

    @Inject
    @ConfigProperty(name = "JWT_EXPIRATION_MS", defaultValue = "86400000")
    private long jwtExpirationMs;

    @Inject
    @ConfigProperty(name = "OPENAI_API_KEY")
    private Optional<String> openAiApiKey;

    @Inject
    @ConfigProperty(name = "OPENAI_MODEL", defaultValue = "gpt-4o-mini")
    private String openAiModel;

    @Inject
    @ConfigProperty(name = "OPENAI_BASE_URL", defaultValue = "https://api.openai.com/v1")
    private String openAiBaseUrl;

    @Inject
    @ConfigProperty(name = "CORS_ALLOWED_ORIGINS", defaultValue = "http://localhost:5173")
    private String corsAllowedOrigins;

    public String cdiMarker() {
        return "active";
    }

    public boolean jwtSecretPresent() {
        return jwtSecret().isPresent();
    }

    public Optional<String> jwtSecret() {
        return jwtSecret.filter(value -> !value.isBlank());
    }

    public long jwtExpirationMs() {
        return jwtExpirationMs;
    }

    public boolean openAiKeyPresent() {
        return openAiApiKey.filter(value -> !value.isBlank()).isPresent();
    }

    public String openAiModel() {
        return openAiModel;
    }

    public String openAiBaseUrl() {
        return openAiBaseUrl;
    }

    public String corsAllowedOrigins() {
        return corsAllowedOrigins;
    }
}
