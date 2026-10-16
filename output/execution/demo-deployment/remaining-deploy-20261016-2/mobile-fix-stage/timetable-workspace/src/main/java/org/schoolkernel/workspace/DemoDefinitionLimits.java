package org.schoolkernel.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@Profile("demo")
class DemoDefinitionLimits {
    static final int DOCUMENT_BYTES = 2 * 1024 * 1024;
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private final long workMax;
    private final int lessonMax;
    private final com.networknt.schema.Schema schema;

    DemoDefinitionLimits(@Value("${demo.complexity.work-max:11000000}") long workMax,
            @Value("${demo.complexity.lesson-max:2500}") int lessonMax) {
        if (workMax < 1 || lessonMax < 1) throw new IllegalArgumentException("Positive demo complexity limits required");
        this.workMax = workMax;
        this.lessonMax = lessonMax;
        try (var input = getClass().getResourceAsStream("/schema/school-definition-v1.schema.json")) {
            schema = com.networknt.schema.SchemaRegistry.withDefaultDialect(com.networknt.schema.SpecificationVersion.DRAFT_2020_12).getSchema(input);
            schema.initializeValidators();
        } catch (IOException exception) { throw new IllegalStateException("Definition schema missing", exception); }
    }

    JsonNode read(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(DOCUMENT_BYTES + 1);
        if (bytes.length > DOCUMENT_BYTES) throw new WorkspaceProblem(HttpStatus.PAYLOAD_TOO_LARGE,
                "DEFINITION_TOO_LARGE", "The definition must be at most 2 MiB UTF-8. It was not used.");
        JsonNode definition;
        try {
            String text = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            definition = JSON.readTree(text);
        }
        catch (JacksonException | java.nio.charset.CharacterCodingException exception) {
            throw invalid("/", "Expected one valid JSON object without duplicate fields or trailing content.");
        }
        check(definition);
        return definition;
    }

    void check(JsonNode definition) { check(definition, true); }

    void check(JsonNode definition, boolean initial) {
        if (definition == null || !definition.isObject()) throw invalid("/", "Expected a definition object.");
        for (String field : List.of("subjects", "teachers", "cohorts", "rooms", "periods", "lessons")) {
            if (!definition.path(field).isArray()) throw invalid("/" + field, "Expected an array.");
            int index = 0;
            for (JsonNode item : definition.path(field)) {
                if (!item.isObject()) throw invalid("/" + field + "/" + index, "Expected an object.");
                index++;
            }
        }
        // Divide before multiplying; neither overflow nor malformed arrays can bypass either gate.
        long lessons = definition.path("lessons").size();
        long periods = definition.path("periods").size(), rooms = definition.path("rooms").size();
        if (lessons > lessonMax || lessons != 0 && periods > workMax / lessons
                || lessons != 0 && periods != 0 && rooms > workMax / (lessons * periods)) {
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "DEFINITION_COMPLEXITY_LIMIT",
                    "/lessons, /periods, /rooms: This definition exceeds the demo complexity limit (at most "
                    + lessonMax + " lessons and placement work " + workMax + "). It was not used.");
        }
        if (CanonicalJson.bytes(definition).length > DOCUMENT_BYTES) throw new WorkspaceProblem(HttpStatus.PAYLOAD_TOO_LARGE, "DEFINITION_TOO_LARGE", "The definition must be at most 2 MiB UTF-8.");
        if (initial && definition.has("basedOnRevision")) throw invalid("/basedOnRevision",
                "An initial definition must omit repair ancestry. Use a complete accepted bundle to retain a repaired timetable.");
        var errors = schema.validate(definition);
        if (!errors.isEmpty()) {
            var error = errors.stream().sorted(java.util.Comparator.comparing(e -> e.getInstanceLocation().toString())).findFirst().orElseThrow();
            throw invalid(error.getInstanceLocation().toString(), error.getMessage());
        }
    }

    private static WorkspaceProblem invalid(String location, String message) {
        return new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_INITIAL_DEFINITION",
                location + ": " + message + " The definition was not used.");
    }
}
