package org.schoolkernel.workspace;

import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Preview-bound, behavior-preserving catalog transition; source and Current are never upgraded here. */
@Service
public class DefinitionCatalogService {
    private final WorkspaceRepository repository;
    private final ObjectMapper json;
    private final DefinitionEntityService entities;
    public DefinitionCatalogService(WorkspaceRepository repository,ObjectMapper json,DefinitionEntityService entities){this.repository=repository;this.json=json;this.entities=entities;}
    public ObjectNode preview(String ifMatch,JsonNode request){
        WorkspaceAggregate current=checked(ifMatch);validate(request);ObjectNode definition=((ObjectNode)current.document().path("definitionDraft").path("definition")).deepCopy();int source=definition.path("catalogVersion").intValue();
        ObjectNode result=json.createObjectNode().put("workspaceVersion",current.version()).put("sourceCatalog",source).put("targetCatalog",9).put("available",source<9).put("token",token(current,request));
        ArrayNode adjustments=result.putArray("adjustments"),enabled=result.putArray("newlyEnabledSettings"),defaults=result.putArray("changedOmittedDefaults");
        if(source<9){DefinitionDraftService.upgrade(definition,9,adjustments);for(var field:List.of(new Setting(2,"soft.cohort-gap and soft.cohort-week-balance"),new Setting(3,"soft.cohort-late-start"),new Setting(4,"Class weekly-balance daily-spread target"),new Setting(5,"Class hard maximum daily gaps"),new Setting(6,"Subject reservation permission/count, day edges/daily count; class hard/preferred start slots"),new Setting(7,"Class hard daily-load spread"),new Setting(8,"Curator subjects, class curator teacher and home room"),new Setting(9,"Hard room-assignment policies")))if(source<field.since())enabled.addObject().put("sinceCatalog",field.since()).put("setting",field.description());
            if(source<2)defaults.add("New cohort-gap and cohort-week-balance preferences default to weight 1; absent choices are compensated with explicit zero.");if(source<3)defaults.add("New cohort-late-start preference defaults to weight 1; absent choice is compensated with explicit zero.");if(source<5)defaults.add("Omitted class maxDailyGaps becomes 0; previously omitted bounds are compensated with explicit No limit.");
        }
        result.set("definition",definition);return result;
    }
    @Transactional public WorkspaceAggregate save(String ifMatch,JsonNode envelope){
        WorkspaceAggregate current=checked(ifMatch);
        if(!(envelope instanceof ObjectNode)||!envelope.propertyNames().equals(Set.of("request","token"))||!envelope.path("token").isTextual()||!token(current,envelope.path("request")).equals(envelope.path("token").asString("")))throw problem("STALE_CATALOG_PREVIEW","Review a refreshed preview of the current saved draft before confirming. No upgrade was committed.");
        ObjectNode preview=preview(ifMatch,envelope.path("request"));if(!preview.path("available").booleanValue())throw problem("CATALOG_ALREADY_LATEST","Catalog 9 is already enabled; no upgrade is available. No values changed.");
        ObjectNode document=((ObjectNode)current.document()).deepCopy(),draft=(ObjectNode)document.path("definitionDraft");draft.set("definition",preview.path("definition").deepCopy());
        ObjectNode review=json.createObjectNode();
        // Preserve the append-only upgrade record if earlier transitions ever become supported.
        ArrayNode history=json.createArrayNode();for(JsonNode row:current.document().path("definitionDraft").path("catalogUpgrades"))history.add(row.deepCopy());review.put("fromCatalog",preview.path("sourceCatalog").intValue()).put("toCatalog",9);review.set("adjustments",preview.path("adjustments").deepCopy());history.add(review);draft.set("catalogUpgrades",history);
        entities.refreshAllEntityIssues(draft);draft.put("draftRevision",DefinitionDraftService.revision(draft.path("definition")));
        if(repository.replace(current.version(),WorkspaceState.DEFINITION_DRAFT,WorkspaceState.DEFINITION_DRAFT,document).isEmpty())throw new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED,"STALE_WORKSPACE_VERSION","The workspace changed. No portion of the catalog upgrade was committed; refresh the preview.");return repository.load();
    }
    private WorkspaceAggregate checked(String ifMatch){WorkspaceAggregate current=repository.load();ImportService.requireMatchingVersion(ifMatch,current);if(current.state()!=WorkspaceState.DEFINITION_DRAFT)throw problem("INVALID_WORKSPACE_TRANSITION","Catalog upgrades require an open definition draft. No values changed.");return current;}
    private void validate(JsonNode request){if(!(request instanceof ObjectNode)||!request.propertyNames().equals(Set.of("targetCatalog"))||!request.path("targetCatalog").isIntegralNumber()||!request.path("targetCatalog").canConvertToInt()||request.path("targetCatalog").intValue()!=9)throw problem("UNSUPPORTED_CATALOG_TARGET","Only an explicit upgrade to the latest supported catalog 9 is available. Downgrades and unsupported targets are refused without mutation.");}
    private static String token(WorkspaceAggregate current,JsonNode request){ObjectNode value=((ObjectNode)current.document()).deepCopy();value.put("previewVersion",current.version());value.set("catalogOperation",request);return DefinitionDraftService.revision(value);}
    private static WorkspaceProblem problem(String code,String message){return new WorkspaceProblem(HttpStatus.CONFLICT,code,message);}
    private record Setting(int since,String description){}
}
