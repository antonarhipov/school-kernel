package org.schoolkernel.workspace;

public record ProblemResponse(
        String code,
        String message,
        String correlationId,
        String state,
        String etag) {}
