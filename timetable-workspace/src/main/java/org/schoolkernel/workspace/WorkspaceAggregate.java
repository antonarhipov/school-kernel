package org.schoolkernel.workspace;

import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record WorkspaceAggregate(
        WorkspaceState state,
        long version,
        UUID activeRunId,
        JsonNode document) {
    public String etag() {
        return "\"ws-" + version + "\"";
    }
}
