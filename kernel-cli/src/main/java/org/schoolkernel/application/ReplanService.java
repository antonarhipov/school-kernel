package org.schoolkernel.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.schoolkernel.contract.CurrentTimetableReader;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.ResultFactory;
import org.schoolkernel.domain.BaselineVerifier;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.domain.ValidationError;
import org.schoolkernel.domain.ValidationReport;
import org.schoolkernel.solver.PlanningMapper;
import org.schoolkernel.solver.PreflightFeasibilityCheck;
import org.schoolkernel.solver.ReplanningSolver;
import org.schoolkernel.solver.ScheduleEvaluator;
import org.schoolkernel.solver.SchoolSchedule;
import org.schoolkernel.solver.SolverAdapter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public final class ReplanService {
    private final PlanFiles files;
    private final DefinitionLoader definitions;
    private final CurrentTimetableReader currentReader;
    private final BaselineVerifier baselineVerifier;
    private final PreflightFeasibilityCheck preflight;
    private final ReplanningSolver solver;
    private final ScheduleEvaluator evaluator;
    private final ResultFactory results;

    public ReplanService(
            DefinitionLoader definitions,
            PlanFiles files,
            CurrentTimetableReader currentReader,
            BaselineVerifier baselineVerifier,
            PreflightFeasibilityCheck preflight,
            ReplanningSolver solver,
            ScheduleEvaluator evaluator,
            ResultFactory results) {
        this.files = files;
        this.definitions = definitions;
        this.currentReader = currentReader;
        this.baselineVerifier = baselineVerifier;
        this.preflight = preflight;
        this.solver = solver;
        this.evaluator = evaluator;
        this.results = results;
    }

    public CommandOutcome handle(ReplanRequest request) {
        long started = System.nanoTime();
        String correlationId = request.correlationId() == null ? UUID.randomUUID().toString() : request.correlationId();
        String schoolId = null;
        Integer catalogVersion = null;
        String inputRevision = null;
        Map<String, Long> weights = null;
        try {
            byte[] currentBytes = files.read(request.currentPath(), maximumInputBytes());
            if (Thread.currentThread().isInterrupted()) {
                return new CommandOutcome.Interrupted();
            }

            JsonNode currentNode;
            try {
                currentNode = JsonSupport.mapper().readTree(currentBytes);
            } catch (StreamConstraintsException exception) {
                return new CommandOutcome.TransportFailure("Input exceeds a JSON token or nesting safeguard");
            } catch (JacksonException exception) {
                return invalid(
                        request, correlationId, started,
                        ValidationReport.from(List.of(new ValidationError("/", List.of(), "Malformed JSON input"))),
                        null, null, null, null);
            }

            DefinitionLoader.Outcome currentDefinitionLoaded = definitions.load(
                    request.currentDefinitionPath(), DefinitionLoader.Mode.BASELINE);
            DefinitionLoader.Outcome updatedDefinitionLoaded = definitions.load(
                    request.definitionPath(), DefinitionLoader.Mode.REPLAN);
            if (currentDefinitionLoaded instanceof DefinitionLoader.LimitExceeded
                    || updatedDefinitionLoaded instanceof DefinitionLoader.LimitExceeded) {
                return new CommandOutcome.TransportFailure("Input exceeds a JSON token or nesting safeguard");
            }
            if (currentDefinitionLoaded instanceof DefinitionLoader.Rejected rejected) {
                return invalid(request, correlationId, started, rejected.report(),
                        rejected.schoolId(), rejected.catalogVersion(), rejected.revision(), null);
            }
            if (updatedDefinitionLoaded instanceof DefinitionLoader.Rejected rejected) {
                return invalid(request, correlationId, started, rejected.report(),
                        rejected.schoolId(), rejected.catalogVersion(), rejected.revision(), null);
            }
            DefinitionLoader.Accepted acceptedCurrentDefinition =
                    (DefinitionLoader.Accepted) currentDefinitionLoaded;
            DefinitionLoader.Accepted acceptedUpdatedDefinition =
                    (DefinitionLoader.Accepted) updatedDefinitionLoaded;

            var errors = new ArrayList<ValidationError>();
            var currentOutcome = currentReader.read(currentNode);
            errors.addAll(currentOutcome.report().errors());
            if (currentOutcome.timetable() != null) {
                if (!acceptedCurrentDefinition.dto().schoolId().equals(currentOutcome.timetable().schoolId())) {
                    errors.add(new ValidationError(
                            "/schoolId",
                            List.of(
                                    acceptedCurrentDefinition.dto().schoolId(),
                                    currentOutcome.timetable().schoolId()),
                            "current result and current definition must identify the same school"));
                }
                if (!acceptedCurrentDefinition.revision().equals(currentOutcome.timetable().inputRevision())) {
                    errors.add(new ValidationError(
                            "/inputRevision",
                            List.of(acceptedCurrentDefinition.revision(), currentOutcome.timetable().inputRevision()),
                            "current result inputRevision does not match the current definition"));
                } else if (errors.isEmpty()) {
                    errors.addAll(baselineVerifier.verify(
                            acceptedCurrentDefinition.definition(), currentOutcome.timetable()).errors());
                }
            }
            if (!errors.isEmpty()) {
                return invalid(
                        request, correlationId, started, ValidationReport.from(errors),
                        acceptedUpdatedDefinition.dto().schoolId(),
                        acceptedUpdatedDefinition.dto().catalogVersion(),
                        acceptedUpdatedDefinition.revision(), null);
            }

            schoolId = acceptedUpdatedDefinition.dto().schoolId();
            catalogVersion = acceptedUpdatedDefinition.dto().catalogVersion();
            inputRevision = acceptedUpdatedDefinition.revision();
            SchoolDefinition definition = acceptedUpdatedDefinition.definition();
            weights = definition.softWeights();
            var current = currentOutcome.timetable();
            if (!definition.schoolId().equals(current.schoolId())) {
                errors.add(new ValidationError(
                        "/schoolId", List.of(definition.schoolId(), current.schoolId()),
                        "updated definition and current timetable must have the same schoolId"));
            }
            if (!definition.basedOnRevision().equals(current.inputRevision())) {
                errors.add(new ValidationError(
                        "/basedOnRevision", List.of(definition.basedOnRevision(), current.inputRevision()),
                        "basedOnRevision must equal the current timetable inputRevision"));
            }
            if (!errors.isEmpty()) {
                return invalid(
                        request, correlationId, started, ValidationReport.from(errors),
                        schoolId, catalogVersion, inputRevision, weights);
            }

            var obviousFailures = preflight.findObviousFailures(definition);
            if (!obviousFailures.isEmpty()) {
                return new CommandOutcome.NoFeasibleSolution(
                        results.noFeasibleSolution(
                                correlationId, elapsed(started), definition, inputRevision,
                                request.controls(), null, obviousFailures));
            }

            Map<String, PlanningMapper.BaselineAssignment> baseline = current.assignments().stream()
                    .collect(Collectors.toMap(
                            CurrentTimetableReader.Assignment::lessonId,
                            value -> new PlanningMapper.BaselineAssignment(value.periodId(), value.roomId())));
            SchoolSchedule schedule;
            String terminationReason;
            if (definition.lessons().isEmpty()) {
                schedule = SchoolSchedule.empty();
                terminationReason = "EMPTY_PROBLEM";
            } else {
                var solveResult = solver.solve(definition, request.controls(), baseline);
                schedule = solveResult.schedule();
                terminationReason = solveResult.terminationReason();
            }
            if (Thread.currentThread().isInterrupted()) {
                return new CommandOutcome.Interrupted();
            }
            var evaluation = evaluator.evaluate(schedule, definition.softWeights());
            if (!evaluation.complete()) {
                throw new IllegalStateException("Solver returned an incomplete candidate");
            }
            if (!evaluation.feasible()) {
                var diagnostics = org.schoolkernel.solver.HardConstraintDiagnostics.from(schedule, evaluation);
                return new CommandOutcome.NoFeasibleSolution(
                        results.noFeasibleSolution(
                                correlationId, elapsed(started), definition, inputRevision,
                                request.controls(), terminationReason, diagnostics));
            }
            return new CommandOutcome.Succeeded(
                    results.replannedFeasible(
                            correlationId, elapsed(started), definition, inputRevision, request.controls(),
                            terminationReason, schedule, evaluation, current));
        } catch (TransportException exception) {
            return new CommandOutcome.TransportFailure(exception.getMessage());
        } catch (RuntimeException failure) {
            if (Thread.currentThread().isInterrupted()) {
                return new CommandOutcome.Interrupted();
            }
            return new CommandOutcome.InternalError(
                    results.internalError(
                            correlationId, elapsed(started), schoolId, catalogVersion,
                            inputRevision, weights, request.controls()),
                    failure);
        }
    }

    private CommandOutcome invalid(
            ReplanRequest request,
            String correlationId,
            long started,
            ValidationReport report,
            String schoolId,
            Integer catalogVersion,
            String inputRevision,
            Map<String, Long> weights) {
        return new CommandOutcome.InvalidInput(results.invalidInput(
                        correlationId, elapsed(started), report, schoolId, catalogVersion,
                        inputRevision, weights, request.controls()));
    }

    private static long maximumInputBytes() {
        return Long.getLong("school.kernel.maxInputBytes", FileBoundary.DEFAULT_MAX_INPUT_BYTES);
    }

    private static long elapsed(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }
}
