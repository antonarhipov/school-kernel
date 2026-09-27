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

class HomeRoomCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("Curator RULE-4: catalog 8 holds a curator lesson in the home room over its own room preference")
    void curatorLessonIsHeldInTheHomeRoom() throws Exception {
        ObjectNode definition = curatorDefinition();
        JsonNode planned = plan("home-room", definition, 0);
        assertEquals("FEASIBLE", planned.path("status").stringValue());
        assertEquals(8, planned.path("catalogVersion").intValue());
        assertEquals("room-1", planned.path("timetable").path("assignments").get(0).path("roomId").stringValue());

        // With its home room closed in the only period, the class hour has nowhere to go.
        ((ObjectNode) definition.withArray("rooms").get(0)).putArray("availablePeriodIds");
        JsonNode refused = plan("home-room-closed", definition, 3);
        assertEquals("NO_FEASIBLE_SOLUTION_FOUND", refused.path("status").stringValue());
        assertFalse(refused.has("timetable"));
        assertEquals("[\"lesson-1\",\"room-1\"]",
                diagnostic(refused, KernelCatalog.COHORT_HOME_ROOM.id()).path("examples").get(0).toString());

        ObjectNode legacy = curatorDefinition();
        legacy.put("catalogVersion", 7);
        plan("curator-field-refused", legacy, 2);
    }

    /** One cohort whose curator teaches its class hour; the lesson prefers room-2 but the home room is room-1. */
    private static ObjectNode curatorDefinition() throws Exception {
        ObjectNode definition = (ObjectNode) JsonSupport.mapper().readTree(
                Path.of("src", "test", "resources", "fixtures", "valid-plan.json"));
        definition.put("catalogVersion", 8);
        ((ObjectNode) definition.withArray("subjects").get(0)).put("curatorLesson", true);
        ((ObjectNode) definition.withArray("cohorts").get(0)).put("curatorTeacherId", "teacher-1")
                .put("homeRoomId", "room-1");
        definition.withArray("rooms").addObject().put("id", "room-2").put("displayName", "Room Two")
                .put("capacity", 25).putArray("capabilityIds");
        ((ObjectNode) definition.withArray("lessons").get(0)).putArray("preferredRoomIds").add("room-2");
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
