package org.schoolkernel.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.erdtman.jcs.JsonCanonicalizer;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class JsonSupport {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
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
