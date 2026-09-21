package org.schoolkernel.workspace;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
}
