package org.schoolkernel.workspace;

import java.io.IOException;

import org.erdtman.jcs.JsonCanonicalizer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

final class CanonicalJson {
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private CanonicalJson() {}

    static byte[] bytes(JsonNode value) {
        try {
            return new JsonCanonicalizer(JSON.writeValueAsString(value)).getEncodedUTF8();
        } catch (JacksonException | IOException exception) {
            throw new IllegalStateException("Canonical JSON serialization failed", exception);
        }
    }
}
