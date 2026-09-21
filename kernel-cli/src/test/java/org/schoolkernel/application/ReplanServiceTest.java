package org.schoolkernel.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.CurrentTimetableReader;
import org.schoolkernel.contract.DefinitionSchemaValidator;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.ResultFactory;
import org.schoolkernel.contract.RevisionService;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.domain.BaselineVerifier;
import org.schoolkernel.solver.PlanningMapper;
import org.schoolkernel.solver.PreflightFeasibilityCheck;
import org.schoolkernel.solver.ReplanningSolver;
import org.schoolkernel.solver.ScheduleEvaluator;
import org.schoolkernel.solver.SolverAdapter;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class ReplanServiceTest {
    private static final SolverAdapter.ExecutionControls CONTROLS =
            new SolverAdapter.ExecutionControls(Duration.ofSeconds(1), null, 0);

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("UC-2 ext 2i: injected internal failure publishes only a safe result")
    void unexpectedFailurePublishesSafeInternalError() throws Exception {
        Baseline baseline = baseline("valid-plan.json");
        var solver = new RecordingSolver();
        solver.failure = new IllegalStateException("secret replan detail");
        CommandOutcome outcome = service(new FileBoundary(), solver)
                .handle(request(baseline, temporaryDirectory.resolve("internal.json"), false, false));

        var internal = assertInstanceOf(CommandOutcome.InternalError.class, outcome);
        JsonNode result = internal.document();
        assertEquals("INTERNAL_ERROR", result.path("status").stringValue());
        assertEquals("An unexpected internal error occurred.", result.path("safeMessage").stringValue());
        assertEquals("test-school", result.path("schoolId").stringValue());
        assertTrue(result.path("inputRevision").stringValue().startsWith("sha256:"));
        assertFalse(result.has("timetable"));
        assertFalse(result.has("changeReport"));
        assertEquals("secret replan detail", internal.cause().getMessage());
    }

    @Test
    @DisplayName("UC-2 ext 2j: an interrupted solver publishes nothing")
    void interruptionPreservesDestination() throws Exception {
        Baseline baseline = baseline("valid-plan.json");
        Path output = temporaryDirectory.resolve("interrupted.json");
        var solver = new RecordingSolver();
        solver.interrupt = true;
        try {
            CommandOutcome outcome = service(new FileBoundary(), solver)
                    .handle(request(baseline, output, false, false));

            assertInstanceOf(CommandOutcome.Interrupted.class, outcome);
            assertFalse(Files.exists(output));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("UC-2 ext 2k and minimal guarantee: publication failure preserves prior destination bytes")
    void publicationFailurePreservesDestination() throws Exception {
        Baseline baseline = baseline("empty-plan.json");
        Path output = temporaryDirectory.resolve("existing.json");
        byte[] previous = "previous-result".getBytes(StandardCharsets.UTF_8);
        Files.write(output, previous);

        FileBoundary files = new FileBoundary();
        assertThrows(TransportException.class,
                () -> files.publish(JsonSupport.mapper().createObjectNode(), output, false));
        assertArrayEquals(previous, Files.readAllBytes(output));
    }

    @Test
    @DisplayName("UC-2 ext 1c: either input exceeding the resource safeguard preserves the destination")
    void resourceSafeguardPreservesDestination() throws Exception {
        Baseline baseline = baseline("empty-plan.json");
        Path output = temporaryDirectory.resolve("existing-resource.json");
        byte[] previous = "previous-result".getBytes(StandardCharsets.UTF_8);
        Files.write(output, previous);
        String prior = System.getProperty("school.kernel.maxInputBytes");
        System.setProperty("school.kernel.maxInputBytes", "1");
        try {
            CommandOutcome outcome = service(new FileBoundary(), new RecordingSolver())
                    .handle(request(baseline, output, true, false));

            assertInstanceOf(CommandOutcome.TransportFailure.class, outcome);
            assertArrayEquals(previous, Files.readAllBytes(output));
        } finally {
            if (prior == null) {
                System.clearProperty("school.kernel.maxInputBytes");
            } else {
                System.setProperty("school.kernel.maxInputBytes", prior);
            }
        }
    }

    private ReplanService service(PlanFiles files, ReplanningSolver solver) {
        return new ReplanService(
                new DefinitionLoader(
                        files,
                        new DefinitionSchemaValidator(),
                        new DefinitionValidator(),
                        new RevisionService()),
                files,
                new CurrentTimetableReader(),
                new BaselineVerifier(),
                new PreflightFeasibilityCheck(),
                solver,
                new ScheduleEvaluator(),
                new ResultFactory());
    }

    private ReplanRequest request(Baseline baseline, Path output, boolean force, boolean debug) {
        return new ReplanRequest(
                baseline.currentDefinition(), baseline.updatedDefinition(), baseline.current(), output,
                CONTROLS, "replan-service-test", force, debug);
    }

    private Baseline baseline(String fixture) throws Exception {
        Path definition = temporaryDirectory.resolve("definition-" + fixture);
        try (InputStream input = ReplanServiceTest.class.getResourceAsStream("/fixtures/" + fixture)) {
            Files.copy(input, definition);
        }
        Path current = temporaryDirectory.resolve("current-" + fixture);
        FileBoundary files = new FileBoundary();
        PlanService plan = new PlanService(
                new DefinitionLoader(
                        files,
                        new DefinitionSchemaValidator(),
                        new DefinitionValidator(),
                        new RevisionService()),
                new PreflightFeasibilityCheck(),
                new SolverAdapter(),
                new ScheduleEvaluator(),
                new ResultFactory());
        var planOutcome = assertInstanceOf(CommandOutcome.Succeeded.class,
                plan.handle(new PlanRequest(
                        definition, current, CONTROLS, "replan-baseline", false, false)));
        files.publish(planOutcome.document(), current, false);
        ObjectNode updated = (ObjectNode) JsonSupport.mapper().readTree(definition);
        updated.put("basedOnRevision", JsonSupport.mapper().readTree(current).path("inputRevision").stringValue());
        Path updatedPath = temporaryDirectory.resolve("updated-" + fixture);
        Files.write(updatedPath, JsonSupport.mapper().writeValueAsBytes(updated));
        return new Baseline(definition, updatedPath, current);
    }

    private record Baseline(Path currentDefinition, Path updatedDefinition, Path current) {}

    private static final class RecordingSolver implements ReplanningSolver {
        private RuntimeException failure;
        private boolean interrupt;

        @Override
        public SolverAdapter.SolveResult solve(
                SchoolDefinition definition,
                SolverAdapter.ExecutionControls controls,
                Map<String, PlanningMapper.BaselineAssignment> baselineAssignments) {
            if (failure != null) {
                throw failure;
            }
            if (interrupt) {
                Thread.currentThread().interrupt();
            }
            return new SolverAdapter.SolveResult(null, null, "TIME_LIMIT", java.util.List.of());
        }
    }

}
