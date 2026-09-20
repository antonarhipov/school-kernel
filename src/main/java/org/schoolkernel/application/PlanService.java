package org.schoolkernel.application;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.schoolkernel.contract.DefinitionSchemaValidator;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.ResultFactory;
import org.schoolkernel.contract.RevisionService;
import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.domain.ValidationError;
import org.schoolkernel.domain.ValidationReport;
import org.schoolkernel.solver.InitialSolver;
import org.schoolkernel.solver.PreflightFeasibilityCheck;
import org.schoolkernel.solver.ScheduleEvaluator;
import org.schoolkernel.solver.SolverAdapter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public final class PlanService {
    private final PlanFiles files;
    private final DefinitionSchemaValidator schemaValidator;
    private final DefinitionValidator definitionValidator;
    private final RevisionService revisions;
    private final PreflightFeasibilityCheck preflight;
    private final InitialSolver solver;
    private final ScheduleEvaluator postSolveEvaluator;
    private final ResultFactory results;

    public PlanService() {
        this(new FileBoundary(), new DefinitionSchemaValidator(), new DefinitionValidator(), new RevisionService(),
                new PreflightFeasibilityCheck(), new SolverAdapter(), new ScheduleEvaluator(), new ResultFactory());
    }

    PlanService(
            PlanFiles files,
            DefinitionSchemaValidator schemaValidator,
            DefinitionValidator definitionValidator,
            RevisionService revisions,
            PreflightFeasibilityCheck preflight,
            InitialSolver solver,
            ScheduleEvaluator postSolveEvaluator,
            ResultFactory results) {
        this.files = files;
        this.schemaValidator = schemaValidator;
        this.definitionValidator = definitionValidator;
        this.revisions = revisions;
        this.preflight = preflight;
        this.solver = solver;
        this.postSolveEvaluator = postSolveEvaluator;
        this.results = results;
    }

    public int plan(PlanRequest request, PrintWriter errorWriter) {
        long started = System.nanoTime();
        String correlationId = request.correlationId() == null ? UUID.randomUUID().toString() : request.correlationId();
        String derivedSchoolId = null;
        Integer acceptedCatalogVersion = null;
        String derivedInputRevision = null;
        Map<String, Long> derivedEffectiveWeights = null;
        try {
            try {
                files.requireDistinct(request.definitionPath(), request.outputPath());
            } catch (TransportException exception) {
                errorWriter.println(exception.getMessage());
                return 64;
            }
            files.prepareDestination(request.outputPath(), request.force());
            byte[] inputBytes = files.read(request.definitionPath(), maximumInputBytes());
            if (Thread.currentThread().isInterrupted()) {
                return 130;
            }

            JsonNode input;
            try {
                input = JsonSupport.mapper().readTree(inputBytes);
            } catch (JacksonException exception) {
                var report = ValidationReport.from(List.of(
                        new ValidationError("/", List.of(), "Malformed JSON input")));
                return publishHandled(
                        results.invalidInput(
                                correlationId,
                                elapsed(started),
                                report,
                                null,
                                null,
                                null,
                                null,
                                request.controls()),
                        request,
                        errorWriter,
                        2);
            }

            var schemaErrors = schemaValidator.validate(input);
            if (!schemaErrors.isEmpty()) {
                var report = ValidationReport.from(schemaErrors);
                Integer catalogVersion = input.path("catalogVersion").isInt()
                                && input.path("catalogVersion").intValue() == 1
                        ? 1
                        : null;
                return publishHandled(
                        results.invalidInput(
                                correlationId,
                                elapsed(started),
                                report,
                                null,
                                catalogVersion,
                                null,
                                null,
                                request.controls()),
                        request,
                        errorWriter,
                        2);
            }

            derivedSchoolId = input.path("schoolId").stringValue();
            acceptedCatalogVersion = 1;
            derivedInputRevision = revisions.definitionRevision(input);

            SchoolDefinitionDto inputDto;
            try {
                inputDto = JsonSupport.mapper().treeToValue(input, SchoolDefinitionDto.class);
            } catch (JacksonException exception) {
                var report = ValidationReport.from(List.of(
                        new ValidationError("/", List.of(), "Input could not be bound to schema version 1")));
                return publishHandled(
                        results.invalidInput(
                                correlationId,
                                elapsed(started),
                                report,
                                derivedSchoolId,
                                acceptedCatalogVersion,
                                derivedInputRevision,
                                null,
                                request.controls()),
                        request,
                        errorWriter,
                        2);
            }

            DefinitionValidator.Outcome validation = definitionValidator.validateForPlan(inputDto);
            if (!validation.report().isValid()) {
                return publishHandled(
                        results.invalidInput(
                                correlationId,
                                elapsed(started),
                                validation.report(),
                                derivedSchoolId,
                                acceptedCatalogVersion,
                                derivedInputRevision,
                                null,
                                request.controls()),
                        request,
                        errorWriter,
                        2);
            }

            SchoolDefinition definition = validation.definition();
            derivedEffectiveWeights = definition.softWeights();
            var obviousFailures = preflight.findObviousFailures(definition);
            if (!obviousFailures.isEmpty()) {
                return publishHandled(
                        results.noFeasibleSolution(
                                correlationId,
                                elapsed(started),
                                definition,
                                derivedInputRevision,
                                request.controls(),
                                null,
                                obviousFailures),
                        request,
                        errorWriter,
                        3);
            }

            if (definition.lessons().isEmpty()) {
                return publishHandled(
                        results.emptyFeasible(
                                correlationId,
                                elapsed(started),
                                definition,
                                derivedInputRevision,
                                request.controls()),
                        request,
                        errorWriter,
                        0);
            }

            var solveResult = solver.solve(definition, request.controls());
            if (Thread.currentThread().isInterrupted()) {
                return 130;
            }
            var verifiedEvaluation = postSolveEvaluator.evaluate(solveResult.schedule(), definition.softWeights());
            if (!verifiedEvaluation.complete()) {
                throw new IllegalStateException("Solver returned an incomplete candidate");
            }
            if (!verifiedEvaluation.feasible()) {
                return publishHandled(
                        results.noFeasibleSolution(
                                correlationId,
                                elapsed(started),
                                definition,
                                derivedInputRevision,
                                request.controls(),
                                solveResult.terminationReason(),
                                solveResult.diagnostics()),
                        request,
                        errorWriter,
                        3);
            }
            return publishHandled(
                    results.feasible(
                            correlationId,
                            elapsed(started),
                            definition,
                            derivedInputRevision,
                            request.controls(),
                            solveResult.terminationReason(),
                            solveResult.schedule(),
                            verifiedEvaluation),
                    request,
                    errorWriter,
                    0);
        } catch (TransportException exception) {
            errorWriter.println(exception.getMessage());
            return 74;
        } catch (Throwable failure) {
            if (failure instanceof ThreadDeath) {
                throw failure;
            }
            if (Thread.currentThread().isInterrupted()) {
                return 130;
            }
            if (request.debug()) {
                failure.printStackTrace(errorWriter);
            } else {
                errorWriter.println("Internal failure; correlation ID: " + correlationId);
            }
            try {
                files.publish(
                        results.internalError(
                                correlationId,
                                elapsed(started),
                                derivedSchoolId,
                                acceptedCatalogVersion,
                                derivedInputRevision,
                                derivedEffectiveWeights,
                                request.controls()),
                        request.outputPath(),
                        request.force());
                return 4;
            } catch (TransportException publicationFailure) {
                errorWriter.println(publicationFailure.getMessage());
                return 74;
            }
        }
    }

    private int publishHandled(
            ObjectNode result,
            PlanRequest request,
            PrintWriter errorWriter,
            int successExitCode) throws TransportException {
        if (Thread.currentThread().isInterrupted()) {
            return 130;
        }
        files.publish(result, request.outputPath(), request.force());
        return successExitCode;
    }

    private static long maximumInputBytes() {
        return Long.getLong("school.kernel.maxInputBytes", FileBoundary.DEFAULT_MAX_INPUT_BYTES);
    }

    private static long elapsed(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }
}
