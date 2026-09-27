package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Workspace documents used by the browser journeys: small demo schools, the 1,000-lesson scale school, proposals. */
final class WorkbenchFixtures {
    static final ObjectMapper JSON = JsonMapper.builder().build();
    static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    private final KernelVerifier verifier;
    private final ProposalReviewService reviews;

    WorkbenchFixtures(KernelVerifier verifier, ProposalReviewService reviews) {
        this.verifier = verifier;
        this.reviews = reviews;
    }

    static Set<String> jsonStrings(JsonNode array) {
        Set<String> values = new HashSet<>();
        array.forEach(item -> values.add(item.stringValue()));
        assertEquals(array.size(), values.size(), "lesson IDs and pin sources must be unique");
        return values;
    }

    static ObjectNode assignment(String lesson, String subject, String cohort, String teacher, String period, String room) {
        return JSON.createObjectNode().put("lessonId", lesson).put("subjectId", subject).put("cohortId", cohort)
                .put("teacherId", teacher).put("periodId", period).put("roomId", room);
    }

    ObjectNode acceptedDocument(boolean empty) {
        ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile());
        definition.withArray("periods").addObject().put("id", "tue-1").put("displayName", "Tuesday 1")
                .put("weekday", "TUESDAY").put("order", 1);
        ((ObjectNode) definition.path("teachers").get(0)).putArray("availablePeriodIds").add("mon-1").add("tue-1");
        if (empty) definition.putArray("lessons");
        ObjectNode result = JSON.createObjectNode();
        result.put("status", "FEASIBLE");
        var assignments = result.putObject("timetable").putArray("assignments");
        if (!empty) {
            assignments.add(assignment("lesson-math-1", "math", "cohort-7a", "teacher-alex", "mon-1", "room-102"));
            assignments.add(assignment("lesson-science-1", "science", "cohort-7a", "teacher-alex", "mon-2", "room-101"));
        }
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "opaque-school-id").put("displayName", "Demo School");
        document.put("definitionRevision", "sha256:definition");
        document.put("timetableRevision", "sha256:timetable");
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition);
        baseline.set("result", result);
        baseline.putObject("manifest").put("manifestVersion", 1);
        return document;
    }

    ObjectNode validAcceptedDocument() {
        JsonNode definition = JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile());
        JsonNode result = JSON.readTree(Path.of("src/test/resources/uc5-accepted-result.json").toFile());
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "demo-school").put("displayName", "Demo School");
        document.put("definitionRevision", result.path("inputRevision").stringValue());
        document.put("timetableRevision", result.path("timetableRevision").stringValue());
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition);
        baseline.set("result", result);
        ObjectNode manifest = baseline.putObject("manifest");
        manifest.put("manifestVersion", 1).put("definitionSchemaVersion", 1).put("resultSchemaVersion", 1)
                .put("catalogVersion", 1).put("schoolId", "demo-school")
                .put("inputRevision", result.path("inputRevision").stringValue())
                .put("timetableRevision", result.path("timetableRevision").stringValue()).putArray("locks");
        return document;
    }

    ObjectNode scaleDocument() {
        ObjectNode definition = JSON.createObjectNode();
        definition.put("schemaVersion", 1).put("catalogVersion", 1).put("schoolId", "opaque-scale-school")
                .put("displayName", "Scale School");
        var subjects = definition.putArray("subjects");
        for (int i = 0; i < 20; i++) subjects.addObject().put("id", "subject-" + i).put("displayName", "Subject " + i);
        var teachers = definition.putArray("teachers");
        for (int i = 0; i < 100; i++) teachers.addObject().put("id", "teacher-" + i).put("displayName", "Teacher " + i)
                .putArray("qualifiedSubjectIds").add("subject-" + (i % 20));
        var cohorts = definition.putArray("cohorts");
        for (int i = 0; i < 60; i++) cohorts.addObject().put("id", "cohort-" + i).put("displayName", "Class " + i).put("size", 25);
        var rooms = definition.putArray("rooms");
        for (int i = 0; i < 100; i++) rooms.addObject().put("id", "room-" + i).put("displayName", "Room " + i).put("capacity", 30).putArray("capabilityIds");
        String[] weekdays = { "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" };
        var periods = definition.putArray("periods");
        for (int i = 0; i < 60; i++) periods.addObject().put("id", "period-" + i)
                .put("displayName", "Declared period " + i).put("weekday", weekdays[i / 12]).put("order", i % 12 + 1);
        var lessons = definition.putArray("lessons");
        ObjectNode result = JSON.createObjectNode().put("status", "FEASIBLE");
        var assignments = result.putObject("timetable").putArray("assignments");
        for (int i = 0; i < 1_000; i++) {
            int group = i / 60;
            String subject = "subject-" + (group % 20);
            String cohort = "cohort-" + group;
            String teacher = "teacher-" + group;
            String room = "room-" + group;
            String lesson = "lesson-" + i;
            lessons.addObject().put("id", lesson).put("displayName", "Declared lesson " + i).put("subjectId", subject)
                    .put("cohortId", cohort).put("teacherId", teacher);
            assignments.add(assignment(lesson, subject, cohort, teacher, "period-" + (i % 60), room));
        }
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "opaque-scale-school").put("displayName", "Scale School");
        document.put("definitionRevision", "sha256:scale-definition").put("timetableRevision", "sha256:scale-timetable");
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition); baseline.set("result", result); baseline.putObject("manifest").put("manifestVersion", 1);
        return document;
    }

    /** The scale school with long authoritative names and teacher-16 availability, verified by the packaged kernel. */
    ObjectNode investigationScaleDocument() throws Exception {
        ObjectNode document = scaleDocument();
        JsonNode baseline = document.path("acceptedBaseline");
        JsonNode definition = baseline.path("definition");
        ((ObjectNode) definition.path("subjects").get(0)).put("displayName", "Subject Zero with a deliberately long authoritative display name for timetable tiles");
        ((ObjectNode) definition.path("teachers").get(16)).put("displayName", "Teacher Sixteen with a deliberately long authoritative display name for timetable tiles");
        ((ObjectNode) definition.path("cohorts").get(16)).put("displayName", "Class Sixteen with a deliberately long authoritative display name for timetable tiles");
        ((ObjectNode) definition.path("rooms").get(16)).put("displayName", "Room Sixteen with a deliberately long authoritative display name for timetable tiles");
        ObjectNode selectedTeacher = (ObjectNode) definition.path("teachers").get(16);
        selectedTeacher.withArray("qualifiedSubjectIds").add("subject-0");
        var available = selectedTeacher.putArray("availablePeriodIds");
        for (int i = 0; i <= 40; i++) available.add("period-" + i);
        ((ObjectNode) definition.path("lessons").get(960)).put("subjectId", "subject-0");
        ((ObjectNode) baseline.path("result").path("timetable").path("assignments").get(960)).put("subjectId", "subject-0");
        String revision = verifier.verify(new ImportDocuments(definition, null, null,
                ImportDocuments.ImportMode.INITIAL_DEFINITION)).definitionRevision();
        ObjectNode result = (ObjectNode) JSON.readTree(Path.of("src/test/resources/uc5-accepted-result.json").toFile());
        result.put("schoolId", "opaque-scale-school").put("inputRevision", revision).put("correlationId", "uc2-scale-generated");
        result.set("timetable", baseline.path("result").path("timetable").deepCopy());
        String timetableRevision = timetableRevision(result, revision);
        result.put("timetableRevision", timetableRevision);
        document.put("definitionRevision", revision).put("timetableRevision", timetableRevision);
        ((ObjectNode) baseline).set("result", result);
        ObjectNode manifest = (ObjectNode) baseline.path("manifest");
        manifest.put("definitionSchemaVersion", 1).put("resultSchemaVersion", 1).put("catalogVersion", 1)
                .put("schoolId", "opaque-scale-school").put("inputRevision", revision)
                .put("timetableRevision", timetableRevision).putArray("locks");
        KernelVerifier.Verification verified = verifier.verify(new ImportDocuments(definition, result, null,
                ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        assertEquals(revision, verified.definitionRevision(), "normative accepted definition must pass the packaged verifier");
        assertEquals(timetableRevision, verified.timetableRevision(), "normative accepted result must pass the packaged verifier");
        return document;
    }

    /** Kernel stand-in: moves lesson-960 to period-40 and lesson-0 to room-50, then verifies with the real kernel. */
    ObjectNode verifiedNormativeRepairResult(List<String> arguments) {
        try {
            JsonNode acceptedDefinition = JSON.readTree(Path.of(arguments.get(arguments.indexOf("--current-definition") + 1)).toFile());
            JsonNode acceptedResult = JSON.readTree(Path.of(arguments.get(arguments.indexOf("--current") + 1)).toFile());
            JsonNode successor = JSON.readTree(Path.of(arguments.get(arguments.indexOf("--definition") + 1)).toFile());
            KernelVerifier.Verification original = verifier.verify(new ImportDocuments(acceptedDefinition, acceptedResult,
                    null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
            assertEquals(acceptedResult.path("timetableRevision").stringValue(), original.timetableRevision());
            assertEquals(original.definitionRevision(), normalizedDefinitionRevision(acceptedDefinition));
            assertFalse(jsonStrings(successor.path("teachers").get(16).path("availablePeriodIds")).contains("period-0"));
            assertEquals("room-8", successor.path("lessons").get(500).path("roomLock").stringValue());
            String revision = normalizedDefinitionRevision(successor);
            ObjectNode result = (ObjectNode) acceptedResult.deepCopy();
            result.put("correlationId", arguments.get(arguments.indexOf("--correlation-id") + 1));
            result.put("inputRevision", revision).put("elapsedTimeMs", 2004).put("terminationReason", "TIME_LIMIT");
            result.putObject("limit").put("type", "TIME").put("duration", "PT1M");
            ObjectNode moved = (ObjectNode) result.path("timetable").path("assignments").get(960);
            ObjectNode roomOnly = (ObjectNode) result.path("timetable").path("assignments").get(0);
            assertEquals("period-0", moved.path("periodId").stringValue());
            assertEquals("room-0", roomOnly.path("roomId").stringValue());
            moved.put("periodId", "period-40");
            roomOnly.put("roomId", "room-50");
            result.withObject("score").put("periodMoves", 1).put("roomOnlyMoves", 1);
            ObjectNode report = result.putObject("changeReport");
            report.putArray("additions"); report.putArray("cancellations"); report.putArray("teacherChanges");
            report.putArray("forcedMoves");
            report.putArray("periodMoves").addObject().put("lessonId", "lesson-960")
                    .put("oldPeriodId", "period-0").put("newPeriodId", "period-40")
                    .put("oldRoomId", "room-16").put("newRoomId", "room-16");
            report.putArray("roomOnlyMoves").addObject().put("lessonId", "lesson-0")
                    .put("oldRoomId", "room-0").put("newRoomId", "room-50");
            result.put("timetableRevision", timetableRevision(result, revision));
            KernelVerifier.Verification verified = verifier.verify(new ImportDocuments(successor, result,
                    null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
            assertEquals(revision, verified.definitionRevision());
            assertEquals(result.path("timetableRevision").stringValue(), verified.timetableRevision());
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Normative candidate must pass the actual kernel verifier before it reaches the planner", exception);
        }
    }

    ObjectNode scaleProposalDocument() {
        ObjectNode document = scaleDocument();
        ObjectNode draft = document.putObject("repairDraft");
        ObjectNode intent = draft.putObject("intent");
        intent.putArray("changes"); intent.putArray("pins"); intent.putArray("bulkActions");
        var direct = draft.putArray("directEffectLessonIds");
        for (int i = 0; i < 50; i++) direct.add("lesson-" + i);
        draft.put("intentRevision", "sha256:scale-intent").put("readyToSolve", true).putArray("conflicts");
        JsonNode baseline = document.path("acceptedBaseline");
        ObjectNode definition = (ObjectNode) baseline.path("definition").deepCopy();
        definition.put("basedOnRevision", "sha256:scale-definition");
        ObjectNode result = (ObjectNode) baseline.path("result").deepCopy();
        var report = result.putObject("changeReport");
        report.putArray("additions"); report.putArray("cancellations"); report.putArray("teacherChanges");
        report.putArray("forcedMoves"); var periodMoves = report.putArray("periodMoves"); report.putArray("roomOnlyMoves");
        for (int i = 0; i < 100; i++) {
            ObjectNode assignment = (ObjectNode) result.path("timetable").path("assignments").get(i);
            String oldPeriod = assignment.path("periodId").stringValue();
            String newPeriod = "period-" + ((i + 1) % 60);
            String room = assignment.path("roomId").stringValue();
            assignment.put("periodId", newPeriod);
            periodMoves.addObject().put("lessonId", "lesson-" + i).put("oldPeriodId", oldPeriod)
                    .put("newPeriodId", newPeriod).put("oldRoomId", room).put("newRoomId", room);
        }
        ObjectNode proposal = document.putObject("proposal");
        proposal.put("kind", "REPAIR").put("sourceWorkspaceVersion", 8)
                .put("acceptedTimetableRevision", "sha256:scale-timetable")
                .put("successorDefinitionRevision", "sha256:scale-successor")
                .put("intentRevision", "sha256:scale-intent").put("proposedTimetableRevision", "sha256:scale-proposed")
                .put("runId", java.util.UUID.randomUUID().toString()).put("limit", "PT1M")
                .put("terminationReason", "TIME_LIMIT").put("elapsedTimeMs", 30_000);
        proposal.set("definition", definition); proposal.set("result", result);
        ObjectNode counts = proposal.putObject("changeCounts");
        ProposalReviewService.CATEGORIES.forEach(category -> counts.put(category, report.path(category).size()));
        proposal.set("review", reviews.create(baseline, draft, definition, result));
        return document;
    }

    /** A proposal mixing same-slot, cancelled, added, forced and pinned changes, one room without a display name. */
    ObjectNode comparisonShapeDocument() {
        ObjectNode document = scaleProposalDocument();
        JsonNode baseline = document.path("acceptedBaseline");
        ObjectNode draft = (ObjectNode) document.path("repairDraft");
        ObjectNode proposal = (ObjectNode) document.path("proposal");
        ObjectNode definition = (ObjectNode) proposal.path("definition");
        ObjectNode result = (ObjectNode) proposal.path("result");
        var report = result.path("changeReport");
        ((ArrayNode) report.path("forcedMoves")).addObject().put("lessonId", "lesson-0");
        ((ObjectNode) result.path("timetable").path("assignments").get(100)).put("roomId", "room-2");
        ((ArrayNode) draft.path("intent").path("pins")).addObject().put("lessonId", "lesson-100")
                .putArray("roomSources").add("INDIVIDUAL");
        ((ArrayNode) report.path("roomOnlyMoves")).addObject().put("lessonId", "lesson-100");
        ((ObjectNode) definition.path("rooms").get(2)).remove("displayName");
        ((ArrayNode) result.path("timetable").path("assignments")).remove(101);
        ((ArrayNode) definition.path("lessons")).remove(101);
        ((ArrayNode) report.path("cancellations")).addObject().put("lessonId", "lesson-101");
        ((ArrayNode) definition.path("lessons")).addObject().put("id", "lesson-added")
                .put("displayName", "Additional school lesson").put("subjectId", "subject-0")
                .put("cohortId", "cohort-0").put("teacherId", "teacher-0");
        ((ArrayNode) result.path("timetable").path("assignments")).add(assignment(
                "lesson-added", "subject-0", "cohort-0", "teacher-0", "period-59", "room-0"));
        ((ArrayNode) report.path("additions")).addObject().put("lessonId", "lesson-added");
        ObjectNode counts = (ObjectNode) proposal.path("changeCounts");
        ProposalReviewService.CATEGORIES.forEach(category -> counts.put(category, report.path(category).size()));
        proposal.set("review", reviews.create(baseline, draft, definition, result));
        return document;
    }

    private static String timetableRevision(JsonNode result, String inputRevision) throws Exception {
        ObjectNode scope = JSON.createObjectNode().put("schemaVersion", 1).put("schoolId", "opaque-scale-school")
                .put("inputRevision", inputRevision);
        var ordered = new ArrayList<JsonNode>();
        result.path("timetable").path("assignments").forEach(ordered::add);
        ordered.sort(Comparator.comparing(item -> item.path("lessonId").stringValue()));
        var assignments = scope.putArray("assignments");
        ordered.forEach(assignments::add);
        return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(CanonicalJson.bytes(scope)));
    }

    private static String normalizedDefinitionRevision(JsonNode definition) throws Exception {
        ObjectNode normalized = (ObjectNode) definition.deepCopy();
        for (String collection : new String[] { "subjects", "teachers", "cohorts", "rooms", "periods", "lessons", "softConstraintOverrides" }) {
            if (!(normalized.path(collection) instanceof ArrayNode array)) continue;
            var ordered = new ArrayList<JsonNode>();
            array.forEach(ordered::add);
            ordered.sort(Comparator.comparing(item -> item.path(collection.equals("softConstraintOverrides") ? "constraintId" : "id").stringValue()));
            array.removeAll();
            ordered.forEach(array::add);
            for (JsonNode value : array) {
                for (String field : new String[] { "qualifiedSubjectIds", "availablePeriodIds", "undesirablePeriodIds", "capabilityIds",
                        "requiredRoomCapabilityIds", "preferredRoomIds" }) {
                    if (!(value.path(field) instanceof ArrayNode set)) continue;
                    var entries = new ArrayList<String>();
                    set.forEach(item -> entries.add(item.stringValue()));
                    entries.sort(String::compareTo);
                    set.removeAll();
                    entries.forEach(set::add);
                }
            }
        }
        return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(CanonicalJson.bytes(normalized)));
    }
}
