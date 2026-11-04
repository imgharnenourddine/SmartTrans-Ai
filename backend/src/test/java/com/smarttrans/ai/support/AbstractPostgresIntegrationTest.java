package com.smarttrans.ai.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shared Embedded PostgreSQL lifecycle for HTTP integration tests.
 */
public abstract class AbstractPostgresIntegrationTest {

	protected static final EmbeddedPostgres embeddedPostgres;

	static {
		try {
			embeddedPostgres = EmbeddedPostgres.builder().start();
		}
		catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	@AfterAll
	static void stopPostgres() throws IOException {
		// Keep instance alive across subclasses in the same JVM; closed by JVM shutdown.
	}

	@DynamicPropertySource
	static void registerDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> embeddedPostgres.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "postgres");
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
	}
}
