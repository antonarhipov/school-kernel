package org.schoolkernel.application;

import java.util.Map;
import java.util.UUID;

import org.schoolkernel.contract.ResultFactory;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.solver.InitialSolver;
import org.schoolkernel.solver.PreflightFeasibilityCheck;
import org.schoolkernel.solver.ScheduleEvaluator;

public final class PlanService {
    private final DefinitionLoader definitions;
    private final PreflightFeasibilityCheck preflight;
    private final InitialSolver solver;
    private final ScheduleEvaluator postSolveEvaluator;
    private final ResultFactory results;

    public PlanService(
            DefinitionLoader definitions,
            PreflightFeasibilityCheck preflight,
            InitialSolver solver,
            ScheduleEvaluator postSolveEvaluator,
            ResultFactory results) {
        this.definitions = definitions;
        this.preflight = preflight;
        this.solver = solver;
        this.postSolveEvaluator = postSolveEvaluator;
        this.results = results;
    }

    public CommandOutcome handle(PlanRequest request) {
        long started = System.nanoTime();
        String correlationId = request.correlationId() == null ? UUID.randomUUID().toString() : request.correlationId();
        String derivedSchoolId = null;
        Integer acceptedCatalogVersion = null;
        String derivedInputRevision = null;
        Map<String, Long> derivedEffectiveWeights = null;
        try {
            if (Thread.currentThread().isInterrupted()) {
                return new CommandOutcome.Interrupted();
            }

            DefinitionLoader.Outcome loaded = definitions.load(
                    request.definitionPath(), DefinitionLoader.Mode.INITIAL);
            if (loaded instanceof DefinitionLoader.LimitExceeded) {
                return new CommandOutcome.TransportFailure("Input exceeds a JSON token or nesting safeguard");
            }
            if (loaded instanceof DefinitionLoader.Rejected rejected) {
                derivedSchoolId = rejected.schoolId();
                acceptedCatalogVersion = rejected.catalogVersion();
                derivedInputRevision = rejected.revision();
                return new CommandOutcome.InvalidInput(
                        results.invalidInput(
                                correlationId,
                                elapsed(started),
                                rejected.report(),
                                rejected.schoolId(),
                                rejected.catalogVersion(),
                                rejected.revision(),
                                null,
                                request.controls()));
            }

            DefinitionLoader.Accepted accepted = (DefinitionLoader.Accepted) loaded;
            derivedSchoolId = accepted.dto().schoolId();
            acceptedCatalogVersion = accepted.dto().catalogVersion();
            derivedInputRevision = accepted.revision();
            SchoolDefinition definition = accepted.definition();
            derivedEffectiveWeights = definition.softWeights();
            var obviousFailures = preflight.findObviousFailures(definition);
            if (!obviousFailures.isEmpty()) {
                return new CommandOutcome.NoFeasibleSolution(
                        results.noFeasibleSolution(
                                correlationId,
                                elapsed(started),
                                definition,
                                derivedInputRevision,
                                request.controls(),
                                null,
                                obviousFailures));
            }

            if (definition.lessons().isEmpty()) {
                return new CommandOutcome.Succeeded(
                        results.emptyFeasible(
                                correlationId,
                                elapsed(started),
                                definition,
                                derivedInputRevision,
                                request.controls()));
            }

            var solveResult = solver.solve(definition, request.controls());
            if (Thread.currentThread().isInterrupted()) {
                return new CommandOutcome.Interrupted();
            }
            var verifiedEvaluation = postSolveEvaluator.evaluate(solveResult.schedule(), definition.softWeights());
            if (!verifiedEvaluation.complete()) {
                throw new IllegalStateException("Solver returned an incomplete candidate");
            }
            if (!verifiedEvaluation.feasible()) {
                return new CommandOutcome.NoFeasibleSolution(
                        results.noFeasibleSolution(
                                correlationId,
                                elapsed(started),
                                definition,
                                derivedInputRevision,
                                request.controls(),
                                solveResult.terminationReason(),
                                solveResult.diagnostics()));
            }
            return new CommandOutcome.Succeeded(
                    results.feasible(
                            correlationId,
                            elapsed(started),
                            definition,
                            derivedInputRevision,
                            request.controls(),
                            solveResult.terminationReason(),
                            solveResult.schedule(),
                            verifiedEvaluation));
        } catch (TransportException exception) {
            return new CommandOutcome.TransportFailure(exception.getMessage());
        } catch (RuntimeException failure) {
            if (Thread.currentThread().isInterrupted()) {
                return new CommandOutcome.Interrupted();
            }
            return new CommandOutcome.InternalError(
                    results.internalError(
                            correlationId,
                            elapsed(started),
                            derivedSchoolId,
                            acceptedCatalogVersion,
                            derivedInputRevision,
                            derivedEffectiveWeights,
                            request.controls()),
                    failure);
        }
    }

    private static long elapsed(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }
}
