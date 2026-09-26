package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.RevisionService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class ReservedPeriodCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;
    private int invalidSequence;

    @Test
    void uc1MainAndG1ReserveExplicitNonZeroPeriodWithoutChangingResourceAvailability() throws Exception {
        ObjectNode definition = base();
        addPeriod(definition, "mon-2", 2);
        definition.putArray("reservedPeriodIds").add("mon-1");
        Path input = write("reserved.json", definition);
        byte[] inputBefore = Files.readAllBytes(input);
        Path resultPath = temporaryDirectory.resolve("reserved-result.json");

        ProcessResult planned = run("plan", "--definition", input.toString(), "--output",
                resultPath.toString(), "--step-limit", "100");
        assertEquals(0, planned.exitCode(), planned.stderr());
        JsonNode result = read(resultPath);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        assertEquals("mon-2", assignmentPeriod(result));
        assertArrayEquals(inputBefore, Files.readAllBytes(input));
        assertFalse(definition.path("teachers").get(0).has("availablePeriodIds"));
        assertFalse(definition.path("cohorts").get(0).has("availablePeriodIds"));
        assertFalse(definition.path("rooms").get(0).has("availablePeriodIds"));

        Path verification = temporaryDirectory.resolve("reserved-verification.json");
        ProcessResult verified = run("verify", "--definition", input.toString(), "--result",
                resultPath.toString(), "--output", verification.toString());
        assertEquals(0, verified.exitCode(), verified.stderr());
        assertEquals("VERIFIED", read(verification).path("status").stringValue());
    }

    @Test
    void uc1Extensions2bAnd2cRefuseInvalidPolicyAndImpossibleRegularRange() throws Exception {
        ObjectNode unknown = base();
        unknown.putArray("reservedPeriodIds").add("missing");
        assertInvalid(unknown, "/reservedPeriodIds/0");

        ObjectNode duplicate = base();
        duplicate.putArray("reservedPeriodIds").add("mon-1").add("mon-1");
        assertInvalid(duplicate, "reservedPeriodIds");

        ObjectNode locked = base();
        locked.putArray("reservedPeriodIds").add("mon-1");
        ((ObjectNode) locked.withArray("lessons").get(0)).put("periodLock", "mon-1");
        assertInvalid(locked, "/lessons/0/periodLock");

        ObjectNode impossible = base();
        impossible.putArray("reservedPeriodIds").add("mon-1");
        Path input = write("all-reserved.json", impossible);
        Path output = temporaryDirectory.resolve("all-reserved-result.json");
        ProcessResult planned = run("plan", "--definition", input.toString(), "--output",
                output.toString(), "--step-limit", "100");
        assertEquals(3, planned.exitCode(), planned.stderr());
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", read(output).path("status").stringValue());
        assertFalse(read(output).has("timetable"));
        assertFalse(planned.stderr().contains("Solving started"));
    }

    @Test
    void uc1Extensions1aAnd1bRepairMovesOldAssignmentAndPreservesPredecessor() throws Exception {
        ObjectNode predecessor = base();
        Path predecessorPath = write("predecessor.json", predecessor);
        Path currentPath = temporaryDirectory.resolve("current.json");
        ProcessResult initial = run("plan", "--definition", predecessorPath.toString(),
                "--output", currentPath.toString(), "--step-limit", "100");
        assertEquals(0, initial.exitCode(), initial.stderr());
        byte[] predecessorBefore = Files.readAllBytes(predecessorPath);
        byte[] currentBefore = Files.readAllBytes(currentPath);
        assertEquals("mon-1", assignmentPeriod(read(currentPath)));

        ObjectNode successor = predecessor.deepCopy();
        successor.put("basedOnRevision", read(currentPath).path("inputRevision").stringValue());
        addPeriod(successor, "mon-2", 2);
        successor.putArray("reservedPeriodIds").add("mon-1");
        Path successorPath = write("successor.json", successor);
        Path proposalPath = temporaryDirectory.resolve("proposal.json");
        ProcessResult repaired = run("replan", "--current-definition", predecessorPath.toString(),
                "--current", currentPath.toString(), "--definition", successorPath.toString(),
                "--output", proposalPath.toString(), "--step-limit", "100");
        assertEquals(0, repaired.exitCode(), repaired.stderr());
        JsonNode proposal = read(proposalPath);
        assertEquals("FEASIBLE", proposal.path("status").stringValue());
        assertEquals("mon-2", assignmentPeriod(proposal));
        assertEquals(1, proposal.path("score").path("periodMoves").longValue());
        assertArrayEquals(predecessorBefore, Files.readAllBytes(predecessorPath));
        assertArrayEquals(currentBefore, Files.readAllBytes(currentPath));

        Path verificationPath = temporaryDirectory.resolve("successor-verification.json");
        ProcessResult verified = run("verify", "--definition", successorPath.toString(),
                "--result", proposalPath.toString(), "--output", verificationPath.toString());
        assertEquals(0, verified.exitCode(), verified.stderr());
        assertEquals("VERIFIED", read(verificationPath).path("status").stringValue());

        ObjectNode forged = (ObjectNode) proposal.deepCopy();
        ArrayNode assignments = (ArrayNode) forged.path("timetable").path("assignments");
        ((ObjectNode) assignments.get(0)).put("periodId", "mon-1");
        forged.put("timetableRevision", new RevisionService().timetableRevision(
                1, "test-school", proposal.path("inputRevision").stringValue(), assignments));
        Path forgedPath = write("forged-reserved-result.json", forged);
        Path rejectedPath = temporaryDirectory.resolve("forged-verification.json");
        ProcessResult rejected = run("verify", "--definition", successorPath.toString(),
                "--result", forgedPath.toString(), "--output", rejectedPath.toString());
        assertEquals(2, rejected.exitCode(), rejected.stderr());
        JsonNode report = read(rejectedPath);
        assertEquals("INVALID_INPUT", report.path("status").stringValue());
        assertTrue(report.path("validationReport").path("errors").toString().contains("reserved period"));
    }

    @Test
    void uc1G3QualityCountsRegularPeriodsOnlyAndG4PreservesLegacyMeaning() throws Exception {
        ObjectNode definition = base();
        definition.put("catalogVersion", 3);
        addPeriod(definition, "mon-2", 2);
        addPeriod(definition, "mon-3", 3);
        addPeriod(definition, "mon-4", 4);
        ((ObjectNode) definition.withArray("lessons").get(0)).put("periodLock", "mon-4");
        definition.putArray("reservedPeriodIds").add("mon-1");
        JsonNode reserved = plan("start-reserved", definition);
        assertEquals(0, matches(reserved, "soft.cohort-late-start"),
                "mon-4 is the third regular slot after mon-1 is reserved");

        ObjectNode legacy = definition.deepCopy();
        legacy.remove("reservedPeriodIds");
        JsonNode unreserved = plan("start-unreserved", legacy);
        assertEquals(1, matches(unreserved, "soft.cohort-late-start"));

        ObjectNode gap = definition.deepCopy();
        gap.putArray("reservedPeriodIds").add("mon-3");
        ObjectNode second = ((ObjectNode) gap.withArray("lessons").get(0)).deepCopy();
        second.put("id", "lesson-2");
        second.put("periodLock", "mon-2");
        gap.withArray("lessons").add(second);
        JsonNode reservedGap = plan("gap-reserved", gap);
        assertEquals(0, matches(reservedGap, "soft.teacher-gap"));
        assertEquals(0, matches(reservedGap, "soft.cohort-gap"));

        gap.remove("reservedPeriodIds");
        JsonNode unreservedGap = plan("gap-unreserved", gap);
        assertEquals(1, matches(unreservedGap, "soft.teacher-gap"));
        assertEquals(1, matches(unreservedGap, "soft.cohort-gap"));

        RevisionService revisions = new RevisionService();
        ObjectNode reordered = definition.deepCopy();
        reordered.putArray("reservedPeriodIds").add("mon-3").add("mon-1");
        ObjectNode sameSet = definition.deepCopy();
        sameSet.putArray("reservedPeriodIds").add("mon-1").add("mon-3");
        assertEquals(revisions.definitionRevision(reordered), revisions.definitionRevision(sameSet));
        assertNotEquals(revisions.definitionRevision(reordered), revisions.definitionRevision(legacy));
    }

    @Test
    void uc1G1DoesNotInferReservationFromZeroSuffixAndEmptyListChangesNoPlacement() throws Exception {
        ObjectNode definition = base();
        ((ObjectNode) definition.withArray("periods").get(0)).put("id", "mon-0");
        JsonNode unconfigured = plan("zero-unconfigured", definition);
        assertEquals("mon-0", assignmentPeriod(unconfigured));

        ObjectNode explicitlyEmpty = definition.deepCopy();
        explicitlyEmpty.putArray("reservedPeriodIds");
        JsonNode empty = plan("zero-empty-reservations", explicitlyEmpty);
        assertEquals("mon-0", assignmentPeriod(empty));
        assertNotEquals(new RevisionService().definitionRevision(definition),
                new RevisionService().definitionRevision(explicitlyEmpty));
    }

    @Test
    void uc1G3DayWithOnlyReservedPeriodIsNotAvailableForWeekBalance() throws Exception {
        ObjectNode definition = base();
        definition.put("catalogVersion", 3);
        addPeriodDay(definition, "tue-1", "TUESDAY", 1);
        addPeriodDay(definition, "tue-2", "TUESDAY", 2);
        addPeriodDay(definition, "tue-3", "TUESDAY", 3);
        ArrayNode lessons = definition.withArray("lessons");
        ObjectNode first = (ObjectNode) lessons.get(0);
        first.put("periodLock", "tue-1");
        for (int index = 2; index <= 3; index++) {
            ObjectNode next = first.deepCopy();
            next.put("id", "lesson-" + index);
            next.put("periodLock", "tue-" + index);
            lessons.add(next);
        }
        definition.putArray("reservedPeriodIds").add("mon-1");
        JsonNode reserved = plan("balance-reserved-monday", definition);
        assertEquals(0, matches(reserved, "soft.cohort-week-balance"));

        definition.remove("reservedPeriodIds");
        JsonNode unreserved = plan("balance-unreserved-monday", definition);
        assertEquals(2, matches(unreserved, "soft.cohort-week-balance"));
    }

    private void assertInvalid(ObjectNode definition, String expectedLocationText) throws Exception {
        Path input = write("invalid-" + invalidSequence++ + ".json", definition);
        Path output = temporaryDirectory.resolve(input.getFileName() + "-result.json");
        ProcessResult process = run("plan", "--definition", input.toString(), "--output",
                output.toString(), "--step-limit", "100");
        assertEquals(2, process.exitCode(), process.stderr());
        JsonNode result = read(output);
        assertEquals("INVALID_INPUT", result.path("status").stringValue());
        assertFalse(result.has("timetable"));
        assertTrue(result.path("validationReport").path("errors").toString().contains(expectedLocationText),
                result.path("validationReport").toString());
        assertFalse(process.stderr().contains("Solving started"));
    }

    private JsonNode plan(String name, ObjectNode definition) throws Exception {
        Path input = write(name + ".json", definition);
        Path output = temporaryDirectory.resolve(name + "-result.json");
        ProcessResult process = run("plan", "--definition", input.toString(), "--output",
                output.toString(), "--step-limit", "100");
        assertEquals(0, process.exitCode(), process.stderr());
        JsonNode result = read(output);
        assertEquals("FEASIBLE", result.path("status").stringValue());
        return result;
    }

    private static long matches(JsonNode result, String id) {
        return StreamSupport.stream(result.path("score").path("constraintBreakdown").spliterator(), false)
                .filter(row -> id.equals(row.path("constraintId").stringValue()))
                .findFirst().orElseThrow().path("matchCount").longValue();
    }

    private static String assignmentPeriod(JsonNode result) {
        return result.path("timetable").path("assignments").get(0).path("periodId").stringValue();
    }

    private static ObjectNode base() throws Exception {
        return (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "valid-plan.json"));
    }

    private static void addPeriod(ObjectNode definition, String id, int order) {
        addPeriodDay(definition, id, "MONDAY", order);
    }

    private static void addPeriodDay(ObjectNode definition, String id, String weekday, int order) {
        definition.withArray("periods").addObject()
                .put("id", id).put("displayName", id).put("weekday", weekday).put("order", order);
    }

    private Path write(String name, JsonNode value) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        Files.write(path, JsonSupport.mapper().writeValueAsBytes(value));
        return path;
    }

    private static JsonNode read(Path path) throws Exception {
        return JsonSupport.mapper().readTree(path);
    }

    private static ProcessResult run(String... arguments) throws Exception {
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
        return new ProcessResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
