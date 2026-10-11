package org.schoolkernel.workspace;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Selected-item authoring: native fields remain JSON values until authoritative finish verification. */
@Service
public class DefinitionEntityService {
    private static final Map<String,Integer> SUBJECT_FIELDS = Map.of("id",1,"displayName",1,
            "reservedPeriodsAllowed",6,"maxWeeklyReservedLessonsPerCohort",6,"dayEdgeOnly",6,
            "maxDailyLessonsPerCohort",6,"curatorLesson",8);
    private static final Map<String,Integer> TEACHER_FIELDS = Map.of("id",1,"displayName",1,
            "qualifiedSubjectIds",1,"availablePeriodIds",1,"undesirablePeriodIds",1);
    private static final Map<String,Integer> COHORT_FIELDS = Map.ofEntries(
            Map.entry("id",1), Map.entry("displayName",1), Map.entry("size",1), Map.entry("availablePeriodIds",1),
            Map.entry("undesirablePeriodIds",1), Map.entry("maxDailyLessonSpread",4), Map.entry("maxDailyGaps",5),
            Map.entry("latestStartSlot",6), Map.entry("preferredLatestStartSlot",6), Map.entry("dailyLessonSpreadLimit",7),
            Map.entry("curatorTeacherId",8), Map.entry("homeRoomId",8));
    private static final Map<String,Integer> ROOM_FIELDS = Map.of("id",1,"displayName",1,"capacity",1,"capabilityIds",1,"availablePeriodIds",1);
    private static final Map<String,Integer> POLICY_FIELDS=Map.of("id",9,"subjectId",9,"teacherId",9,"allowedRoomIds",9,"useHomeRoom",9);
    private final WorkspaceRepository repository;
    private final ObjectMapper json;
    public DefinitionEntityService(WorkspaceRepository repository, ObjectMapper json) { this.repository=repository; this.json=json; }

    @Transactional
    public WorkspaceAggregate saveSubject(String ifMatch, JsonNode request) {
        return saveEntity(ifMatch, request, "subjects", "Subject", SUBJECT_FIELDS);
    }

    @Transactional
    public WorkspaceAggregate saveTeacher(String ifMatch, JsonNode request) {
        return saveEntity(ifMatch, request, "teachers", "Teacher", TEACHER_FIELDS);
    }

    @Transactional
    public WorkspaceAggregate saveCohort(String ifMatch, JsonNode request) {
        return saveEntity(ifMatch, request, "cohorts", "Cohort", COHORT_FIELDS);
    }

    @Transactional
    public WorkspaceAggregate saveRoom(String ifMatch, JsonNode request) {
        return saveEntity(ifMatch, request, "rooms", "Room", ROOM_FIELDS);
    }

    @Transactional
    public WorkspaceAggregate saveRoomPolicy(String ifMatch,JsonNode request){return saveEntity(ifMatch,request,"roomAssignments","RoomAssignment",POLICY_FIELDS);}

