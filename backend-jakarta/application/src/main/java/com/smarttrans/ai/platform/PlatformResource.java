package com.smarttrans.ai.platform;

import com.smarttrans.ai.bootstrap.DatabaseProbe;
import com.smarttrans.ai.config.ExternalConfig;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Phase 1 diagnostic. Reports that CDI, Jakarta Persistence and MicroProfile Config are live.
 * Secret values are never written into the response.
 */
@Path("/api/v1/platform")
@RequestScoped
public class PlatformResource {

    @Inject
    private ExternalConfig externalConfig;

    @Inject
    private DatabaseProbe databaseProbe;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String platform() {
        long roles = databaseProbe.countRoles();
        return """
                {
                  "cdi":"%s",
                  "jpaRoles":%d,
                  "config":{
                    "jwtSecretPresent":%s,
                    "jwtExpirationMs":%d,
                    "jwtExpirationSources":%s,
                    "openAiKeyPresent":%s,
                    "openAiModel":"%s",
                    "openAiBaseUrl":"%s",
                    "corsAllowedOrigins":"%s"
                  }
                }
                """.formatted(
                externalConfig.cdiMarker(),
                roles,
                externalConfig.jwtSecretPresent(),
                externalConfig.jwtExpirationMs(),
                jwtExpirationSources(),
                externalConfig.openAiKeyPresent(),
                escape(externalConfig.openAiModel()),
                escape(externalConfig.openAiBaseUrl()),
                escape(externalConfig.corsAllowedOrigins()))
                .strip();
    }

    /**
     * Lists only {@code JWT_EXPIRATION_MS}. Secret properties are not read from each source.
     */
    private static String jwtExpirationSources() {
        StringBuilder sources = new StringBuilder("[");
        boolean first = true;
        for (ConfigSource source : ConfigProvider.getConfig().getConfigSources()) {
            String value = source.getValue("JWT_EXPIRATION_MS");
            if (value == null) {
                continue;
            }
            if (!first) {
                sources.append(',');
            }
            first = false;
            sources.append("{\"name\":\"")
                    .append(escape(source.getName()))
                    .append("\",\"ordinal\":")
                    .append(source.getOrdinal())
                    .append(",\"value\":\"")
                    .append(escape(value))
                    .append("\"}");
        }
        return sources.append(']').toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
