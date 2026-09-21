package org.schoolkernel.workspace;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    @Test
    @DisplayName("RULE-1: workspace production classes do not compile against kernel implementation packages")
    void workspaceDoesNotDependOnKernelImplementation() {
        var classes = new ClassFileImporter().importPackages("org.schoolkernel.workspace");

        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                        "org.schoolkernel.application..",
                        "org.schoolkernel.cli..",
                        "org.schoolkernel.contract..",
                        "org.schoolkernel.domain..",
                        "org.schoolkernel.solver..")
                .check(classes);
    }

    @Test
    @DisplayName("RULE-21: committed configuration contains no database credential or fallback secret")
    void databaseCredentialsAreExternalOnly() throws Exception {
        String configuration = Files.readString(Path.of("src/main/resources/application.yml"));
        assertTrue(configuration.contains("username: ${WORKSPACE_DATABASE_USERNAME}"));
        assertTrue(configuration.contains("password: ${WORKSPACE_DATABASE_PASSWORD}"));
        assertFalse(configuration.contains("WORKSPACE_DATABASE_USERNAME:"));
        assertFalse(configuration.contains("WORKSPACE_DATABASE_PASSWORD:"));
        assertFalse(configuration.contains("password: school_workspace"));
    }

    @Test
    @DisplayName("RULE-30: packaged local startup owns a loopback PostgreSQL 18.6 Compose lifecycle")
    void localStartupOwnsWalkthroughDatabase() throws Exception {
        String compose = Files.readString(Path.of("..", "compose.yaml"));
        String pom = Files.readString(Path.of("pom.xml"));
        String testConfiguration = Files.readString(Path.of("src/test/resources/application.yml"));

        assertTrue(compose.contains("image: postgres:18.6"));
        assertTrue(compose.contains("host_ip: 127.0.0.1"));
        assertTrue(compose.contains("pg_isready -U school_workspace -d school_workspace"));
        assertTrue(compose.contains("workspace-postgres-data:/var/lib/postgresql"));
        assertTrue(compose.contains("POSTGRES_PASSWORD: local_walkthrough_only"));
        assertTrue(pom.contains("<artifactId>spring-boot-docker-compose</artifactId>"));
        assertTrue(pom.contains("<scope>runtime</scope>"));
        assertTrue(pom.contains("<optional>true</optional>"));
        assertTrue(pom.contains("<excludeDockerCompose>false</excludeDockerCompose>"));
        assertTrue(pom.contains("<includeOptional>true</includeOptional>"));
        assertTrue(testConfiguration.contains("enabled: false"));
    }

    @Test
    @DisplayName("UC-3 RULE-20: native workspace English is sourced from one message catalog")
    void workspaceUsesOneMessageCatalog() throws Exception {
        Path assets = Path.of("src/main/resources/static/workspace");
        String application = Files.readString(assets.resolve("app.js"));
        String index = Files.readString(assets.resolve("index.html"));
        String catalog = Files.readString(assets.resolve("messages.js"));

        assertTrue(application.startsWith("import { M } from './messages.js';"));
        assertTrue(catalog.contains("export const M = Object.freeze"));
        assertTrue(catalog.contains("technicalMapping: 'Administrator term Class corresponds to kernel term cohort.'"));
        assertFalse(index.contains(">Operations workspace<"));
        assertFalse(index.contains(">Import school data<"));
        assertFalse(application.contains("'Complete whole-school matrix'"));
    }
}
