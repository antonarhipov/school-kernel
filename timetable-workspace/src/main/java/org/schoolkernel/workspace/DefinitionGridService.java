package org.schoolkernel.workspace;

import java.time.LocalTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-3: one version-bound grid operation; never rewrite availability or source data. */
@Service
public class DefinitionGridService {
    private static final Set<String> FIELDS = Set.of("id", "displayName", "weekday", "order", "startTime", "endTime");
    private static final Set<String> DAYS = Set.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
    private final WorkspaceStore repository;
    private final ObjectMapper json;

    public DefinitionGridService(WorkspaceStore repository, ObjectMapper json) {
        this.repository = repository; this.json = json;
    }

    public ObjectNode preview(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = checked(ifMatch);
        ObjectNode document = candidate(current, request);
        ObjectNode draft = (ObjectNode) document.path("definitionDraft");
        ObjectNode preview = json.createObjectNode().put("workspaceVersion", current.version());
        preview.set("periods", draft.path("definition").path("periods"));
        if (draft.path("definition").has("reservedPeriodIds")) preview.set("reservedPeriodIds", draft.path("definition").path("reservedPeriodIds"));
        preview.set("issues", draft.path("issues"));
        preview.set("timingImpacts", draft.path("gridTimingImpacts"));
        ArrayNode availability = preview.putArray("availabilityEffects");
        Set<String> previous = ids(current.document().path("definitionDraft").path("definition").path("periods"));
        for (JsonNode period : draft.path("definition").path("periods")) {
            String id = period.path("id").stringValue();
            if (previous.contains(id)) continue;
            for (String collection : List.of("teachers", "cohorts", "rooms")) for (JsonNode resource : draft.path("definition").path(collection)) {
                availability.addObject().put("periodId", id).put("ownerType", collection).put("ownerId", resource.path("id").stringValue())
                        .put("displayName", resource.path("displayName").asString())
                        .put("effect", resource.has("availablePeriodIds") ? "Excluded from explicit selected availability" : "Included in all-period availability");
            }
        }
        return preview;
    }

    @Transactional
    public WorkspaceAggregate save(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = checked(ifMatch);
        ObjectNode document = candidate(current, request);
        if (repository.replace(current.version(), WorkspaceState.DEFINITION_DRAFT, WorkspaceState.DEFINITION_DRAFT, document).isEmpty()) {
            throw problem(HttpStatus.PRECONDITION_FAILED, "STALE_WORKSPACE_VERSION", "The workspace changed. Review the newer saved draft before retrying. No change was committed.");
        }
        return repository.load();
    }

