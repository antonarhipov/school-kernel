package org.schoolkernel.workspace;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Finishes only the complete saved, reviewed and authoritatively verified candidate; never solves or publishes. */
@Service
public class DefinitionFinishService {
    private final WorkspaceStore repository;
    private final DefinitionReviewService reviews;
    private final KernelVerifier verifier;
    private final ObjectMapper json;
    private final RepairDraftService repairs;
    public DefinitionFinishService(WorkspaceStore repository,DefinitionReviewService reviews,KernelVerifier verifier,ObjectMapper json,RepairDraftService repairs){
        this.repository=repository;this.reviews=reviews;this.verifier=verifier;this.json=json;this.repairs=repairs;
    }
    public ObjectNode preview(String ifMatch){
        WorkspaceAggregate current=checked(ifMatch);
        ObjectNode result=json.createObjectNode().put("token",token(current)).put("workspaceVersion",current.version());
        result.set("review",reviews.create(current.document().path("definitionDraft"),current.version()));
        result.put("destination",acceptedSource(current)?"REPAIR_DRAFT":"INITIAL_DRAFT");
        return result;
    }
    @Transactional public WorkspaceAggregate finish(String ifMatch,JsonNode request){
        WorkspaceAggregate current=checked(ifMatch);
        if(!(request instanceof ObjectNode)||!request.propertyNames().equals(Set.of("token","confirmed"))
                ||!request.path("confirmed").isBoolean()||!request.path("confirmed").booleanValue()
                ||!request.path("token").isTextual()||!token(current).equals(request.path("token").asString()))
            throw problem("STALE_FINISH_REVIEW","Confirm the exact current saved review. No finish was committed.");
        JsonNode draft=current.document().path("definitionDraft"),review=reviews.create(draft,current.version());
        if(review.path("counts").path("blockers").longValue()!=0)
            throw problem("DEFINITION_BLOCKERS","Finish refused: resolve every blocking issue in the saved review. The entire draft remains saved.");
        ObjectNode candidate=((ObjectNode)draft.path("definition")).deepCopy();
        if(candidate.path("schemaVersion").intValue()!=1)throw problem("INVALID_DEFINITION_LINEAGE","Schema version must be 1. No finish was committed.");
        boolean accepted=acceptedSource(current);
        JsonNode source=draft.path("sourceDocument");
        if(accepted){
            String revision=source.path("acceptedBaseline").path("result").path("inputRevision").asString("");
            if(!revision.matches("sha256:[0-9a-f]{64}")||!candidate.path("basedOnRevision").asString("").equals(revision)
                    ||!candidate.path("schoolId").equals(source.path("acceptedBaseline").path("definition").path("schoolId")))
                throw problem("INVALID_DEFINITION_LINEAGE","Successor must retain the source school and bind its accepted input revision. No finish was committed.");
        }else if(candidate.has("basedOnRevision"))throw problem("INVALID_DEFINITION_LINEAGE","Initial definitions cannot carry a successor revision. No finish was committed.");
        if (accepted) verifier.verify(new ImportDocuments(source.path("acceptedBaseline").path("definition"),
                source.path("acceptedBaseline").path("result"),null,ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        KernelVerifier.Verification verified=verifier.verify(new ImportDocuments(candidate,null,null,accepted?ImportDocuments.ImportMode.SUCCESSOR_DEFINITION:ImportDocuments.ImportMode.INITIAL_DEFINITION));
        ObjectNode document;
        WorkspaceState destination;
        if(accepted){
            document=((ObjectNode)source).deepCopy();destination=WorkspaceState.REPAIR_DRAFT;
            ObjectNode repair=document.putObject("repairDraft");
            repair.set("seedDefinition",candidate);repair.put("seedDefinitionRevision",verified.definitionRevision());
            repair.set("retainedAuthoring",draft.deepCopy());
            ObjectNode intent=repair.putObject("intent");intent.putArray("changes");intent.putArray("pins");intent.putArray("bulkActions");
            repair.put("intentRevision",DefinitionDraftService.revision(intent));repair.putArray("directEffectLessonIds");repair.putArray("conflicts");
            repair.put("directEffectRevision",DefinitionDraftService.revision(repair.path("directEffectLessonIds")));
            repairs.refreshSavedDraft(document);
        }else{
            document=json.createObjectNode();destination=WorkspaceState.INITIAL_DRAFT;
            document.putObject("school").put("id",verified.schoolId()).put("displayName",candidate.path("displayName").asString());
            document.put("importMode","AUTHORED_INITIAL");document.put("definitionRevision",verified.definitionRevision());
            document.set("initialDefinition",candidate);document.set("authoringContext",draft.path("editingContext").deepCopy());
        }
        if(repository.replace(current.version(),WorkspaceState.DEFINITION_DRAFT,destination,document).isEmpty())
            throw new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED,"STALE_WORKSPACE_VERSION","Finish was not committed. Refresh the saved authoring review before retrying.");
        return repository.load();
    }
    private WorkspaceAggregate checked(String ifMatch){WorkspaceAggregate current=repository.load();ImportService.requireMatchingVersion(ifMatch,current);if(current.state()!=WorkspaceState.DEFINITION_DRAFT)throw problem("INVALID_WORKSPACE_TRANSITION","Finish requires an open saved definition draft.");return current;}
    private static boolean acceptedSource(WorkspaceAggregate current){return "ACCEPTED_BASELINE".equals(current.document().path("definitionDraft").path("sourceState").asString());}
    private static String token(WorkspaceAggregate current){ObjectNode binding=((ObjectNode)current.document()).deepCopy();binding.put("finishVersion",current.version());return DefinitionDraftService.revision(binding);}
    private static WorkspaceProblem problem(String code,String message){return new WorkspaceProblem(HttpStatus.CONFLICT,code,message);}
}
