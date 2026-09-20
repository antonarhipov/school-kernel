package org.schoolkernel.application;

import java.nio.file.Path;

import org.schoolkernel.solver.SolverAdapter.ExecutionControls;

public record ReplanRequest(
        Path definitionPath,
        Path currentPath,
        Path outputPath,
        ExecutionControls controls,
        String correlationId,
        boolean force,
        boolean debug) {}
