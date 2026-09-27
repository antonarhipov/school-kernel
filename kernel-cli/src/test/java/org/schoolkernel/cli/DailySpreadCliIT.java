package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
import org.schoolkernel.domain.KernelCatalog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class DailySpreadCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("Daily spread RULE-4: catalog 7 refuses a forced spread beyond the limit that catalog 6 accepts")
    void catalogSevenRefusesAForcedSpreadThatCatalogSixAccepts() throws Exception {
        // Both lessons are locked to Monday, so Monday has two lessons and Tuesday none.
        ObjectNode definition = twoDayDefinition("mon-1", "mon-2");
        ObjectNode cohort = (ObjectNode) definition.withArray("cohorts").get(0);
        cohort.put("dailyLessonSpreadLimit", 1);
        JsonNode refused = plan("forced-spread", definition, 3);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", refused.path("status").stringValue());
        assertFalse(refused.has("timetable"));
        JsonNode diagnostic = diagnostic(refused, KernelCatalog.COHORT_DAILY_SPREAD.id());
        assertEquals(1, diagnostic.path("matchCount").intValue());
        assertEquals("[\"cohort-1\",\"MONDAY\",\"TUESDAY\"]", diagnostic.path("examples").get(0).toString());

        cohort.put("dailyLessonSpreadLimit", 2);
        JsonNode allowed = plan("spread-allowed", definition, 0);
        assertEquals("FEASIBLE", allowed.path("status").stringValue());
        assertEquals(7, allowed.path("catalogVersion").intValue());

        definition.put("catalogVersion", 6);
        plan("spread-field-refused", definition, 2);
        cohort.remove("dailyLessonSpreadLimit");
        assertEquals("FEASIBLE", plan("spread-legacy", definition, 0).path("status").stringValue());
    }

    /** One cohort and teacher over Monday and Tuesday, with one lesson locked to each given period. */
    private static ObjectNode twoDayDefinition(String... lockedPeriods) throws Exception {
        ObjectNode definition = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "valid-plan.json"));
        definition.put("catalogVersion", 7);
        var periods = definition.withArray("periods");
        periods.removeAll();
        int order = 1;
        for (String day : List.of("MONDAY", "TUESDAY")) {
            String prefix = day.substring(0, 3).toLowerCase();
            for (int slot = 1; slot <= 2; slot++) {
                periods.addObject().put("id", prefix + "-" + slot).put("displayName", day + " " + slot)
                        .put("weekday", day).put("order", order++);
            }
        }
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

    private JsonNode plan(String name, ObjectNode definition, int expectedExit) throws Exception {
        Path output = temporaryDirectory.resolve(name + "-result.json");
        ProcessResult process = run("plan", "--definition", write(name + ".json", definition).toString(),
                "--output", output.toString(), "--step-limit", "10");
        assertEquals(expectedExit, process.exitCode(), process.stderr());
        return Files.exists(output) ? JsonSupport.mapper().readTree(output) : null;
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
