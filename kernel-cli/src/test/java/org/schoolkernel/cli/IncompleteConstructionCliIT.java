package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class IncompleteConstructionCliIT {
    @TempDir Path directory;

    @Test
    void boundedConstructionHasTheSameSafeOutcomeForPlanningAndRepair() throws Exception {
        Path definition = Path.of("src", "test", "resources", "fixtures", "initial-school.json").toAbsolutePath();
        byte[] original = Files.readAllBytes(definition);
        Path partialPlan = directory.resolve("partial-plan.json");
        assertEquals(3, run("plan", "--definition", definition.toString(), "--output", partialPlan.toString(),
                "--time-limit", "1ms"));
        assertIncomplete(partialPlan);

        Path current = directory.resolve("current.json");
        assertEquals(0, run("plan", "--definition", definition.toString(), "--output", current.toString(),
                "--step-limit", "100"));
        byte[] currentBytes = Files.readAllBytes(current);
        assertEquals("FEASIBLE", read(current).path("status").stringValue());

        ObjectNode successor = (ObjectNode) read(definition);
        successor.put("basedOnRevision", read(current).path("inputRevision").stringValue());
        ObjectNode added = ((ArrayNode) successor.path("lessons")).addObject();
        added.put("id", "lesson-math-2");
        added.put("displayName", "Mathematics 2");
        added.put("subjectId", "math");
        added.put("cohortId", "cohort-7a");
        added.put("teacherId", "teacher-alex");
        Path successorPath = directory.resolve("successor.json");
        Files.write(successorPath, JsonSupport.mapper().writeValueAsBytes(successor));
        Path partialRepair = directory.resolve("partial-repair.json");
        assertEquals(3, run("replan", "--current-definition", definition.toString(),
                "--definition", successorPath.toString(), "--current", current.toString(),
                "--output", partialRepair.toString(), "--time-limit", "1ms"));
        assertIncomplete(partialRepair);
        assertArrayEquals(currentBytes, Files.readAllBytes(current));
        assertArrayEquals(original, Files.readAllBytes(definition));

        Path invalid = directory.resolve("invalid.json");
        Files.writeString(invalid, "{invalid");
        Path invalidResult = directory.resolve("invalid-result.json");
        assertEquals(2, run("plan", "--definition", invalid.toString(), "--output", invalidResult.toString(),
                "--time-limit", "1ms"));
        assertEquals("INVALID_INPUT", read(invalidResult).path("status").stringValue());
        assertEquals(74, run("plan", "--definition", directory.resolve("missing.json").toString(),
                "--output", directory.resolve("absent.json").toString(), "--time-limit", "1ms"));
        assertFalse(Files.exists(directory.resolve("absent.json")));
    }

    private void assertIncomplete(Path path) throws Exception {
        JsonNode result = read(path);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", result.path("status").stringValue());
        assertEquals("TIME_LIMIT", result.path("terminationReason").stringValue());
        assertTrue(result.path("safeMessage").stringValue().contains("before all lessons were placed"));
        for (String field : List.of("timetable", "timetableRevision", "score", "changeReport")) {
            assertFalse(result.has(field), field);
        }
        assertEquals(0, result.path("searchDiagnostics").path("totalMatches").longValue());
        try (var schema = getClass().getResourceAsStream("/schema/result-v1.schema.json")) {
            assertTrue(SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                    .getSchema(schema).validate(result).isEmpty());
        }
    }

    private static JsonNode read(Path path) {
        return JsonSupport.mapper().readTree(path);
    }

    private int run(String... arguments) throws Exception {
        var command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-jar", Path.of("target", "school-kernel.jar").toAbsolutePath().toString()));
        command.addAll(List.of(arguments));
        Path stderr = directory.resolve("process-" + System.nanoTime() + ".log");
        Process process = new ProcessBuilder(command).redirectError(stderr.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "packaged kernel must finish");
            assertEquals("", new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            return process.exitValue();
        } finally {
            process.destroyForcibly();
        }
    }
}
