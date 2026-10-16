package org.schoolkernel.solver;

import org.schoolkernel.domain.SchoolDefinition;

public interface InitialSolver {
    SolverAdapter.SolveResult solve(
            SchoolDefinition definition,
            SolverAdapter.ExecutionControls controls);
}
