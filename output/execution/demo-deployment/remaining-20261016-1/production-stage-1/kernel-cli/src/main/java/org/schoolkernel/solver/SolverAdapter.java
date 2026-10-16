package org.schoolkernel.solver;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.schoolkernel.domain.SchoolDefinition;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

public final class SolverAdapter implements InitialSolver, ReplanningSolver {
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
        return solve(definition, controls, Map.of());
    }

    public SolveResult solve(
            SchoolDefinition definition,
            ExecutionControls controls,
            Map<String, PlanningMapper.BaselineAssignment> baselineAssignments) {
        SchoolSchedule problem = mapper.toPlanningProblem(definition, baselineAssignments);
        SolverConfig config = baseConfig(controls);
        SchoolSchedule solution;
        String terminationReason;
        try (SolverManager<SchoolSchedule> manager = SolverManager.create(config)) {
            SolverJob<SchoolSchedule> job = manager.solve(UUID.randomUUID().toString(), problem);
            SolveCompletion completion = await(job, controls);
            solution = completion.solution();
            terminationReason = completion.terminationReason();
        }
        var evaluation = evaluator.evaluate(solution, definition.softWeights());
        List<ConstraintDiagnostic> diagnostics = evaluation.feasible()
                ? List.of()
                : HardConstraintDiagnostics.from(solution, evaluation);
        return new SolveResult(solution, evaluation, terminationReason, diagnostics);
    }

    private static SolveCompletion await(SolverJob<SchoolSchedule> job, ExecutionControls controls) {
        try {
            SchoolSchedule solution;
            boolean applicationDeadline = false;
            if (controls.timeLimit() == null) {
                solution = job.getFinalBestSolution();
            } else {
                try (var waiter = Executors.newVirtualThreadPerTaskExecutor()) {
                    Future<SchoolSchedule> future = waiter.submit(job::getFinalBestSolution);
                    try {
                        solution = future.get(
                                controls.timeLimit().plusSeconds(1).toMillis(), TimeUnit.MILLISECONDS);
                    } catch (TimeoutException exception) {
                        applicationDeadline = true;
                        job.terminateEarly();
                        solution = future.get(2, TimeUnit.SECONDS);
                    }
                }
            }
            return new SolveCompletion(solution, terminationReason(job, controls, applicationDeadline));
        } catch (InterruptedException exception) {
            job.terminateEarly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solver interrupted", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Solver failed", cause);
        } catch (TimeoutException exception) {
            job.terminateEarly();
            throw new IllegalStateException("Solver did not stop after its application deadline", exception);
        }
    }

    private static String terminationReason(
            SolverJob<SchoolSchedule> job,
            ExecutionControls controls,
            boolean applicationDeadline) {
        if (applicationDeadline) {
            return "TIME_LIMIT";
        }
        if (controls.stepLimit() != null) {
            return job.getMoveEvaluationCount() >= controls.stepLimit()
                    ? "STEP_LIMIT"
                    : "SEARCH_EXHAUSTED";
        }
        long thresholdMillis = Math.max(0, controls.timeLimit().toMillis() - 1);
        return job.getSolvingDuration().toMillis() >= thresholdMillis
                ? "TIME_LIMIT"
                : "SEARCH_EXHAUSTED";
    }

    private record SolveCompletion(SchoolSchedule solution, String terminationReason) {}

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
