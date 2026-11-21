package com.smarttrans.ai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Calls the real WildFly REST API. PostgreSQL comes from Testcontainers and Flyway runs through {@code start.sh}.
 */
@Testcontainers
class ApiIntegrationIT {

    private static final int PORT_OFFSET = 13000;
    private static final int HTTP_PORT = 8080 + PORT_OFFSET;
    private static final String PASSWORD = "Password1!";
    private static final String JWT_SECRET = "integration-test-secret-must-be-32b!!";
    private static final String ADMIN_EMAIL = "it-admin@smarttrans.test";
    private static final String MANAGER_EMAIL = "it-manager@smarttrans.test";
    private static final String DRIVER_EMAIL = "it-driver@smarttrans.test";
    private static final String DISABLED_EMAIL = "it-disabled@smarttrans.test";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    private static Process server;
    private static Path log;
    private static String adminToken;
    private static String managerToken;
    private static String driverToken;
    private static String adminUserId;
    private static String driverUserId;
    private static String fixtureDriverId;
    private static String fixtureVehicleId;

    @BeforeAll
    static void startServer() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        log = Files.createTempFile("smarttrans-api-it-", ".log");
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
        builder.environment().put("JWT_SECRET", JWT_SECRET);
        builder.environment().put("JWT_EXPIRATION_MS", "86400000");
        builder.environment().remove("OPENAI_API_KEY");
        builder.environment().put("WILDFLY_JAVA_OPTS", "-Djboss.socket.binding.port-offset=" + PORT_OFFSET);
        server = builder.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                server.destroyForcibly();
            }
        }));
        waitUntilHealthy();
        seedUsers();
        RestAssured.baseURI = "http://127.0.0.1:" + HTTP_PORT;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        Response admin = login(ADMIN_EMAIL);
        admin.then().statusCode(200);
        adminToken = admin.path("accessToken");
        adminUserId = admin.path("userId");
        Response manager = login(MANAGER_EMAIL);
        manager.then().statusCode(200);
        managerToken = manager.path("accessToken");
        Response driver = login(DRIVER_EMAIL);
        driver.then().statusCode(200);
        driverToken = driver.path("accessToken");
        driverUserId = driver.path("userId");
        fixtureDriverId = createDriver(adminToken, driverUserId, "LIC-FIX" + suffix());
        fixtureVehicleId = createVehicle(adminToken, "ITFIX" + suffix().substring(0, 4));
    }

    @AfterAll
    static void stopServer() throws Exception {
        try {
            if (adminToken != null) {
                sweepCreatedData();
            }
        } finally {
            if (server != null) {
                server.destroy();
                if (!server.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)) {
                    server.destroyForcibly();
                }
            }
            if (log != null) {
                Files.deleteIfExists(log);
            }
        }
    }

    @Test
    void authenticationRejectsInvalidCredentialsAndAcceptsAValidLogin() {
        login("missing@smarttrans.test").then()
                .statusCode(401)
                .body("message", equalTo("Invalid email or password"));
        login(DISABLED_EMAIL).then()
                .statusCode(401)
                .body("message", equalTo("User account is disabled"));
        given().contentType(ContentType.JSON)
                .body(Map.of("email", ADMIN_EMAIL, "password", "short"))
                .post("/api/v1/auth/login")
                .then()
                .statusCode(400)
                .body("message", equalTo("Validation failed"))
                .body("fieldErrors.password", notNullValue());
        json(null).get("/api/v1/clients").then()
                .statusCode(401)
                .body("message", equalTo("Authentication required"));

        login(ADMIN_EMAIL).then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("expiresInMs", equalTo(86_400_000))
                .body("email", equalTo(ADMIN_EMAIL))
                .body("roleCodes", hasItem("ADMIN"))
                .body("accessToken", notNullValue());
    }

    @Test
    void clientCrudRecordsAnActivityAndEnforcesRoles() {
        String marker = suffix();
        String company = "IT " + marker;
        String clientId = null;
        try {
            json(adminToken).body(Map.of(
                            "companyName", " ",
                            "address", "1 rue de test",
                            "phone", "0600000091"))
                    .post("/api/v1/clients")
                    .then()
                    .statusCode(400)
                    .body("message", equalTo("Validation failed"))
                    .body("fieldErrors.companyName", notNullValue());

            clientId = json(adminToken).body(Map.of(
                            "companyName", "  " + company + "  ",
                            "address", " 1 rue de test ",
                            "phone", " 0600000091 ",
                            "email", "Ops@Client.TEST"))
                    .post("/api/v1/clients")
                    .then()
                    .statusCode(201)
                    .body("companyName", equalTo(company))
                    .body("address", equalTo("1 rue de test"))
                    .body("phone", equalTo("0600000091"))
                    .body("email", equalTo("ops@client.test"))
                    .extract().path("id");

            json(adminToken).get("/api/v1/clients/{id}", clientId).then()
                    .statusCode(200)
                    .body("id", equalTo(clientId));
            json(adminToken).get("/api/v1/clients").then()
                    .statusCode(200)
                    .body("id", hasItem(clientId));
            json(managerToken).get("/api/v1/clients/{id}", clientId).then()
                    .statusCode(200)
                    .body("companyName", equalTo(company));
            json(driverToken).get("/api/v1/clients").then()
                    .statusCode(403)
                    .body("message", equalTo("Access denied"));

            json(adminToken).body(Map.of("phone", " 0600000092 ")).patch("/api/v1/clients/{id}", clientId).then()
                    .statusCode(200)
                    .body("phone", equalTo("0600000092"))
                    .body("companyName", equalTo(company));

            List<Map<String, Object>> activities = json(adminToken).get("/api/v1/activities").then()
                    .statusCode(200)
                    .extract().jsonPath().getList("$");
            String finalClientId = clientId;
            Map<String, Object> created = activities.stream()
                    .filter(row -> finalClientId.equals(row.get("entityId")) && "CLIENT".equals(row.get("entityType")))
                    .filter(row -> "CREATE".equals(String.valueOf(row.get("action"))))
                    .findFirst()
                    .orElseThrow();
            assertEquals(adminUserId, created.get("actorUserId"));
            json(adminToken).get("/api/v1/activities/{id}", created.get("id")).then()
                    .statusCode(200)
                    .body("entityType", equalTo("CLIENT"))
                    .body("metadata.companyName", equalTo(company));
            json(driverToken).get("/api/v1/activities").then()
                    .statusCode(403)
                    .body("message", equalTo("Access denied"));
        } finally {
            deleteResource("/api/v1/clients/" + clientId);
        }

        json(adminToken).get("/api/v1/clients/{id}", clientId).then()
                .statusCode(404)
                .body("message", containsString("Client not found"));
    }

    @Test
    void driverCrudRejectsConflictsAndServesTheLinkedProfile() {
        String license = "LIC-" + suffix();
        String driverId = null;
        try {
            driverId = createDriver(adminToken, null, license);
            json(adminToken).get("/api/v1/drivers/{id}", driverId).then()
                    .statusCode(200)
                    .body("licenseNumber", equalTo(license))
                    .body("status", equalTo("AVAILABLE"))
                    .body("availableForMission", equalTo(true));
            json(adminToken).body(Map.of("experienceYears", 6)).patch("/api/v1/drivers/{id}", driverId).then()
                    .statusCode(200)
                    .body("experienceYears", equalTo(6));
            json(adminToken).body(driverBody(null, license)).post("/api/v1/drivers").then()
                    .statusCode(409)
                    .body("message", equalTo("License number already in use: " + license));
            json(adminToken).body(Map.of("status", "UNAVAILABLE"))
                    .patch("/api/v1/drivers/{id}/availability", driverId)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("UNAVAILABLE"));
            json(adminToken).get("/api/v1/drivers/expiring-licenses?withinDays=-1").then()
                    .statusCode(422)
                    .body("message", equalTo("withinDays must be zero or positive"));
            json(driverToken).body(driverBody(null, "LIC-" + suffix())).post("/api/v1/drivers").then()
                    .statusCode(403)
                    .body("message", equalTo("Access denied"));
        } finally {
            deleteResource("/api/v1/drivers/" + driverId);
        }

        json(adminToken).get("/api/v1/drivers/{id}", driverId).then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"));
        json(driverToken).get("/api/v1/drivers/me").then()
                .statusCode(200)
                .body("id", equalTo(fixtureDriverId))
                .body("userId", equalTo(driverUserId));
    }

    @Test
    void vehicleCrudNormalizesRegistrationAndRejectsDuplicates() {
        String registration = "IT" + suffix().substring(0, 6);
        String vehicleId = null;
        try {
            vehicleId = json(adminToken).body(Map.of(
                            "registrationNumber", "  " + registration.toLowerCase() + "  ",
                            "type", "VAN",
                            "capacityKg", 1200,
                            "insuranceExpiresAt", "2028-01-01",
                            "technicalVisitExpiresAt", "2028-06-01"))
                    .post("/api/v1/vehicles")
                    .then()
                    .statusCode(201)
                    .body("registrationNumber", equalTo(registration))
                    .body("type", equalTo("VAN"))
                    .body("mileageKm", equalTo(0))
                    .body("status", equalTo("AVAILABLE"))
                    .extract().path("id");
            json(adminToken).body(Map.of("mileageKm", 42)).patch("/api/v1/vehicles/{id}", vehicleId).then()
                    .statusCode(200)
                    .body("mileageKm", equalTo(42))
                    .body("registrationNumber", equalTo(registration));
            json(adminToken).body(Map.of(
                            "registrationNumber", registration,
                            "type", "VAN",
                            "capacityKg", 1200))
                    .post("/api/v1/vehicles")
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("Registration number already in use: " + registration));
            json(driverToken).body(Map.of("registrationNumber", "ITOTHER1", "type", "VAN", "capacityKg", 1000))
                    .post("/api/v1/vehicles")
                    .then()
                    .statusCode(403)
                    .body("message", equalTo("Access denied"));
            json(driverToken).get("/api/v1/vehicles/{id}", vehicleId).then()
                    .statusCode(200)
                    .body("registrationNumber", equalTo(registration));
        } finally {
            deleteResource("/api/v1/vehicles/" + vehicleId);
        }

        json(adminToken).get("/api/v1/vehicles/{id}", vehicleId).then()
                .statusCode(200)
                .body("status", equalTo("OUT_OF_SERVICE"));
    }

    @Test
    void missionLifecycleHonorsTransitionsAndDriverRestrictions() {
        String reference = "M-" + suffix();
        String clientId = null;
        String unassignedId = null;
        String missionId = null;
        try {
            clientId = json(adminToken).body(Map.of(
                            "companyName", "IT " + suffix(),
                            "address", "Rabat",
                            "phone", "0600000093"))
                    .post("/api/v1/clients")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            json(adminToken).body(missionBody("M-" + suffix(), UUID.randomUUID().toString(), null, null))
                    .post("/api/v1/missions")
                    .then()
                    .statusCode(422)
                    .body("message", containsString("Client not found"));

            unassignedId = json(adminToken).body(missionBody("M-" + suffix(), clientId, null, null))
                    .post("/api/v1/missions")
                    .then()
                    .statusCode(201)
                    .body("status", equalTo("PLANIFIED"))
                    .extract().path("id");
            json(driverToken).get("/api/v1/missions/{id}", unassignedId).then()
                    .statusCode(403)
                    .body("message", equalTo("Access denied"));
            json(driverToken).get("/api/v1/missions").then()
                    .statusCode(200)
                    .body("id", not(hasItem(unassignedId)));
            deleteResource("/api/v1/missions/" + unassignedId);
            unassignedId = null;

            missionId = json(adminToken).body(missionBody(reference.toLowerCase(), clientId, fixtureDriverId, fixtureVehicleId))
                    .post("/api/v1/missions")
                    .then()
                    .statusCode(201)
                    .body("reference", equalTo(reference))
                    .body("status", equalTo("PLANIFIED"))
                    .body("departureLocation", equalTo("Rabat"))
                    .extract().path("id");
            json(adminToken).body(Map.of("destination", "Fes")).patch("/api/v1/missions/{id}", missionId).then()
                    .statusCode(200)
                    .body("destination", equalTo("Fes"))
                    .body("reference", equalTo(reference));
            json(adminToken).body(Map.of("status", "COMPLETED")).patch("/api/v1/missions/{id}/status", missionId).then()
                    .statusCode(409)
                    .body("message", equalTo("Invalid status transition: PLANIFIED -> COMPLETED"));

            json(adminToken).body(Map.of("status", "IN_PROGRESS")).patch("/api/v1/missions/{id}/status", missionId).then()
                    .statusCode(200)
                    .body("status", equalTo("IN_PROGRESS"))
                    .body("startedAt", notNullValue());
            json(adminToken).get("/api/v1/drivers/{id}", fixtureDriverId).then()
                    .statusCode(200)
                    .body("status", equalTo("ON_MISSION"));
            json(adminToken).get("/api/v1/vehicles/{id}", fixtureVehicleId).then()
                    .statusCode(200)
                    .body("status", equalTo("IN_USE"));
            json(adminToken).delete("/api/v1/missions/{id}", missionId).then()
                    .statusCode(409)
                    .body("message", equalTo("Only planned or cancelled missions can be deleted: " + reference));
            json(driverToken).body(Map.of("status", "CANCELLED")).patch("/api/v1/missions/{id}/status", missionId).then()
                    .statusCode(403)
                    .body("message", equalTo("Access denied"));
            json(adminToken).get("/api/v1/missions/{id}", missionId).then()
                    .statusCode(200)
                    .body("status", equalTo("IN_PROGRESS"));

            json(adminToken).body(Map.of("status", "CANCELLED")).patch("/api/v1/missions/{id}/status", missionId).then()
                    .statusCode(200)
                    .body("status", equalTo("CANCELLED"));
            json(adminToken).get("/api/v1/drivers/{id}", fixtureDriverId).then()
                    .body("status", equalTo("AVAILABLE"));
            json(adminToken).get("/api/v1/vehicles/{id}", fixtureVehicleId).then()
                    .body("status", equalTo("AVAILABLE"));
            json(adminToken).delete("/api/v1/missions/{id}", missionId).then().statusCode(204);
            String deletedMission = missionId;
            missionId = null;
            json(adminToken).get("/api/v1/missions/{id}", deletedMission).then()
                    .statusCode(404)
                    .body("message", containsString("Mission not found"));
        } finally {
            releaseAndDelete(missionId);
            deleteResource("/api/v1/missions/" + unassignedId);
            deleteResource("/api/v1/clients/" + clientId);
        }
    }

    @Test
    void assistantAnswersFromGroundingAndRejectsADriver() {
        json(adminToken).body(Map.of("message", "x")).post("/api/v1/assistant/ask").then()
                .statusCode(400)
                .body("message", equalTo("Validation failed"))
                .body("fieldErrors.message", notNullValue());
        json(adminToken).body(Map.of("message", "Combien de missions ?")).post("/api/v1/assistant/ask").then()
                .statusCode(200)
                .body("mode", equalTo("GROUNDED"))
                .body("answer", org.hamcrest.Matchers.matchesPattern("\\d+ missions?"));
        json(driverToken).body(Map.of("message", "Combien de missions ?")).post("/api/v1/assistant/ask").then()
                .statusCode(403)
                .body("message", equalTo("Access denied"));
        json(managerToken).body(Map.of("message", "Combien de missions ?")).post("/api/v1/assistant/ask").then()
                .statusCode(200)
                .body("mode", equalTo("GROUNDED"));
    }

    private static Response login(String email) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .post("/api/v1/auth/login");
    }

    private static RequestSpecification json(String token) {
        RequestSpecification specification = given().contentType(ContentType.JSON).accept(ContentType.JSON);
        if (token != null) {
            specification.header("Authorization", "Bearer " + token);
        }
        return specification;
    }

    private static Map<String, Object> driverBody(String userId, String license) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (userId != null) {
            body.put("userId", userId);
        }
        body.put("firstName", "Sara");
        body.put("lastName", "Bennani");
        body.put("phone", "0600000094");
        body.put("licenseNumber", license);
        body.put("licenseExpiresAt", "2028-01-01");
        body.put("experienceYears", 4);
        return body;
    }

    private static String createDriver(String token, String userId, String license) {
        return json(token).body(driverBody(userId, license)).post("/api/v1/drivers").then()
                .statusCode(201)
                .body("licenseNumber", equalTo(license))
                .extract().path("id");
    }

    private static String createVehicle(String token, String registration) {
        return json(token).body(Map.of(
                        "registrationNumber", registration,
                        "type", "VAN",
                        "capacityKg", 1200,
                        "insuranceExpiresAt", "2028-01-01",
                        "technicalVisitExpiresAt", "2028-06-01"))
                .post("/api/v1/vehicles")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private static Map<String, Object> missionBody(String reference, String clientId, String driverId, String vehicleId) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("reference", reference);
        body.put("clientId", clientId);
        if (driverId != null) {
            body.put("driverId", driverId);
        }
        if (vehicleId != null) {
            body.put("vehicleId", vehicleId);
        }
        body.put("departureLocation", " Rabat ");
        body.put("destination", " Tanger ");
        body.put("scheduledAt", "2026-12-15T09:30:00Z");
        body.put("weightKg", 800);
        body.put("cargoType", "Colis");
        return body;
    }

    private static void deleteResource(String path) {
        if (path == null || path.endsWith("null") || adminToken == null) {
            return;
        }
        json(adminToken).delete(path);
    }

    private static void releaseAndDelete(String missionId) {
        if (missionId == null || adminToken == null) {
            return;
        }
        Response current = json(adminToken).get("/api/v1/missions/{id}", missionId);
        if (current.statusCode() == 200 && "IN_PROGRESS".equals(current.path("status"))) {
            json(adminToken).body(Map.of("status", "CANCELLED")).patch("/api/v1/missions/{id}/status", missionId);
        }
        if (current.statusCode() == 200) {
            json(adminToken).delete("/api/v1/missions/{id}", missionId);
        }
    }

    private static void sweepCreatedData() {
        releaseAndDeleteOwnedMissions();
        deleteResource("/api/v1/drivers/" + fixtureDriverId);
        deleteResource("/api/v1/vehicles/" + fixtureVehicleId);
    }

    private static void releaseAndDeleteOwnedMissions() {
        Response response = json(adminToken).get("/api/v1/missions");
        if (response.statusCode() != 200) {
            return;
        }
        List<Map<String, Object>> missions = response.jsonPath().getList("$");
        for (Map<String, Object> mission : missions) {
            String reference = String.valueOf(mission.get("reference"));
            if (!reference.startsWith("M-")) {
                continue;
            }
            releaseAndDelete(String.valueOf(mission.get("id")));
        }
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static void seedUsers() throws Exception {
        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            insertUser(connection, ADMIN_EMAIL, "Admin", "ADMIN", hash, true);
            insertUser(connection, MANAGER_EMAIL, "Manager", "TRANSPORT_MANAGER", hash, true);
            insertUser(connection, DRIVER_EMAIL, "Driver", "DRIVER", hash, true);
            insertUser(connection, DISABLED_EMAIL, "Disabled", "ADMIN", hash, false);
        }
    }

    private static void insertUser(
            Connection connection, String email, String lastName, String role, String hash, boolean enabled)
            throws Exception {
        try (PreparedStatement user = connection.prepareStatement(
                "INSERT INTO users (email, password_hash, first_name, last_name, enabled) VALUES (?, ?, 'IT', ?, ?)")) {
            user.setString(1, email);
            user.setString(2, hash);
            user.setString(3, lastName);
            user.setBoolean(4, enabled);
            user.executeUpdate();
        }
        try (PreparedStatement link = connection.prepareStatement("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT users.id, roles.id FROM users JOIN roles ON roles.code = ? WHERE users.email = ?
                """)) {
            link.setString(1, role);
            link.setString(2, email);
            link.executeUpdate();
        }
    }

    private static void waitUntilHealthy() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + HTTP_PORT + "/actuator/health"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        long deadline = System.nanoTime() + Duration.ofSeconds(120).toNanos();
        String last = "";
        while (System.nanoTime() < deadline) {
            if (!server.isAlive()) {
                throw new AssertionError("Server exited early:\n" + Files.readString(log));
            }
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                last = response.statusCode() + " " + response.body();
                if (response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"")) {
                    return;
                }
            } catch (Exception ignored) {
                last = ignored.getClass().getSimpleName();
            }
            Thread.sleep(1000);
        }
        throw new AssertionError("Health did not become UP. Last response: " + last + "\n" + Files.readString(log));
    }
}
