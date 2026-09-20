package org.schoolkernel.contract;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import org.schoolkernel.domain.ValidationError;

import tools.jackson.databind.JsonNode;

public final class DefinitionSchemaValidator {
    private final Schema schema;

    public DefinitionSchemaValidator() {
        try (InputStream input = DefinitionSchemaValidator.class
                .getResourceAsStream("/schema/school-definition-v1.schema.json")) {
            if (input == null) {
                throw new IllegalStateException("Bundled school definition schema is missing");
            }
            schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(input);
            schema.initializeValidators();
        } catch (IOException exception) {
            throw new IllegalStateException("Bundled school definition schema cannot be loaded", exception);
        }
    }

    public List<ValidationError> validate(JsonNode input) {
        return schema.validate(input).stream()
                .map(error -> new ValidationError(
                        error.getInstanceLocation().toString(),
                        List.of(),
                        error.getMessage()))
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
