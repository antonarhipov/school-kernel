package org.schoolkernel.application;

import java.nio.file.Path;
import java.util.List;

import org.schoolkernel.contract.DefinitionSchemaValidator;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.contract.RevisionService;
import org.schoolkernel.contract.SchoolDefinitionDto;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.domain.KernelCatalog;
import org.schoolkernel.domain.SchoolDefinition;
import org.schoolkernel.domain.ValidationError;
import org.schoolkernel.domain.ValidationReport;

import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.JsonNode;

/** Shared strict definition parsing, schema, semantic, normalization, and revision pipeline. */
public final class DefinitionLoader {
    public enum Mode {
        INITIAL,
        REPLAN,
        BASELINE
    }

    public sealed interface Outcome permits Accepted, Rejected, LimitExceeded {}

    public record Accepted(
            JsonNode document,
            SchoolDefinitionDto dto,
            SchoolDefinition definition,
            String revision) implements Outcome {}

    public record Rejected(
            ValidationReport report,
            String schoolId,
            Integer catalogVersion,
            String revision) implements Outcome {}

    public record LimitExceeded() implements Outcome {}

    private final PlanFiles files;
    private final DefinitionSchemaValidator schema;
    private final DefinitionValidator semantics;
    private final RevisionService revisions;

    public DefinitionLoader(
            PlanFiles files,
            DefinitionSchemaValidator schema,
            DefinitionValidator semantics,
            RevisionService revisions) {
        this.files = files;
        this.schema = schema;
        this.semantics = semantics;
        this.revisions = revisions;
    }

    public Outcome load(Path path, Mode mode) throws TransportException {
        JsonNode document;
        try {
            document = JsonSupport.mapper().readTree(
                    files.read(path, Long.getLong(
                            "school.kernel.maxInputBytes", FileBoundary.DEFAULT_MAX_INPUT_BYTES)));
        } catch (StreamConstraintsException exception) {
            return new LimitExceeded();
        } catch (JacksonException exception) {
            return rejected("Malformed JSON input", null, null, null);
        }

        List<ValidationError> schemaErrors = schema.validate(document);
        if (!schemaErrors.isEmpty()) {
            Integer catalogVersion = document.path("catalogVersion").isInt()
                            && document.path("catalogVersion").intValue() == KernelCatalog.VERSION
                    ? KernelCatalog.VERSION
                    : null;
            return new Rejected(ValidationReport.from(schemaErrors), null, catalogVersion, null);
        }

        String schoolId = document.path("schoolId").stringValue();
        String revision = revisions.definitionRevision(document);
        SchoolDefinitionDto dto;
        try {
            dto = JsonSupport.mapper().treeToValue(document, SchoolDefinitionDto.class);
        } catch (JacksonException exception) {
            return rejected("Input could not be bound to schema version 1",
                    schoolId, KernelCatalog.VERSION, revision);
        }

        DefinitionValidator.Outcome validation = switch (mode) {
            case INITIAL -> semantics.validateForPlan(dto);
            case REPLAN -> semantics.validateForReplan(dto);
            case BASELINE -> dto.basedOnRevision() == null
                    ? semantics.validateForPlan(dto)
                    : semantics.validateForReplan(dto);
        };
        if (!validation.report().isValid()) {
            return new Rejected(validation.report(), schoolId, KernelCatalog.VERSION, revision);
        }
        return new Accepted(document, dto, validation.definition(), revision);
    }

    private static Rejected rejected(
            String message,
            String schoolId,
            Integer catalogVersion,
            String revision) {
        return new Rejected(
                ValidationReport.from(List.of(new ValidationError("/", List.of(), message))),
                schoolId,
                catalogVersion,
                revision);
    }
}
