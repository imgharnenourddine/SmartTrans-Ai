package com.smarttrans.ai.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class PreWildFlyMigratorTest {

    private static final Path MIGRATIONS = Path.of("..", "..", "backend", "src", "main", "resources", "db", "migration")
            .toAbsolutePath()
            .normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @Test
    void missingEnvironmentExitsBeforeAnyMigration() throws Exception {
        CommandResult result = run(Map.of());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("POSTGRESQL_URL"));
        assertTrue(result.stderr().contains("FLYWAY_MIGRATIONS_PATH"));
        assertFalse(result.stdout().contains("Flyway completed"));
    }

    @Test
    void missingMigrationDirectoryExitsBeforeWildFlyCouldStart() throws Exception {
        CommandResult result = run(environment(POSTGRES, Path.of("missing-migrations")));

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("does not exist"));
    }

    @Test
    void rejectedPasswordExitsWithoutApplyingMigrations() throws Exception {
        Map<String, String> environment = environment(POSTGRES, MIGRATIONS);
        environment.put("POSTGRESQL_PASSWORD", "wrong-password");

        CommandResult result = run(environment);

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("WildFly will not be started"));
        assertFalse(result.stdout().contains("Flyway completed"));
    }

    @Test
    void appliesExistingMigrationsOnceAndCanReadPostgres() throws Exception {
        Map<String, String> environment = environment(POSTGRES, MIGRATIONS);

        CommandResult first = run(environment);
        CommandResult second = run(environment);

        assertEquals(0, first.exitCode(), first.stderr());
        assertTrue(first.stdout().contains("Applied 3 migration(s). Schema version: 3"), first.stdout());
        assertEquals(0, second.exitCode(), second.stderr());
        assertTrue(second.stdout().contains("Applied 0 migration(s). Schema version: unchanged"), second.stdout());

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            assertEquals(3, queryInt(connection, "select count(*) from roles"));
            assertEquals(3, queryInt(connection, "select count(*) from flyway_schema_history where success"));
            assertEquals(0, queryInt(connection, "select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'documents'"));
        }
    }

    private static Map<String, String> environment(PostgreSQLContainer postgres, Path migrations) {
        Map<String, String> environment = new HashMap<>();
        environment.put("POSTGRESQL_URL", postgres.getJdbcUrl());
        environment.put("POSTGRESQL_USER", postgres.getUsername());
        environment.put("POSTGRESQL_PASSWORD", postgres.getPassword());
        environment.put("FLYWAY_MIGRATIONS_PATH", migrations.toString());
        return environment;
    }

    private static int queryInt(Connection connection, String sql) throws Exception {
        try (ResultSet resultSet = connection.createStatement().executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private static CommandResult run(Map<String, String> overrides) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        ProcessBuilder builder = new ProcessBuilder(
                java.toString(),
                "-cp",
                System.getProperty("java.class.path"),
                "com.smarttrans.ai.launch.PreWildFlyMigrator");
        builder.environment().remove("POSTGRESQL_URL");
        builder.environment().remove("POSTGRESQL_USER");
        builder.environment().remove("POSTGRESQL_PASSWORD");
        builder.environment().remove("FLYWAY_MIGRATIONS_PATH");
        builder.environment().putAll(overrides);
        Process process = builder.start();
        CompletableFuture<String> stdout = read(process.getInputStream());
        CompletableFuture<String> stderr = read(process.getErrorStream());
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout.join(), stderr.join());
    }

    private static CompletableFuture<String> read(java.io.InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(stream.readAllBytes());
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    private record CommandResult(int exitCode, String stdout, String stderr) {
    }
}
