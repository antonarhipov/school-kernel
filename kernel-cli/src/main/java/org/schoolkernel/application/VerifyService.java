package org.schoolkernel.application;

import java.util.List;
import java.util.UUID;

import org.schoolkernel.contract.CurrentTimetableReader;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.VerificationResultFactory;
import org.schoolkernel.domain.BaselineVerifier;
import org.schoolkernel.domain.ValidationError;
import org.schoolkernel.domain.ValidationReport;

import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public final class VerifyService {
    private final PlanFiles files;
    private final DefinitionLoader definitions;
    private final CurrentTimetableReader timetableReader;
    private final BaselineVerifier baselineVerifier;
    private final VerificationResultFactory results;

    public VerifyService(
            DefinitionLoader definitions,
            PlanFiles files,
            CurrentTimetableReader timetableReader,
            BaselineVerifier baselineVerifier,
            VerificationResultFactory results) {
        this.files = files;
        this.definitions = definitions;
        this.timetableReader = timetableReader;
        this.baselineVerifier = baselineVerifier;
        this.results = results;
    }

    public CommandOutcome handle(VerifyRequest request) {
        long started = System.nanoTime();
        String correlationId = request.correlationId() == null ? UUID.randomUUID().toString() : request.correlationId();
        try {
            DefinitionLoader.Mode mode = request.resultPath() == null
                    ? request.successor() ? DefinitionLoader.Mode.REPLAN : DefinitionLoader.Mode.INITIAL
                    : DefinitionLoader.Mode.BASELINE;
            DefinitionLoader.Outcome loaded = definitions.load(request.definitionPath(), mode);
            if (loaded instanceof DefinitionLoader.LimitExceeded) {
                return new CommandOutcome.TransportFailure("Input exceeds a JSON token or nesting safeguard");
            }
            if (loaded instanceof DefinitionLoader.Rejected rejected) {
                return new CommandOutcome.InvalidInput(
                        results.invalid(correlationId, elapsed(started), rejected.report()));
            }

            DefinitionLoader.Accepted accepted = (DefinitionLoader.Accepted) loaded;
            String definitionRevision = accepted.revision();

            if (request.resultPath() == null) {
                ObjectNode evidence = results.verifiedInitial(correlationId, elapsed(started), accepted.dto().schoolId(),
                        accepted.dto().catalogVersion(), definitionRevision);
                if (request.successor()) {
                    evidence.put("mode", "SUCCESSOR_DEFINITION");
                    evidence.put("basedOnRevision", accepted.dto().basedOnRevision());
                }
                return new CommandOutcome.Succeeded(evidence);
            }

            JsonNode resultNode = parse(files.read(request.resultPath(), maximumInputBytes()), "result");
            if(org.schoolkernel.domain.AudienceDefinition.enabled(accepted.document())){
                var pairValidator=new org.schoolkernel.contract.AudiencePairValidator();var errors=pairValidator.envelope(accepted.document(),definitionRevision,resultNode);
                if(!errors.isEmpty())return new CommandOutcome.InvalidInput(results.invalid(correlationId,elapsed(started),ValidationReport.from(errors)));
                var model=new org.schoolkernel.solver.AudienceSchedule(accepted.document(),accepted.definition());model.assign(resultNode.path("timetable").path("assignments"));
                if(!model.errors.isEmpty())return new CommandOutcome.InvalidInput(results.invalid(correlationId,elapsed(started),ValidationReport.from(model.errors)));
                try{
                    JsonNode context=resultNode.has("repairContext")?resultNode.path("repairContext"):null;var baseline=AudienceBaseline.load(context,model,definitions);if(!baseline.errors().isEmpty())return new CommandOutcome.InvalidInput(results.invalid(correlationId,elapsed(started),ValidationReport.from(baseline.errors())));var comparison=baseline.model()==null?null:new org.schoolkernel.solver.AudienceComparison().compare(baseline.model(),model);
                    var evaluation=new org.schoolkernel.solver.AudienceReview().evaluate(model,resultNode.path("coordinationMatches"));
                    if(!evaluation.valid())return new CommandOutcome.InvalidInput(results.invalid(correlationId,elapsed(started),ValidationReport.from(evaluation.conflicts().stream().map(c->new ValidationError("/timetable/assignments",c.lessonIds(),c.constraintId()+": "+c.examples())).toList())));
                    var computed=new org.schoolkernel.contract.AudienceResultFactory().feasible(model,evaluation,definitionRevision,correlationId,elapsed(started),"MANUAL_REVIEW",context,comparison);errors=pairValidator.allocations(resultNode,computed);
                    if(!errors.isEmpty())return new CommandOutcome.InvalidInput(results.invalid(correlationId,elapsed(started),ValidationReport.from(errors)));
                    var evidence=results.verifiedBaseline(correlationId,elapsed(started),accepted.dto().schoolId(),11,definitionRevision,resultNode.path("timetableRevision").asString());evidence.set("recomputedScore",computed.path("score"));evidence.set("recomputedQuality",computed.path("quality"));evidence.put("assertedQualityMismatch",!org.schoolkernel.contract.AudiencePairValidator.equivalent(computed.path("score"),resultNode.path("score"))||!org.schoolkernel.contract.AudiencePairValidator.equivalent(computed.path("quality"),resultNode.path("quality")));return new CommandOutcome.Succeeded(evidence);
                }catch(org.schoolkernel.solver.AudienceCoordination.ReviewLimit|ArithmeticException limit){return new CommandOutcome.InvalidInput(results.invalid(correlationId,elapsed(started),ValidationReport.from(List.of(new ValidationError("/timetable",List.of(),"Validity is unestablished: "+limit.getMessage())))));}
            }
            var timetableOutcome = timetableReader.read(resultNode);
            var errors = new java.util.ArrayList<ValidationError>();
            errors.addAll(timetableOutcome.report().errors());
            if (timetableOutcome.timetable() != null) {
                var timetable = timetableOutcome.timetable();
                if (!accepted.dto().schoolId().equals(timetable.schoolId())) {
                    errors.add(new ValidationError(
                            "/schoolId", List.of(accepted.dto().schoolId(), timetable.schoolId()),
                            "definition and result must identify the same school"));
                }
                if (!definitionRevision.equals(timetable.inputRevision())) {
                    errors.add(new ValidationError(
                            "/inputRevision", List.of(definitionRevision, timetable.inputRevision()),
                            "result inputRevision does not match the complete definition"));
                }
                if (errors.isEmpty()) {
                    errors.addAll(baselineVerifier.verify(accepted.definition(), timetable).errors());
                }
            }
            if (!errors.isEmpty()) {
                return new CommandOutcome.InvalidInput(
                        results.invalid(correlationId, elapsed(started), ValidationReport.from(errors)));
            }
            return new CommandOutcome.Succeeded(results.verifiedBaseline(
                    correlationId,
                    elapsed(started),
                    accepted.dto().schoolId(),
                    accepted.dto().catalogVersion(),
                    definitionRevision,
                    timetableOutcome.timetable().timetableRevision()));
        } catch (InputLimitExceeded exception) {
            return new CommandOutcome.TransportFailure("Input exceeds a JSON token or nesting safeguard");
        } catch (MalformedDocument exception) {
            ValidationReport report = ValidationReport.from(List.of(new ValidationError(
                    "/", List.of(), "Malformed JSON " + exception.documentName())));
            return new CommandOutcome.InvalidInput(results.invalid(correlationId, elapsed(started), report));
        } catch (TransportException exception) {
            return new CommandOutcome.TransportFailure(exception.getMessage());
        } catch (RuntimeException exception) {
            return new CommandOutcome.InternalError(
                    results.internal(correlationId, elapsed(started)), exception);
        }
    }

    private static JsonNode parse(byte[] bytes, String documentName)
            throws MalformedDocument, InputLimitExceeded {
        try {
            return JsonSupport.mapper().readTree(bytes);
        } catch (StreamConstraintsException exception) {
            throw new InputLimitExceeded();
        } catch (JacksonException exception) {
            throw new MalformedDocument(documentName);
        }
    }

    private static long maximumInputBytes() {
        return Long.getLong("school.kernel.maxInputBytes", FileBoundary.DEFAULT_MAX_INPUT_BYTES);
    }

    private static long elapsed(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }

    private static final class MalformedDocument extends Exception {
        private final String documentName;

        private MalformedDocument(String documentName) {
            this.documentName = documentName;
        }

        private String documentName() {
            return documentName;
        }
    }

    private static final class InputLimitExceeded extends Exception {}
}
