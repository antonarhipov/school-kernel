package org.schoolkernel.application;

import java.nio.file.Path;

import org.schoolkernel.solver.SolverAdapter.ExecutionControls;

public record PlanRequest(
        Path definitionPath,
        Path outputPath,
        ExecutionControls controls,
        String correlationId,
        boolean force,
        boolean debug) {}
