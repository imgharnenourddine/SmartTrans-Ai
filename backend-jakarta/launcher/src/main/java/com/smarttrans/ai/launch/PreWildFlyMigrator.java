package com.smarttrans.ai.launch;

import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Applies the existing Spring Boot SQL migrations, then exits.
 * {@code backend-jakarta/start.sh} starts WildFly only when this process exits 0.
 * This class is not a CDI observer and is not deployed inside WildFly.
 */
public final class PreWildFlyMigrator {

    private PreWildFlyMigrator() {
    }

    public static void main(String[] args) {
        String url = env("POSTGRESQL_URL");
        String user = env("POSTGRESQL_USER");
        String password = env("POSTGRESQL_PASSWORD");
        String migrations = env("FLYWAY_MIGRATIONS_PATH");
        if (url == null || user == null || password == null || migrations == null) {
            System.err.println("""
                    Flyway failed before WildFly start.
                    Required environment variables:
                      POSTGRESQL_URL
                      POSTGRESQL_USER
                      POSTGRESQL_PASSWORD
                      FLYWAY_MIGRATIONS_PATH
                    """);
            System.exit(1);
        }

        Path directory = Path.of(migrations).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            System.err.println("Flyway migrations directory does not exist: " + directory);
            System.exit(1);
        }

        try {
            MigrateResult result = Flyway.configure()
                    .dataSource(url, user, password)
                    .locations("filesystem:" + directory)
                    .load()
                    .migrate();
            String schemaVersion = result.targetSchemaVersion == null
                    ? "unchanged"
                    : result.targetSchemaVersion;
            System.out.printf(
                    "Flyway completed. Applied %d migration(s). Schema version: %s%n",
                    result.migrationsExecuted,
                    schemaVersion);
        } catch (RuntimeException ex) {
            System.err.println("Flyway failed. WildFly will not be started.");
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
