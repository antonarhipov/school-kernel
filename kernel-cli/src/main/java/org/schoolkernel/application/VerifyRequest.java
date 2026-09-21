package org.schoolkernel.application;

import java.nio.file.Path;

public record VerifyRequest(
        Path definitionPath,
        Path resultPath,
        Path outputPath,
        String correlationId,
        boolean force,
        boolean debug) {}
