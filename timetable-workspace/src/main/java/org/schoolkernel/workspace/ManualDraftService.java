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
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class ManualDraftService {
    private final WorkspaceRepository repository;
    private final WorkspaceMutation mutation;
    private final KernelVerifier verifier;
    private final ManifestService manifests;
    private final ObjectMapper json;

    public ManualDraftService(
            WorkspaceRepository repository,
            WorkspaceMutation mutation,
            KernelVerifier verifier,
            ManifestService manifests,
            ObjectMapper json) {
        this.repository = repository;
        this.mutation = mutation;
        this.verifier = verifier;
        this.manifests = manifests;
        this.json = json;
    }

    public WorkspaceAggregate start(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireState(current, WorkspaceState.ACCEPTED_BASELINE,
                "Manual editing can start only from an accepted baseline.");

        ObjectNode document = (ObjectNode) current.document().deepCopy();
        ObjectNode acceptedBaseline = (ObjectNode) document.path("acceptedBaseline");
        JsonNode baselineAssignments = acceptedBaseline.path("result").path("timetable").path("assignments");

        ObjectNode draft = document.putObject("manualDraft");
        draft.set("assignments", baselineAssignments.deepCopy());
        draft.putObject("modifications");
        draft.putArray("conflicts");
        draft.put("draftRevision", "sha256:" + Long.toHexString(System.currentTimeMillis()));

        return mutation.replaceManualDraft(
                expectedVersion,
                WorkspaceState.ACCEPTED_BASELINE,
                WorkspaceState.MANUAL_DRAFT,
                document);
    }

    public WorkspaceAggregate discard(String ifMatch, JsonNode payload) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireState(current, WorkspaceState.MANUAL_DRAFT,
                "Discarding manual draft requires MANUAL_DRAFT state.");

        if (payload == null || !payload.path("confirmed").asBoolean(false)) {
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "CONFIRMATION_REQUIRED",
                    "Explicit confirmation is required to discard the manual draft.");
        }

        ObjectNode document = (ObjectNode) current.document().deepCopy();
        document.remove("manualDraft");

        return mutation.replaceManualDraft(
                expectedVersion,
                WorkspaceState.MANUAL_DRAFT,
                WorkspaceState.ACCEPTED_BASELINE,
                document);
    }

    public WorkspaceAggregate mutateDraft(String ifMatch, JsonNode payload) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        boolean isBaseline = current.state() == WorkspaceState.ACCEPTED_BASELINE;
        if (!isBaseline) {
            requireState(current, WorkspaceState.MANUAL_DRAFT,
                    "Manual editing mutations require MANUAL_DRAFT state.");
        }

        ObjectNode document = (ObjectNode) current.document().deepCopy();
        ObjectNode draft;
        if (isBaseline) {
            ObjectNode acceptedBaseline = (ObjectNode) document.path("acceptedBaseline");
            JsonNode baselineAssignments = acceptedBaseline.path("result").path("timetable").path("assignments");
            draft = document.putObject("manualDraft");
            draft.set("assignments", baselineAssignments.deepCopy());
            draft.putObject("modifications");
            draft.putArray("conflicts");
            draft.put("draftRevision", "sha256:" + Long.toHexString(System.currentTimeMillis()));
        } else {
            draft = (ObjectNode) document.path("manualDraft");
            if (draft.isMissingNode() || draft.isNull()) {
                throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_STATE", "Manual draft is missing.");
            }
        }

        String action = payload.path("action").stringValue();
        if (action == null || action.isBlank()) {
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_MANUAL_DRAFT", "Action is required.");
        }

        JsonNode definition = document.path("acceptedBaseline").path("definition");
        ArrayNode assignments = (ArrayNode) draft.path("assignments");
        ObjectNode modifications = (ObjectNode) draft.path("modifications");
        JsonNode baselineAssignments = document.path("acceptedBaseline").path("result").path("timetable").path("assignments");

        Map<String, JsonNode> baselineByLesson = byId(baselineAssignments, "lessonId");
        Map<String, JsonNode> definitionPeriods = byId(definition.path("periods"));
        Map<String, JsonNode> definitionRooms = byId(definition.path("rooms"));
        Map<String, JsonNode> definitionTeachers = byId(definition.path("teachers"));

        if ("REASSIGN_LESSON".equals(action)) {
            String lessonId = payload.path("lessonId").stringValue();
            if (lessonId == null || lessonId.isBlank()) {
                throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_MANUAL_DRAFT", "The lessonId value is required.");
            }
            ObjectNode targetAssignment = null;
            for (JsonNode node : assignments) {
                if (lessonId.equals(node.path("lessonId").stringValue())) {
                    targetAssignment = (ObjectNode) node;
                    break;
                }
            }
            if (targetAssignment == null) {
                throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_LESSON", "Unknown lesson id: " + lessonId);
            }

            if (payload.has("periodId") && !payload.path("periodId").isNull()) {
                String newPeriodId = payload.path("periodId").stringValue();
                if (!definitionPeriods.containsKey(newPeriodId)) {
                    throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PERIOD", "Unknown period id: " + newPeriodId);
                }
                targetAssignment.put("periodId", newPeriodId);
            }

            if (payload.has("roomId") && !payload.path("roomId").isNull()) {
                String newRoomId = payload.path("roomId").stringValue();
                if (!definitionRooms.containsKey(newRoomId)) {
                    throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ROOM", "Unknown room id: " + newRoomId);
                }
                targetAssignment.put("roomId", newRoomId);
            }

            if (payload.has("teacherId") && !payload.path("teacherId").isNull()) {
                String newTeacherId = payload.path("teacherId").stringValue();
                if (!definitionTeachers.containsKey(newTeacherId)) {
                    throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TEACHER", "Unknown teacher id: " + newTeacherId);
                }
                targetAssignment.put("teacherId", newTeacherId);
            }

            // Update modification record
            JsonNode orig = baselineByLesson.get(lessonId);
            if (orig != null) {
                String currPeriod = targetAssignment.path("periodId").stringValue();
                String currRoom = targetAssignment.path("roomId").stringValue();
                String currTeacher = targetAssignment.path("teacherId").stringValue();

                String origPeriod = orig.path("periodId").stringValue();
                String origRoom = orig.path("roomId").stringValue();
                String origTeacher = orig.path("teacherId").stringValue();

                boolean periodChanged = !currPeriod.equals(origPeriod);
                boolean roomChanged = !currRoom.equals(origRoom);
                boolean teacherChanged = !currTeacher.equals(origTeacher);

                if (periodChanged || roomChanged || teacherChanged) {
                    ObjectNode mod = modifications.putObject(lessonId);
                    mod.put("lessonId", lessonId);
                    mod.put("periodChanged", periodChanged);
                    mod.put("roomChanged", roomChanged);
                    mod.put("teacherChanged", teacherChanged);
                    mod.put("originalPeriodId", origPeriod);
                    mod.put("originalRoomId", origRoom);
                    mod.put("originalTeacherId", origTeacher);
                } else {
                    modifications.remove(lessonId);
                }
            }
        } else if ("REVERT_LESSON".equals(action)) {
            String lessonId = payload.path("lessonId").stringValue();
            if (lessonId == null || lessonId.isBlank()) {
                throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_MANUAL_DRAFT", "The lessonId value is required.");
            }
            ObjectNode targetAssignment = null;
            for (JsonNode node : assignments) {
                if (lessonId.equals(node.path("lessonId").stringValue())) {
                    targetAssignment = (ObjectNode) node;
                    break;
                }
            }
            if (targetAssignment == null) {
                throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_LESSON", "Unknown lesson id: " + lessonId);
            }
            JsonNode orig = baselineByLesson.get(lessonId);
            if (orig != null) {
                targetAssignment.put("periodId", orig.path("periodId").stringValue());
                targetAssignment.put("roomId", orig.path("roomId").stringValue());
                targetAssignment.put("teacherId", orig.path("teacherId").stringValue());
            }
            modifications.remove(lessonId);
        } else {
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ACTION", "Unsupported action: " + action);
        }

        // Authoritatively re-evaluate conflicts
        List<ObjectNode> conflicts = evaluateConflicts(definition, assignments);
        ArrayNode conflictsArray = draft.putArray("conflicts");
        conflicts.forEach(conflictsArray::add);

        draft.put("draftRevision", "sha256:" + Long.toHexString(System.nanoTime()));

        return mutation.replaceManualDraft(
                expectedVersion,
                isBaseline ? WorkspaceState.ACCEPTED_BASELINE : WorkspaceState.MANUAL_DRAFT,
                WorkspaceState.MANUAL_DRAFT,
                document);
    }

    public List<ObjectNode> evaluateConflicts(JsonNode definition, JsonNode assignments) {
        List<ObjectNode> conflicts = new ArrayList<>();

        Map<String, JsonNode> teachers = byId(definition.path("teachers"));
        Map<String, JsonNode> rooms = byId(definition.path("rooms"));
        Map<String, JsonNode> cohorts = byId(definition.path("cohorts"));
        Map<String, JsonNode> periods = byId(definition.path("periods"));
        Map<String, JsonNode> lessons = byId(definition.path("lessons"));

        // 1. TEACHER_UNAVAILABLE & ROOM_UNAVAILABLE
        for (JsonNode assignment : assignments) {
            String lessonId = assignment.path("lessonId").stringValue();
            String periodId = assignment.path("periodId").stringValue();
            String teacherId = assignment.path("teacherId").stringValue();
            String roomId = assignment.path("roomId").stringValue();

            JsonNode period = periods.get(periodId);
            String periodName = period != null ? textOrDefault(period, "displayName", periodId) : periodId;

            // Teacher availability
            JsonNode teacher = teachers.get(teacherId);
            if (teacher != null && teacher.has("availablePeriodIds")) {
                Set<String> teacherAvail = textSet(teacher.path("availablePeriodIds"));
                if (!teacherAvail.isEmpty() && !teacherAvail.contains(periodId)) {
                    String teacherName = textOrDefault(teacher, "displayName", teacherId);
                    ObjectNode c = json.createObjectNode();
                    c.put("code", "TEACHER_UNAVAILABLE");
                    c.put("lessonId", lessonId);
                    c.putArray("competingLessonIds");
                    c.put("periodId", periodId);
                    c.put("resourceType", "TEACHER");
                    c.put("resourceId", teacherId);
                    c.put("description", "Teacher " + teacherName + " is unavailable in period " + periodName + ".");
                    conflicts.add(c);
                }
            }

            // Room availability
            JsonNode room = rooms.get(roomId);
            if (room != null && room.has("availablePeriodIds")) {
                Set<String> roomAvail = textSet(room.path("availablePeriodIds"));
                if (!roomAvail.isEmpty() && !roomAvail.contains(periodId)) {
                    String roomName = textOrDefault(room, "displayName", roomId);
                    ObjectNode c = json.createObjectNode();
                    c.put("code", "ROOM_UNAVAILABLE");
                    c.put("lessonId", lessonId);
                    c.putArray("competingLessonIds");
                    c.put("periodId", periodId);
                    c.put("resourceType", "ROOM");
                    c.put("resourceId", roomId);
                    c.put("description", "Room " + roomName + " is unavailable in period " + periodName + ".");
                    conflicts.add(c);
                }
            }

            // 6. ROOM_INCOMPATIBLE
            JsonNode lesson = lessons.get(lessonId);
            boolean roomIncompatible = false;
            String reason = null;

            if (lesson != null && room != null) {
                // Check requiredRoomCapabilityIds
                if (lesson.has("requiredRoomCapabilityIds")) {
                    Set<String> requiredCaps = textSet(lesson.path("requiredRoomCapabilityIds"));
                    Set<String> roomCaps = textSet(room.path("capabilityIds"));
                    if (!roomCaps.containsAll(requiredCaps)) {
                        roomIncompatible = true;
                        reason = "Room " + textOrDefault(room, "displayName", roomId)
                                + " does not satisfy capability requirements for " + textOrDefault(lesson, "displayName", lessonId) + ".";
                    }
                }

                // Check definition.roomAssignments
                if (!roomIncompatible && definition.has("roomAssignments")) {
                    String subjectId = textOrNull(lesson, "subjectId");
                    for (JsonNode policy : definition.path("roomAssignments")) {
                        if (subjectId == null || !subjectId.equals(textOrNull(policy, "subjectId"))) continue;
                        String policyTeacher = textOrNull(policy, "teacherId");
                        if (policyTeacher != null && !policyTeacher.isEmpty() && !policyTeacher.equals(teacherId)) continue;

                        if (policy.path("useHomeRoom").asBoolean(false)) {
                            JsonNode cohort = cohorts.get(textOrNull(assignment, "cohortId"));
                            String homeRoomId = cohort != null ? textOrNull(cohort, "homeRoomId") : null;
                            if (homeRoomId == null || !homeRoomId.equals(roomId)) {
                                roomIncompatible = true;
                                String cohortName = cohort != null ? textOrDefault(cohort, "displayName", assignment.path("cohortId").stringValue()) : assignment.path("cohortId").stringValue();
                                reason = "Room " + textOrDefault(room, "displayName", roomId)
                                        + " is not the assigned home room for class " + cohortName + ".";
                                break;
                            }
                        } else if (policy.has("allowedRoomIds")) {
                            Set<String> allowed = textSet(policy.path("allowedRoomIds"));
                            if (!allowed.isEmpty() && !allowed.contains(roomId)) {
                                roomIncompatible = true;
                                reason = "Room " + textOrDefault(room, "displayName", roomId)
                                        + " is not permitted by room assignment policy for " + textOrDefault(lesson, "displayName", lessonId) + ".";
                                break;
                            }
                        }
                    }
                }

                // Check lesson.allowedRoomIds
                if (!roomIncompatible && lesson.has("allowedRoomIds")) {
                    Set<String> allowed = textSet(lesson.path("allowedRoomIds"));
                    if (!allowed.isEmpty() && !allowed.contains(roomId)) {
                        roomIncompatible = true;
                        reason = "Room " + textOrDefault(room, "displayName", roomId)
                                + " is not permitted for lesson " + textOrDefault(lesson, "displayName", lessonId) + ".";
                    }
                }
            }

            if (roomIncompatible) {
                ObjectNode c = json.createObjectNode();
                c.put("code", "ROOM_INCOMPATIBLE");
                c.put("lessonId", lessonId);
                c.putArray("competingLessonIds");
                c.put("periodId", periodId);
                c.put("resourceType", "ROOM");
                c.put("resourceId", roomId);
                c.put("description", reason);
                conflicts.add(c);
            }
        }

        // 2. TEACHER_CLASH
        Map<String, List<JsonNode>> byTeacherPeriod = new HashMap<>();
        for (JsonNode a : assignments) {
            String key = a.path("teacherId").stringValue() + "\u0000" + a.path("periodId").stringValue();
            byTeacherPeriod.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }
        for (List<JsonNode> group : byTeacherPeriod.values()) {
            if (group.size() > 1) {
                String teacherId = group.get(0).path("teacherId").stringValue();
                String periodId = group.get(0).path("periodId").stringValue();
                JsonNode teacher = teachers.get(teacherId);
                JsonNode period = periods.get(periodId);
                String teacherName = teacher != null ? textOrDefault(teacher, "displayName", teacherId) : teacherId;
                String periodName = period != null ? textOrDefault(period, "displayName", periodId) : periodId;

                for (JsonNode a : group) {
                    String lessonId = a.path("lessonId").stringValue();
                    List<String> competing = group.stream()
                            .map(item -> item.path("lessonId").stringValue())
                            .filter(id -> !id.equals(lessonId))
                            .toList();
                    List<String> competingNames = group.stream()
                            .filter(item -> !item.path("lessonId").stringValue().equals(lessonId))
                            .map(item -> {
                                JsonNode l = lessons.get(item.path("lessonId").stringValue());
                                return l != null ? textOrDefault(l, "displayName", item.path("lessonId").stringValue()) : item.path("lessonId").stringValue();
                            })
                            .toList();

                    ObjectNode c = json.createObjectNode();
                    c.put("code", "TEACHER_CLASH");
                    c.put("lessonId", lessonId);
                    ArrayNode compArray = c.putArray("competingLessonIds");
                    competing.forEach(compArray::add);
                    c.put("periodId", periodId);
                    c.put("resourceType", "TEACHER");
                    c.put("resourceId", teacherId);
                    c.put("description", "Teacher " + teacherName + " is double-booked in period " + periodName + " with " + String.join(", ", competingNames) + ".");
                    conflicts.add(c);
                }
            }
        }

        // 3. ROOM_CLASH
        Map<String, List<JsonNode>> byRoomPeriod = new HashMap<>();
        for (JsonNode a : assignments) {
            String key = a.path("roomId").stringValue() + "\u0000" + a.path("periodId").stringValue();
            byRoomPeriod.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }
        for (List<JsonNode> group : byRoomPeriod.values()) {
            if (group.size() > 1) {
                String roomId = group.get(0).path("roomId").stringValue();
                String periodId = group.get(0).path("periodId").stringValue();
                JsonNode room = rooms.get(roomId);
                JsonNode period = periods.get(periodId);
                String roomName = room != null ? textOrDefault(room, "displayName", roomId) : roomId;
                String periodName = period != null ? textOrDefault(period, "displayName", periodId) : periodId;

                for (JsonNode a : group) {
                    String lessonId = a.path("lessonId").stringValue();
                    List<String> competing = group.stream()
                            .map(item -> item.path("lessonId").stringValue())
                            .filter(id -> !id.equals(lessonId))
                            .toList();
                    List<String> competingNames = group.stream()
                            .filter(item -> !item.path("lessonId").stringValue().equals(lessonId))
                            .map(item -> {
                                JsonNode l = lessons.get(item.path("lessonId").stringValue());
                                return l != null ? textOrDefault(l, "displayName", item.path("lessonId").stringValue()) : item.path("lessonId").stringValue();
                            })
                            .toList();

                    ObjectNode c = json.createObjectNode();
                    c.put("code", "ROOM_CLASH");
                    c.put("lessonId", lessonId);
                    ArrayNode compArray = c.putArray("competingLessonIds");
                    competing.forEach(compArray::add);
                    c.put("periodId", periodId);
                    c.put("resourceType", "ROOM");
                    c.put("resourceId", roomId);
                    c.put("description", "Room " + roomName + " is double-booked in period " + periodName + " with " + String.join(", ", competingNames) + ".");
                    conflicts.add(c);
                }
            }
        }

        // 4. COHORT_CLASH
        Map<String, List<JsonNode>> byCohortPeriod = new HashMap<>();
        for (JsonNode a : assignments) {
            String key = a.path("cohortId").stringValue() + "\u0000" + a.path("periodId").stringValue();
            byCohortPeriod.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }
        for (List<JsonNode> group : byCohortPeriod.values()) {
            if (group.size() > 1) {
                String cohortId = group.get(0).path("cohortId").stringValue();
                String periodId = group.get(0).path("periodId").stringValue();
                JsonNode cohort = cohorts.get(cohortId);
                JsonNode period = periods.get(periodId);
                String cohortName = cohort != null ? textOrDefault(cohort, "displayName", cohortId) : cohortId;
                String periodName = period != null ? textOrDefault(period, "displayName", periodId) : periodId;

                for (JsonNode a : group) {
                    String lessonId = a.path("lessonId").stringValue();
                    List<String> competing = group.stream()
                            .map(item -> item.path("lessonId").stringValue())
                            .filter(id -> !id.equals(lessonId))
                            .toList();
                    List<String> competingNames = group.stream()
                            .filter(item -> !item.path("lessonId").stringValue().equals(lessonId))
                            .map(item -> {
                                JsonNode l = lessons.get(item.path("lessonId").stringValue());
                                return l != null ? textOrDefault(l, "displayName", item.path("lessonId").stringValue()) : item.path("lessonId").stringValue();
                            })
                            .toList();

                    ObjectNode c = json.createObjectNode();
                    c.put("code", "COHORT_CLASH");
                    c.put("lessonId", lessonId);
                    ArrayNode compArray = c.putArray("competingLessonIds");
                    competing.forEach(compArray::add);
                    c.put("periodId", periodId);
                    c.put("resourceType", "COHORT");
                    c.put("resourceId", cohortId);
                    c.put("description", "Class " + cohortName + " is double-booked in period " + periodName + " with " + String.join(", ", competingNames) + ".");
                    conflicts.add(c);
                }
            }
        }

        conflicts.sort(Comparator.comparing((ObjectNode o) -> o.path("lessonId").stringValue())
                .thenComparing(o -> o.path("code").stringValue())
                .thenComparing(o -> o.path("resourceId").stringValue()));
        return conflicts;
    }

    private static void requireState(WorkspaceAggregate current, WorkspaceState expected, String message) {
        if (current.state() != expected) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", message);
        }
    }

    private static Map<String, JsonNode> byId(JsonNode array) {
        return byId(array, "id");
    }

    private static Map<String, JsonNode> byId(JsonNode array, String field) {
        Map<String, JsonNode> result = new HashMap<>();
        if (array != null && array.isArray()) {
            for (JsonNode node : array) {
                if (node.has(field) && node.path(field).isTextual()) {
                    result.put(node.path(field).stringValue(), node);
                }
            }
        }
        return result;
    }

    private static Set<String> textSet(JsonNode array) {
        Set<String> result = new HashSet<>();
        if (array != null && array.isArray()) {
            for (JsonNode value : array) {
                if (value.isTextual()) {
                    result.add(value.stringValue());
                }
            }
        }
        return result;
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode child = node.path(field);
        return child.isTextual() ? child.stringValue() : null;
    }

    private static String textOrDefault(JsonNode node, String field, String defaultValue) {
        if (node == null) return defaultValue;
        JsonNode child = node.path(field);
        return child.isTextual() ? child.stringValue() : defaultValue;
    }

    @Transactional(noRollbackFor = WorkspaceProblem.class)
    public WorkspaceAggregate publish(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireState(current, WorkspaceState.MANUAL_DRAFT,
                "Manual draft publication requires MANUAL_DRAFT state.");

        JsonNode manualDraft = current.document().path("manualDraft");
        ArrayNode conflicts = (ArrayNode) manualDraft.path("conflicts");
        if (conflicts != null && !conflicts.isEmpty()) {
            throw new WorkspaceProblem(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "UNRESOLVED_CONFLICTS",
                    "All hard conflicts must be resolved before publishing (" + conflicts.size() + " remaining).");
        }

        JsonNode acceptedBaseline = current.document().path("acceptedBaseline");
        JsonNode definition = acceptedBaseline.path("definition");
        JsonNode originalResult = acceptedBaseline.path("result");

        // Construct candidate timetable result
        ObjectNode candidateResult = (ObjectNode) originalResult.deepCopy();
        candidateResult.put("correlationId", UUID.randomUUID().toString());
        ArrayNode candidateAssignments = (ArrayNode) manualDraft.path("assignments").deepCopy();
        ObjectNode timetable = (ObjectNode) candidateResult.path("timetable");
        timetable.set("assignments", candidateAssignments);

        String timetableRevision = calculateTimetableRevision(
                candidateResult.path("schoolId").stringValue(),
                candidateResult.path("inputRevision").stringValue(),
                candidateAssignments);
        candidateResult.put("timetableRevision", timetableRevision);

        // Kernel verification
        KernelVerifier.Verification verified;
        try {
            verified = verifier.verify(new ImportDocuments(
                    definition, candidateResult, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        } catch (WorkspaceProblem problem) {
            if ("KERNEL_VERIFICATION_FAILED".equals(problem.code())) {
                throw new WorkspaceProblem(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "VERIFICATION_FAILED",
                        problem.getMessage());
            }
            throw problem;
        }

        // Generate updated manifest
        JsonNode newManifest = manifests.validatedOrGenerated(new ImportDocuments(
                definition, candidateResult, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));

        ObjectNode updatedDocument = json.createObjectNode();
        updatedDocument.set("school", current.document().path("school").deepCopy());
        updatedDocument.put("importMode", "MANUAL_EDIT_PUBLICATION");
        updatedDocument.put("definitionRevision", verified.definitionRevision());
        updatedDocument.put("timetableRevision", verified.timetableRevision());

        ObjectNode newAccepted = updatedDocument.putObject("acceptedBaseline");
        newAccepted.set("definition", definition.deepCopy());
        newAccepted.set("result", candidateResult);
        if (newManifest != null) {
            newAccepted.set("manifest", newManifest);
        }

        if (current.document().has("lastRun")) {
            updatedDocument.set("lastRun", current.document().path("lastRun").deepCopy());
        }

        return mutation.replaceManualDraft(
                expectedVersion,
                WorkspaceState.MANUAL_DRAFT,
                WorkspaceState.ACCEPTED_BASELINE,
                updatedDocument);
    }

    public static String calculateTimetableRevision(String schoolId, String inputRevision, ArrayNode assignments) {
        ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        ObjectNode scope = mapper.createObjectNode();
        scope.put("schemaVersion", 1);
        scope.put("schoolId", schoolId);
        scope.put("inputRevision", inputRevision);
        var ordered = new ArrayList<JsonNode>();
        assignments.forEach(ordered::add);
        ordered.sort(Comparator.comparing(item -> item.path("lessonId").stringValue()));
        ArrayNode normalizedAssignments = mapper.createArrayNode();
        ordered.forEach(normalizedAssignments::add);
        scope.set("assignments", normalizedAssignments);
        try {
            byte[] canonical = CanonicalJson.bytes(scope);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
