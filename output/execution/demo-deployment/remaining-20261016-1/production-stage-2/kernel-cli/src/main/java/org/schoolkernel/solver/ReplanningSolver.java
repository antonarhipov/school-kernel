package org.schoolkernel.solver;

import java.util.Map;

import org.schoolkernel.domain.SchoolDefinition;

public interface ReplanningSolver {
    SolverAdapter.SolveResult solve(
            SchoolDefinition definition,
            SolverAdapter.ExecutionControls controls,
            Map<String, PlanningMapper.BaselineAssignment> baselineAssignments);
}
