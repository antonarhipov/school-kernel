package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class FlywayFailureIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("flyway_failures")
            .withUsername("workspace")
            .withPassword("workspace");

    @TempDir
    Path migrations;

    @Test
    @DisplayName("RULE-4: application startup refuses a checksum-drifted migration")
    void checksumDriftRefusesStartup() throws Exception {
        String schema = schema();
        Path migration = migrations.resolve("V1__baseline.sql");
        Files.writeString(migration, "CREATE TABLE sample(id INTEGER PRIMARY KEY);");
        Flyway.configure()
                .dataSource(url(schema), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + migrations)
                .load()
                .migrate();
        Files.writeString(migration, "CREATE TABLE sample(id BIGINT PRIMARY KEY);");

        assertThrows(RuntimeException.class, () -> start(schema));
    }

    @Test
    @DisplayName("RULE-4: failed PostgreSQL migration rolls back and refuses application startup")
    void failedMigrationRollsBackAndRefusesStartup() throws Exception {
        String schema = schema();
        Files.writeString(migrations.resolve("V1__broken.sql"), """
                CREATE TABLE must_rollback(id INTEGER PRIMARY KEY);
                THIS IS NOT SQL;
                """);

        assertThrows(RuntimeException.class, () -> start(schema));

        try (var connection = DriverManager.getConnection(
                        url(schema), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement("""
                        SELECT count(*) FROM information_schema.tables
                        WHERE table_schema=? AND table_name='must_rollback'
                        """)) {
            statement.setString(1, schema);
            try (var rows = statement.executeQuery()) {
                rows.next();
                assertEquals(0, rows.getInt(1));
            }
        }
    }

    private void start(String schema) {
        SpringApplication application = new SpringApplication(WorkspaceApplication.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.run(
                "--server.port=0",
                "--spring.datasource.url=" + url(schema),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.flyway.locations=filesystem:" + migrations,
                "--workspace.kernel-executable=unused");
    }

    private String schema() throws Exception {
        String schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        return schema;
    }

    private static String url(String schema) {
        return POSTGRES.getJdbcUrl() + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?")
                + "currentSchema=" + schema;
    }
}
