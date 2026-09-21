package org.schoolkernel.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.schoolkernel.application.CommandOutcome;
import org.schoolkernel.application.FileBoundary;
import org.schoolkernel.application.PlanService;
import org.schoolkernel.application.ReplanService;
import org.schoolkernel.application.VerifyService;
import org.schoolkernel.solver.SolverAdapter;

class KernelArchitectureTest {
    private final com.tngtech.archunit.core.domain.JavaClasses production =
            new ClassFileImporter().importPath(Path.of("target", "classes"));

    @Test
    @DisplayName("RULE-26: only the CLI composition root constructs filesystem and solver adapters")
    void onlyCliConstructsAdapters() {
        noClasses().that().resideOutsideOfPackage("org.schoolkernel.cli..")
                .should().callConstructor(FileBoundary.class)
                .orShould().callConstructor(SolverAdapter.class)
                .check(production);
    }

    @Test
    @DisplayName("RULE-26: separate handlers return one exhaustive sealed typed outcome family")
    void handlersReturnSealedOutcomes() throws Exception {
        assertTrue(CommandOutcome.class.isSealed());
        assertTrue(CommandOutcome.DocumentOutcome.class.isSealed());
        assertTrue(CommandOutcome.Succeeded.class.isRecord());
        assertTrue(CommandOutcome.InvalidInput.class.isRecord());
        assertTrue(CommandOutcome.NoFeasibleSolution.class.isRecord());
        assertTrue(CommandOutcome.InternalError.class.isRecord());
        assertTrue(CommandOutcome.TransportFailure.class.isRecord());
        assertTrue(CommandOutcome.Interrupted.class.isRecord());
        assertTrue(PlanService.class.getMethod("handle", org.schoolkernel.application.PlanRequest.class)
                .getReturnType() == CommandOutcome.class);
        assertTrue(ReplanService.class.getMethod("handle", org.schoolkernel.application.ReplanRequest.class)
                .getReturnType() == CommandOutcome.class);
        assertTrue(VerifyService.class.getMethod("handle", org.schoolkernel.application.VerifyRequest.class)
                .getReturnType() == CommandOutcome.class);
    }

    @Test
    @DisplayName("RULE-26: production sources do not catch Throwable")
    void productionDoesNotCatchThrowable() throws Exception {
        try (var paths = Files.walk(Path.of("src", "main", "java"))) {
            assertFalse(paths.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> read(path))
                    .anyMatch(source -> source.contains("catch (Throwable")));
        }
    }

    @Test
    @DisplayName("RULE-27: Timefold types stay in the solver adapter package and internal APIs are absent")
    void timefoldBoundaryUsesPublicApiOnly() {
        classes().that().resideOutsideOfPackage("org.schoolkernel.solver..")
                .should().onlyDependOnClassesThat().resideOutsideOfPackage("ai.timefold..")
                .check(production);
        noClasses().should().dependOnClassesThat().resideInAPackage("ai.timefold..impl..")
                .check(production);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
