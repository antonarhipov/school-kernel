package org.schoolkernel.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.erdtman.jcs.JsonCanonicalizer;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class JsonSupport {
    public static final int MAXIMUM_NESTING_DEPTH = 64;
    public static final int MAXIMUM_STRING_LENGTH = 1024 * 1024;
    public static final int MAXIMUM_NUMBER_LENGTH = 100;

    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(MAXIMUM_NESTING_DEPTH)
                    .maxStringLength(MAXIMUM_STRING_LENGTH)
                    .maxNumberLength(MAXIMUM_NUMBER_LENGTH)
                    .build())
            .build();

    private static final ObjectMapper MAPPER = JsonMapper.builder(JSON_FACTORY)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    private JsonSupport() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static byte[] canonicalBytes(JsonNode node) throws IOException {
        return new JsonCanonicalizer(MAPPER.writeValueAsString(node)).getEncodedUTF8();
    }

    public static String canonicalString(JsonNode node) throws IOException {
        return new String(canonicalBytes(node), StandardCharsets.UTF_8);
    }
}
