package org.schoolkernel.application;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.schoolkernel.contract.CurrentTimetableReader;
import org.schoolkernel.contract.DefinitionSchemaValidator;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.ResultFactory;
import org.schoolkernel.contract.RevisionService;
import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.domain.ValidationError;
import org.schoolkernel.domain.ValidationReport;
import org.schoolkernel.solver.PlanningMapper;
import org.schoolkernel.solver.PreflightFeasibilityCheck;
import org.schoolkernel.solver.ScheduleEvaluator;
import org.schoolkernel.solver.SchoolSchedule;
import org.schoolkernel.solver.SolverAdapter;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public final class ReplanService {
    private final PlanFiles files = new FileBoundary();
    private final DefinitionSchemaValidator definitionSchema = new DefinitionSchemaValidator();
    private final DefinitionValidator definitionValidator = new DefinitionValidator();
    private final CurrentTimetableReader currentReader = new CurrentTimetableReader();
    private final RevisionService revisions = new RevisionService();
    private final PreflightFeasibilityCheck preflight = new PreflightFeasibilityCheck();
    private final SolverAdapter solver = new SolverAdapter();
    private final ScheduleEvaluator evaluator = new ScheduleEvaluator();
    private final ResultFactory results = new ResultFactory();

    public int replan(ReplanRequest request, PrintWriter errorWriter) {
        long started = System.nanoTime();
        String correlationId = request.correlationId() == null ? UUID.randomUUID().toString() : request.correlationId();
        String schoolId = null;
        Integer catalogVersion = null;
        String inputRevision = null;
        Map<String, Long> weights = null;
        try {
            try {
                files.requireDistinct(request.definitionPath(), request.currentPath());
                files.requireDistinct(request.definitionPath(), request.outputPath());
                files.requireDistinct(request.currentPath(), request.outputPath());
            } catch (TransportException exception) {
                errorWriter.println(exception.getMessage());
                return 64;
            }
            files.prepareDestination(request.outputPath(), request.force());
            byte[] definitionBytes = files.read(request.definitionPath(), maximumInputBytes());
            byte[] currentBytes = files.read(request.currentPath(), maximumInputBytes());
            if (Thread.currentThread().isInterrupted()) {
                return 130;
            }

            JsonNode definitionNode;
            JsonNode currentNode;
            try {
                definitionNode = JsonSupport.mapper().readTree(definitionBytes);
                currentNode = JsonSupport.mapper().readTree(currentBytes);
            } catch (JacksonException exception) {
                return publishInvalid(
                        request, errorWriter, correlationId, started,
                        ValidationReport.from(List.of(new ValidationError("/", List.of(), "Malformed JSON input"))),
                        null, null, null, null);
            }

            var errors = new ArrayList<ValidationError>();
            errors.addAll(definitionSchema.validate(definitionNode));
            var currentOutcome = currentReader.read(currentNode);
            errors.addAll(currentOutcome.report().errors());
            if (!errors.isEmpty()) {
                Integer acceptedCatalog = definitionNode.path("catalogVersion").intValue() == 1 ? 1 : null;
                return publishInvalid(
                        request, errorWriter, correlationId, started, ValidationReport.from(errors),
                        null, acceptedCatalog, null, null);
            }

            schoolId = definitionNode.path("schoolId").stringValue();
            catalogVersion = 1;
            inputRevision = revisions.definitionRevision(definitionNode);
            SchoolDefinitionDto dto = JsonSupport.mapper().treeToValue(definitionNode, SchoolDefinitionDto.class);
            var definitionOutcome = definitionValidator.validateForReplan(dto);
            if (!definitionOutcome.report().isValid()) {
                return publishInvalid(
                        request, errorWriter, correlationId, started, definitionOutcome.report(),
                        schoolId, catalogVersion, inputRevision, null);
            }
            SchoolDefinition definition = definitionOutcome.definition();
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
                return publishInvalid(
                        request, errorWriter, correlationId, started, ValidationReport.from(errors),
                        schoolId, catalogVersion, inputRevision, weights);
            }

            var obviousFailures = preflight.findObviousFailures(definition);
            if (!obviousFailures.isEmpty()) {
                return publish(
                        results.noFeasibleSolution(
                                correlationId, elapsed(started), definition, inputRevision,
                                request.controls(), null, obviousFailures),
                        request, errorWriter, 3);
            }

            Map<String, PlanningMapper.BaselineAssignment> baseline = current.assignments().stream()
                    .collect(Collectors.toMap(
                            CurrentTimetableReader.Assignment::lessonId,
                            value -> new PlanningMapper.BaselineAssignment(value.periodId(), value.roomId())));
            SchoolSchedule schedule;
            String terminationReason;
            if (definition.lessons().isEmpty()) {
                schedule = new SchoolSchedule(
                        List.of(), List.of(), List.of(), ConstraintWeightOverrides.none());
                terminationReason = "EMPTY_PROBLEM";
            } else {
                var solveResult = solver.solve(definition, request.controls(), baseline);
                schedule = solveResult.schedule();
                terminationReason = solveResult.terminationReason();
            }
            if (Thread.currentThread().isInterrupted()) {
                return 130;
            }
            var evaluation = evaluator.evaluate(schedule, definition.softWeights());
            if (!evaluation.complete()) {
                throw new IllegalStateException("Solver returned an incomplete candidate");
            }
            if (!evaluation.feasible()) {
                var diagnostics = org.schoolkernel.solver.HardConstraintDiagnostics.from(schedule, evaluation);
                return publish(
                        results.noFeasibleSolution(
                                correlationId, elapsed(started), definition, inputRevision,
                                request.controls(), terminationReason, diagnostics),
                        request, errorWriter, 3);
            }
            return publish(
                    results.replannedFeasible(
                            correlationId, elapsed(started), definition, inputRevision, request.controls(),
                            terminationReason, schedule, evaluation, current),
                    request, errorWriter, 0);
        } catch (TransportException exception) {
            errorWriter.println(exception.getMessage());
            return 74;
        } catch (Throwable failure) {
            if (Thread.currentThread().isInterrupted()) {
                return 130;
            }
            if (request.debug()) {
                failure.printStackTrace(errorWriter);
            } else {
                errorWriter.println("Internal failure; correlation ID: " + correlationId);
            }
            try {
                files.publish(results.internalError(
                                correlationId, elapsed(started), schoolId, catalogVersion,
                                inputRevision, weights, request.controls()),
                        request.outputPath(), request.force());
                return 4;
            } catch (TransportException publicationFailure) {
                errorWriter.println(publicationFailure.getMessage());
                return 74;
            }
        }
    }

    private int publishInvalid(
            ReplanRequest request,
            PrintWriter errorWriter,
            String correlationId,
            long started,
            ValidationReport report,
            String schoolId,
            Integer catalogVersion,
            String inputRevision,
            Map<String, Long> weights) throws TransportException {
        return publish(results.invalidInput(
                        correlationId, elapsed(started), report, schoolId, catalogVersion,
                        inputRevision, weights, request.controls()),
                request, errorWriter, 2);
    }

    private int publish(
            ObjectNode result,
            ReplanRequest request,
            PrintWriter errorWriter,
            int exitCode) throws TransportException {
        if (Thread.currentThread().isInterrupted()) {
            return 130;
        }
        files.publish(result, request.outputPath(), request.force());
        return exitCode;
    }

    private static long maximumInputBytes() {
        return Long.getLong("school.kernel.maxInputBytes", FileBoundary.DEFAULT_MAX_INPUT_BYTES);
    }

    private static long elapsed(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }
}
