package org.schoolkernel.workspace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class ProposalReviewService {
    static final List<String> CATEGORIES = List.of(
            "additions", "cancellations", "teacherChanges", "forcedMoves", "periodMoves", "roomOnlyMoves");

    private final ObjectMapper json;

    public ProposalReviewService(ObjectMapper json) {
        this.json = json;
    }

    public ObjectNode create(JsonNode acceptedBaseline, JsonNode repairDraft, JsonNode successorDefinition, JsonNode result) {
        JsonNode report = result.path("changeReport");
        ObjectNode review = json.createObjectNode();
        ArrayNode categories = review.putArray("categories");
        Set<String> changedIds = new LinkedHashSet<>();
        Map<String, List<String>> lessonCategories = new LinkedHashMap<>();
        for (String category : CATEGORIES) {
            JsonNode items = report.path(category);
            ObjectNode summary = categories.addObject();
            summary.put("id", category);
            summary.put("count", items.size());
            ArrayNode lessonIds = summary.putArray("lessonIds");
            for (JsonNode item : items) {
                String lessonId = text(item.path("lessonId"));
                if (lessonId == null) continue;
                lessonIds.add(lessonId);
                changedIds.add(lessonId);
                lessonCategories.computeIfAbsent(lessonId, ignored -> new ArrayList<>()).add(category);
            }
        }
        review.put("uniqueChangedLessonCount", changedIds.size());
        Set<String> directIds = textSet(repairDraft.path("directEffectLessonIds"));
        ArrayNode directEffects = review.putArray("directEffectLessonIds");
        directIds.stream().sorted().forEach(directEffects::add);

        Map<String, JsonNode> oldAssignments = byId(
                acceptedBaseline.path("result").path("timetable").path("assignments"), "lessonId");
        Map<String, JsonNode> proposedAssignments = byId(result.path("timetable").path("assignments"), "lessonId");
        Map<String, JsonNode> oldLessons = byId(acceptedBaseline.path("definition").path("lessons"), "id");
        Map<String, JsonNode> proposedLessons = byId(successorDefinition.path("lessons"), "id");
        Map<String, JsonNode> oldPeriods = byId(acceptedBaseline.path("definition").path("periods"), "id");
        Map<String, JsonNode> proposedPeriods = byId(successorDefinition.path("periods"), "id");

        ArrayNode changedLessons = review.putArray("changedLessons");
        Map<String, Group> classGroups = new LinkedHashMap<>();
        Map<String, Group> teacherGroups = new LinkedHashMap<>();
        Map<String, Group> roomGroups = new LinkedHashMap<>();
        Map<String, Group> dayGroups = new LinkedHashMap<>();
        changedIds.stream().sorted().forEach(lessonId -> {
            JsonNode oldAssignment = oldAssignments.get(lessonId);
            JsonNode proposedAssignment = proposedAssignments.get(lessonId);
            JsonNode oldLesson = oldLessons.get(lessonId);
            JsonNode proposedLesson = proposedLessons.get(lessonId);
            ObjectNode item = changedLessons.addObject();
            item.put("lessonId", lessonId);
            item.put("directEffect", directIds.contains(lessonId));
            item.put("rippleEffect", !directIds.contains(lessonId));
            ArrayNode itemCategories = item.putArray("categories");
            lessonCategories.getOrDefault(lessonId, List.of()).forEach(itemCategories::add);
            ObjectNode oldValue = side(oldAssignment, oldLesson);
            ObjectNode proposedValue = side(proposedAssignment, proposedLesson);
            if (oldValue != null) item.set("old", oldValue);
            if (proposedValue != null) item.set("proposed", proposedValue);
            ArrayNode dimensions = item.putArray("changedDimensions");
            for (String dimension : List.of("subjectId", "cohortId", "teacherId", "periodId", "roomId")) {
                if (!same(oldValue, proposedValue, dimension)) dimensions.add(dimension);
            }
            addGroup(classGroups, oldValue, proposedValue, "cohortId", lessonId);
            addGroup(teacherGroups, oldValue, proposedValue, "teacherId", lessonId);
            addGroup(roomGroups, oldValue, proposedValue, "roomId", lessonId);
            addDayGroup(dayGroups, oldValue, proposedValue, oldPeriods, proposedPeriods, lessonId);
        });
        review.put("directEffectChangedCount", changedIds.stream().filter(directIds::contains).count());
        review.put("rippleEffectCount", changedIds.stream().filter(id -> !directIds.contains(id)).count());
        ObjectNode groupings = review.putObject("groupings");
        writeGroups(groupings.putArray("classes"), classGroups);
        writeGroups(groupings.putArray("teachers"), teacherGroups);
        writeGroups(groupings.putArray("rooms"), roomGroups);
        writeGroups(groupings.putArray("days"), dayGroups);
        return review;
    }

    private ObjectNode side(JsonNode assignment, JsonNode lesson) {
        if (assignment == null && lesson == null) return null;
        ObjectNode side = json.createObjectNode();
        if (assignment != null) {
            for (String field : List.of("subjectId", "cohortId", "teacherId", "periodId", "roomId")) {
                if (assignment.path(field).isTextual()) side.put(field, assignment.path(field).stringValue());
            }
        }
        if (lesson != null) {
            for (String field : List.of("subjectId", "cohortId", "teacherId")) {
                if (!side.has(field) && lesson.path(field).isTextual()) side.put(field, lesson.path(field).stringValue());
            }
        }
        return side;
    }

    private static boolean same(JsonNode oldValue, JsonNode proposedValue, String field) {
        return oldValue != null && proposedValue != null
                && text(oldValue.path(field)) != null
                && text(oldValue.path(field)).equals(text(proposedValue.path(field)));
    }

    private static void addGroup(
            Map<String, Group> groups, JsonNode oldValue, JsonNode proposedValue, String field, String lessonId) {
        addGroupSide(groups, oldValue, field, lessonId, true);
        addGroupSide(groups, proposedValue, field, lessonId, false);
    }

    private static void addGroupSide(
            Map<String, Group> groups, JsonNode value, String field, String lessonId, boolean old) {
        if (value == null || !value.path(field).isTextual()) return;
        String id = value.path(field).stringValue();
        Group group = groups.computeIfAbsent(id, Group::new);
        group.lessonIds.add(lessonId);
        if (old) group.old = true; else group.proposed = true;
    }

    private static void addDayGroup(
            Map<String, Group> groups,
            JsonNode oldValue,
            JsonNode proposedValue,
            Map<String, JsonNode> oldPeriods,
            Map<String, JsonNode> proposedPeriods,
            String lessonId) {
        addDayGroupSide(groups, oldValue, oldPeriods, lessonId, true);
        addDayGroupSide(groups, proposedValue, proposedPeriods, lessonId, false);
    }

    private static void addDayGroupSide(
            Map<String, Group> groups, JsonNode value, Map<String, JsonNode> periods, String lessonId, boolean old) {
        if (value == null) return;
        JsonNode period = periods.get(text(value.path("periodId")));
        if (period == null || !period.path("weekday").isTextual()) return;
        String day = period.path("weekday").stringValue();
        Group group = groups.computeIfAbsent(day, Group::new);
        group.lessonIds.add(lessonId);
        if (old) group.old = true; else group.proposed = true;
    }

    private static void writeGroups(ArrayNode target, Map<String, Group> groups) {
        groups.values().stream().sorted((left, right) -> left.id.compareTo(right.id)).forEach(group -> {
            ObjectNode item = target.addObject();
            item.put("id", group.id);
            item.put("context", group.old && group.proposed ? "BOTH" : group.old ? "OLD" : "PROPOSED");
            ArrayNode lessonIds = item.putArray("lessonIds");
            group.lessonIds.stream().sorted().forEach(lessonIds::add);
        });
    }

    private static Map<String, JsonNode> byId(JsonNode array, String field) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode item : array) result.put(text(item.path(field)), item);
        return result;
    }

    private static Set<String> textSet(JsonNode array) {
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode item : array) if (item.isTextual()) values.add(item.stringValue());
        return values;
    }

    private static String text(JsonNode node) {
        return node != null && node.isTextual() ? node.stringValue() : null;
    }

    private static final class Group {
        private final String id;
        private final Set<String> lessonIds = new LinkedHashSet<>();
        private boolean old;
        private boolean proposed;

        private Group(String id) {
            this.id = id;
        }
    }
}
