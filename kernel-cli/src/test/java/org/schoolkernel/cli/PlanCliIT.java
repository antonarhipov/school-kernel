package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.RevisionService;

import tools.jackson.databind.JsonNode;

class PlanCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("UC-1 main and success postcondition: packaged CLI publishes complete hard-valid timetable")
    void mainSuccessScenario() throws Exception {
        Path input = copyFixture("valid-plan.json");
        Path output = temporaryDirectory.resolve("result.json");

        ProcessResult process = run("plan", "--definition", input.toString(), "--output", output.toString(),
                "--step-limit", "100", "--correlation-id", "test-main");

        assertEquals(0, process.exitCode());
        assertEquals("", process.stdout());
        assertTrue(process.stderr().contains("Solving ended"));
        assertFalse(process.stderr().contains("Teacher One"));
        assertFalse(process.stderr().contains("Math lesson"));
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        assertEquals("test-main", result.path("correlationId").stringValue());
        assertEquals("test-school", result.path("schoolId").stringValue());
        assertEquals("SEARCH_EXHAUSTED", result.path("terminationReason").stringValue());
        assertEquals(1, result.path("timetable").path("assignments").size());
        JsonNode assignment = result.path("timetable").path("assignments").get(0);
        assertEquals("lesson-1", assignment.path("lessonId").stringValue());
        assertEquals("mon-1", assignment.path("periodId").stringValue());
        assertEquals("room-1", assignment.path("roomId").stringValue());
        assertEquals(0, result.path("score").path("periodMoves").longValue());
        assertEquals(0, result.path("score").path("roomOnlyMoves").longValue());
        assertEquals(4, result.path("score").path("constraintBreakdown").size());
        assertEquals(List.of(
                        "soft.teacher-gap",
                        "soft.series-same-day",
                        "soft.undesirable-period",
                        "soft.non-preferred-room"),
                java.util.stream.StreamSupport.stream(
                                result.path("score").path("constraintBreakdown").spliterator(), false)
                        .map(item -> item.path("constraintId").stringValue())
                        .toList());
        assertEquals(
                result.path("timetableRevision").stringValue(),
                new RevisionService().timetableRevision(
                        result.path("schemaVersion").intValue(),
                        result.path("schoolId").stringValue(),
                        result.path("inputRevision").stringValue(),
                        (tools.jackson.databind.node.ArrayNode) result.path("timetable").path("assignments")));
        assertTrue(resultSchema().validate(result).isEmpty());
        assertArrayEquals(JsonSupport.canonicalBytes(result), Files.readAllBytes(output));
    }

    @Test
    @DisplayName("UC-1 ext 2d: empty definition bypasses search and publishes EMPTY_PROBLEM")
    void emptyProblem() throws Exception {
        Path input = copyFixture("empty-plan.json");
        Path output = temporaryDirectory.resolve("empty-result.json");

        ProcessResult process = run("plan", "--definition", input.toString(), "--output", output.toString(),
                "--step-limit", "10");

        assertEquals(0, process.exitCode());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("EMPTY_PROBLEM", result.path("terminationReason").stringValue());
        assertEquals(0, result.path("timetable").path("assignments").size());
        assertFalse(process.stderr().contains("Solving started"));
        assertTrue(resultSchema().validate(result).isEmpty());
    }

    @Test
    @DisplayName("UC-1 ext 2e: a reached step limit publishes the best complete feasible timetable")
    void reachedStepLimitPublishesBestFeasibleResult() throws Exception {
        Path input = Path.of("..", "examples", "initial-school.json").toAbsolutePath();
        Path output = temporaryDirectory.resolve("step-limited-result.json");

        ProcessResult process = run("plan", "--definition", input.toString(), "--output", output.toString(),
                "--step-limit", "1", "--seed", "0");

        assertEquals(0, process.exitCode());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        assertEquals("STEP_LIMIT", result.path("terminationReason").stringValue());
        assertEquals(2, result.path("timetable").path("assignments").size());
        assertTrue(resultSchema().validate(result).isEmpty());
    }

    @Test
    @DisplayName("UC-1 ext 2a and 2b: malformed and successor inputs publish INVALID_INPUT without timetable")
    void invalidInputs() throws Exception {
        Path malformed = temporaryDirectory.resolve("malformed.json");
        Files.writeString(malformed, "{not-json", StandardCharsets.UTF_8);
        Path malformedOutput = temporaryDirectory.resolve("malformed-result.json");

        ProcessResult malformedProcess = run("plan", "--definition", malformed.toString(), "--output",
                malformedOutput.toString(), "--step-limit", "10");
        assertEquals(2, malformedProcess.exitCode());
        JsonNode malformedResult = JsonSupport.mapper().readTree(malformedOutput);
        assertEquals("INVALID_INPUT", malformedResult.path("status").stringValue());
        assertFalse(malformedResult.has("timetable"));
        assertEquals(0, malformedResult.path("seed").longValue());
        assertEquals(10, malformedResult.path("limit").path("steps").intValue());
        assertTrue(resultSchema().validate(malformedResult).isEmpty());
        assertFalse(malformedProcess.stderr().contains("Solving started"));

        Path successor = copyFixture("valid-plan.json");
        var successorJson = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper().readTree(successor);
        successorJson.put("basedOnRevision", "sha256:" + "0".repeat(64));
        Files.write(successor, JsonSupport.mapper().writeValueAsBytes(successorJson));
        Path successorOutput = temporaryDirectory.resolve("successor-result.json");
        ProcessResult successorProcess = run("plan", "--definition", successor.toString(), "--output",
                successorOutput.toString(), "--step-limit", "10");
        assertEquals(2, successorProcess.exitCode());
        JsonNode successorResult = JsonSupport.mapper().readTree(successorOutput);
        assertEquals("INVALID_INPUT", successorResult.path("status").stringValue());
        assertFalse(successorResult.has("timetable"));
        assertEquals("test-school", successorResult.path("schoolId").stringValue());
        assertEquals(10, successorResult.path("limit").path("steps").intValue());
        assertTrue(resultSchema().validate(successorResult).isEmpty());
        assertFalse(successorProcess.stderr().contains("Solving started"));

        Path unsupported = copyFixture("valid-plan.json", "unsupported.json");
        var unsupportedJson = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper().readTree(unsupported);
        unsupportedJson.put("catalogVersion", 6);
        Files.write(unsupported, JsonSupport.mapper().writeValueAsBytes(unsupportedJson));
        Path unsupportedOutput = temporaryDirectory.resolve("unsupported-result.json");
        ProcessResult unsupportedProcess = run("plan", "--definition", unsupported.toString(), "--output",
                unsupportedOutput.toString(), "--step-limit", "10");
        assertEquals(2, unsupportedProcess.exitCode());
        JsonNode unsupportedResult = JsonSupport.mapper().readTree(unsupportedOutput);
        assertFalse(unsupportedResult.has("catalogVersion"));
        assertEquals(10, unsupportedResult.path("limit").path("steps").intValue());
        assertTrue(resultSchema().validate(unsupportedResult).isEmpty());

        for (String name : List.of("missing-school-name", "blank-school-name")) {
            Path invalid = copyFixture("valid-plan.json", name + ".json");
            var invalidJson = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper().readTree(invalid);
            if (name.startsWith("missing")) {
                invalidJson.remove("displayName");
            } else {
                invalidJson.put("displayName", " ");
            }
            Files.write(invalid, JsonSupport.mapper().writeValueAsBytes(invalidJson));
            Path invalidOutput = temporaryDirectory.resolve(name + "-result.json");

            ProcessResult invalidProcess = run("plan", "--definition", invalid.toString(), "--output",
                    invalidOutput.toString(), "--step-limit", "10");

            assertEquals(2, invalidProcess.exitCode(), name);
            JsonNode invalidResult = JsonSupport.mapper().readTree(invalidOutput);
            assertEquals("INVALID_INPUT", invalidResult.path("status").stringValue(), name);
            assertFalse(invalidResult.has("timetable"), name);
            assertFalse(invalidProcess.stderr().contains("Solving started"), name);
            assertTrue(resultSchema().validate(invalidResult).isEmpty(), name);
        }
    }

    @Test
    @DisplayName("UC-1 ext 2c: obvious room impossibility publishes diagnostics without invoking search")
    void obviousNoRoom() throws Exception {
        Path input = copyFixture("no-room-plan.json");
        Path output = temporaryDirectory.resolve("no-room-result.json");

        ProcessResult process = run("plan", "--definition", input.toString(), "--output", output.toString(),
                "--step-limit", "10");

        assertEquals(3, process.exitCode());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", result.path("status").stringValue());
        assertFalse(result.has("timetable"));
        assertEquals("hard.room-capacity",
                result.path("searchDiagnostics").path("constraints").get(0).path("constraintId").stringValue());
        assertFalse(process.stderr().contains("Solving started"));
        assertTrue(resultSchema().validate(result).isEmpty());
    }

    @Test
    @DisplayName("UC-1 ext 2f and G5: exhausted search publishes diagnostics and never a candidate timetable")
    void exhaustedSearchDoesNotPublishCandidate() throws Exception {
        Path input = copyFixture("search-conflict-plan.json");
        Path output = temporaryDirectory.resolve("search-conflict-result.json");

        ProcessResult process = run("plan", "--definition", input.toString(), "--output", output.toString(),
                "--step-limit", "10");

        assertEquals(3, process.exitCode());
        assertEquals("", process.stdout());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", result.path("status").stringValue());
        assertEquals("SEARCH_EXHAUSTED", result.path("terminationReason").stringValue());
        assertFalse(result.has("timetable"));
        assertFalse(Files.readString(output).contains("INFEASIBLE"));
        assertFalse(Files.readString(output).contains("OPTIMAL"));
        assertTrue(result.path("searchDiagnostics").path("totalMatches").longValue() > 0);
        JsonNode firstDiagnostic = result.path("searchDiagnostics").path("constraints").get(0);
        assertEquals("hard.teacher-period", firstDiagnostic.path("constraintId").stringValue());
        JsonNode example = firstDiagnostic.path("examples").get(0);
        assertEquals("lesson-1", example.get(0).stringValue());
        assertEquals("lesson-2", example.get(1).stringValue());
        assertEquals("teacher-1", example.get(2).stringValue());
        assertEquals("mon-1", example.get(3).stringValue());
        assertTrue(resultSchema().validate(result).isEmpty());
    }

    @Test
    @DisplayName("UC-1 ext 1a and 1b: misuse exits 64 and preserves every existing file")
    void cliMisusePreservesFiles() throws Exception {
        Path input = copyFixture("valid-plan.json");
        byte[] originalInput = Files.readAllBytes(input);

        ProcessResult samePath = run("plan", "--definition", input.toString(), "--output", input.toString(),
                "--step-limit", "10");
        assertEquals(64, samePath.exitCode());
        assertArrayEquals(originalInput, Files.readAllBytes(input));

        Path output = temporaryDirectory.resolve("unused.json");
        ProcessResult bothLimits = run("plan", "--definition", input.toString(), "--output", output.toString(),
                "--time-limit", "1s", "--step-limit", "10");
        assertEquals(64, bothLimits.exitCode());
        assertFalse(Files.exists(output));
    }

    @Test
    @DisplayName("UC-1 ext 1c and G9: overwrite refusal preserves bytes and --force atomically replaces")
    void overwriteRules() throws Exception {
        Path input = copyFixture("empty-plan.json");
        Path output = temporaryDirectory.resolve("existing.json");
        byte[] original = "existing-content".getBytes(StandardCharsets.UTF_8);
        Files.write(output, original);

        ProcessResult refused = run("plan", "--definition", input.toString(), "--output", output.toString(),
                "--step-limit", "10");
        assertEquals(74, refused.exitCode());
        assertArrayEquals(original, Files.readAllBytes(output));

        ProcessResult replaced = run("plan", "--definition", input.toString(), "--output", output.toString(),
                "--step-limit", "10", "--force");
        assertEquals(0, replaced.exitCode());
        assertNotEquals("existing-content", Files.readString(output));
        assertEquals("FEASIBLE", JsonSupport.mapper().readTree(output).path("status").stringValue());
    }

    @Test
    @DisplayName("UC-1 ext 1c: unreadable input and unpreparable destination exit 74 without a result")
    void transportFailuresDoNotPublish() throws Exception {
        Path missingInput = temporaryDirectory.resolve("missing.json");
        Path output = temporaryDirectory.resolve("unused.json");

        ProcessResult unreadable = run("plan", "--definition", missingInput.toString(), "--output", output.toString());
        assertEquals(74, unreadable.exitCode());
        assertFalse(Files.exists(output));

        Path input = copyFixture("valid-plan.json");
        Path missingParentOutput = temporaryDirectory.resolve("missing-parent").resolve("result.json");
        ProcessResult unpreparable = run(
                "plan", "--definition", input.toString(), "--output", missingParentOutput.toString());
        assertEquals(74, unpreparable.exitCode());
        assertFalse(Files.exists(missingParentOutput));
    }

    @Test
    @DisplayName("UC-1 ext 2h and minimal guarantee: interruption exits 130 without publication")
    void interruptionDoesNotPublish() throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }
        Path input = copyFixture("valid-plan.json");
        Path output = temporaryDirectory.resolve("interrupted.json");
        Process process = start("plan", "--definition", input.toString(), "--output", output.toString(),
                "--time-limit", "30s");
        Thread.sleep(500);
        new ProcessBuilder("kill", "-INT", Long.toString(process.pid())).start().waitFor();

        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        assertEquals(130, process.exitValue());
        assertFalse(Files.exists(output));
    }

    @Test
    @DisplayName("RULE-12: launcher and executable JAR expose the same plan contract")
    void launcherAndJarAgree() throws Exception {
        Path input = copyFixture("empty-plan.json");
        Path jarOutput = temporaryDirectory.resolve("jar-result.json");
        Path launcherOutput = temporaryDirectory.resolve("launcher-result.json");
        String[] common = {"plan", "--definition", input.toString(),
                "--correlation-id", "distribution-smoke"};

        ProcessResult jar = run(concat(common, "--output", jarOutput.toString()));
        ProcessResult launcher = runLauncher(concat(common, "--output", launcherOutput.toString()));

        assertEquals(0, jar.exitCode());
        assertEquals(0, launcher.exitCode());
        var jarJson = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper().readTree(jarOutput);
        var launcherJson = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper().readTree(launcherOutput);
        assertEquals(0, jarJson.path("seed").longValue());
        assertEquals("TIME", jarJson.path("limit").path("type").stringValue());
        assertEquals("PT30S", jarJson.path("limit").path("duration").stringValue());
        jarJson.remove("elapsedTimeMs");
        launcherJson.remove("elapsedTimeMs");
        assertEquals(JsonSupport.canonicalString(jarJson), JsonSupport.canonicalString(launcherJson));
    }

    private Path copyFixture(String name) throws Exception {
        return copyFixture(name, name);
    }

    private Path copyFixture(String resourceName, String targetName) throws Exception {
        Path target = temporaryDirectory.resolve(targetName);
        try (InputStream input = PlanCliIT.class.getResourceAsStream("/fixtures/" + resourceName)) {
            Files.copy(input, target);
        }
        return target;
    }

    private static Schema resultSchema() throws Exception {
        try (InputStream input = PlanCliIT.class.getResourceAsStream("/schema/result-v1.schema.json")) {
            return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(input);
        }
    }

    private static ProcessResult run(String... arguments) throws Exception {
        Process process = start(arguments);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "packaged CLI did not terminate");
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    private static ProcessResult runLauncher(String... arguments) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of("..", "school-kernel").toAbsolutePath().toString());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "launcher did not terminate");
        return new ProcessResult(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static String[] concat(String[] prefix, String... suffix) {
        String[] result = java.util.Arrays.copyOf(prefix, prefix.length + suffix.length);
        System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
        return result;
    }

    private static Process start(String... arguments) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(JAR.toString());
        command.addAll(List.of(arguments));
        return new ProcessBuilder(command).start();
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
