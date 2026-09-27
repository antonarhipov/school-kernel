package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.RevisionService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class RoomAssignmentCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir Path temporaryDirectory;

    @Test
    @DisplayName("Room assignment UC-1 main, ext 3a/5a and RULE-3: packaged plan and independent verify enforce the policy")
    void planVerifyAndRejectViolation() throws Exception {
        ObjectNode definition = base(9);
        policy(definition, "required");
        Path definitionPath = write("definition.json", definition);
        Path resultPath = temporaryDirectory.resolve("result.json");
        Result planned = run("plan", "--definition", definitionPath.toString(), "--output", resultPath.toString(),
                "--step-limit", "20");
        assertEquals(0, planned.exitCode(), planned.stderr());
        JsonNode result = JsonSupport.mapper().readTree(resultPath);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        assertEquals("required", result.path("timetable").path("assignments").get(0).path("roomId").stringValue());
        Result checked = run("verify", "--definition", definitionPath.toString(), "--result", resultPath.toString(),
                "--output", temporaryDirectory.resolve("verified.json").toString());
        assertEquals(0, checked.exitCode(), checked.stderr());
        assertEquals("VERIFIED", JsonSupport.mapper().readTree(temporaryDirectory.resolve("verified.json"))
                .path("status").stringValue());

        ObjectNode tampered = (ObjectNode) result.deepCopy();
        ArrayNode assignments = (ArrayNode) tampered.path("timetable").path("assignments");
        ((ObjectNode) assignments.get(0)).put("roomId", "original");
        tampered.put("timetableRevision", new RevisionService().timetableRevision(
                1, tampered.path("schoolId").stringValue(), tampered.path("inputRevision").stringValue(), assignments));
        Path tamperedOutput = temporaryDirectory.resolve("tampered-verification.json");
        Result invalid = run("verify", "--definition", definitionPath.toString(), "--result",
                write("tampered.json", tampered).toString(), "--output", tamperedOutput.toString());
        assertEquals(2, invalid.exitCode(), invalid.stderr());
        JsonNode refusal = JsonSupport.mapper().readTree(tamperedOutput);
        assertEquals("INVALID_INPUT", refusal.path("status").stringValue());
        assertTrue(refusal.path("validationReport").path("errors").toString().contains("hard.room-assignment"));
        assertTrue(refusal.path("validationReport").path("errors").toString().contains("math-rooms"));
        assertFalse(refusal.has("timetable"));

        ObjectNode closed = definition.deepCopy();
        ((ObjectNode) closed.withArray("rooms").get(1)).putArray("availablePeriodIds");
        Path noSolutionPath = temporaryDirectory.resolve("no-solution.json");
        Result noSolution = run("plan", "--definition", write("closed.json", closed).toString(),
                "--output", noSolutionPath.toString(), "--step-limit", "20");
        assertEquals(3, noSolution.exitCode(), noSolution.stderr());
        JsonNode noSolutionResult = JsonSupport.mapper().readTree(noSolutionPath);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", noSolutionResult.path("status").stringValue());
        assertFalse(noSolutionResult.has("timetable"));
        assertTrue(noSolutionResult.path("searchDiagnostics").path("constraints").toString()
                .contains("hard.room-assignment"));

        ObjectNode legacy = definition.deepCopy();
        legacy.put("catalogVersion", 8);
        Path legacyOutput = temporaryDirectory.resolve("legacy.json");
        Result oldCatalog = run("plan", "--definition", write("legacy-definition.json", legacy).toString(),
                "--output", legacyOutput.toString(), "--step-limit", "20");
        assertEquals(2, oldCatalog.exitCode(), oldCatalog.stderr());
        assertEquals("INVALID_INPUT", JsonSupport.mapper().readTree(legacyOutput).path("status").stringValue());
        assertFalse(oldCatalog.stderr().contains("Solving started"));
    }

    @Test
    @DisplayName("Room assignment UC-1 ext 1a/RULE-4: catalog-8 predecessor is immutable and new rule forces only excluded room")
    void repairClassifiesPolicyForcedRoomMove() throws Exception {
        ObjectNode predecessor = base(8);
        Path predecessorDefinition = write("predecessor.json", predecessor);
        Path acceptedPath = temporaryDirectory.resolve("accepted.json");
        Result initial = run("plan", "--definition", predecessorDefinition.toString(),
                "--output", acceptedPath.toString(), "--step-limit", "20");
        assertEquals(0, initial.exitCode(), initial.stderr());
        JsonNode accepted = JsonSupport.mapper().readTree(acceptedPath);
        assertEquals("original", accepted.path("timetable").path("assignments").get(0)
                .path("roomId").stringValue());
        byte[] acceptedBytes = Files.readAllBytes(acceptedPath);
        byte[] definitionBytes = Files.readAllBytes(predecessorDefinition);

        ObjectNode successor = base(9);
        successor.put("basedOnRevision", accepted.path("inputRevision").stringValue());
        policy(successor, "required");
        Path successorPath = write("successor.json", successor);
        Path revisedPath = temporaryDirectory.resolve("revised.json");
        Result replanned = run("replan", "--current-definition", predecessorDefinition.toString(),
                "--definition", successorPath.toString(), "--current", acceptedPath.toString(),
                "--output", revisedPath.toString(), "--step-limit", "20");
        assertEquals(0, replanned.exitCode(), replanned.stderr());
        JsonNode revised = JsonSupport.mapper().readTree(revisedPath);
        assertEquals("FEASIBLE", revised.path("status").stringValue());
        assertEquals(9, revised.path("catalogVersion").intValue());
        assertEquals("required", revised.path("timetable").path("assignments").get(0)
                .path("roomId").stringValue());
        assertEquals(0, revised.path("score").path("roomOnlyMoves").intValue());
        assertEquals(1, revised.path("changeReport").path("forcedMoves").size());
        assertEquals("original", revised.path("changeReport").path("forcedMoves").get(0)
                .path("oldRoomId").stringValue());
        assertEquals("required", revised.path("changeReport").path("forcedMoves").get(0)
                .path("newRoomId").stringValue());
        assertArrayEquals(acceptedBytes, Files.readAllBytes(acceptedPath));
        assertArrayEquals(definitionBytes, Files.readAllBytes(predecessorDefinition));

        Result checked = run("verify", "--definition", successorPath.toString(), "--result", revisedPath.toString(),
                "--output", temporaryDirectory.resolve("revised-verified.json").toString());
        assertEquals(0, checked.exitCode(), checked.stderr());
        assertEquals("VERIFIED", JsonSupport.mapper().readTree(temporaryDirectory.resolve("revised-verified.json"))
                .path("status").stringValue());

        ObjectNode permittedAlternative = successor.deepCopy();
        ((ObjectNode) permittedAlternative.withArray("roomAssignments").get(0))
                .withArray("allowedRoomIds").add("original");
        ((ObjectNode) permittedAlternative.withArray("rooms").get(0)).putArray("availablePeriodIds");
        Path ordinaryPath = temporaryDirectory.resolve("ordinary.json");
        Result ordinary = run("replan", "--current-definition", predecessorDefinition.toString(),
                "--definition", write("permitted-alternative.json", permittedAlternative).toString(),
                "--current", acceptedPath.toString(), "--output", ordinaryPath.toString(), "--step-limit", "20");
        assertEquals(0, ordinary.exitCode(), ordinary.stderr());
        JsonNode ordinaryResult = JsonSupport.mapper().readTree(ordinaryPath);
        assertEquals(1, ordinaryResult.path("score").path("roomOnlyMoves").intValue());
        assertEquals(0, ordinaryResult.path("changeReport").path("forcedMoves").size());

        ObjectNode impossible = successor.deepCopy();
        ((ObjectNode) impossible.withArray("rooms").get(1)).putArray("availablePeriodIds");
        Path impossiblePath = temporaryDirectory.resolve("impossible-repair.json");
        Result failed = run("replan", "--current-definition", predecessorDefinition.toString(),
                "--definition", write("impossible.json", impossible).toString(),
                "--current", acceptedPath.toString(), "--output", impossiblePath.toString(), "--step-limit", "20");
        assertEquals(3, failed.exitCode(), failed.stderr());
        JsonNode failure = JsonSupport.mapper().readTree(impossiblePath);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", failure.path("status").stringValue());
        assertFalse(failure.has("timetable"));
        assertArrayEquals(acceptedBytes, Files.readAllBytes(acceptedPath));
    }

    private static ObjectNode base(int catalog) throws Exception {
        ObjectNode definition = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "valid-plan.json"));
        definition.put("catalogVersion", catalog);
        ((ObjectNode) definition.withArray("rooms").get(0)).put("id", "original");
        definition.withArray("rooms").addObject().put("id", "required").put("displayName", "Required")
                .put("capacity", 25).putArray("capabilityIds");
        ((ObjectNode) definition.withArray("lessons").get(0)).putArray("preferredRoomIds").add("original");
        return definition;
    }

    private static void policy(ObjectNode definition, String roomId) {
        definition.withArray("roomAssignments").addObject().put("id", "math-rooms")
                .put("subjectId", "math").putArray("allowedRoomIds").add(roomId);
    }

    private Path write(String name, JsonNode value) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        Files.write(path, JsonSupport.mapper().writeValueAsBytes(value));
        return path;
    }

    private static Result run(String... arguments) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(JAR.toString());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Packaged kernel did not terminate in 60 seconds");
        }
        return new Result(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) {}
}