    private WorkspaceAggregate checked(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.DEFINITION_DRAFT) throw problem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION",
                "Grid editing requires DEFINITION_DRAFT; " + current.state() + " is active. No change was committed.");
        return current;
    }

    private ObjectNode candidate(WorkspaceAggregate current, JsonNode request) {
        if (!(request instanceof ObjectNode) || !request.path("periods").isArray()
                || request.properties().stream().anyMatch(e -> !Set.of("periods", "reservedPeriodIds").contains(e.getKey()))
                || (request.has("reservedPeriodIds") && !request.path("reservedPeriodIds").isArray())) {
            throw problem(HttpStatus.BAD_REQUEST, "INVALID_GRID_FIELDS", "Supply period rows and optional reservations only. No change was committed.");
        }
        ObjectNode document = ((ObjectNode) current.document()).deepCopy();
        ObjectNode draft = (ObjectNode) document.path("definitionDraft");
        ObjectNode definition = (ObjectNode) draft.path("definition");
        Set<String> oldIds = ids(definition.path("periods"));
        Set<String> newIds = new HashSet<>();
        for (JsonNode row : request.path("periods")) {
            if (!(row instanceof ObjectNode) || row.properties().stream().anyMatch(e -> !FIELDS.contains(e.getKey()))
                    || !row.path("id").isTextual() || !row.path("id").stringValue().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
                    || !newIds.add(row.path("id").stringValue())) {
                throw problem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PERIOD_IDENTITY", "Period identifiers must be valid and unique; unsupported fields are refused. No change was committed.");
            }
            if (!oldIds.contains(row.path("id").stringValue())) {
                for (JsonNode reserved : draft.path("removedIdentities")) if ("Period".equals(reserved.path("ownerType").asString())
                        && row.path("id").equals(reserved.path("id"))) throw problem(HttpStatus.CONFLICT, "PERIOD_ID_RESERVED", "That period identity is reserved. Restore source items through source restoration. No change was committed.");
            }
        }
        if (!newIds.containsAll(oldIds)) throw problem(HttpStatus.CONFLICT, "GUIDED_REMOVAL_REQUIRED", "Existing period identifiers cannot be replaced or removed here. Use guided removal to resolve dependencies. No change was committed.");
        definition.set("periods", request.path("periods").deepCopy());
        if (request.has("reservedPeriodIds")) definition.set("reservedPeriodIds", request.path("reservedPeriodIds").deepCopy());
        else definition.remove("reservedPeriodIds");
        refreshIssues(draft);
        draft.set("gridTimingImpacts", timingImpacts(draft));
        draft.put("draftRevision", DefinitionDraftService.revision(definition));
        return document;
    }

    void refreshIssues(ObjectNode draft) {
        JsonNode definition = draft.path("definition");
        ArrayNode issues = json.createArrayNode();
        for (JsonNode issue : draft.path("issues")) if (!"Period".equals(issue.path("ownerType").asString())
                && !("School".equals(issue.path("ownerType").asString()) && "reservedPeriodIds".equals(issue.path("field").asString()))) issues.add(issue);
        validate(definition, issues);
        List<JsonNode> sorted = new ArrayList<>(); issues.forEach(sorted::add);
        sorted.sort(Comparator.comparing((JsonNode n) -> "BLOCKING".equals(n.path("severity").asString()) ? 0 : 1)
                .thenComparing(n -> n.path("ownerType").asString()).thenComparing(n -> n.path("ownerId").asString())
                .thenComparing(n -> n.path("field").asString()).thenComparing(n -> n.path("message").asString()));
        ArrayNode ordered = json.createArrayNode(); sorted.forEach(ordered::add); draft.set("issues", ordered);
        DefinitionPreferenceService.refresh(draft, json);
    }

    private void validate(JsonNode definition, ArrayNode issues) {
        List<JsonNode> rows = new ArrayList<>(); definition.path("periods").forEach(rows::add);
        for (JsonNode row : rows) {
            String id = row.path("id").asString();
            if (!row.path("displayName").isTextual() || row.path("displayName").stringValue().isBlank()) issue(issues, "Period", id, "displayName", "Enter a nonblank period name.");
            if (!DAYS.contains(row.path("weekday").asString())) issue(issues, "Period", id, "weekday", "Choose a declared weekday.");
            if (!row.path("order").isIntegralNumber() || !row.path("order").canConvertToInt() || row.path("order").intValue() < 1) issue(issues, "Period", id, "order", "Order must be an integer from 1 to 2147483647.");
            if (row.has("startTime") != row.has("endTime")) issue(issues, "Period", id, "startTime", "Supply both start and end times, or omit both.");
            if (row.has("startTime") && row.has("endTime")) {
                LocalTime start = time(row.path("startTime")), end = time(row.path("endTime"));
                if (start == null || end == null || !end.isAfter(start)) issue(issues, "Period", id, "startTime", "Use paired HH:mm or HH:mm:ss times with positive duration.");
            }
            for (JsonNode other : rows) {
                if (row == other || !row.path("weekday").equals(other.path("weekday"))) continue;
                if (row.path("order").equals(other.path("order"))) issue(issues, "Period", id, "order", "Duplicate weekday/order with " + other.path("id").asString() + ".", other.path("id").asString());
                LocalTime start = time(row.path("startTime")), end = time(row.path("endTime"));
                LocalTime a = time(other.path("startTime")), b = time(other.path("endTime"));
                if (start != null && end != null && a != null && b != null && start.isBefore(b) && a.isBefore(end)) issue(issues, "Period", id, "startTime", "Clock interval overlaps " + other.path("id").asString() + ".", other.path("id").asString());
            }
        }
        Set<String> seen = new HashSet<>(), periodIds = ids(definition.path("periods"));
        for (JsonNode reserved : definition.path("reservedPeriodIds")) if (!reserved.isTextual() || !periodIds.contains(reserved.asString()) || !seen.add(reserved.asString())) {
            issue(issues, "School", definition.path("schoolId").asString(), "reservedPeriodIds", "Reservations require unique declared period IDs: " + reserved.asString() + ".");
        }
    }

    private ArrayNode timingImpacts(JsonNode draft) {
        ArrayNode impacts = json.createArrayNode();
        JsonNode source = draft.path("sourceDocument");
        JsonNode original = source.has("initialDefinition") ? source.path("initialDefinition") : source.path("acceptedBaseline").path("definition");
        JsonNode definition = draft.path("definition");
        for (JsonNode before : original.path("periods")) {
            JsonNode after = find(definition.path("periods"), before.path("id").asString());
            if (after == null || timing(before).equals(timing(after))) continue;
            Set<String> related = new TreeSet<>();
            for (JsonNode placement : source.path("acceptedBaseline").path("result").path("timetable").path("assignments")) if (before.path("id").equals(placement.path("periodId"))) related.add(placement.path("lessonId").asString());
            for (JsonNode lesson : original.path("lessons")) if (before.path("id").equals(lesson.path("periodLock"))) related.add(lesson.path("id").asString());
            for (JsonNode lesson : definition.path("lessons")) if (before.path("id").equals(lesson.path("periodLock"))) related.add(lesson.path("id").asString());
            for (String id : related) {
                JsonNode lesson = find(definition.path("lessons"), id); if (lesson == null) lesson = find(original.path("lessons"), id);
                ObjectNode impact = impacts.addObject().put("lessonId", id).put("lessonName", lesson == null ? id : lesson.path("displayName").asString()).put("periodId", before.path("id").asString());
                impact.set("before", timing(before)); impact.set("after", timing(after));
            }
        }
        return impacts;
    }

    private ObjectNode timing(JsonNode row) {
        ObjectNode result = json.createObjectNode();
        for (String field : List.of("weekday", "order", "startTime", "endTime")) if (row.has(field)) result.set(field, row.path(field));
        return result;
    }
    private static JsonNode find(JsonNode rows, String id) { for (JsonNode row : rows) if (id.equals(row.path("id").asString())) return row; return null; }
    private static Set<String> ids(JsonNode rows) { Set<String> result = new HashSet<>(); for (JsonNode row : rows) result.add(row.path("id").asString()); return result; }
    private static LocalTime time(JsonNode value) {
        if (!value.isTextual() || !value.stringValue().matches("[0-9]{2}:[0-9]{2}(:[0-9]{2})?")) return null;
        try { return LocalTime.parse(value.stringValue()); } catch (RuntimeException e) { return null; }
    }
    private static void issue(ArrayNode issues, String type, String id, String field, String message, String... related) {
        ArrayNode ids = issues.addObject().put("severity", "BLOCKING").put("ownerType", type).put("ownerId", id).put("field", field).put("message", message).putArray("relatedIds");
        for (String value : related) ids.add(value);
    }
    private static WorkspaceProblem problem(HttpStatus status, String code, String message) { return new WorkspaceProblem(status, code, message); }
}