    private WorkspaceAggregate saveEntity(String ifMatch, JsonNode request, String collection,
            String type, Map<String,Integer> fields) {
        WorkspaceAggregate current = repository.load();
        long version = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.DEFINITION_DRAFT)
            throw problem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION",
                    type + " editing requires DEFINITION_DRAFT; " + current.state() + " is active. No change was committed.");
        if (!(request instanceof ObjectNode) || request.size() != 2 || !request.path("originalId").isTextual()
                || !(request.path("item") instanceof ObjectNode item))
            throw problem(HttpStatus.BAD_REQUEST, "INVALID_ENTITY_FIELDS", "Supply originalId and the selected item only. No change was committed.");
        ObjectNode document = ((ObjectNode) current.document()).deepCopy();
        ObjectNode draft = (ObjectNode) document.path("definitionDraft");
        ObjectNode definition = (ObjectNode) draft.path("definition");
        for (var entry : item.properties()) {
            Integer since = fields.get(entry.getKey());
            if (since == null)
                throw problem(HttpStatus.BAD_REQUEST, "UNSUPPORTED_ENTITY_FIELD", "Unsupported " + type + " field " + entry.getKey() + ". No change was committed.");
            if (since > definition.path("catalogVersion").intValue())
                throw problem(HttpStatus.CONFLICT, "CATALOG_UPGRADE_REQUIRED", entry.getKey() + " requires catalog " + since + ". Upgrade the catalog before choosing this setting. No change was committed.");
        }
        String id = item.path("id").asString("");
        String original = request.path("originalId").stringValue();
        if (!item.path("id").isTextual() || !validId(id))
            throw problem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ENTITY_ID", type + " identity must be a valid 1–128 character contract identifier. No change was committed.");
        ArrayNode rows = definition.has(collection) ? (ArrayNode) definition.path(collection) : definition.putArray(collection);
        int index = -1;
        for (int i = 0; i < rows.size(); i++) if (original.equals(rows.get(i).path("id").asString(""))) index = i;
        if (!original.isEmpty() && (index < 0 || !original.equals(id)))
            throw problem(HttpStatus.CONFLICT, "ENTITY_ID_IMMUTABLE", "The saved " + type + " identity is immutable; reload if its item was removed. No change was committed.");
        if (original.isEmpty()) {
            for (JsonNode row : rows) if (id.equals(row.path("id").asString("")))
                throw problem(HttpStatus.CONFLICT, "ENTITY_ID_COLLISION", "That " + type + " identifier already exists. Choose another identity. No change was committed.");
            for (JsonNode removed : draft.path("removedIdentities"))
                if (type.equals(removed.path("ownerType").asString("")) && id.equals(removed.path("id").asString("")))
                    throw problem(HttpStatus.CONFLICT, "ENTITY_ID_RESERVED", "That " + type + " identity is reserved in this lineage. Use source restoration where available. No change was committed.");
            JsonNode source = draft.path("sourceDocument");
            JsonNode sourceDefinition = source.has("initialDefinition") ? source.path("initialDefinition") : source.path("acceptedBaseline").path("definition");
            for (JsonNode row : sourceDefinition.path(collection)) if (id.equals(row.path("id").asString("")))
                throw problem(HttpStatus.CONFLICT, "ENTITY_ID_RESERVED", "That source " + type + " identity is reserved. Use source restoration. No change was committed.");
            rows.add(item.deepCopy());
        } else rows.set(index, item.deepCopy());
        ArrayNode issues = json.createArrayNode();
        Set<String> affected = new HashSet<>();
        String reference = type.equals("Subject") ? "subjectId" : type.equals("Teacher") ? "teacherId" : "cohortId";
        for (JsonNode lesson : definition.path("lessons")) if (type.equals("Room") || id.equals(lesson.path(reference).asString(""))) affected.add(lesson.path("id").asString(""));
        for (JsonNode issue : draft.path("issues")) {
            boolean owned = type.equals(issue.path("ownerType").asString("")) && id.equals(issue.path("ownerId").asString(""));
            boolean dependent = Set.of("SUBJECT_POLICY", "TEACHER_POLICY", "ENTITY_LESSON", "COHORT_FEASIBILITY").contains(issue.path("diagnosticSource").asString(""))
                    && affected.contains(issue.path("ownerId").asString(""));
            if (!owned && !dependent && !(type.equals("Room") && "COHORT_FEASIBILITY".equals(issue.path("diagnosticSource").asString("")))) issues.add(issue);
        }
        if (type.equals("Subject")) validateSubject(definition, item, issues);
        else if (type.equals("Teacher")) validateTeacher(definition, item, issues);
        else if (type.equals("Cohort")) validateCohort(definition, item, issues);
        else if(type.equals("RoomAssignment")) validateRoomPolicy(definition,item,issues);
        else {
            validateRoom(definition, item, issues);
            for (JsonNode cohort : definition.path("cohorts")) validateCohortFeasibility(definition, cohort, issues);
        }
        validateLessons(definition, affected, issues);
        draft.set("issues", ordered(issues));
        refreshTeachingIssues(draft);
        draft.put("draftRevision", DefinitionDraftService.revision(definition));
        if (repository.replace(version, WorkspaceState.DEFINITION_DRAFT, WorkspaceState.DEFINITION_DRAFT, document).isEmpty())
            throw problem(HttpStatus.PRECONDITION_FAILED, "STALE_WORKSPACE_VERSION", "The workspace changed. Review the newer saved draft before retrying. No change was committed.");
        return repository.load();
    }

    private void validateTeacher(JsonNode definition, JsonNode teacher, ArrayNode issues) {
        String id = teacher.path("id").asString("");
        if (!teacher.path("displayName").isTextual() || teacher.path("displayName").stringValue().isBlank())
            issue(issues, "Teacher", id, "displayName", "Enter a nonblank teacher name.");
        validateReferences(teacher, "qualifiedSubjectIds", true, definition.path("subjects"), "Teacher", issues);
        validateReferences(teacher, "availablePeriodIds", false, definition.path("periods"), "Teacher", issues);
        validateReferences(teacher, "undesirablePeriodIds", false, definition.path("periods"), "Teacher", issues);
    }

    private void validateReferences(JsonNode item, String field, boolean required, JsonNode targets,
            String type, ArrayNode issues) {
        String id = item.path("id").asString("");
        if (!item.has(field) && !required) return;
        JsonNode values = item.path(field);
        if (!values.isArray()) {
            issue(issues, type, id, field, "Choose an explicit unique identifier selection" + (required ? "." : ", or use the omitted default."));
            return;
        }
        Set<String> seen = new HashSet<>();
        for (JsonNode value : values) {
            String target = value.asString("");
            if (!value.isTextual() || !validId(target)) issue(issues, type, id, field, "Invalid reference: " + value + ".");
            else if (!seen.add(target)) issue(issues, type, id, field, "Duplicate reference: " + target + ".");
            else if (find(targets, target) == null) {
                ObjectNode issue = issue(issues, type, id, field, "Unresolved reference: " + target + ".");
                issue.withArray("relatedIds").add(target);
            }
        }
    }

    private static boolean isTrue(JsonNode value) { return value.isBoolean() && value.booleanValue(); }

    private static boolean validId(String id) { return id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"); }

    private void validateSubject(JsonNode definition, JsonNode subject, ArrayNode issues) {
        String id=subject.path("id").asString("");
        if(!subject.path("displayName").isTextual() || subject.path("displayName").stringValue().isBlank()) issue(issues,"Subject",id,"displayName","Enter a nonblank subject name.");
        for(String field:List.of("reservedPeriodsAllowed","dayEdgeOnly","curatorLesson")) if(subject.has(field) && !subject.path(field).isBoolean()) issue(issues,"Subject",id,field,"Choose default, true or false for this setting.");
        for(String field:List.of("maxWeeklyReservedLessonsPerCohort","maxDailyLessonsPerCohort")) if(subject.has(field)) {
            JsonNode value=subject.path(field);
            if(!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue()<1) issue(issues,"Subject",id,field,"Use an integer from 1 to 2147483647, or omit for no limit.");
        }
        if(subject.has("maxWeeklyReservedLessonsPerCohort") && !(subject.path("reservedPeriodsAllowed").isBoolean() && subject.path("reservedPeriodsAllowed").booleanValue())) issue(issues,"Subject",id,"maxWeeklyReservedLessonsPerCohort","A reserved-weekly cap requires explicit permission to use reserved periods.");
    }
    private static JsonNode find(JsonNode rows,String id) { for(JsonNode row:rows) if(id.equals(row.path("id").asString(""))) return row;return null; }
    private static boolean contains(JsonNode rows,String id) { for(JsonNode value:rows) if(id.equals(value.asString(""))) return true;return false; }
    private void validateCohort(JsonNode definition, JsonNode cohort, ArrayNode issues) {
        String id = cohort.path("id").asString("");
        if (!cohort.path("displayName").isTextual() || cohort.path("displayName").stringValue().isBlank())
            issue(issues, "Cohort", id, "displayName", "Enter a nonblank class name.");
        numeric(cohort, "size", true, 1, "Cohort", issues);
        for (String field : List.of("maxDailyLessonSpread", "maxDailyGaps", "dailyLessonSpreadLimit")) numeric(cohort, field, false, 0, "Cohort", issues);
        for (String field : List.of("latestStartSlot", "preferredLatestStartSlot")) numeric(cohort, field, false, 1, "Cohort", issues);
        validateReferences(cohort, "availablePeriodIds", false, definition.path("periods"), "Cohort", issues);
        validateReferences(cohort, "undesirablePeriodIds", false, definition.path("periods"), "Cohort", issues);
        for (String field : List.of("curatorTeacherId", "homeRoomId")) if (cohort.has(field)) {
            JsonNode value = cohort.path(field);
            if (!value.isTextual() || !validId(value.asString("")) || find(definition.path(field.equals("homeRoomId") ? "rooms" : "teachers"), value.asString("")) == null) {
                ObjectNode issue = issue(issues, "Cohort", id, field, "Choose an existing " + (field.equals("homeRoomId") ? "room" : "teacher") + " identifier, or use the omitted default.");
                issue.withArray("relatedIds").add(value.asString(""));
            }
        }
        validateCohortFeasibility(definition, cohort, issues);
    }

    private void validateRoom(JsonNode definition, JsonNode room, ArrayNode issues) {
        String id = room.path("id").asString("");
        if (!room.path("displayName").isTextual() || room.path("displayName").stringValue().isBlank()) issue(issues, "Room", id, "displayName", "Enter a nonblank room name.");
        numeric(room, "capacity", true, 0, "Room", issues);
        JsonNode tokens = room.path("capabilityIds");
        if (!tokens.isArray()) issue(issues, "Room", id, "capabilityIds", "Enter a unique array of capability tokens; empty is allowed.");
        else {
            Set<String> seen = new HashSet<>();
            for (JsonNode token : tokens) {
                String value = token.asString("");
                if (!token.isTextual() || !validId(value)) issue(issues, "Room", id, "capabilityIds", "Invalid capability token: " + token + ".");
                else if (!seen.add(value)) issue(issues, "Room", id, "capabilityIds", "Duplicate capability token: " + value + ".");
            }
        }
        validateReferences(room, "availablePeriodIds", false, definition.path("periods"), "Room", issues);
    }

    private void validateCohortFeasibility(JsonNode definition, JsonNode cohort, ArrayNode issues) {
        String id = cohort.path("id").asString("");
        List<JsonNode> lessons = new ArrayList<>();
        for (JsonNode lesson : definition.path("lessons")) if (id.equals(lesson.path("cohortId").asString(""))) lessons.add(lesson);
        if (lessons.isEmpty()) return;
        Set<String> eligible = new HashSet<>();
        for (JsonNode lesson : lessons) {
            JsonNode subject = find(definition.path("subjects"), lesson.path("subjectId").asString(""));
            JsonNode teacher = find(definition.path("teachers"), lesson.path("teacherId").asString(""));
            for (JsonNode period : definition.path("periods")) {
                String periodId = period.path("id").asString("");
                if (cohort.has("availablePeriodIds") && !contains(cohort.path("availablePeriodIds"), periodId)) continue;
                if (teacher != null && teacher.has("availablePeriodIds") && !contains(teacher.path("availablePeriodIds"), periodId)) continue;
                if (contains(definition.path("reservedPeriodIds"), periodId) && (subject == null || !isTrue(subject.path("reservedPeriodsAllowed")))) continue;
                if (lesson.has("periodLock") && !periodId.equals(lesson.path("periodLock").asString(""))) continue;
                eligible.add(periodId);
            }
        }
        if (lessons.size() > eligible.size()) advisory(issues, "Cohort", id, "availablePeriodIds", "Configured class load " + lessons.size() + " exceeds its eligible-period upper bound " + eligible.size() + ". This is a necessary-condition check, not a complete feasibility analysis.");
        for (JsonNode lesson : lessons) if (lesson.path("requiredRoomCapabilityIds").isArray() && !lesson.path("requiredRoomCapabilityIds").isEmpty()) {
            boolean supplied = false;
            for (JsonNode room : definition.path("rooms")) if (capable(room, lesson)) supplied = true;
            if (!supplied) {
                ObjectNode issue = issue(issues, "Lesson", lesson.path("id").asString(""), "requiredRoomCapabilityIds", "No configured room supplies this lesson's required capability combination. This advisory does not refuse definition finish.");
                issue.put("severity", "ADVISORY"); issue.put("diagnosticSource", "ENTITY_LESSON"); issue.withArray("relatedIds").add(id);
            }
        }
        if (validNumber(cohort.path("size"), 1)) {
            boolean host = false;
            for (JsonNode room : definition.path("rooms")) if (validNumber(room.path("capacity"), 0) && room.path("capacity").intValue() >= cohort.path("size").intValue()) host = true;
            if (!host) advisory(issues, "Cohort", id, "size", "No configured room can hold this taught class. This advisory does not refuse definition finish.");
        }
    }

    private void numeric(JsonNode item, String field, boolean required, int minimum, String type, ArrayNode issues) {
        if (!required && !item.has(field)) return;
        if (!validNumber(item.path(field), minimum)) issue(issues, type, item.path("id").asString(""), field,
                "Use an integer from " + minimum + " to 2147483647" + (required ? "." : ", or use the omitted default."));
    }
    private static boolean validNumber(JsonNode value, int minimum) { return value.isIntegralNumber() && value.canConvertToInt() && value.intValue() >= minimum; }
    private void advisory(ArrayNode issues, String type, String id, String field, String message) {
        ObjectNode issue = issue(issues, type, id, field, message);
        issue.put("severity", "ADVISORY"); issue.put("diagnosticSource", "COHORT_FEASIBILITY");
    }

    /** Recompute dependent diagnostics from exact saved values, regardless of which selected entity changed. */
    private void validateLessons(JsonNode definition, Set<String> affected, ArrayNode issues) {
        for (JsonNode lesson : definition.path("lessons")) {
            String id = lesson.path("id").asString("");
            if (!affected.contains(id)) continue;
            JsonNode subject = find(definition.path("subjects"), lesson.path("subjectId").asString(""));
            JsonNode teacher = find(definition.path("teachers"), lesson.path("teacherId").asString(""));
            JsonNode cohort = find(definition.path("cohorts"), lesson.path("cohortId").asString(""));
            boolean curator = subject != null && subject.path("curatorLesson").isBoolean() && subject.path("curatorLesson").booleanValue();
            if (curator) {
                if (cohort == null || !cohort.has("curatorTeacherId") || !cohort.path("curatorTeacherId").equals(lesson.path("teacherId")))
                    dependent(issues, lesson, "teacherId", "Curator lessons require the class's configured curator; no assignment or qualification is changed automatically.");
                if (cohort != null && cohort.has("homeRoomId")) {
                    String home = cohort.path("homeRoomId").asString("");
                    if (lesson.has("roomLock") && !home.equals(lesson.path("roomLock").asString(""))) dependent(issues, lesson, "roomLock", "Persistent room lock contradicts the curator class home room.");
                    JsonNode room = find(definition.path("rooms"), home);
                    if (room != null && cohort.path("size").isIntegralNumber() && room.path("capacity").isIntegralNumber() && room.path("capacity").longValue() < cohort.path("size").longValue())
                        dependent(issues, lesson, "cohortId", "Curator home room contradicts the class size and room capacity.");
                    if (room != null && !capable(room, lesson)) dependent(issues, lesson, "requiredRoomCapabilityIds", "Curator home room contradicts required room capabilities.");
                }
            } else if (teacher == null || !contains(teacher.path("qualifiedSubjectIds"), lesson.path("subjectId").asString("")))
                dependent(issues, lesson, "teacherId", "The assigned teacher is not qualified for this ordinary subject; teaching reassignment is explicit.");
            if (lesson.has("periodLock")) {
                String period = lesson.path("periodLock").asString("");
                if (teacher != null && teacher.has("availablePeriodIds") && !contains(teacher.path("availablePeriodIds"), period)) dependent(issues, lesson, "periodLock", "The persistent period lock is outside this teacher's saved availability.");
                if (cohort != null && cohort.has("availablePeriodIds") && !contains(cohort.path("availablePeriodIds"), period)) dependent(issues, lesson, "periodLock", "The persistent period lock is outside this class's saved availability.");
                if (contains(definition.path("reservedPeriodIds"), period) && (subject == null || !isTrue(subject.path("reservedPeriodsAllowed")))) dependent(issues, lesson, "periodLock", "The subject does not permit this reserved-period lock.");
            }
            JsonNode locked = find(definition.path("rooms"), lesson.path("roomLock").asString(""));
            if (lesson.has("roomLock") && locked != null) {
                if (cohort != null && validNumber(cohort.path("size"), 1) && validNumber(locked.path("capacity"), 0) && locked.path("capacity").intValue() < cohort.path("size").intValue()) dependent(issues, lesson, "roomLock", "Persistent room lock has insufficient capacity for the class.");
                if (!capable(locked, lesson)) dependent(issues, lesson, "roomLock", "Persistent room lock cannot supply required room capabilities.");
            }
            Set<String> allowed = null;
            for (JsonNode policy : definition.path("roomAssignments")) {
                if (!policy.path("subjectId").equals(lesson.path("subjectId")) || policy.has("teacherId") && !policy.path("teacherId").equals(lesson.path("teacherId"))) continue;
                Set<String> target = new HashSet<>();
                if (isTrue(policy.path("useHomeRoom"))) {
                    if (cohort == null || !cohort.has("homeRoomId")) dependent(issues, lesson, "cohortId", "Matched room policy " + policy.path("id").asString("") + " requires this class to declare a home room.");
                    else target.add(cohort.path("homeRoomId").asString(""));
                } else for (JsonNode room : policy.path("allowedRoomIds")) target.add(room.asString(""));
                if (allowed == null) allowed = target; else allowed.retainAll(target);
            }
            if (lesson.has("periodLock")) {
                String period = lesson.path("periodLock").asString("");
                JsonNode home = curator && cohort != null ? find(definition.path("rooms"), cohort.path("homeRoomId").asString("")) : null;
                if (locked != null && locked.has("availablePeriodIds") && !contains(locked.path("availablePeriodIds"), period)) dependent(issues, lesson, "periodLock", "Persistent room lock is unavailable at the locked period.");
                if (home != null && home.has("availablePeriodIds") && !contains(home.path("availablePeriodIds"), period)) dependent(issues, lesson, "periodLock", "Curator home room is unavailable at the locked period.");
                if (allowed != null) {
                    boolean available = false;
                    for (JsonNode room : definition.path("rooms")) if (allowed.contains(room.path("id").asString("")) && (!room.has("availablePeriodIds") || contains(room.path("availablePeriodIds"), period))) available = true;
                    if (!available) dependent(issues, lesson, "periodLock", "No matched policy room is available at the persistent period lock.");
                }
            }
            if (allowed != null) {
                if (allowed.isEmpty()) dependent(issues, lesson, "roomAssignments", "Matching room policies have no room in common.");
                if (lesson.has("roomLock") && !allowed.contains(lesson.path("roomLock").asString(""))) dependent(issues, lesson, "roomLock", "Matched room policies contradict the persistent room lock.");
                if (curator && cohort != null && cohort.has("homeRoomId") && !allowed.contains(cohort.path("homeRoomId").asString(""))) dependent(issues, lesson, "homeRoomId", "Matched room policies contradict the curator home room.");
                boolean host = false;
                for (JsonNode room : definition.path("rooms")) if (allowed.contains(room.path("id").asString("")) && cohort != null && validNumber(cohort.path("size"), 1) && validNumber(room.path("capacity"), 0) && room.path("capacity").intValue() >= cohort.path("size").intValue() && capable(room, lesson)) host = true;
                if (!host) dependent(issues, lesson, "roomAssignments", "Matched room policies have no room with sufficient capacity and required capabilities.");
            }
        }
    }
    private static boolean capable(JsonNode room, JsonNode lesson) {
        for (JsonNode capability : lesson.path("requiredRoomCapabilityIds")) if (!contains(room.path("capabilityIds"), capability.asString(""))) return false;
        return true;
    }
    private void dependent(ArrayNode issues, JsonNode lesson, String field, String message) {
        ObjectNode issue = issue(issues, "Lesson", lesson.path("id").asString(""), field, message);
        issue.put("diagnosticSource", "ENTITY_LESSON");
        for (String reference : List.of("subjectId", "teacherId", "cohortId", "roomLock")) if (lesson.has(reference)) issue.withArray("relatedIds").add(lesson.path(reference).asString(""));
        // Room edits can affect alternatives, home-room obligations and matching policy intersections.
        // The room view also shows all recomputed lesson/class advisories, so no central capability catalog is inferred.
    }

    private void validateRoomPolicy(JsonNode definition,JsonNode policy,ArrayNode issues){
        String id=policy.path("id").asString("");int start=issues.size();
        for(String field:List.of("subjectId","teacherId"))if(field.equals("subjectId")||policy.has(field))if(!policy.path(field).isTextual()||!validId(policy.path(field).asString(""))||find(definition.path(field.equals("subjectId")?"subjects":"teachers"),policy.path(field).asString(""))==null)issue(issues,"RoomAssignment",id,field,"Choose an existing "+field+" for this room policy.");
        boolean rooms=policy.has("allowedRoomIds"),home=policy.has("useHomeRoom");
        if(rooms==home)issue(issues,"RoomAssignment",id,"target","Choose exactly one target: a nonempty allowed-room set or Use cohort home room.");
        if(home&&!isTrue(policy.path("useHomeRoom")))issue(issues,"RoomAssignment",id,"useHomeRoom","Use cohort home room must be explicitly true in this target mode.");
        if(rooms){validateReferences(policy,"allowedRoomIds",true,definition.path("rooms"),"RoomAssignment",issues);if(policy.path("allowedRoomIds").isArray()&&policy.path("allowedRoomIds").isEmpty())issue(issues,"RoomAssignment",id,"allowedRoomIds","Select at least one allowed room; an empty target is invalid.");}
        for(int i=start;i<issues.size();i++)((ObjectNode)issues.get(i)).put("diagnosticSource","ROOM_POLICY_FIELDS");
    }

    /** Refresh complete teaching consequences after an atomic occurrence operation. */
    void refreshTeachingIssues(ObjectNode draft) {
        JsonNode definition=draft.path("definition");ArrayNode issues=json.createArrayNode();
        Set<String> refreshed=Set.of("ENTITY_LESSON","COHORT_FEASIBILITY","TEACHING_FIELDS","TEACHING_FEASIBILITY","REQUIREMENT_CONTEXT","ROOM_POLICY_FIELDS");
        for(JsonNode issue:draft.path("issues"))if(!refreshed.contains(issue.path("diagnosticSource").asString("")))issues.add(issue);
        for(JsonNode policy:definition.path("roomAssignments")) validateRoomPolicy(definition,policy,issues);
        Set<String> ids=new HashSet<>();Map<String,JsonNode> series=new HashMap<>();
        for(JsonNode lesson:definition.path("lessons")) {
            String id=lesson.path("id").asString("");ids.add(id);validateLessonFields(definition,lesson,issues);
            if(lesson.path("seriesId").isTextual()) {
                JsonNode previous=series.putIfAbsent(lesson.path("seriesId").asString(""),lesson);
                if(previous!=null&&(!previous.path("cohortId").equals(lesson.path("cohortId"))||!previous.path("subjectId").equals(lesson.path("subjectId"))))dependent(issues,lesson,"seriesId","Every series member must share its class and subject.");
            }
        }
        for(JsonNode cohort:definition.path("cohorts"))validateCohortFeasibility(definition,cohort,issues);
        validateLessons(definition,ids,issues);
        for(JsonNode teacher:definition.path("teachers")) {
            Set<String> eligible=new HashSet<>();int load=0;
            for(JsonNode lesson:definition.path("lessons"))if(teacher.path("id").equals(lesson.path("teacherId"))) {
                load++;JsonNode cohort=find(definition.path("cohorts"),lesson.path("cohortId").asString("")),subject=find(definition.path("subjects"),lesson.path("subjectId").asString(""));
                for(JsonNode period:definition.path("periods")){String periodId=period.path("id").asString("");if(teacher.has("availablePeriodIds")&&!contains(teacher.path("availablePeriodIds"),periodId))continue;if(cohort!=null&&cohort.has("availablePeriodIds")&&!contains(cohort.path("availablePeriodIds"),periodId))continue;if(contains(definition.path("reservedPeriodIds"),periodId)&&(subject==null||!isTrue(subject.path("reservedPeriodsAllowed"))))continue;if(lesson.has("periodLock")&&!periodId.equals(lesson.path("periodLock").asString("")))continue;eligible.add(periodId);}
            }
            if(load>eligible.size()){ObjectNode issue=issue(issues,"Teacher",teacher.path("id").asString(""),"availablePeriodIds","Configured teacher load "+load+" exceeds its eligible-period upper bound "+eligible.size()+". Necessary-condition advisory, not complete feasibility.");issue.put("severity","ADVISORY").put("diagnosticSource","TEACHING_FEASIBILITY");}
        }
        List<JsonNode> lessons=new ArrayList<>();definition.path("lessons").forEach(lessons::add);
        for(int i=0;i<lessons.size();i++)for(int j=i+1;j<lessons.size();j++) {
            JsonNode a=lessons.get(i),b=lessons.get(j);if(!a.has("periodLock")||!a.path("periodLock").equals(b.path("periodLock")))continue;
            for(String field:List.of("teacherId","cohortId","roomLock"))if(a.has(field)&&a.path(field).equals(b.path(field))){dependent(issues,a,"periodLock","Persistent locked collision with "+b.path("id").asString("")+" on "+field+".");dependent(issues,b,"periodLock","Persistent locked collision with "+a.path("id").asString("")+" on "+field+".");}
        }
        for(var entry:draft.path("editingContext").path("requirements").properties()) {
            JsonNode row=entry.getValue(),count=row.path("count");String id=entry.getKey();
            if(!count.isIntegralNumber()||!count.canConvertToInt()||count.intValue()<0) requirementIssue(issues,id,"count","Complete the nonnegative integer weekly count; existing occurrences are unchanged.");
            if(row.path("mixedFields").isEmpty()) validateRequirementDefaults(definition,row,issues);
            else if(count.isIntegralNumber() && count.intValue()==0) for(JsonNode field:row.path("mixedFields")) requirementIssue(issues,id,field.asString(""),"Complete the Mixed shared setting in this zero-count requirement, or explicitly remove its context.");
            for(String field:List.of("cohortId","subjectId"))if(!row.path("mixedFields").isEmpty() && (!row.path(field).isTextual()||find(definition.path(field.equals("cohortId")?"cohorts":"subjects"),row.path(field).asString(""))==null)) requirementIssue(issues,id,field,"Choose an existing "+field+" for this saved requirement.");
        }
        draft.set("issues",ordered(issues));
        DefinitionPreferenceService.refresh(draft,json);
    }

    private void requirementIssue(ArrayNode issues,String id,String field,String message) { issue(issues,"Requirement",id,field,message).put("diagnosticSource","REQUIREMENT_CONTEXT"); }

    private void validateLessonFields(JsonNode definition,JsonNode lesson,ArrayNode issues) {
        int start=issues.size();String id=lesson.path("id").asString("");
        if(!lesson.path("displayName").isTextual()||lesson.path("displayName").asString("").isBlank())issue(issues,"Lesson",id,"displayName","Enter a nonblank occurrence name.");
        for(String field:List.of("subjectId","cohortId","teacherId","periodLock","roomLock","seriesId")) {
            if(!lesson.has(field)&&List.of("periodLock","roomLock","seriesId").contains(field))continue;
            String target=lesson.path(field).asString("");String collection=switch(field){case "subjectId"->"subjects";case "cohortId"->"cohorts";case "teacherId"->"teachers";case "periodLock"->"periods";case "roomLock"->"rooms";default->null;};
            if(!lesson.path(field).isTextual()||!validId(target)||(collection!=null&&find(definition.path(collection),target)==null))issue(issues,"Lesson",id,field,"Choose a valid "+field+(collection==null?" identity.":" from the configured school."));
        }
        validateReferences(lesson,"preferredRoomIds",false,definition.path("rooms"),"Lesson",issues);validateReferences(lesson,"undesirablePeriodIds",false,definition.path("periods"),"Lesson",issues);
        if(lesson.has("requiredRoomCapabilityIds")) {
            JsonNode tokens=lesson.path("requiredRoomCapabilityIds");Set<String> seen=new HashSet<>();
            if(!tokens.isArray())issue(issues,"Lesson",id,"requiredRoomCapabilityIds","Use a unique capability token array or omitted default.");
            else for(JsonNode token:tokens)if(!token.isTextual()||!validId(token.asString(""))||!seen.add(token.asString("")))issue(issues,"Lesson",id,"requiredRoomCapabilityIds","Invalid or duplicate capability token: "+token+".");
        }
        for(int i=start;i<issues.size();i++)((ObjectNode)issues.get(i)).put("diagnosticSource","TEACHING_FIELDS");
    }

    void validateRequirementDefaults(JsonNode definition,JsonNode row,ArrayNode issues) {
        ObjectNode sample=json.createObjectNode().put("id",row.path("seriesId").asString("")).put("displayName","Requirement defaults");sample.set("subjectId",row.path("subjectId"));sample.set("cohortId",row.path("cohortId"));for(var field:row.path("defaults").properties())sample.set(field.getKey(),field.getValue());
        int start=issues.size();validateLessonFields(definition,sample,issues);
        for(int i=start;i<issues.size();i++)((ObjectNode)issues.get(i)).put("ownerType","Requirement").put("diagnosticSource","REQUIREMENT_CONTEXT");
    }

    private ObjectNode issue(ArrayNode issues,String type,String id,String field,String message) {
        ObjectNode issue=issues.addObject().put("severity","BLOCKING").put("ownerType",type).put("ownerId",id).put("field",field).put("message",message);issue.putArray("relatedIds");return issue;
    }
    private ArrayNode ordered(ArrayNode issues) {
        List<JsonNode> sorted=new ArrayList<>();issues.forEach(sorted::add);
        sorted.sort(Comparator.comparing((JsonNode n)->"BLOCKING".equals(n.path("severity").asString(""))?0:1).thenComparing(n->n.path("ownerType").asString("")).thenComparing(n->n.path("ownerId").asString("")).thenComparing(n->n.path("field").asString("")).thenComparing(n->n.path("message").asString("")));
        ArrayNode result=json.createArrayNode();sorted.forEach(result::add);return result;
    }
    private static WorkspaceProblem problem(HttpStatus status,String code,String message) { return new WorkspaceProblem(status,code,message); }
}
