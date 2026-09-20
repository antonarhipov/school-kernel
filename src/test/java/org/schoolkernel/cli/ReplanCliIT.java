package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class ReplanCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("UC-2 main and RULE-14: replan consumes UC-1 output, preserves assignments, and supports direct successor")
    void mainSuccessAndDirectSuccessor() throws Exception {
        Path current = produceCurrent();
        byte[] originalCurrent = Files.readAllBytes(current);
        ObjectNode updated = updatedFromCurrent(current);
        Path updatedPath = write("updated.json", updated);
        Path output = temporaryDirectory.resolve("revised.json");

        ProcessResult first = run("replan", "--definition", updatedPath.toString(), "--current", current.toString(),
                "--output", output.toString(), "--step-limit", "100", "--correlation-id", "replan-main");

        assertEquals(0, first.exitCode());
        assertEquals("", first.stdout());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        assertEquals("demo-school", result.path("schoolId").stringValue());
        assertEquals(2, result.path("timetable").path("assignments").size());
        assertEquals(0, result.path("score").path("periodMoves").longValue());
        assertEquals(0, result.path("score").path("roomOnlyMoves").longValue());
        assertEquals(0, result.path("changeReport").path("periodMoves").size());
        assertEquals(0, result.path("changeReport").path("roomOnlyMoves").size());
        assertTrue(resultSchema().validate(result).isEmpty());
        assertArrayEquals(originalCurrent, Files.readAllBytes(current));

        ObjectNode directSuccessor = updated.deepCopy();
        directSuccessor.put("basedOnRevision", result.path("inputRevision").stringValue());
        Path successorPath = write("successor.json", directSuccessor);
        Path secondOutput = temporaryDirectory.resolve("revised-again.json");
        ProcessResult second = run("replan", "--definition", successorPath.toString(), "--current", output.toString(),
                "--output", secondOutput.toString(), "--step-limit", "100");
        assertEquals(0, second.exitCode());
        assertEquals("FEASIBLE", JsonSupport.mapper().readTree(secondOutput).path("status").stringValue());
    }

    @Test
    @DisplayName("UC-2 ext 2b: tampering, lineage mismatch, and school mismatch are rejected before solving")
    void rejectsTamperingAndLineageMismatch() throws Exception {
        Path current = produceCurrent();
        ObjectNode updated = updatedFromCurrent(current);

        ObjectNode tampered = (ObjectNode) JsonSupport.mapper().readTree(current);
        ((ObjectNode) tampered.path("timetable").path("assignments").get(0)).put("periodId", "mon-3");
        Path tamperedPath = write("tampered.json", tampered);
        Path tamperedOutput = temporaryDirectory.resolve("tampered-result.json");
        ProcessResult tamperedProcess = run("replan", "--definition", write("updated-a.json", updated).toString(),
                "--current", tamperedPath.toString(), "--output", tamperedOutput.toString(), "--step-limit", "10");
        assertEquals(2, tamperedProcess.exitCode());
        assertFalse(JsonSupport.mapper().readTree(tamperedOutput).has("timetable"));
        assertFalse(tamperedProcess.stderr().contains("Solving started"));

        ObjectNode wrongLineage = updated.deepCopy();
        wrongLineage.put("basedOnRevision", "sha256:" + "0".repeat(64));
        Path lineageOutput = temporaryDirectory.resolve("lineage-result.json");
        ProcessResult lineageProcess = run("replan", "--definition", write("wrong-lineage.json", wrongLineage).toString(),
                "--current", current.toString(), "--output", lineageOutput.toString(), "--step-limit", "10");
        assertEquals(2, lineageProcess.exitCode());
        assertFalse(lineageProcess.stderr().contains("Solving started"));

        ObjectNode wrongSchool = updated.deepCopy();
        wrongSchool.put("schoolId", "other-school");
        Path schoolOutput = temporaryDirectory.resolve("school-result.json");
        ProcessResult schoolProcess = run("replan", "--definition", write("wrong-school.json", wrongSchool).toString(),
                "--current", current.toString(), "--output", schoolOutput.toString(), "--step-limit", "10");
        assertEquals(2, schoolProcess.exitCode());
        assertFalse(schoolProcess.stderr().contains("Solving started"));
    }

    @Test
    @DisplayName("UC-2 G4-G9: additions, cancellations, teacher changes, and forced moves are classified exactly")
    void classifiesObservableChanges() throws Exception {
        Path current = produceCurrent();
        ObjectNode updated = updatedFromCurrent(current);
        ArrayNode teachers = updated.withArray("teachers");
        teachers.addObject()
                .put("id", "teacher-new")
                .put("displayName", "New Teacher")
                .putArray("qualifiedSubjectIds").add("science");
        ArrayNode lessons = updated.withArray("lessons");
        lessons.remove(0);
        ObjectNode science = (ObjectNode) lessons.get(0);
        science.put("teacherId", "teacher-new");
        science.put("periodLock", "mon-3");
        lessons.addObject()
                .put("id", "lesson-new")
                .put("displayName", "New mathematics")
                .put("subjectId", "math")
                .put("cohortId", "cohort-7a")
                .put("teacherId", "teacher-alex");
        Path output = temporaryDirectory.resolve("changes-result.json");

        ProcessResult process = run("replan", "--definition", write("changes.json", updated).toString(),
                "--current", current.toString(), "--output", output.toString(), "--step-limit", "100");

        assertEquals(0, process.exitCode());
        JsonNode result = JsonSupport.mapper().readTree(output);
        JsonNode report = result.path("changeReport");
        assertEquals("lesson-new", report.path("additions").get(0).path("lessonId").stringValue());
        assertEquals("lesson-math-1", report.path("cancellations").get(0).path("lessonId").stringValue());
        assertEquals("teacher-alex", report.path("teacherChanges").get(0).path("oldTeacherId").stringValue());
        assertEquals("teacher-new", report.path("teacherChanges").get(0).path("newTeacherId").stringValue());
        assertEquals("mon-3", report.path("forcedMoves").get(0).path("newPeriodId").stringValue());
        assertEquals(0, result.path("score").path("periodMoves").longValue());
        assertTrue(resultSchema().validate(result).isEmpty());
    }

    @Test
    @DisplayName("UC-2 ext 2e: empty updated definition reports all baseline lessons as cancellations")
    void emptyUpdateReportsCancellations() throws Exception {
        Path current = produceCurrent();
        ObjectNode updated = updatedFromCurrent(current);
        updated.withArray("lessons").removeAll();
        Path output = temporaryDirectory.resolve("empty-replan-result.json");

        ProcessResult process = run("replan", "--definition", write("empty-update.json", updated).toString(),
                "--current", current.toString(), "--output", output.toString(), "--step-limit", "10");

        assertEquals(0, process.exitCode());
        JsonNode result = JsonSupport.mapper().readTree(output);
        assertEquals("EMPTY_PROBLEM", result.path("terminationReason").stringValue());
        assertEquals(0, result.path("timetable").path("assignments").size());
        assertEquals(2, result.path("changeReport").path("cancellations").size());
        assertFalse(process.stderr().contains("Solving started"));
    }

    @Test
    @DisplayName("UC-2 ext 1a-1c and 2a: misuse and malformed inputs preserve current and destinations")
    void misuseAndMalformedInputsPreserveFiles() throws Exception {
        Path current = produceCurrent();
        byte[] original = Files.readAllBytes(current);
        Path updated = write("updated.json", updatedFromCurrent(current));

        ProcessResult samePath = run("replan", "--definition", updated.toString(), "--current", current.toString(),
                "--output", current.toString(), "--step-limit", "10");
        assertEquals(64, samePath.exitCode());
        assertArrayEquals(original, Files.readAllBytes(current));

        Path unused = temporaryDirectory.resolve("unused.json");
        ProcessResult bothLimits = run("replan", "--definition", updated.toString(), "--current", current.toString(),
                "--output", unused.toString(), "--step-limit", "10", "--time-limit", "1s");
        assertEquals(64, bothLimits.exitCode());
        assertFalse(Files.exists(unused));

        Path malformed = temporaryDirectory.resolve("malformed-current.json");
        Files.writeString(malformed, "{broken", StandardCharsets.UTF_8);
        Path malformedOutput = temporaryDirectory.resolve("malformed-result.json");
        ProcessResult malformedProcess = run("replan", "--definition", updated.toString(),
                "--current", malformed.toString(), "--output", malformedOutput.toString(), "--step-limit", "10");
        assertEquals(2, malformedProcess.exitCode());
        assertFalse(JsonSupport.mapper().readTree(malformedOutput).has("timetable"));
        assertArrayEquals(original, Files.readAllBytes(current));

        Path transportOutput = temporaryDirectory.resolve("transport-result.json");
        ProcessResult transport = run("replan", "--definition", temporaryDirectory.resolve("missing.json").toString(),
                "--current", current.toString(), "--output", transportOutput.toString(), "--step-limit", "10");
        assertEquals(74, transport.exitCode());
        assertFalse(Files.exists(transportOutput));
        assertArrayEquals(original, Files.readAllBytes(current));
    }

    @Test
    @DisplayName("UC-2 ext 2c, 2f, and 2h: lock contradictions validate, preflight and search failures disclose no timetable")
    void lockAndFeasibilityFailures() throws Exception {
        Path current = produceCurrent();

        ObjectNode lockConflict = updatedFromCurrent(current);
        ((ObjectNode) lockConflict.withArray("teachers").get(0)).putArray("availablePeriodIds").add("mon-2");
        ((ObjectNode) lockConflict.withArray("lessons").get(0)).put("periodLock", "mon-1");
        Path lockOutput = temporaryDirectory.resolve("lock-result.json");
        ProcessResult lockProcess = run("replan", "--definition", write("lock-conflict.json", lockConflict).toString(),
                "--current", current.toString(), "--output", lockOutput.toString(), "--step-limit", "10");
        assertEquals(2, lockProcess.exitCode());
        assertFalse(lockProcess.stderr().contains("Solving started"));

        ObjectNode noRoom = updatedFromCurrent(current);
        noRoom.withArray("rooms").forEach(node -> ((ObjectNode) node).put("capacity", 0));
        Path roomOutput = temporaryDirectory.resolve("room-result.json");
        ProcessResult roomProcess = run("replan", "--definition", write("no-room.json", noRoom).toString(),
                "--current", current.toString(), "--output", roomOutput.toString(), "--step-limit", "10");
        assertEquals(3, roomProcess.exitCode());
        assertFalse(JsonSupport.mapper().readTree(roomOutput).has("timetable"));
        assertFalse(roomProcess.stderr().contains("Solving started"));

        ObjectNode conflict = updatedFromCurrent(current);
        ArrayNode lessons = conflict.withArray("lessons");
        for (int index = 2; index < 4; index++) {
            lessons.addObject()
                    .put("id", "lesson-extra-" + index)
                    .put("displayName", "Extra " + index)
                    .put("subjectId", "math")
                    .put("cohortId", "cohort-7a")
                    .put("teacherId", "teacher-alex");
        }
        Path conflictOutput = temporaryDirectory.resolve("conflict-result.json");
        ProcessResult conflictProcess = run("replan", "--definition", write("conflict.json", conflict).toString(),
                "--current", current.toString(), "--output", conflictOutput.toString(), "--step-limit", "10");
        assertEquals(3, conflictProcess.exitCode());
        JsonNode failed = JsonSupport.mapper().readTree(conflictOutput);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", failed.path("status").stringValue());
        assertFalse(failed.has("timetable"));
        assertFalse(failed.has("changeReport"));
    }

    @Test
    @DisplayName("UC-2 G5-G6: solver-chosen period and room-only moves have non-overlapping scores and reports")
    void classifiesSolverChosenMoves() throws Exception {
        Path current = produceCurrent();

        ObjectNode periodUpdate = updatedFromCurrent(current);
        ((ObjectNode) periodUpdate.withArray("cohorts").get(0))
                .putArray("availablePeriodIds").add("mon-1").add("mon-3");
        Path periodOutput = temporaryDirectory.resolve("period-move-result.json");
        ProcessResult periodProcess = run("replan", "--definition", write("period-update.json", periodUpdate).toString(),
                "--current", current.toString(), "--output", periodOutput.toString(), "--step-limit", "100");
        assertEquals(0, periodProcess.exitCode());
        JsonNode periodResult = JsonSupport.mapper().readTree(periodOutput);
        assertEquals(1, periodResult.path("score").path("periodMoves").longValue());
        assertEquals(0, periodResult.path("score").path("roomOnlyMoves").longValue());
        assertEquals("lesson-science-1",
                periodResult.path("changeReport").path("periodMoves").get(0).path("lessonId").stringValue());

        ObjectNode roomUpdate = updatedFromCurrent(current);
        ((ObjectNode) roomUpdate.withArray("rooms").get(1))
                .putArray("availablePeriodIds").add("mon-2").add("mon-3");
        Path roomOutput = temporaryDirectory.resolve("room-move-result.json");
        ProcessResult roomProcess = run("replan", "--definition", write("room-update.json", roomUpdate).toString(),
                "--current", current.toString(), "--output", roomOutput.toString(), "--step-limit", "100");
        assertEquals(0, roomProcess.exitCode());
        JsonNode roomResult = JsonSupport.mapper().readTree(roomOutput);
        assertEquals(0, roomResult.path("score").path("periodMoves").longValue());
        assertEquals(1, roomResult.path("score").path("roomOnlyMoves").longValue());
        assertEquals("lesson-math-1",
                roomResult.path("changeReport").path("roomOnlyMoves").get(0).path("lessonId").stringValue());
    }

    private Path produceCurrent() throws Exception {
        Path output = temporaryDirectory.resolve("current-" + System.nanoTime() + ".json");
        ProcessResult process = run("plan", "--definition", Path.of("examples", "initial-school.json").toString(),
                "--output", output.toString(), "--step-limit", "100");
        assertEquals(0, process.exitCode());
        return output;
    }

    private ObjectNode updatedFromCurrent(Path current) throws Exception {
        ObjectNode updated = (ObjectNode) JsonSupport.mapper().readTree(Path.of("examples", "initial-school.json"));
        updated.put("basedOnRevision", JsonSupport.mapper().readTree(current).path("inputRevision").stringValue());
        return updated;
    }

    private Path write(String name, JsonNode content) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        Files.write(path, JsonSupport.mapper().writeValueAsBytes(content));
        return path;
    }

    private static Schema resultSchema() throws Exception {
        try (InputStream input = ReplanCliIT.class.getResourceAsStream("/schema/result-v1.schema.json")) {
            return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(input);
        }
    }

    private static ProcessResult run(String... arguments) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(JAR.toString());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "packaged CLI did not terminate");
        return new ProcessResult(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
