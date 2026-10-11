package org.schoolkernel.workspace;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Ordinary preference choices only; hard feasibility and repair stability are not editable weights. */
@Service
public class DefinitionPreferenceService {
    static final Map<String,Integer> SINCE=Map.of("soft.teacher-gap",1,"soft.series-same-day",1,"soft.undesirable-period",1,"soft.non-preferred-room",1,"soft.cohort-gap",2,"soft.cohort-week-balance",2,"soft.cohort-late-start",3);
    private final WorkspaceRepository repository;
    private final ObjectMapper json;
    public DefinitionPreferenceService(WorkspaceRepository repository,ObjectMapper json){this.repository=repository;this.json=json;}
    @Transactional
    public WorkspaceAggregate save(String ifMatch,JsonNode request){
        WorkspaceAggregate current=repository.load();long version=ImportService.requireMatchingVersion(ifMatch,current);
        if(current.state()!=WorkspaceState.DEFINITION_DRAFT)throw problem("INVALID_WORKSPACE_TRANSITION","Preference editing requires an open definition draft. No weight changed.");
        String id=request.path("constraintId").asString(""),mode=request.path("mode").asString("");
        if(!(request instanceof ObjectNode)||!request.path("constraintId").isTextual()||!SINCE.containsKey(id))throw problem("UNSUPPORTED_PREFERENCE","Only ordinary preferences are supported: "+String.join(", ",new TreeSet<>(SINCE.keySet()))+". Hard feasibility and period/room stability are fixed.");
        if(!Set.of("default","value").contains(mode)||!request.propertyNames().equals(mode.equals("value")?Set.of("constraintId","mode","weight"):Set.of("constraintId","mode")))throw problem("INVALID_PREFERENCE_CHOICE","Choose the catalog default or an explicit weight only.");
        ObjectNode document=((ObjectNode)current.document()).deepCopy(),draft=(ObjectNode)document.path("definitionDraft"),definition=(ObjectNode)draft.path("definition");
        if(SINCE.get(id)>definition.path("catalogVersion").intValue())throw problem("CATALOG_UPGRADE_REQUIRED",id+" requires catalog "+SINCE.get(id)+". Review a catalog upgrade before choosing this setting.");
        ArrayNode rows=json.createArrayNode();int insert=-1;for(JsonNode row:definition.path("softConstraintOverrides")){if(id.equals(row.path("constraintId").asString(""))){if(insert<0)insert=rows.size();}else rows.add(row.deepCopy());}
        if(mode.equals("value")){ObjectNode override=json.createObjectNode().put("constraintId",id);override.set("weight",request.path("weight").deepCopy());if(insert<0)rows.add(override);else rows.insert(insert,override);}
        if(definition.has("softConstraintOverrides")||!rows.isEmpty())definition.set("softConstraintOverrides",rows);
        refresh(draft,json);draft.put("draftRevision",DefinitionDraftService.revision(definition));
        if(repository.replace(version,WorkspaceState.DEFINITION_DRAFT,WorkspaceState.DEFINITION_DRAFT,document).isEmpty())throw new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED,"STALE_WORKSPACE_VERSION","The workspace changed; no preference was committed. Refresh and review again.");return repository.load();
    }
    static void refresh(ObjectNode draft,ObjectMapper json){
        JsonNode definition=draft.path("definition");ArrayNode issues=json.createArrayNode();for(JsonNode row:draft.path("issues"))if(!"PREFERENCE_FIELDS".equals(row.path("diagnosticSource").asString("")))issues.add(row);
        int catalog=definition.path("catalogVersion").asInt(1);Set<String> seen=new HashSet<>();Map<String,Long> weights=new HashMap<>();for(var entry:SINCE.entrySet())if(entry.getValue()<=catalog)weights.put(entry.getKey(),1L);boolean computable=true;
        for(JsonNode row:definition.path("softConstraintOverrides")){String id=row.path("constraintId").asString("");JsonNode weight=row.path("weight");if(!SINCE.containsKey(id)||SINCE.get(id)>catalog){issue(issues,id,"constraintId","Choose an available ordinary preference; hard and stability weights are fixed.");computable=false;}if(!seen.add(id)){issue(issues,id,"constraintId","A preference may be overridden only once.");computable=false;}if(!weight.isIntegralNumber()||!weight.canConvertToInt()||weight.intValue()<0||weight.intValue()>1000000){issue(issues,id,"weight","Use an integer weight from 0 to 1000000, or explicitly choose the catalog default.");computable=false;}else weights.put(id,weight.longValue());}
        if(computable)try{maximumPenalty(definition.path("teachers").size(),definition.path("cohorts").size(),definition.path("periods").size(),definition.path("lessons").size(),weights);}catch(ArithmeticException overflow){issue(issues,"","softConstraintOverrides","Maximum possible weighted preference penalty exceeds signed 64-bit range; retain this work and reduce the weights.");}
        List<JsonNode> sorted=new ArrayList<>();issues.forEach(sorted::add);sorted.sort(Comparator.comparing((JsonNode row)->"BLOCKING".equals(row.path("severity").asString(""))?0:1).thenComparing(row->row.path("ownerType").asString("")).thenComparing(row->row.path("ownerId").asString("")).thenComparing(row->row.path("field").asString("")).thenComparing(row->row.path("message").asString("")));ArrayNode ordered=json.createArrayNode();sorted.forEach(ordered::add);draft.set("issues",ordered);
    }
    /** Same conservative penalty populations as the authoritative kernel, with exact signed-long arithmetic. */
    static long maximumPenalty(long teachers,long cohorts,long periods,long lessons,Map<String,Long> weights){
        long result=0;for(var term:Map.of("soft.teacher-gap",Math.multiplyExact(teachers,periods),"soft.cohort-gap",Math.multiplyExact(cohorts,periods),"soft.cohort-late-start",lessons,"soft.cohort-week-balance",Math.multiplyExact(21L,lessons),"soft.series-same-day",lessons,"soft.undesirable-period",Math.multiplyExact(3L,lessons),"soft.non-preferred-room",lessons).entrySet())result=Math.addExact(result,Math.multiplyExact(term.getValue(),weights.getOrDefault(term.getKey(),0L)));return result;
    }
    private static void issue(ArrayNode issues,String id,String field,String message){ObjectNode issue=issues.addObject().put("severity","BLOCKING").put("ownerType","Preference").put("ownerId",id).put("field",field).put("message",message).put("diagnosticSource","PREFERENCE_FIELDS");issue.putArray("relatedIds");}
    private static WorkspaceProblem problem(String code,String message){return new WorkspaceProblem(HttpStatus.CONFLICT,code,message);}
}
