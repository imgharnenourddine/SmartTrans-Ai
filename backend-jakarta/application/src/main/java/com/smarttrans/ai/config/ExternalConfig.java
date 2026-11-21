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
    @ConfigProperty(name = "MISTRAL_API_KEY")
    private Optional<String> mistralApiKey;

    @Inject
    @ConfigProperty(name = "MISTRAL_MODEL", defaultValue = "mistral-small-latest")
    private String mistralModel;

    @Inject
    @ConfigProperty(name = "MISTRAL_BASE_URL", defaultValue = "https://api.mistral.ai/v1")
    private String mistralBaseUrl;

    @Inject
    @ConfigProperty(name = "MISTRAL_REQUEST_TIMEOUT_MS", defaultValue = "12000")
    private long mistralRequestTimeoutMs;

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

    public Optional<String> mistralApiKey() {
        return mistralApiKey.filter(value -> !value.isBlank());
    }

    public boolean mistralKeyPresent() {
        return mistralApiKey().isPresent();
    }

    public String mistralModel() {
        return mistralModel;
    }

    public String mistralBaseUrl() {
        return mistralBaseUrl;
    }

    public long mistralRequestTimeoutMs() {
        return mistralRequestTimeoutMs > 0 ? mistralRequestTimeoutMs : 12_000L;
    }

    public String corsAllowedOrigins() {
        return corsAllowedOrigins;
    }
}
