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
}
