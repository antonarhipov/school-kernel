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
        String id = item.path("id").asString();
        String original = request.path("originalId").stringValue();
        if (!item.path("id").isTextual() || !validId(id))
            throw problem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ENTITY_ID", type + " identity must be a valid 1–128 character contract identifier. No change was committed.");
        ArrayNode rows = (ArrayNode) definition.path(collection);
        int index = -1;
        for (int i = 0; i < rows.size(); i++) if (original.equals(rows.get(i).path("id").asString())) index = i;
        if (!original.isEmpty() && (index < 0 || !original.equals(id)))
            throw problem(HttpStatus.CONFLICT, "ENTITY_ID_IMMUTABLE", "The saved " + type + " identity is immutable; reload if its item was removed. No change was committed.");
        if (original.isEmpty()) {
            for (JsonNode row : rows) if (id.equals(row.path("id").asString()))
                throw problem(HttpStatus.CONFLICT, "ENTITY_ID_COLLISION", "That " + type + " identifier already exists. Choose another identity. No change was committed.");
            for (JsonNode removed : draft.path("removedIdentities"))
                if (type.equals(removed.path("ownerType").asString()) && id.equals(removed.path("id").asString()))
                    throw problem(HttpStatus.CONFLICT, "ENTITY_ID_RESERVED", "That " + type + " identity is reserved in this lineage. Use source restoration where available. No change was committed.");
            JsonNode source = draft.path("sourceDocument");
            JsonNode sourceDefinition = source.has("initialDefinition") ? source.path("initialDefinition") : source.path("acceptedBaseline").path("definition");
            for (JsonNode row : sourceDefinition.path(collection)) if (id.equals(row.path("id").asString()))
                throw problem(HttpStatus.CONFLICT, "ENTITY_ID_RESERVED", "That source " + type + " identity is reserved. Use source restoration. No change was committed.");
            rows.add(item.deepCopy());
        } else rows.set(index, item.deepCopy());
        ArrayNode issues = json.createArrayNode();
        Set<String> affected = new HashSet<>();
        String reference = type.equals("Subject") ? "subjectId" : "teacherId";
        for (JsonNode lesson : definition.path("lessons")) if (id.equals(lesson.path(reference).asString())) affected.add(lesson.path("id").asString());
        for (JsonNode issue : draft.path("issues")) {
            boolean owned = type.equals(issue.path("ownerType").asString()) && id.equals(issue.path("ownerId").asString());
            boolean dependent = Set.of("SUBJECT_POLICY", "TEACHER_POLICY").contains(issue.path("diagnosticSource").asString())
                    && affected.contains(issue.path("ownerId").asString());
            if (!owned && !dependent) issues.add(issue);
        }
        if (type.equals("Subject")) validateSubject(definition, item, issues);
        else validateTeacher(definition, item, issues);
        draft.set("issues", ordered(issues));
        draft.put("draftRevision", DefinitionDraftService.revision(definition));
        if (repository.replace(version, WorkspaceState.DEFINITION_DRAFT, WorkspaceState.DEFINITION_DRAFT, document).isEmpty())
            throw problem(HttpStatus.PRECONDITION_FAILED, "STALE_WORKSPACE_VERSION", "The workspace changed. Review the newer saved draft before retrying. No change was committed.");
        return repository.load();
    }

    private void validateTeacher(JsonNode definition, JsonNode teacher, ArrayNode issues) {
        String id = teacher.path("id").asString();
        if (!teacher.path("displayName").isTextual() || teacher.path("displayName").stringValue().isBlank())
            issue(issues, "Teacher", id, "displayName", "Enter a nonblank teacher name.");
        validateReferences(teacher, "qualifiedSubjectIds", true, definition.path("subjects"), "Teacher", issues);
        validateReferences(teacher, "availablePeriodIds", false, definition.path("periods"), "Teacher", issues);
        validateReferences(teacher, "undesirablePeriodIds", false, definition.path("periods"), "Teacher", issues);
        for (JsonNode lesson : definition.path("lessons")) if (id.equals(lesson.path("teacherId").asString())) {
            JsonNode subject = find(definition.path("subjects"), lesson.path("subjectId").asString());
            JsonNode cohort = find(definition.path("cohorts"), lesson.path("cohortId").asString());
            String lessonId = lesson.path("id").asString();
            boolean curator = subject != null && subject.path("curatorLesson").isBoolean() && subject.path("curatorLesson").booleanValue();
            if (curator) {
                if (cohort == null || !id.equals(cohort.path("curatorTeacherId").asString()))
                    teacherLessonIssue(issues, lessonId, "teacherId", "Curator lessons require the class's configured curator; no teaching assignment is changed automatically.", id);
                if (cohort != null && cohort.has("homeRoomId") && lesson.has("roomLock") && !cohort.path("homeRoomId").equals(lesson.path("roomLock")))
                    teacherLessonIssue(issues, lessonId, "roomLock", "Persistent room lock contradicts the curator class home room.", id);
            } else if (!contains(teacher.path("qualifiedSubjectIds"), lesson.path("subjectId").asString()))
                teacherLessonIssue(issues, lessonId, "teacherId", "The assigned teacher is not qualified for this ordinary subject; reassignment is an explicit occurrence edit.", id);
            if (lesson.has("periodLock") && teacher.has("availablePeriodIds") && !contains(teacher.path("availablePeriodIds"), lesson.path("periodLock").asString()))
                teacherLessonIssue(issues, lessonId, "periodLock", "The persistent period lock is outside this teacher's saved availability.", id);
            if (lesson.has("periodLock") && contains(definition.path("reservedPeriodIds"), lesson.path("periodLock").asString()) && subject != null && !subject.path("reservedPeriodsAllowed").booleanValue())
                teacherLessonIssue(issues, lessonId, "periodLock", "The subject does not permit this reserved-period lock.", id);
        }
    }

    private void validateReferences(JsonNode item, String field, boolean required, JsonNode targets,
            String type, ArrayNode issues) {
        String id = item.path("id").asString();
        if (!item.has(field) && !required) return;
        JsonNode values = item.path(field);
        if (!values.isArray()) {
            issue(issues, type, id, field, "Choose an explicit unique identifier selection" + (required ? "." : ", or use the omitted default."));
            return;
        }
        Set<String> seen = new HashSet<>();
        for (JsonNode value : values) {
            String target = value.asString();
            if (!value.isTextual() || !validId(target)) issue(issues, type, id, field, "Invalid reference: " + value + ".");
            else if (!seen.add(target)) issue(issues, type, id, field, "Duplicate reference: " + target + ".");
            else if (find(targets, target) == null) {
                ObjectNode issue = issue(issues, type, id, field, "Unresolved reference: " + target + ".");
                issue.withArray("relatedIds").add(target);
            }
        }
    }

    private void teacherLessonIssue(ArrayNode issues, String id, String field, String message, String teacher) {
        ObjectNode issue = issue(issues, "Lesson", id, field, message);
        issue.put("diagnosticSource", "TEACHER_POLICY");
        issue.withArray("relatedIds").add(teacher);
    }

    private static boolean validId(String id) { return id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"); }

    private void validateSubject(JsonNode definition, JsonNode subject, ArrayNode issues) {
        String id=subject.path("id").asString();
        if(!subject.path("displayName").isTextual() || subject.path("displayName").stringValue().isBlank()) issue(issues,"Subject",id,"displayName","Enter a nonblank subject name.");
        for(String field:List.of("reservedPeriodsAllowed","dayEdgeOnly","curatorLesson")) if(subject.has(field) && !subject.path(field).isBoolean()) issue(issues,"Subject",id,field,"Choose default, true or false for this setting.");
        for(String field:List.of("maxWeeklyReservedLessonsPerCohort","maxDailyLessonsPerCohort")) if(subject.has(field)) {
            JsonNode value=subject.path(field);
            if(!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue()<1) issue(issues,"Subject",id,field,"Use an integer from 1 to 2147483647, or omit for no limit.");
        }
        if(subject.has("maxWeeklyReservedLessonsPerCohort") && !(subject.path("reservedPeriodsAllowed").isBoolean() && subject.path("reservedPeriodsAllowed").booleanValue())) issue(issues,"Subject",id,"maxWeeklyReservedLessonsPerCohort","A reserved-weekly cap requires explicit permission to use reserved periods.");
        for(JsonNode lesson:definition.path("lessons")) if(id.equals(lesson.path("subjectId").asString())) {
            String lessonId=lesson.path("id").asString();JsonNode teacher=find(definition.path("teachers"),lesson.path("teacherId").asString());
            JsonNode cohort=find(definition.path("cohorts"),lesson.path("cohortId").asString());
            boolean curator=subject.path("curatorLesson").isBoolean() && subject.path("curatorLesson").booleanValue();
            if(curator) {
                if(cohort==null || !cohort.has("curatorTeacherId") || !cohort.path("curatorTeacherId").equals(lesson.path("teacherId"))) lessonIssue(issues,lessonId,"teacherId","Curator lessons require this class's configured curator; no teacher or qualification is changed automatically.",id);
                if(cohort!=null && cohort.has("homeRoomId") && lesson.has("roomLock") && !cohort.path("homeRoomId").equals(lesson.path("roomLock"))) lessonIssue(issues,lessonId,"roomLock","Persistent room lock contradicts the curator class home room.",id);
            } else if(teacher==null || !contains(teacher.path("qualifiedSubjectIds"),id)) lessonIssue(issues,lessonId,"teacherId","The assigned teacher is not qualified for this ordinary subject.",id);
            if (lesson.has("periodLock") && teacher != null && teacher.has("availablePeriodIds") && !contains(teacher.path("availablePeriodIds"), lesson.path("periodLock").asString()))
                lessonIssue(issues, lessonId, "periodLock", "The persistent period lock is outside this teacher's saved availability.", id);
            if(lesson.has("periodLock") && contains(definition.path("reservedPeriodIds"),lesson.path("periodLock").asString()) && !subject.path("reservedPeriodsAllowed").booleanValue()) lessonIssue(issues,lessonId,"periodLock","The subject does not permit this reserved-period lock.",id);
        }
    }
    private static JsonNode find(JsonNode rows,String id) { for(JsonNode row:rows) if(id.equals(row.path("id").asString())) return row;return null; }
    private static boolean contains(JsonNode rows,String id) { for(JsonNode value:rows) if(id.equals(value.asString())) return true;return false; }
    private void lessonIssue(ArrayNode issues,String id,String field,String message,String subject) {
        ObjectNode issue=issue(issues,"Lesson",id,field,message);issue.put("diagnosticSource","SUBJECT_POLICY");issue.withArray("relatedIds").add(subject);
    }
    private ObjectNode issue(ArrayNode issues,String type,String id,String field,String message) {
        ObjectNode issue=issues.addObject().put("severity","BLOCKING").put("ownerType",type).put("ownerId",id).put("field",field).put("message",message);issue.putArray("relatedIds");return issue;
    }
    private ArrayNode ordered(ArrayNode issues) {
        List<JsonNode> sorted=new ArrayList<>();issues.forEach(sorted::add);
        sorted.sort(Comparator.comparing((JsonNode n)->"BLOCKING".equals(n.path("severity").asString())?0:1).thenComparing(n->n.path("ownerType").asString()).thenComparing(n->n.path("ownerId").asString()).thenComparing(n->n.path("field").asString()).thenComparing(n->n.path("message").asString()));
        ArrayNode result=json.createArrayNode();sorted.forEach(result::add);return result;
    }
    private static WorkspaceProblem problem(HttpStatus status,String code,String message) { return new WorkspaceProblem(status,code,message); }
}
