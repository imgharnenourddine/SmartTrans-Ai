package com.smarttrans.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts the real {@code start.sh} entrypoint once.
 * The same property is defined in three sources at the same time.
 */
@Testcontainers
class JakartaRuntimeIT {

    private static final int PORT_OFFSET = 12000;
    private static final int HTTP_PORT = 8080 + PORT_OFFSET;
    private static final Pattern SOURCE = Pattern.compile(
            "\\{\"name\":\"(.*?)\",\"ordinal\":(-?\\d+),\"value\":\"(.*?)\"\\}");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @Test
    void flywayPrecedesWildFlyAndConfigSourcesKeepTheirPriority() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        Path log = Files.createTempFile("smarttrans-jakarta-", ".log");
        ProcessBuilder builder = new ProcessBuilder("bash", "start.sh");
        builder.directory(root.toFile());
        builder.redirectOutput(log.toFile());
        builder.redirectErrorStream(true);
        builder.environment().put("SKIP_BUILD", "1");
        builder.environment().put("POSTGRESQL_URL", POSTGRES.getJdbcUrl());
        builder.environment().put("POSTGRESQL_USER", POSTGRES.getUsername());
        builder.environment().put("POSTGRESQL_PASSWORD", POSTGRES.getPassword());
        builder.environment().put(
                "FLYWAY_MIGRATIONS_PATH",
                root.getParent().resolve("backend/src/main/resources/db/migration").toString());
        builder.environment().put("JWT_SECRET", "phase1-test-secret");
        builder.environment().put("JWT_EXPIRATION_MS", "11111");
        builder.environment().put("MISTRAL_API_KEY", "phase1-test-key");
        builder.environment().put(
                "WILDFLY_JAVA_OPTS",
                "-Djboss.socket.binding.port-offset=" + PORT_OFFSET + " -DJWT_EXPIRATION_MS=22222");

        Process process = builder.start();
        try {
            String health = waitFor(process, log, "http://127.0.0.1:" + HTTP_PORT + "/actuator/health");
            String platform = body(process, log, "http://127.0.0.1:" + HTTP_PORT + "/api/v1/platform");
            String output = Files.readString(log);

            int flywayCompleted = output.indexOf("Flyway completed");
            int wildflyStarted = output.indexOf("WFLYSRV0025");
            assertTrue(health.contains("\"status\":\"UP\""), health);
            assertTrue(health.contains("\"roles\":3"), health);
            assertTrue(flywayCompleted >= 0 && wildflyStarted > flywayCompleted, output);
            assertTrue(output.contains("Applied 3 migration(s)"), output);
            assertTrue(platform.contains("\"cdi\":\"active\""), platform);
            assertTrue(platform.contains("\"jpaRoles\":3"), platform);
            assertTrue(platform.contains("\"jwtSecretPresent\":true"), platform);
            assertTrue(platform.contains("\"mistralKeyPresent\":true"), platform);
            assertTrue(platform.contains("\"mistralModel\":\"mistral-small-latest\""), platform);
            assertFalse(platform.contains("phase1-test-key"), platform);
            assertTrue(platform.contains("\"jwtExpirationMs\":22222"), platform);
            assertEquals("22222", sourceValue(platform, "SysPropConfigSource"));
            assertEquals("11111", sourceValue(platform, "EnvConfigSource"));
            assertEquals("86400000", sourceValue(platform, "PropertiesConfigSource"));
            assertTrue(sourceOrdinal(platform, "SysPropConfigSource") > sourceOrdinal(platform, "EnvConfigSource"));
            assertTrue(sourceOrdinal(platform, "EnvConfigSource") > sourceOrdinal(platform, "PropertiesConfigSource"));
        } finally {
            process.destroy();
            if (!process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            Files.deleteIfExists(log);
        }
    }

    private static String waitFor(Process process, Path log, String url) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build();
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        String last = "";
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new AssertionError("Server exited early:\n" + Files.readString(log));
            }
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                last = response.statusCode() + " " + response.body();
                if (response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"")) {
                    return response.body();
                }
            } catch (Exception ignored) {
                last = ignored.getClass().getSimpleName();
            }
            Thread.sleep(1000);
        }
        throw new AssertionError("Health did not become UP. Last response: " + last + "\n" + Files.readString(log));
    }

    private static String body(Process process, Path log, String url) throws Exception {
        if (!process.isAlive()) {
            throw new AssertionError(Files.readString(log));
        }
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body() + "\n" + Files.readString(log));
        return response.body();
    }

    private static String sourceValue(String platform, String namePrefix) {
        Matcher matcher = SOURCE.matcher(platform);
        while (matcher.find()) {
            if (matcher.group(1).startsWith(namePrefix)) {
                return matcher.group(3);
            }
        }
        throw new AssertionError("Missing config source " + namePrefix + " in " + platform);
    }

    private static int sourceOrdinal(String platform, String namePrefix) {
        Matcher matcher = SOURCE.matcher(platform);
        while (matcher.find()) {
            if (matcher.group(1).startsWith(namePrefix)) {
                return Integer.parseInt(matcher.group(2));
            }
        }
        throw new AssertionError("Missing config source " + namePrefix + " in " + platform);
    }
}
