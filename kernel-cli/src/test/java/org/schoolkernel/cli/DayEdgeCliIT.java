package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.domain.KernelCatalog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class DayEdgeCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;

    @Test
    void catalogSixRefusesAForcedLateStartThatCatalogFiveAccepts() throws Exception {
        ObjectNode definition = mondayDefinition("mon-2");
        ((ObjectNode) definition.withArray("cohorts").get(0)).put("latestStartSlot", 1);
        JsonNode refused = plan("late-start", definition, 3);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", refused.path("status").stringValue());
        assertTrue(!refused.has("timetable"));
        JsonNode diagnostic = diagnostic(refused, KernelCatalog.COHORT_LATEST_START.id());
        assertEquals("[\"cohort-1\",\"MONDAY\",\"mon-2\"]", diagnostic.path("examples").get(0).toString());

        // Nothing is reserved here, so mon-2 is the third regular slot.
        ((ObjectNode) definition.withArray("cohorts").get(0)).put("latestStartSlot", 2);
        plan("late-start-second", definition, 3);
        ((ObjectNode) definition.withArray("cohorts").get(0)).put("latestStartSlot", 3);
        assertEquals("FEASIBLE", plan("late-start-allowed", definition, 0).path("status").stringValue());

        ((ObjectNode) definition.withArray("cohorts").get(0)).remove("latestStartSlot");
        definition.put("catalogVersion", 5);
        assertEquals("FEASIBLE", plan("late-start-legacy", definition, 0).path("status").stringValue());
    }

    @Test
    void catalogSixRefusesAnEdgeOnlyLessonInTheMiddleOfTheDay() throws Exception {
        ObjectNode definition = mondayDefinition("mon-1", "mon-2", "mon-3");
        ObjectNode support = supportSubject(definition);
        support.put("dayEdgeOnly", true);
        ((ObjectNode) definition.withArray("lessons").get(1)).put("subjectId", "support");
        JsonNode refused = plan("middle-edge", definition, 3);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", refused.path("status").stringValue());
        assertEquals("[\"lesson-2\",\"cohort-1\",\"mon-2\"]",
                diagnostic(refused, KernelCatalog.SUBJECT_DAY_EDGE.id()).path("examples").get(0).toString());

        ((ObjectNode) definition.withArray("lessons").get(0)).put("periodLock", "mon-2");
        ((ObjectNode) definition.withArray("lessons").get(1)).put("periodLock", "mon-1");
        assertEquals("FEASIBLE", plan("first-edge", definition, 0).path("status").stringValue());

        support.remove("dayEdgeOnly");
        ((ObjectNode) definition.withArray("lessons").get(0)).put("periodLock", "mon-1");
        ((ObjectNode) definition.withArray("lessons").get(1)).put("periodLock", "mon-2");
        definition.put("catalogVersion", 5);
        assertEquals("FEASIBLE", plan("middle-legacy", definition, 0).path("status").stringValue());
    }

    @Test
    void aPermittedSubjectUsesAReservedPeriodAndTheResultVerifies() throws Exception {
        ObjectNode definition = mondayDefinition("mon-0", "mon-1");
        definition.putArray("reservedPeriodIds").add("mon-0");
        supportSubject(definition).put("reservedPeriodsAllowed", true)
                .put("maxWeeklyReservedLessonsPerCohort", 1).put("dayEdgeOnly", true);
        ((ObjectNode) definition.withArray("lessons").get(0)).put("subjectId", "support");
        Path definitionPath = write("reserved-permitted.json", definition);
        JsonNode result = plan("reserved-permitted", definition, 0);
        assertEquals(6, result.path("catalogVersion").intValue());
        assertEquals("mon-0", result.path("timetable").path("assignments").get(0).path("periodId").stringValue());

        Path verification = temporaryDirectory.resolve("reserved-permitted-verification.json");
        ProcessResult verified = run("verify", "--definition", definitionPath.toString(),
                "--result", temporaryDirectory.resolve("reserved-permitted-result.json").toString(),
                "--output", verification.toString());
        assertEquals(0, verified.exitCode(), verified.stderr());
        assertEquals("VERIFIED", JsonSupport.mapper().readTree(verification).path("status").stringValue());

        ((ObjectNode) definition.withArray("lessons").get(0)).put("subjectId", "math");
        write("reserved-refused.json", definition);
        ProcessResult invalid = run("plan", "--definition",
                temporaryDirectory.resolve("reserved-refused.json").toString(),
                "--output", temporaryDirectory.resolve("reserved-refused-result.json").toString());
        assertEquals(2, invalid.exitCode(), invalid.stderr());
    }

    /** One cohort and teacher on Monday, with one lesson locked to each given period. */
    private static ObjectNode mondayDefinition(String... lockedPeriods) throws Exception {
        ObjectNode definition = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "valid-plan.json"));
        definition.put("catalogVersion", 6);
        var periods = definition.withArray("periods");
        periods.removeAll();
        for (int slot = 0; slot <= 4; slot++) {
            periods.addObject().put("id", "mon-" + slot).put("displayName", "Monday " + slot)
                    .put("weekday", "MONDAY").put("order", slot + 1);
        }
        definition.withArray("subjects").addObject().put("id", "support").put("displayName", "Support");
        ((ObjectNode) definition.withArray("teachers").get(0)).withArray("qualifiedSubjectIds").add("support");
        var lessons = definition.withArray("lessons");
        ObjectNode template = (ObjectNode) lessons.get(0).deepCopy();
        lessons.removeAll();
        for (int index = 0; index < lockedPeriods.length; index++) {
            ObjectNode lesson = template.deepCopy();
            lesson.put("id", "lesson-" + (index + 1)).put("periodLock", lockedPeriods[index]);
            lessons.add(lesson);
        }
        return definition;
    }

    private static ObjectNode supportSubject(ObjectNode definition) {
        return (ObjectNode) definition.withArray("subjects").get(1);
    }

    private JsonNode plan(String name, ObjectNode definition, int expectedExit) throws Exception {
        Path output = temporaryDirectory.resolve(name + "-result.json");
        ProcessResult process = run("plan", "--definition", write(name + ".json", definition).toString(),
                "--output", output.toString(), "--step-limit", "10");
        assertEquals(expectedExit, process.exitCode(), process.stderr());
        return JsonSupport.mapper().readTree(output);
    }

    private static JsonNode diagnostic(JsonNode result, String constraintId) {
        return java.util.stream.StreamSupport.stream(
                        result.path("searchDiagnostics").path("constraints").spliterator(), false)
                .filter(value -> constraintId.equals(value.path("constraintId").stringValue()))
                .findFirst().orElseThrow();
    }

    private Path write(String name, JsonNode value) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        Files.write(path, JsonSupport.mapper().writeValueAsBytes(value));
        return path;
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
