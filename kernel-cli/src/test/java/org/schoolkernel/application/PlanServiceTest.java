package org.schoolkernel.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.DefinitionSchemaValidator;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.ResultFactory;
import org.schoolkernel.contract.RevisionService;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.solver.InitialSolver;
import org.schoolkernel.solver.PlanningMapper;
import org.schoolkernel.solver.PreflightFeasibilityCheck;
import org.schoolkernel.solver.ScheduleEvaluator;
import org.schoolkernel.solver.SolverAdapter;

import tools.jackson.databind.JsonNode;

class PlanServiceTest {
    private static final SolverAdapter.ExecutionControls CONTROLS =
            new SolverAdapter.ExecutionControls(Duration.ofSeconds(1), null, 0);

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("RULE-4 and UC-1 ext 2a: invalid input never invokes the solver")
    void invalidInputNeverInvokesSolver() throws Exception {
        Path input = temporaryDirectory.resolve("invalid.json");
        Files.writeString(input, "{\"unexpected\":true}", StandardCharsets.UTF_8);
        Path output = temporaryDirectory.resolve("result.json");
        var solver = new RecordingSolver();

        CommandOutcome outcome = service(new FileBoundary(), solver).handle(request(input, output, false, false));

        var invalid = assertInstanceOf(CommandOutcome.InvalidInput.class, outcome);
        assertEquals(0, solver.invocations);
        JsonNode result = invalid.document();
        assertEquals("INVALID_INPUT", result.path("status").stringValue());
        assertFalse(result.has("timetable"));
    }

    @Test
    @DisplayName("RULE-13 and UC-1 ext 2d: empty input bypasses the solver")
    void emptyInputBypassesSolver() throws Exception {
        Path input = copyFixture("empty-plan.json");
        Path output = temporaryDirectory.resolve("result.json");
        var solver = new RecordingSolver();

        CommandOutcome outcome = service(new FileBoundary(), solver).handle(request(input, output, false, false));

        var succeeded = assertInstanceOf(CommandOutcome.Succeeded.class, outcome);
        assertEquals(0, solver.invocations);
        assertEquals("EMPTY_PROBLEM",
                succeeded.document().path("terminationReason").stringValue());
    }

    @Test
    @DisplayName("UC-1 ext 2g: unexpected failure publishes only a safe INTERNAL_ERROR")
    void unexpectedFailurePublishesSafeInternalError() throws Exception {
        Path input = copyFixture("valid-plan.json");
        Path output = temporaryDirectory.resolve("result.json");
        var solver = new RecordingSolver();
        solver.failure = new IllegalStateException("secret technical detail");
        CommandOutcome outcome = service(new FileBoundary(), solver)
                .handle(request(input, output, false, false));

        var internal = assertInstanceOf(CommandOutcome.InternalError.class, outcome);
        JsonNode result = internal.document();
        assertEquals("INTERNAL_ERROR", result.path("status").stringValue());
        assertEquals("An unexpected internal error occurred.", result.path("safeMessage").stringValue());
        assertEquals("test-school", result.path("schoolId").stringValue());
        assertEquals(1, result.path("catalogVersion").intValue());
        assertTrue(result.path("inputRevision").stringValue().startsWith("sha256:"));
        assertEquals(4, result.path("effectiveSoftWeights").size());
        assertEquals(0, result.path("seed").longValue());
        assertEquals("PT1S", result.path("limit").path("duration").stringValue());
        assertFalse(result.has("timetable"));
        assertEquals("secret technical detail", internal.cause().getMessage());
    }

    @Test
    @DisplayName("RULE-13: an incomplete solver candidate is independently refused")
    void incompleteSolverCandidateIsRefused() throws Exception {
        Path input = copyFixture("valid-plan.json");
        Path output = temporaryDirectory.resolve("result.json");
        SchoolDefinition definition = validatedDefinition(input);
        var solver = new RecordingSolver();
        var incomplete = new PlanningMapper().toPlanningProblem(definition);
        solver.result = new SolverAdapter.SolveResult(
                incomplete,
                new ScheduleEvaluator.Evaluation(true, true, java.util.Map.of(), java.util.Map.of(), 0, 0, 0),
                "STEP_LIMIT",
                List.of());

        CommandOutcome outcome = service(new FileBoundary(), solver).handle(request(input, output, false, false));

        var internal = assertInstanceOf(CommandOutcome.InternalError.class, outcome);
        JsonNode result = internal.document();
        assertEquals("INTERNAL_ERROR", result.path("status").stringValue());
        assertFalse(result.has("timetable"));
    }

    @Test
    @DisplayName("UC-1 ext 2i and minimal guarantee: publication failure preserves the destination")
    void publicationFailurePreservesDestination() throws Exception {
        Path input = copyFixture("empty-plan.json");
        Path output = temporaryDirectory.resolve("existing.json");
        byte[] original = "previous-result".getBytes(StandardCharsets.UTF_8);
        Files.write(output, original);
        FileBoundary files = new FileBoundary();
        assertThrows(TransportException.class,
                () -> files.publish(JsonSupport.mapper().createObjectNode(), output, false));
        assertArrayEquals(original, Files.readAllBytes(output));
    }

    @Test
    @DisplayName("UC-1 ext 1c: pre-parse safeguard failure preserves an existing destination")
    void resourceSafeguardPreservesDestination() throws Exception {
        Path input = copyFixture("valid-plan.json");
        Path output = temporaryDirectory.resolve("existing.json");
        byte[] original = "previous-result".getBytes(StandardCharsets.UTF_8);
        Files.write(output, original);
        String previous = System.getProperty("school.kernel.maxInputBytes");
        System.setProperty("school.kernel.maxInputBytes", "1");
        try {
            CommandOutcome outcome = service(new FileBoundary(), new RecordingSolver())
                    .handle(request(input, output, true, false));

            assertInstanceOf(CommandOutcome.TransportFailure.class, outcome);
            assertArrayEquals(original, Files.readAllBytes(output));
        } finally {
            if (previous == null) {
                System.clearProperty("school.kernel.maxInputBytes");
            } else {
                System.setProperty("school.kernel.maxInputBytes", previous);
            }
        }
    }

    private PlanService service(PlanFiles files, InitialSolver solver) {
        return new PlanService(
                new DefinitionLoader(
                        files,
                        new DefinitionSchemaValidator(),
                        new DefinitionValidator(),
                        new RevisionService()),
                new PreflightFeasibilityCheck(),
                solver,
                new ScheduleEvaluator(),
                new ResultFactory());
    }

    private PlanRequest request(Path input, Path output, boolean force, boolean debug) {
        return new PlanRequest(input, output, CONTROLS, "test-correlation", force, debug);
    }

    private Path copyFixture(String name) throws Exception {
        Path target = temporaryDirectory.resolve(name);
        try (var input = PlanServiceTest.class.getResourceAsStream("/fixtures/" + name)) {
            Files.copy(input, target);
        }
        return target;
    }

    private static SchoolDefinition validatedDefinition(Path input) throws Exception {
        var dto = JsonSupport.mapper().treeToValue(
                JsonSupport.mapper().readTree(input),
                org.schoolkernel.contract.SchoolDefinitionDto.class);
        return new DefinitionValidator().validateForPlan(dto).definition();
    }

    private static final class RecordingSolver implements InitialSolver {
        private int invocations;
        private RuntimeException failure;
        private SolverAdapter.SolveResult result;

        @Override
        public SolverAdapter.SolveResult solve(
                SchoolDefinition definition,
                SolverAdapter.ExecutionControls controls) {
            invocations++;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

}
