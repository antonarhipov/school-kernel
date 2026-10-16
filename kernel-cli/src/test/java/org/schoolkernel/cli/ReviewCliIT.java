package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.*;

class ReviewCliIT {
    @TempDir Path temporary;
    private final Path samples = Path.of("../timetable-workspace/src/main/resources/demo/timetables");
    @Test void preservesExactAssignmentsAndProducesVerifiedActualScoreWithoutSolving() throws Exception {
        var source = read(samples.resolve("full-a-result.json"));
        var output = review(read(samples.resolve("full-definition.json")), source.path("timetable").path("assignments"), 0);
        assertTrue(output.path("valid").asBoolean());
        assertEquals(source.path("timetable"), output.path("result").path("timetable"));
        assertEquals(source.path("score"), output.path("result").path("score"));
        assertEquals(19, output.path("hardMatchCounts").size()); assertEquals(7, output.path("softMatchCounts").size());
        assertEquals("MANUAL_REVIEW", output.path("result").path("terminationReason").asString());
        assertEquals("NONE", output.path("result").path("limit").path("type").asString());
        assertTrue(new CurrentTimetableReader().read(output.path("result")).report().isValid());
    }
    @Test void reportsAllCollisionParticipantsAndRoomEquipmentWithoutPublishingResult() throws Exception {
        var definition = read(samples.resolve("two-class-definition.json"));
        var assignments = (ArrayNode)read(samples.resolve("two-class-a-result.json")).path("timetable").path("assignments").deepCopy();
        var first = (ObjectNode)assignments.get(0); var second = assignments.get(1);
        first.put("periodId", second.path("periodId").asString()); first.put("roomId", second.path("roomId").asString());
        var output = review(definition, assignments, 0);
        assertFalse(output.path("valid").asBoolean()); assertFalse(output.has("result"));
        var conflict = java.util.stream.StreamSupport.stream(output.path("conflicts").spliterator(),false)
                .filter(c -> c.path("constraintId").asString().equals("hard.cohort-period")).findFirst().orElseThrow();
        assertTrue(conflict.path("lessonIds").toString().contains(first.path("lessonId").asString()));
        assertTrue(conflict.path("lessonIds").toString().contains(second.path("lessonId").asString()));
        assertTrue(output.path("hardMatchCounts").path("hard.room-period").asLong() > 0);
    }
    @Test void unqualifiedTeacherAndIncompleteAssignmentsAreLocatedInvalidInput() throws Exception {
        var definition = (ObjectNode)read(samples.resolve("two-class-definition.json")).deepCopy();
        var assignments = read(samples.resolve("two-class-a-result.json")).path("timetable").path("assignments");
        ((ObjectNode)definition.path("lessons").get(0)).put("teacherId", "demo-teacher-extra-pe");
        var invalid = review(definition,assignments,2);
        assertTrue(invalid.path("validationReport").toString().contains("not qualified")); assertFalse(invalid.has("result"));
        definition = (ObjectNode)read(samples.resolve("two-class-definition.json"));
        var missing = assignments.deepCopy(); ((ArrayNode)missing).remove(0);
        assertTrue(review(definition,missing,2).path("validationReport").toString().contains("missing"));
    }
    private JsonNode review(JsonNode definition, JsonNode assignments, int exit) throws Exception {
        Path d=temporary.resolve("definition.json"), a=temporary.resolve("assignments.json"), o=temporary.resolve("review.json");
        Files.write(d,JsonSupport.mapper().writeValueAsBytes(definition)); Files.write(a,JsonSupport.mapper().writeValueAsBytes(assignments));
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-jar","target/school-kernel.jar","review",
                "--definition",d.toString(),"--assignments",a.toString(),"--output",o.toString(),"--force").redirectErrorStream(true).redirectOutput(temporary.resolve("process.log").toFile()).start();
        assertTrue(process.waitFor(20,TimeUnit.SECONDS)); assertEquals(exit,process.exitValue(),Files.readString(temporary.resolve("process.log")));
        return read(o);
    }
    private static JsonNode read(Path path) { return JsonSupport.mapper().readTree(path); }
}
