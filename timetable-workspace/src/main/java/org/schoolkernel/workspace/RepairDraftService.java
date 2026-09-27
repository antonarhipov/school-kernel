package org.schoolkernel.workspace;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class RepairDraftService {
    private static final Set<String> RESOURCE_TYPES = Set.of("TEACHER", "ROOM");
    private static final Set<String> DIMENSIONS = Set.of("PERIOD", "ROOM");
    private static final Set<String> BULK_SCOPES = Set.of("DAY", "CLASS", "UNAFFECTED");

    private final WorkspaceRepository repository;
    private final WorkspaceMutation mutation;
    private final ObjectMapper json;
    private volatile long failedAutosaveVersion = -1;

    public RepairDraftService(WorkspaceRepository repository, WorkspaceMutation mutation, ObjectMapper json) {
        this.repository = repository;
        this.mutation = mutation;
        this.json = json;
    }

    public WorkspaceAggregate start(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireState(current, WorkspaceState.ACCEPTED_BASELINE,
                "A repair can start only from an accepted baseline.");
        ObjectNode document = copy(current.document());
        ObjectNode draft = document.putObject("repairDraft");
        ObjectNode intent = draft.putObject("intent");
        intent.putArray("changes");
        intent.putArray("pins");
        intent.putArray("bulkActions");
        stageAvailability(intent, request, acceptedDefinition(current));
        refresh(draft, acceptedDefinition(current), acceptedAssignments(current), current.document().path("acceptedBaseline").path("manifest"));
        WorkspaceAggregate started = mutation.replaceRepair(expectedVersion, WorkspaceState.ACCEPTED_BASELINE,
                WorkspaceState.REPAIR_DRAFT, document);
        failedAutosaveVersion = -1;
        return started;
    }

    public WorkspaceAggregate update(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireState(current, WorkspaceState.REPAIR_DRAFT,
                "A repair draft can be edited only while no solve is running.");
        ObjectNode document = copy(current.document());
        ObjectNode draft = (ObjectNode) document.path("repairDraft");
        ObjectNode intent = (ObjectNode) draft.path("intent");
        String action = requiredText(request, "action");
        switch (action) {
            case "STAGE_UNAVAILABILITY" -> stageAvailability(intent, request, acceptedDefinition(current));
            case "PIN" -> applyIndividualPin(intent, request, acceptedAssignments(current));
            case "UNPIN" -> removeIndividualPin(intent, request);
            case "CONFIRM_BULK_PIN" -> confirmBulk(intent, request.path("preview"), current);
            case "UNDO_BULK_PIN" -> undoBulk(intent, requiredText(request, "bulkActionId"));
            default -> throw invalid("This repair action is not supported in the current increment.");
        }
        refresh(draft, acceptedDefinition(current), acceptedAssignments(current), current.document().path("acceptedBaseline").path("manifest"));
        try {
            WorkspaceAggregate saved = mutation.replaceRepair(expectedVersion, WorkspaceState.REPAIR_DRAFT,
                    WorkspaceState.REPAIR_DRAFT, document);
            failedAutosaveVersion = -1;
            return saved;
        } catch (RuntimeException failure) {
            failedAutosaveVersion = expectedVersion;
            throw failure;
        }
    }

    public MinimalUpdate updateMinimalPin(String ifMatch, JsonNode request) {
        String requestedLessonId = request.path("lessonId").isTextual()
                ? request.path("lessonId").stringValue() : "";
        WorkspaceRepository.RepairPinContext current = repository.loadRepairPinContext(requestedLessonId);
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current.version());
        if (current.state() != WorkspaceState.REPAIR_DRAFT) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION",
                    "A repair draft can be edited only while no solve is running.");
        }
        String lessonId = requiredText(request, "lessonId");
        ObjectNode draft = current.repairDraft();
        ObjectNode intent = (ObjectNode) draft.path("intent");
        switch (requiredText(request, "action")) {
            case "PIN" -> {
                if (current.assignment() == null) throw invalid("Only an accepted lesson can be pinned.");
                applyIndividualPin(intent, request);
            }
            case "UNPIN" -> removeIndividualPin(intent, request);
            default -> throw invalid("This repair action is not supported in the current increment.");
        }
        refreshPin(draft, lessonId, current.assignment(), current.lesson(), current.manifestLock());
        try {
            long version = mutation.replaceRepairDraft(expectedVersion, draft);
            failedAutosaveVersion = -1;
            return new MinimalUpdate(draft, version);
        } catch (RuntimeException failure) {
            failedAutosaveVersion = expectedVersion;
            throw failure;
        }
    }

    public BulkPinPreview preview(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = repository.load();
        ImportService.requireMatchingVersion(ifMatch, current);
        requireState(current, WorkspaceState.REPAIR_DRAFT,
                "Bulk pins can be previewed only for a repair draft.");
        ObjectNode preview = buildPreview(current, request, UUID.randomUUID().toString());
        return new BulkPinPreview(preview, current.etag());
    }

    private ObjectNode buildPreview(WorkspaceAggregate current, JsonNode request, String previewId) {
        String scope = requiredEnum(request, "scope", BULK_SCOPES);
        List<String> dimensions = requiredDimensions(request.path("dimensions"));
        String scopeId = request.path("scopeId").isTextual() ? request.path("scopeId").stringValue() : null;
        JsonNode draft = current.document().path("repairDraft");
        Set<String> directlyAffected = textSet(draft.path("directEffectLessonIds"));
        List<JsonNode> selected = new ArrayList<>();
        Map<String, JsonNode> lessons = byId(acceptedDefinition(current).path("lessons"));
        Map<String, JsonNode> periods = byId(acceptedDefinition(current).path("periods"));
        for (JsonNode assignment : acceptedAssignments(current)) {
            JsonNode lesson = lessons.get(assignment.path("lessonId").stringValue());
            JsonNode period = periods.get(assignment.path("periodId").stringValue());
            boolean matches = switch (scope) {
                case "DAY" -> scopeId != null && scopeId.equals(period == null ? null : period.path("weekday").stringValue());
                case "CLASS" -> scopeId != null && scopeId.equals(assignment.path("cohortId").stringValue());
                case "UNAFFECTED" -> !directlyAffected.contains(assignment.path("lessonId").stringValue());
                default -> false;
            };
            if (matches && lesson != null) selected.add(assignment);
        }
        selected.sort(Comparator.comparing(node -> node.path("lessonId").stringValue()));
        ObjectNode preview = json.createObjectNode();
        preview.put("previewId", previewId);
        preview.put("sourceWorkspaceVersion", current.version());
        preview.put("directEffectRevision", draft.path("directEffectRevision").stringValue());
        preview.put("scope", scope);
        if (scopeId != null) preview.put("scopeId", scopeId);
        ArrayNode dimensionArray = preview.putArray("dimensions");
        dimensions.forEach(dimensionArray::add);
        ArrayNode lessonIds = preview.putArray("lessonIds");
        selected.forEach(node -> lessonIds.add(node.path("lessonId").stringValue()));
        preview.put("count", selected.size());
        ArrayNode conflicts = preview.putArray("conflicts");
        Set<String> selectedIds = textSet(lessonIds);
        conflictsFor(acceptedDefinition(current), acceptedAssignments(current), draft.path("intent"),
                current.document().path("acceptedBaseline").path("manifest"), selectedIds, Set.copyOf(dimensions))
                .forEach(conflicts::add);
        return preview;
    }

    public WorkspaceAggregate discard(String ifMatch, JsonNode request) {
        if (!request.path("confirmed").isBoolean() || !request.path("confirmed").booleanValue()) {
            throw invalid("Discarding a repair draft requires explicit confirmation.");
        }
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireState(current, WorkspaceState.REPAIR_DRAFT,
                "Only a repair draft can be discarded.");
        ObjectNode document = copy(current.document());
        document.remove("repairDraft");
        return mutation.replaceRepair(expectedVersion, WorkspaceState.REPAIR_DRAFT,
                WorkspaceState.ACCEPTED_BASELINE, document);
    }

    ObjectNode compiledDefinition(JsonNode workspaceDocument) {
        ObjectNode definition = copy(workspaceDocument.path("acceptedBaseline").path("definition"));
        JsonNode result = workspaceDocument.path("acceptedBaseline").path("result");
        JsonNode intent = workspaceDocument.path("repairDraft").path("intent");
        clearPriorAttemptScopedLocks(
                definition, workspaceDocument.path("acceptedBaseline").path("manifest"));
        definition.put("catalogVersion", Math.max(8, definition.path("catalogVersion").intValue()));
        definition.put("basedOnRevision", result.path("inputRevision").stringValue());
        Map<String, ObjectNode> teachers = mutableById(definition.path("teachers"));
        Map<String, ObjectNode> rooms = mutableById(definition.path("rooms"));
        List<String> allPeriods = textValues(definition.path("periods"), "id");
        for (JsonNode change : intent.path("changes")) {
            Map<String, ObjectNode> resources = "TEACHER".equals(change.path("resourceType").stringValue()) ? teachers : rooms;
            ObjectNode resource = resources.get(change.path("resourceId").stringValue());
            List<String> available = resource.has("availablePeriodIds")
                    ? textValues(resource.path("availablePeriodIds"), null) : new ArrayList<>(allPeriods);
            available.removeAll(textSet(change.path("unavailablePeriodIds")));
            available.sort(String::compareTo);
            ArrayNode materialized = resource.putArray("availablePeriodIds");
            available.forEach(materialized::add);
        }
        Map<String, ObjectNode> lessons = mutableById(definition.path("lessons"));
        Map<String, JsonNode> assignments = byId(workspaceDocument.path("acceptedBaseline").path("result")
                .path("timetable").path("assignments"), "lessonId");
        for (JsonNode pin : intent.path("pins")) {
            ObjectNode lesson = lessons.get(pin.path("lessonId").stringValue());
            JsonNode assignment = assignments.get(pin.path("lessonId").stringValue());
            if (pin.path("periodSources").size() > 0) lesson.put("periodLock", assignment.path("periodId").stringValue());
            if (pin.path("roomSources").size() > 0) lesson.put("roomLock", assignment.path("roomId").stringValue());
        }
        return definition;
    }

    private static void clearPriorAttemptScopedLocks(ObjectNode definition, JsonNode manifest) {
        Map<String, ObjectNode> lessons = mutableById(definition.path("lessons"));
        for (JsonNode lock : manifest.path("locks")) {
            ObjectNode lesson = lessons.get(lock.path("lessonId").stringValue());
            if (lesson == null) continue;
            if ("ATTEMPT_SCOPED".equals(text(lock.path("periodLockOrigin")))) lesson.remove("periodLock");
            if ("ATTEMPT_SCOPED".equals(text(lock.path("roomLockOrigin")))) lesson.remove("roomLock");
        }
    }

    boolean hasUnsavedBrowserState(long durableVersion) {
        return failedAutosaveVersion == durableVersion;
    }

    private void stageAvailability(ObjectNode intent, JsonNode request, JsonNode definition) {
        if (request.path("action").isTextual()
                && !"STAGE_UNAVAILABILITY".equals(request.path("action").stringValue())) {
            throw invalid("This repair action is outside the current increment.");
        }
        String type = requiredEnum(request, "resourceType", RESOURCE_TYPES);
        String resourceId = requiredText(request, "resourceId");
        List<String> periodIds = requiredTextArray(request.path("periodIds"), "Select one or more weekly periods.");
        String collection = "TEACHER".equals(type) ? "teachers" : "rooms";
        JsonNode resource = byId(definition.path(collection)).get(resourceId);
        if (resource == null) throw invalid("The selected resource does not exist in the accepted definition.");
        Set<String> declaredPeriods = byId(definition.path("periods")).keySet();
        if (!declaredPeriods.containsAll(periodIds)) throw invalid("Every unavailable period must be declared by the school.");
        ArrayNode changes = (ArrayNode) intent.path("changes");
        for (int i = changes.size() - 1; i >= 0; i--) {
            JsonNode change = changes.get(i);
            if (type.equals(change.path("resourceType").stringValue())
                    && resourceId.equals(change.path("resourceId").stringValue())) changes.remove(i);
        }
        ObjectNode change = changes.addObject();
        change.put("resourceType", type);
        change.put("resourceId", resourceId);
        ArrayNode unavailable = change.putArray("unavailablePeriodIds");
        periodIds.stream().sorted().forEach(unavailable::add);
        sortObjects(changes, node -> node.path("resourceType").stringValue() + "\u0000" + node.path("resourceId").stringValue());
    }

    private void applyIndividualPin(ObjectNode intent, JsonNode request, JsonNode assignments) {
        String lessonId = requiredText(request, "lessonId");
        boolean accepted = false;
        for (JsonNode assignment : assignments) {
            if (lessonId.equals(assignment.path("lessonId").stringValue())) {
                accepted = true;
                break;
            }
        }
        if (!accepted) throw invalid("Only an accepted lesson can be pinned.");
        applyIndividualPin(intent, request);
    }

    private void applyIndividualPin(ObjectNode intent, JsonNode request) {
        String lessonId = requiredText(request, "lessonId");
        List<String> dimensions = requiredDimensions(request.path("dimensions"));
        ObjectNode pin = pin(intent, lessonId, true);
        dimensions.forEach(dimension -> addSource(pin, dimension, "INDIVIDUAL"));
    }

    private void removeIndividualPin(ObjectNode intent, JsonNode request) {
        ObjectNode pin = pin(intent, requiredText(request, "lessonId"), false);
        if (pin == null) return;
        for (String dimension : requiredDimensions(request.path("dimensions"))) removeSource(pin, dimension, "INDIVIDUAL");
        removeEmptyPins(intent);
    }

    private void confirmBulk(ObjectNode intent, JsonNode preview, WorkspaceAggregate current) {
        if (!preview.isObject()
                || preview.path("sourceWorkspaceVersion").longValue() != current.version()
                || !current.document().path("repairDraft").path("directEffectRevision").stringValue()
                        .equals(preview.path("directEffectRevision").stringValue())) {
            throw stalePreview();
        }
        String source = requiredText(preview, "previewId");
        try {
            UUID.fromString(source);
        } catch (IllegalArgumentException exception) {
            throw stalePreview();
        }
        ObjectNode expected = buildPreview(current, preview, source);
        if (!revision(expected).equals(revision(preview))) throw stalePreview();
        for (JsonNode action : intent.path("bulkActions")) {
            if (source.equals(action.path("id").stringValue())) throw stalePreview();
        }
        List<String> dimensions = textValues(preview.path("dimensions"), null);
        List<String> lessonIds = textValues(preview.path("lessonIds"), null);
        ObjectNode bulk = ((ArrayNode) intent.path("bulkActions")).addObject();
        bulk.put("id", source);
        bulk.put("scope", requiredEnum(preview, "scope", BULK_SCOPES));
        if (preview.path("scopeId").isTextual()) bulk.put("scopeId", preview.path("scopeId").stringValue());
        bulk.set("dimensions", preview.path("dimensions").deepCopy());
        bulk.set("lessonIds", preview.path("lessonIds").deepCopy());
        for (String lessonId : lessonIds) {
            ObjectNode pin = pin(intent, lessonId, true);
            dimensions.forEach(dimension -> addSource(pin, dimension, source));
        }
    }

    private static WorkspaceProblem stalePreview() {
        return new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED, "STALE_WORKSPACE_VERSION",
                "The bulk pin preview is stale. Preview the selection again.");
    }

    private void undoBulk(ObjectNode intent, String id) {
        ArrayNode actions = (ArrayNode) intent.path("bulkActions");
        boolean found = false;
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (id.equals(actions.get(i).path("id").stringValue())) {
                actions.remove(i);
                found = true;
            }
        }
        if (!found) throw invalid("The bulk pin action no longer exists.");
        for (JsonNode pinNode : intent.path("pins")) {
            ObjectNode pin = (ObjectNode) pinNode;
            removeSource(pin, "PERIOD", id);
            removeSource(pin, "ROOM", id);
        }
        removeEmptyPins(intent);
    }

    private void refresh(ObjectNode draft, JsonNode definition, JsonNode assignments, JsonNode manifest) {
        ObjectNode intent = (ObjectNode) draft.path("intent");
        sortObjects((ArrayNode) intent.path("pins"), node -> node.path("lessonId").stringValue());
        draft.put("intentRevision", revision(intent));
        List<String> direct = directEffects(assignments, intent);
        ArrayNode directIds = draft.putArray("directEffectLessonIds");
        direct.forEach(directIds::add);
        draft.put("directEffectRevision", revision(directIds));
        ArrayNode conflicts = draft.putArray("conflicts");
        conflictsFor(definition, assignments, intent, manifest, null, null).forEach(conflicts::add);
        draft.put("readyToSolve", conflicts.isEmpty());
        draft.put("persisted", true);
    }

    private void refreshPin(ObjectNode draft, String lessonId, JsonNode assignment, JsonNode lesson, JsonNode manifestLock) {
        ObjectNode intent = (ObjectNode) draft.path("intent");
        sortObjects((ArrayNode) intent.path("pins"), node -> node.path("lessonId").stringValue());
        draft.put("intentRevision", revision(intent));
        List<JsonNode> retained = new ArrayList<>();
        for (JsonNode conflict : draft.path("conflicts")) {
            if (!lessonId.equals(conflict.path("lessonId").stringValue())) retained.add(conflict);
        }
        ArrayNode conflicts = draft.putArray("conflicts");
        retained.forEach(conflicts::add);
        if (assignment != null) {
            ObjectNode selectedPin = pin(intent, lessonId, false);
            boolean periodPinned = manifestLock != null
                            && "PERSISTENT_POLICY".equals(text(manifestLock.path("periodLockOrigin")))
                    || sourceCount(selectedPin, "PERIOD") > 0;
            boolean roomPinned = manifestLock != null
                            && "PERSISTENT_POLICY".equals(text(manifestLock.path("roomLockOrigin")))
                    || sourceCount(selectedPin, "ROOM") > 0;
            for (JsonNode change : intent.path("changes")) {
                boolean atPeriod = textSet(change.path("unavailablePeriodIds"))
                        .contains(assignment.path("periodId").stringValue());
                boolean conflict = atPeriod && (("TEACHER".equals(change.path("resourceType").stringValue())
                        && change.path("resourceId").stringValue().equals(assignment.path("teacherId").stringValue())
                        && periodPinned) || ("ROOM".equals(change.path("resourceType").stringValue())
                        && change.path("resourceId").stringValue().equals(assignment.path("roomId").stringValue())
                        && roomPinned));
                if (conflict) {
                    ObjectNode item = conflicts.addObject();
                    item.put("lessonId", lessonId);
                    item.put("code", "PIN_CONTRADICTS_UNAVAILABILITY");
                    item.put("message", "The accepted "
                            + ("TEACHER".equals(change.path("resourceType").stringValue()) ? "period" : "room")
                            + " is pinned while that resource is unavailable.");
                    item.put("resourceType", change.path("resourceType").stringValue());
                    item.put("resourceId", change.path("resourceId").stringValue());
                    if (lesson != null) item.put("lessonDisplayName", lesson.path("displayName").stringValue());
                }
            }
        }
        sortObjects(conflicts, node -> node.path("lessonId").stringValue());
        draft.put("readyToSolve", conflicts.isEmpty());
        draft.put("persisted", true);
    }

    private List<String> directEffects(JsonNode assignments, JsonNode intent) {
        List<String> result = new ArrayList<>();
        for (JsonNode assignment : assignments) {
            for (JsonNode change : intent.path("changes")) {
                boolean resource = ("TEACHER".equals(change.path("resourceType").stringValue())
                                && change.path("resourceId").stringValue().equals(assignment.path("teacherId").stringValue()))
                        || ("ROOM".equals(change.path("resourceType").stringValue())
                                && change.path("resourceId").stringValue().equals(assignment.path("roomId").stringValue()));
                if (resource && textSet(change.path("unavailablePeriodIds")).contains(assignment.path("periodId").stringValue())) {
                    result.add(assignment.path("lessonId").stringValue());
                    break;
                }
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    private List<ObjectNode> conflictsFor(JsonNode definition, JsonNode assignments, JsonNode intent, JsonNode manifest,
            Set<String> selectedIds, Set<String> previewDimensions) {
        Map<String, JsonNode> lessons = byId(definition.path("lessons"));
        Map<String, ObjectNode> pins = new HashMap<>();
        for (JsonNode pin : intent.path("pins")) pins.put(pin.path("lessonId").stringValue(), (ObjectNode) pin);
        Set<String> persistentPeriod = new HashSet<>();
        Set<String> persistentRoom = new HashSet<>();
        for (JsonNode lock : manifest.path("locks")) {
            if ("PERSISTENT_POLICY".equals(text(lock.path("periodLockOrigin")))) persistentPeriod.add(lock.path("lessonId").stringValue());
            if ("PERSISTENT_POLICY".equals(text(lock.path("roomLockOrigin")))) persistentRoom.add(lock.path("lessonId").stringValue());
        }
        List<ObjectNode> conflicts = new ArrayList<>();
        for (JsonNode assignment : assignments) {
            String lessonId = assignment.path("lessonId").stringValue();
            JsonNode lesson = lessons.get(lessonId);
            boolean periodPinned = persistentPeriod.contains(lessonId) || sourceCount(pins.get(lessonId), "PERIOD") > 0
                    || (selectedIds != null && selectedIds.contains(lessonId) && previewDimensions.contains("PERIOD"));
            boolean roomPinned = persistentRoom.contains(lessonId) || sourceCount(pins.get(lessonId), "ROOM") > 0
                    || (selectedIds != null && selectedIds.contains(lessonId) && previewDimensions.contains("ROOM"));
            for (JsonNode change : intent.path("changes")) {
                boolean atPeriod = textSet(change.path("unavailablePeriodIds")).contains(assignment.path("periodId").stringValue());
                boolean conflict = atPeriod && (("TEACHER".equals(change.path("resourceType").stringValue())
                        && change.path("resourceId").stringValue().equals(assignment.path("teacherId").stringValue()) && periodPinned)
                        || ("ROOM".equals(change.path("resourceType").stringValue())
                        && change.path("resourceId").stringValue().equals(assignment.path("roomId").stringValue()) && roomPinned));
                if (conflict) {
                    ObjectNode item = json.createObjectNode();
                    item.put("lessonId", lessonId);
                    item.put("code", "PIN_CONTRADICTS_UNAVAILABILITY");
                    item.put("message", "The accepted " + ("TEACHER".equals(change.path("resourceType").stringValue()) ? "period" : "room")
                            + " is pinned while that resource is unavailable.");
                    item.put("resourceType", change.path("resourceType").stringValue());
                    item.put("resourceId", change.path("resourceId").stringValue());
                    if (lesson != null) item.put("lessonDisplayName", lesson.path("displayName").stringValue());
                    conflicts.add(item);
                }
            }
        }
        conflicts.sort(Comparator.comparing(node -> node.path("lessonId").stringValue()));
        return conflicts;
    }

    private ObjectNode pin(ObjectNode intent, String lessonId, boolean create) {
        for (JsonNode node : intent.path("pins")) if (lessonId.equals(node.path("lessonId").stringValue())) return (ObjectNode) node;
        if (!create) return null;
        ObjectNode pin = ((ArrayNode) intent.path("pins")).addObject();
        pin.put("lessonId", lessonId);
        pin.putArray("periodSources");
        pin.putArray("roomSources");
        return pin;
    }

    private static void addSource(ObjectNode pin, String dimension, String source) {
        ArrayNode sources = (ArrayNode) pin.path(sourceField(dimension));
        if (!textSet(sources).contains(source)) sources.add(source);
        sortText(sources);
    }

    private static void removeSource(ObjectNode pin, String dimension, String source) {
        ArrayNode sources = (ArrayNode) pin.path(sourceField(dimension));
        for (int i = sources.size() - 1; i >= 0; i--) if (source.equals(sources.get(i).stringValue())) sources.remove(i);
    }

    private static int sourceCount(ObjectNode pin, String dimension) {
        return pin == null ? 0 : pin.path(sourceField(dimension)).size();
    }

    private static String sourceField(String dimension) {
        return "PERIOD".equals(dimension) ? "periodSources" : "roomSources";
    }

    private static void removeEmptyPins(ObjectNode intent) {
        ArrayNode pins = (ArrayNode) intent.path("pins");
        for (int i = pins.size() - 1; i >= 0; i--) if (pins.get(i).path("periodSources").isEmpty() && pins.get(i).path("roomSources").isEmpty()) pins.remove(i);
    }

    private static List<String> requiredDimensions(JsonNode node) {
        List<String> dimensions = requiredTextArray(node, "Select period, room, or both pin dimensions.");
        if (!DIMENSIONS.containsAll(dimensions)) throw invalid("A pin dimension must be PERIOD or ROOM.");
        return dimensions.stream().distinct().sorted().toList();
    }

    private static List<String> requiredTextArray(JsonNode node, String message) {
        if (!node.isArray() || node.isEmpty()) throw invalid(message);
        List<String> result = new ArrayList<>();
        for (JsonNode value : node) if (!value.isTextual() || value.stringValue().isBlank()) throw invalid(message); else result.add(value.stringValue());
        return result.stream().distinct().toList();
    }

    private static String requiredEnum(JsonNode request, String field, Set<String> values) {
        String value = requiredText(request, field);
        if (!values.contains(value)) throw invalid("The selected " + field + " is not supported.");
        return value;
    }

    private static String requiredText(JsonNode request, String field) {
        JsonNode value = request.path(field);
        if (!value.isTextual() || value.stringValue().isBlank()) throw invalid("The " + field + " value is required.");
        return value.stringValue();
    }

    private static WorkspaceProblem invalid(String message) {
        return new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_REPAIR_DRAFT", message);
    }

    private static void requireState(WorkspaceAggregate current, WorkspaceState expected, String message) {
        if (current.state() != expected) throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", message);
    }

    private static JsonNode acceptedDefinition(WorkspaceAggregate current) { return current.document().path("acceptedBaseline").path("definition"); }
    private static JsonNode acceptedAssignments(WorkspaceAggregate current) { return current.document().path("acceptedBaseline").path("result").path("timetable").path("assignments"); }

    private ObjectNode copy(JsonNode node) { return (ObjectNode) node.deepCopy(); }

    private static Map<String, JsonNode> byId(JsonNode array) { return byId(array, "id"); }
    private static Map<String, JsonNode> byId(JsonNode array, String field) {
        Map<String, JsonNode> result = new HashMap<>();
        for (JsonNode node : array) result.put(node.path(field).stringValue(), node);
        return result;
    }

    private static Map<String, ObjectNode> mutableById(JsonNode array) {
        Map<String, ObjectNode> result = new HashMap<>();
        for (JsonNode node : array) result.put(node.path("id").stringValue(), (ObjectNode) node);
        return result;
    }

    private static Set<String> textSet(JsonNode array) {
        Set<String> result = new HashSet<>();
        for (JsonNode value : array) if (value.isTextual()) result.add(value.stringValue());
        return result;
    }

    private static List<String> textValues(JsonNode array, String field) {
        List<String> result = new ArrayList<>();
        for (JsonNode value : array) result.add(field == null ? value.stringValue() : value.path(field).stringValue());
        return result;
    }

    private static String revision(JsonNode value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(CanonicalJson.bytes(value));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String text(JsonNode value) {
        return value.isTextual() ? value.stringValue() : null;
    }

    private static void sortText(ArrayNode array) {
        List<String> values = textValues(array, null);
        values.sort(String::compareTo);
        array.removeAll();
        values.forEach(array::add);
    }

    private static void sortObjects(ArrayNode array, java.util.function.Function<JsonNode, String> key) {
        List<JsonNode> values = new ArrayList<>();
        array.forEach(values::add);
        values.sort(Comparator.comparing(key));
        array.removeAll();
        values.forEach(array::add);
    }

    public record BulkPinPreview(ObjectNode document, String etag) {}

    public record MinimalUpdate(ObjectNode repairDraft, long version) {
        public String etag() {
            return "\"ws-" + version + "\"";
        }
    }
}
