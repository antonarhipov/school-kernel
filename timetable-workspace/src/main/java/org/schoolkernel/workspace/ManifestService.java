package org.schoolkernel.workspace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class ManifestService {
    private static final Set<String> FIELDS = Set.of(
            "manifestVersion", "definitionSchemaVersion", "resultSchemaVersion", "catalogVersion",
            "schoolId", "inputRevision", "timetableRevision", "locks");
    private static final Set<String> LOCK_FIELDS = Set.of("lessonId", "periodLockOrigin", "roomLockOrigin");
    private static final Set<String> ORIGINS = Set.of("PERSISTENT_POLICY", "ATTEMPT_SCOPED");

    public JsonNode validatedOrGenerated(ImportDocuments documents) {
        if (documents.mode() != ImportDocuments.ImportMode.ACCEPTED_BASELINE) {
            return null;
        }
        if (documents.manifest() == null) {
            return generated(documents.definition(), documents.result());
        }
        validate(documents.manifest(), documents.definition(), documents.result());
        return documents.manifest();
    }

    public JsonNode forAcceptedRepair(
            JsonNode definition, JsonNode result, JsonNode previousManifest, JsonNode repairDraft) {
        ObjectNode manifest = generated(definition, result);
        Set<String> persistentPeriod = lockIds(previousManifest, "periodLockOrigin", "PERSISTENT_POLICY");
        Set<String> persistentRoom = lockIds(previousManifest, "roomLockOrigin", "PERSISTENT_POLICY");
        Set<String> attemptPeriod = pinIds(repairDraft, "periodSources");
        Set<String> attemptRoom = pinIds(repairDraft, "roomSources");
        for (JsonNode lockNode : manifest.path("locks")) {
            ObjectNode lock = (ObjectNode) lockNode;
            String lessonId = lock.path("lessonId").stringValue();
            if (lock.has("periodLockOrigin")) {
                lock.put("periodLockOrigin", persistentPeriod.contains(lessonId)
                        ? "PERSISTENT_POLICY" : attemptPeriod.contains(lessonId) ? "ATTEMPT_SCOPED" : "PERSISTENT_POLICY");
            }
            if (lock.has("roomLockOrigin")) {
                lock.put("roomLockOrigin", persistentRoom.contains(lessonId)
                        ? "PERSISTENT_POLICY" : attemptRoom.contains(lessonId) ? "ATTEMPT_SCOPED" : "PERSISTENT_POLICY");
            }
        }
        return manifest;
    }

    private static ObjectNode generated(JsonNode definition, JsonNode result) {
        ObjectNode manifest = tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
        manifest.put("manifestVersion", 1);
        manifest.put("definitionSchemaVersion", definition.path("schemaVersion").intValue());
        manifest.put("resultSchemaVersion", result.path("schemaVersion").intValue());
        manifest.put("catalogVersion", definition.path("catalogVersion").intValue());
        manifest.put("schoolId", definition.path("schoolId").stringValue());
        manifest.put("inputRevision", result.path("inputRevision").stringValue());
        manifest.put("timetableRevision", result.path("timetableRevision").stringValue());
        ArrayNode locks = manifest.putArray("locks");
        List<JsonNode> lessons = new ArrayList<>();
        definition.path("lessons").forEach(lessons::add);
        lessons.stream()
                .filter(lesson -> lesson.has("periodLock") || lesson.has("roomLock"))
                .sorted(Comparator.comparing(lesson -> lesson.path("id").stringValue()))
                .forEach(lesson -> {
                    ObjectNode lock = locks.addObject();
                    lock.put("lessonId", lesson.path("id").stringValue());
                    if (lesson.has("periodLock")) {
                        lock.put("periodLockOrigin", "PERSISTENT_POLICY");
                    }
                    if (lesson.has("roomLock")) {
                        lock.put("roomLockOrigin", "PERSISTENT_POLICY");
                    }
                });
        return manifest;
    }

    private static void validate(JsonNode manifest, JsonNode definition, JsonNode result) {
        if (!manifest.isObject() || !fieldNames(manifest).equals(FIELDS)
                || manifest.path("manifestVersion").intValue() != 1
                || manifest.path("definitionSchemaVersion").intValue() != definition.path("schemaVersion").intValue()
                || manifest.path("resultSchemaVersion").intValue() != result.path("schemaVersion").intValue()
                || manifest.path("catalogVersion").intValue() != definition.path("catalogVersion").intValue()
                || !sameText(manifest, "schoolId", definition, "schoolId")
                || !sameText(manifest, "inputRevision", result, "inputRevision")
                || !sameText(manifest, "timetableRevision", result, "timetableRevision")
                || !manifest.path("locks").isArray()) {
            throw invalidManifest();
        }
        List<String> expected = new ArrayList<>();
        definition.path("lessons").forEach(lesson -> {
            if (lesson.has("periodLock") || lesson.has("roomLock")) {
                expected.add(lesson.path("id").stringValue());
            }
        });
        expected.sort(String::compareTo);
        List<String> actual = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode lock : manifest.path("locks")) {
            Set<String> names = fieldNames(lock);
            String lessonId = lock.path("lessonId").stringValue();
            if (!lock.isObject() || !LOCK_FIELDS.containsAll(names) || names.size() < 2
                    || lessonId == null || !seen.add(lessonId)
                    || (lock.has("periodLockOrigin") && !ORIGINS.contains(lock.path("periodLockOrigin").stringValue()))
                    || (lock.has("roomLockOrigin") && !ORIGINS.contains(lock.path("roomLockOrigin").stringValue()))) {
                throw invalidManifest();
            }
            JsonNode lesson = findLesson(definition, lessonId);
            if (lesson == null
                    || lesson.has("periodLock") != lock.has("periodLockOrigin")
                    || lesson.has("roomLock") != lock.has("roomLockOrigin")) {
                throw invalidManifest();
            }
            actual.add(lessonId);
        }
        if (!actual.equals(expected)) {
            throw invalidManifest();
        }
    }

    private static JsonNode findLesson(JsonNode definition, String id) {
        for (JsonNode lesson : definition.path("lessons")) {
            if (id.equals(lesson.path("id").stringValue())) {
                return lesson;
            }
        }
        return null;
    }

    private static Set<String> lockIds(JsonNode manifest, String field, String origin) {
        Set<String> result = new HashSet<>();
        for (JsonNode lock : manifest.path("locks")) {
            if (lock.has(field) && origin.equals(lock.path(field).stringValue())) {
                result.add(lock.path("lessonId").stringValue());
            }
        }
        return result;
    }

    private static Set<String> pinIds(JsonNode repairDraft, String sourceField) {
        Set<String> result = new HashSet<>();
        for (JsonNode pin : repairDraft.path("intent").path("pins")) {
            if (!pin.path(sourceField).isEmpty()) result.add(pin.path("lessonId").stringValue());
        }
        return result;
    }

    private static boolean sameText(JsonNode left, String leftField, JsonNode right, String rightField) {
        return left.path(leftField).isTextual()
                && left.path(leftField).stringValue().equals(right.path(rightField).stringValue());
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> result = new HashSet<>();
        node.propertyStream().forEach(entry -> result.add(entry.getKey()));
        return result;
    }

    private static WorkspaceProblem invalidManifest() {
        return new WorkspaceProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_WORKSPACE_MANIFEST",
                "The workspace manifest does not match the accepted definition and result.");
    }
}
