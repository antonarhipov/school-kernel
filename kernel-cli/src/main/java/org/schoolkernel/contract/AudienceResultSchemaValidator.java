package org.schoolkernel.contract;

import java.io.IOException;
import java.util.List;
import org.schoolkernel.domain.ValidationError;
import tools.jackson.databind.JsonNode;

public final class AudienceResultSchemaValidator {
    private final com.networknt.schema.Schema schema;
    public AudienceResultSchemaValidator(){
        try(var input=getClass().getResourceAsStream("/schema/result-v2.schema.json")){
            schema=com.networknt.schema.SchemaRegistry.withDefaultDialect(com.networknt.schema.SpecificationVersion.DRAFT_2020_12).getSchema(input);schema.initializeValidators();
        }catch(IOException error){throw new IllegalStateException("Audience result schema is missing",error);}
    }
    public List<ValidationError> validate(JsonNode value){return schema.validate(value).stream().map(error->new ValidationError(error.getInstanceLocation().toString(),List.of(),error.getMessage())).sorted().toList();}
}
