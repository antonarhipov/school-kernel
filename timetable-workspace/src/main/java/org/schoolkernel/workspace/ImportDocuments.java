package org.schoolkernel.workspace;

import tools.jackson.databind.JsonNode;

public record ImportDocuments(JsonNode definition, JsonNode result, JsonNode manifest, ImportMode mode) {
    public enum ImportMode {
        INITIAL_DEFINITION,
        ACCEPTED_BASELINE
    }
}
