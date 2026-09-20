package org.schoolkernel.contract;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import org.schoolkernel.domain.ValidationError;
import org.schoolkernel.domain.ValidationReport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

public final class CurrentTimetableReader {
    public record Assignment(
            String lessonId,
            String subjectId,
            String cohortId,
            String teacherId,
            String periodId,
            String roomId) {}

    public record CurrentTimetable(
            int schemaVersion,
            String schoolId,
            String inputRevision,
            String timetableRevision,
            List<Assignment> assignments) {}

    public record Outcome(CurrentTimetable timetable, ValidationReport report) {}

    private final Schema schema;
    private final RevisionService revisions = new RevisionService();

    public CurrentTimetableReader() {
        try (InputStream input = CurrentTimetableReader.class.getResourceAsStream("/schema/result-v1.schema.json")) {
            if (input == null) {
                throw new IllegalStateException("Bundled result schema is missing");
            }
            schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(input);
            schema.initializeValidators();
        } catch (IOException exception) {
            throw new IllegalStateException("Bundled result schema cannot be loaded", exception);
        }
    }

    public Outcome read(JsonNode input) {
        var errors = new ArrayList<ValidationError>();
        schema.validate(input).forEach(error -> errors.add(new ValidationError(
                error.getInstanceLocation().toString(), List.of(), error.getMessage())));
        if (!errors.isEmpty()) {
            return new Outcome(null, ValidationReport.from(errors));
        }
        if (!"FEASIBLE".equals(input.path("status").stringValue())) {
            errors.add(new ValidationError("/status", List.of(), "current timetable must have FEASIBLE status"));
            return new Outcome(null, ValidationReport.from(errors));
        }
        ArrayNode assignmentsNode = (ArrayNode) input.path("timetable").path("assignments");
        String expectedRevision = revisions.timetableRevision(
                input.path("schemaVersion").intValue(),
                input.path("schoolId").stringValue(),
                input.path("inputRevision").stringValue(),
                assignmentsNode);
        if (!expectedRevision.equals(input.path("timetableRevision").stringValue())) {
            errors.add(new ValidationError(
                    "/timetableRevision", List.of(), "current timetable revision does not match its assignments"));
            return new Outcome(null, ValidationReport.from(errors));
        }
        var assignments = new ArrayList<Assignment>();
        var lessonIds = new HashSet<String>();
        var duplicateLessonIds = new TreeSet<String>();
        assignmentsNode.forEach(node -> {
            String lessonId = node.path("lessonId").stringValue();
            if (!lessonIds.add(lessonId)) {
                duplicateLessonIds.add(lessonId);
            }
            assignments.add(new Assignment(
                    lessonId,
                    node.path("subjectId").stringValue(),
                    node.path("cohortId").stringValue(),
                    node.path("teacherId").stringValue(),
                    node.path("periodId").stringValue(),
                    node.path("roomId").stringValue()));
        });
        duplicateLessonIds.forEach(lessonId -> errors.add(new ValidationError(
                "/timetable/assignments", List.of(lessonId),
                "current timetable contains more than one assignment for the lesson")));
        if (!errors.isEmpty()) {
            return new Outcome(null, ValidationReport.from(errors));
        }
        assignments.sort(Comparator.comparing(Assignment::lessonId));
        return new Outcome(new CurrentTimetable(
                input.path("schemaVersion").intValue(),
                input.path("schoolId").stringValue(),
                input.path("inputRevision").stringValue(),
                input.path("timetableRevision").stringValue(),
                List.copyOf(assignments)),
                ValidationReport.from(List.of()));
    }
}
