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
                    ? DefinitionLoader.Mode.INITIAL
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
                return new CommandOutcome.Succeeded(results.verifiedInitial(
                        correlationId, elapsed(started), accepted.dto().schoolId(),
                        accepted.dto().catalogVersion(), definitionRevision));
            }

            JsonNode resultNode = parse(files.read(request.resultPath(), maximumInputBytes()), "result");
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
