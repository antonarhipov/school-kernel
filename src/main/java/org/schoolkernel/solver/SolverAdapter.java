package org.schoolkernel.solver;

import java.time.Duration;
import java.util.List;

import org.schoolkernel.domain.SchoolDefinition;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolver;

public final class SolverAdapter implements InitialSolver {
    public record ExecutionControls(Duration timeLimit, Integer stepLimit, long seed) {
        public ExecutionControls {
            if ((timeLimit == null) == (stepLimit == null)) {
                throw new IllegalArgumentException("Exactly one solve limit is required");
            }
            if (timeLimit != null && (timeLimit.isZero() || timeLimit.isNegative())) {
                throw new IllegalArgumentException("Time limit must be positive");
            }
            if (stepLimit != null && stepLimit <= 0) {
                throw new IllegalArgumentException("Step limit must be positive");
            }
        }
    }

    public record SolveResult(
            SchoolSchedule schedule,
            ScheduleEvaluator.Evaluation evaluation,
            String terminationReason,
            List<ConstraintDiagnostic> diagnostics) {}

    private final PlanningMapper mapper = new PlanningMapper();
    private final ScheduleEvaluator evaluator = new ScheduleEvaluator();

    public SolveResult solve(SchoolDefinition definition, ExecutionControls controls) {
        SchoolSchedule problem = mapper.toPlanningProblem(definition);
        SolverConfig config = baseConfig(controls);
        SolverFactory<SchoolSchedule> factory = SolverFactory.create(config);
        Solver<SchoolSchedule> solver = factory.buildSolver();
        SchoolSchedule solution = solver.solve(problem);
        var evaluation = evaluator.evaluate(solution, definition.softWeights());
        List<ConstraintDiagnostic> diagnostics = evaluation.feasible()
                ? List.of()
                : HardConstraintDiagnostics.from(solution, evaluation);
        String terminationReason = terminationReason(solver, controls);
        return new SolveResult(solution, evaluation, terminationReason, diagnostics);
    }

    private static String terminationReason(Solver<SchoolSchedule> solver, ExecutionControls controls) {
        if (!(solver instanceof DefaultSolver<SchoolSchedule> defaultSolver)) {
            return controls.stepLimit() == null ? "TIME_LIMIT" : "STEP_LIMIT";
        }
        if (controls.stepLimit() != null) {
            return defaultSolver.getMoveEvaluationCount() >= controls.stepLimit()
                    ? "STEP_LIMIT"
                    : "SEARCH_EXHAUSTED";
        }
        long thresholdMillis = Math.max(0, controls.timeLimit().toMillis() - 1);
        return defaultSolver.getTimeMillisSpent() >= thresholdMillis
                ? "TIME_LIMIT"
                : "SEARCH_EXHAUSTED";
    }

    public static SolverConfig baseConfig(ExecutionControls controls) {
        var construction = new ConstructionHeuristicPhaseConfig();
        var localSearch = new LocalSearchPhaseConfig().withLocalSearchType(LocalSearchType.LATE_ACCEPTANCE);
        SolverConfig config = new SolverConfig()
                .withSolutionClass(SchoolSchedule.class)
                .withEntityClasses(PlanningLesson.class)
                .withConstraintProviderClass(SchoolConstraintProvider.class)
                .withRandomSeed(controls.seed())
                .withMoveThreadCount(SolverConfig.MOVE_THREAD_COUNT_NONE);
        if (controls.timeLimit() != null) {
            config.withTerminationConfig(new TerminationConfig().withSpentLimit(controls.timeLimit()));
        } else {
            // Timefold's phase step counter advances only after an accepted move. A flat score can therefore
            // make a nominal step limit wait forever. Move evaluations are deterministic under one thread and
            // the fixed seed, so the public deterministic step budget is enforced at that lower-level boundary.
            // Construction is allowed to finish first so the bounded search always has a complete candidate.
            localSearch.withTerminationConfig(
                    new TerminationConfig().withMoveCountLimit(controls.stepLimit().longValue()));
        }
        return config.withPhases(construction, localSearch);
    }
}
