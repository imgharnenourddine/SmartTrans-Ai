package com.smarttrans.ai.platform;

import com.smarttrans.ai.bootstrap.DatabaseProbe;
import com.smarttrans.ai.config.ExternalConfig;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
                externalConfig.openAiKeyPresent(),
                escape(externalConfig.openAiModel()),
                escape(externalConfig.openAiBaseUrl()),
                escape(externalConfig.corsAllowedOrigins()))
                .strip();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
