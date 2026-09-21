package org.schoolkernel.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class RevisionService {
    private static final List<String> ENTITY_ARRAYS = List.of(
            "subjects", "teachers", "cohorts", "rooms", "periods", "lessons");
    private static final List<String> SET_ARRAYS = List.of(
            "qualifiedSubjectIds", "availablePeriodIds", "undesirablePeriodIds", "capabilityIds",
            "requiredRoomCapabilityIds", "preferredRoomIds");

    public String definitionRevision(JsonNode definition) {
        ObjectNode normalized = (ObjectNode) definition.deepCopy();
        ENTITY_ARRAYS.forEach(name -> sortObjectArray(normalized, name, "id"));
        sortObjectArray(normalized, "softConstraintOverrides", "constraintId");
        normalizeNestedSets(normalized);
        return hash(normalized);
    }

    public String timetableRevision(int schemaVersion, String schoolId, String inputRevision, ArrayNode assignments) {
        ObjectNode scope = JsonSupport.mapper().createObjectNode();
        scope.put("schemaVersion", schemaVersion);
        scope.put("schoolId", schoolId);
        scope.put("inputRevision", inputRevision);
        var orderedAssignments = new ArrayList<JsonNode>();
        assignments.forEach(orderedAssignments::add);
        orderedAssignments.sort(Comparator.comparing(node -> node.path("lessonId").stringValue()));
        ArrayNode normalizedAssignments = JsonSupport.mapper().createArrayNode();
        orderedAssignments.forEach(normalizedAssignments::add);
        scope.set("assignments", normalizedAssignments);
        return hash(scope);
    }

    private static void normalizeNestedSets(ObjectNode root) {
        ENTITY_ARRAYS.forEach(name -> {
            JsonNode array = root.get(name);
            if (array instanceof ArrayNode values) {
                values.forEach(value -> {
                    if (value instanceof ObjectNode object) {
                        SET_ARRAYS.forEach(field -> sortStringArray(object, field));
                    }
                });
            }
        });
    }

    private static void sortObjectArray(ObjectNode object, String field, String key) {
        JsonNode existing = object.get(field);
        if (!(existing instanceof ArrayNode array)) {
            return;
        }
        var values = new ArrayList<JsonNode>();
        array.forEach(values::add);
        values.sort(Comparator.comparing(node -> node.path(key).stringValue()));
        ArrayNode sorted = JsonSupport.mapper().createArrayNode();
        values.forEach(sorted::add);
        object.set(field, sorted);
    }

    private static void sortStringArray(ObjectNode object, String field) {
        JsonNode existing = object.get(field);
        if (!(existing instanceof ArrayNode array)) {
            return;
        }
        var values = new ArrayList<String>();
        array.forEach(value -> values.add(value.stringValue()));
        values.sort(String::compareTo);
        ArrayNode sorted = JsonSupport.mapper().createArrayNode();
        values.forEach(sorted::add);
        object.set(field, sorted);
    }

    private static String hash(JsonNode node) {
        try {
            byte[] canonical = JsonSupport.canonicalBytes(node);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Canonical revision calculation failed", exception);
        }
    }

    public static String sha256OfCanonicalText(String canonicalText) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalText.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
