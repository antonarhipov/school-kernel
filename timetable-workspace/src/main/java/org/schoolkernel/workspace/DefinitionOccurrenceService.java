package org.schoolkernel.workspace;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Reviewed, selected-only edits and explicit membership/rule transfers. */
@Service
public class DefinitionOccurrenceService {
    private static final Set<String> FIELDS=Set.of("displayName","subjectId","cohortId","teacherId","requiredRoomCapabilityIds","preferredRoomIds","undesirablePeriodIds","periodLock","roomLock");
    private final WorkspaceStore repository;
    private final ObjectMapper json;
    private final DefinitionRequirementService requirements;
    private final DefinitionEntityService entities;
    public DefinitionOccurrenceService(WorkspaceStore repository,ObjectMapper json,DefinitionRequirementService requirements,DefinitionEntityService entities){this.repository=repository;this.json=json;this.requirements=requirements;this.entities=entities;}

    public ObjectNode preview(String ifMatch,JsonNode request) {
        WorkspaceAggregate current=repository.load();ImportService.requireMatchingVersion(ifMatch,current);requireDraft(current);
        Operation operation=operation(current,request);ObjectNode result=json.createObjectNode().put("version",current.version()).put("token",token(current,request));
        result.set("before",operation.before());result.set("after",operation.after());result.set("rulesBefore",operation.rulesBefore());result.set("rulesAfter",operation.rulesAfter());result.set("beforeContext",requirements.context((ObjectNode)current.document().path("definitionDraft")));result.set("afterContext",operation.document().path("definitionDraft").path("editingContext").path("requirements"));result.set("issues",operation.document().path("definitionDraft").path("issues"));
        ArrayNode placements=result.putArray("sourcePlacements");Set<String> ids=selections(request.path("selectedIds"));for(JsonNode row:current.document().path("definitionDraft").path("sourceDocument").path("acceptedBaseline").path("result").path("timetable").path("assignments"))if(ids.contains(row.path("lessonId").asString("")))placements.add(row);
        return result;
    }
    @Transactional
    public WorkspaceAggregate save(String ifMatch,JsonNode envelope) {
        WorkspaceAggregate current=repository.load();long version=ImportService.requireMatchingVersion(ifMatch,current);requireDraft(current);
        if(!(envelope instanceof ObjectNode)||envelope.size()!=2||!envelope.has("request")||!envelope.path("token").isTextual()||!token(current,envelope.path("request")).equals(envelope.path("token").asString("")))throw problem("STALE_OCCURRENCE_PREVIEW","Review the exact current selected operation before saving. No change was committed.");
        Operation operation=operation(current,envelope.path("request"));if(repository.replace(version,WorkspaceState.DEFINITION_DRAFT,WorkspaceState.DEFINITION_DRAFT,operation.document()).isEmpty())throw new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED,"STALE_WORKSPACE_VERSION","The workspace changed; no selected change was committed. Review again.");return repository.load();
    }

    private Operation operation(WorkspaceAggregate current,JsonNode request) {
        if(!(request instanceof ObjectNode)||!Set.of("selectedIds","changes","grouping","transferRuleIds").equals(request.propertyNames())||!request.path("changes").isObject()||!request.path("grouping").isObject())throw problem("INVALID_OCCURRENCE_OPERATION","Supply only selected occurrence identities, supported changes, explicit grouping and selected room rules.");
        Set<String> selected=selections(request.path("selectedIds")),transfers=selections(request.path("transferRuleIds"));if(selected.isEmpty())throw problem("OCCURRENCE_SELECTION_REQUIRED","Select at least one existing occurrence.");
        ObjectNode document=((ObjectNode)current.document()).deepCopy(),draft=(ObjectNode)document.path("definitionDraft"),definition=(ObjectNode)draft.path("definition"),context=requirements.context(draft);ArrayNode before=json.createArrayNode(),after=json.createArrayNode(),rulesBefore=json.createArrayNode(),rulesAfter=json.createArrayNode();
        for(String id:selected)if(find(definition.path("lessons"),id)==null)throw problem("OCCURRENCE_REMOVED","A selected occurrence no longer exists. Refresh and review the selection.");
        JsonNode grouping=request.path("grouping");String mode=grouping.path("mode").asString("");if(!Set.of("keep","detach","existing","new").contains(mode)||grouping.size()!=(Set.of("existing","new").contains(mode)?2:1)||!Set.of("mode","seriesId").containsAll(grouping.propertyNames()))throw problem("INVALID_SERIES_OPERATION","Choose keep, remove membership, an existing compatible series, or a new stable identity.");
        String target=grouping.path("seriesId").asString("");if(Set.of("existing","new").contains(mode)) {
            if(!grouping.path("seriesId").isTextual()||!validId(target))throw problem("INVALID_SERIES_ID","Choose a valid series identity; no membership was changed.");
            if(mode.equals("existing")&&!context.has(target))throw problem("UNKNOWN_SERIES","Choose an existing compatible series.");
            if(mode.equals("new")){if(context.has(target))throw problem("SERIES_ID_COLLISION","That series exists; choose it explicitly or use a fresh new identity.");JsonNode source=draft.path("sourceDocument");JsonNode sourceDef=source.has("initialDefinition")?source.path("initialDefinition"):source.path("acceptedBaseline").path("definition");for(JsonNode lesson:sourceDef.path("lessons"))if(target.equals(lesson.path("seriesId").asString("")))throw problem("SERIES_ID_RESERVED","That source series is reserved for explicit restoration.");for(JsonNode row:draft.path("removedIdentities"))if("Series".equals(row.path("ownerType").asString(""))&&target.equals(row.path("id").asString("")))throw problem("SERIES_ID_RESERVED","That removed series identity is reserved.");}
        }
        for(var entry:request.path("changes").properties()) {
            String field=entry.getKey();JsonNode setting=entry.getValue();String choice=setting.path("mode").asString("");
            if(!FIELDS.contains(field)||!setting.isObject()||!Set.of("value","omitted","inherit").contains(choice)||setting.size()!=(choice.equals("value")?2:1)||!Set.of("mode","value").containsAll(setting.propertyNames()))throw problem("INVALID_OCCURRENCE_FIELD","Choose an explicit supported occurrence value, omission or shared inheritance. Identifiers are permanent.");
            if(choice.equals("inherit")&&!DefinitionRequirementService.SHARED.contains(field))throw problem("NO_SHARED_SETTING",field+" is not an inheritable requirement setting.");
        }
        ObjectNode authored=draft.path("editingContext").path("authoredLocks").isObject()?(ObjectNode)draft.path("editingContext").path("authoredLocks").deepCopy():json.createObjectNode();
        for(JsonNode row:definition.path("lessons"))if(selected.contains(row.path("id").asString(""))) {
            ObjectNode lesson=(ObjectNode)row;before.add(row.deepCopy());String id=lesson.path("id").asString(""),oldSeries=lesson.path("seriesId").asString("");
            for(var entry:request.path("changes").properties())if(!"inherit".equals(entry.getValue().path("mode").asString(""))) {if("omitted".equals(entry.getValue().path("mode").asString("")))lesson.remove(entry.getKey());else lesson.set(entry.getKey(),entry.getValue().path("value").deepCopy());}
            if(mode.equals("detach"))lesson.remove("seriesId");else if(Set.of("existing","new").contains(mode))lesson.put("seriesId",target);
            else if(!oldSeries.isEmpty()&&(!context.path(oldSeries).path("cohortId").equals(lesson.path("cohortId"))||!context.path(oldSeries).path("subjectId").equals(lesson.path("subjectId"))))lesson.remove("seriesId");
            String series=lesson.path("seriesId").asString("");
            if(mode.equals("existing")&&(!context.path(target).path("cohortId").equals(lesson.path("cohortId"))||!context.path(target).path("subjectId").equals(lesson.path("subjectId"))))throw problem("INCOMPATIBLE_SERIES","The selected occurrence's class/subject differs from the target requirement. No membership was changed.");
            for(var entry:request.path("changes").properties())if("inherit".equals(entry.getValue().path("mode").asString(""))) {
                String field=entry.getKey();JsonNode shared=context.path(series);if(series.isEmpty()||!shared.isObject()||contains(shared.path("mixedFields"),field))throw problem("NO_SHARED_SETTING","Establish a shared "+field+" setting through weekly requirements before choosing inheritance.");
                if(shared.path("defaults").has(field))lesson.set(field,shared.path("defaults").path(field).deepCopy());else lesson.remove(field);
            }
            if(!series.equals(oldSeries)&&!oldSeries.isEmpty()&&context.has(oldSeries))((ObjectNode)context.path(oldSeries).path("exceptions")).remove(id);
            if(!series.isEmpty()&&context.has(series)) {
                ObjectNode exceptions=(ObjectNode)context.path(series).path("exceptions");Set<String> intent=values(exceptions.path(id));
                if(!series.equals(oldSeries))intent.addAll(DefinitionRequirementService.SHARED);
                for(var entry:request.path("changes").properties())if(DefinitionRequirementService.SHARED.contains(entry.getKey())){if("inherit".equals(entry.getValue().path("mode").asString("")))intent.remove(entry.getKey());else intent.add(entry.getKey());}
                exceptions.set(id,array(intent));
            }
            for(String field:List.of("periodLock","roomLock"))if(request.path("changes").has(field)) {ObjectNode locks=authored.withObject(id);if(lesson.has(field))locks.set(field,lesson.path(field).deepCopy());else locks.remove(field);}
            after.add(lesson.deepCopy());
        }
        if(mode.equals("new")) {JsonNode first=after.get(0);if(!first.path("cohortId").isTextual()||!validId(first.path("cohortId").asString(""))||!first.path("subjectId").isTextual()||!validId(first.path("subjectId").asString("")))throw problem("INCOMPATIBLE_SERIES","Grouping needs one common class and subject identity.");for(JsonNode lesson:after)if(!first.path("cohortId").equals(lesson.path("cohortId"))||!first.path("subjectId").equals(lesson.path("subjectId")))throw problem("INCOMPATIBLE_SERIES","All grouped occurrences must share class and subject. No membership was changed.");}
        // Infer only newly established groups; existing defaults and equal-valued exception intent stay durable.
        ((ObjectNode)draft.path("editingContext")).set("requirements",context);context=requirements.context(draft);
        if(mode.equals("new"))for(JsonNode lesson:after) {
            JsonNode previous=find(before,lesson.path("id").asString(""));Set<String> intent=values(context.path(target).path("exceptions").path(lesson.path("id").asString("")));
            if(previous!=null)intent.addAll(values(requirements.context((ObjectNode)current.document().path("definitionDraft")).path(previous.path("seriesId").asString("")).path("exceptions").path(lesson.path("id").asString(""))));
            for(var entry:request.path("changes").properties())if(DefinitionRequirementService.SHARED.contains(entry.getKey())&&!"inherit".equals(entry.getValue().path("mode").asString("")))intent.add(entry.getKey());
            ((ObjectNode)context.path(target).path("exceptions")).set(lesson.path("id").asString(""),array(intent));
        }
        Set<String> affectedSeries=new HashSet<>();for(JsonNode row:before)if(row.has("seriesId"))affectedSeries.add(row.path("seriesId").asString(""));for(JsonNode row:after)if(row.has("seriesId"))affectedSeries.add(row.path("seriesId").asString(""));
        for(var entry:context.properties()) {
            if(!affectedSeries.contains(entry.getKey()))continue;
            ObjectNode requirement=(ObjectNode)entry.getValue();int count=0;Set<String> members=new HashSet<>();for(JsonNode lesson:definition.path("lessons"))if(entry.getKey().equals(lesson.path("seriesId").asString(""))){count++;members.add(lesson.path("id").asString(""));}
            JsonNode savedCount=requirement.path("count");if(savedCount.isIntegralNumber()&&savedCount.canConvertToInt()&&savedCount.intValue()>=0)requirement.put("count",count);
            ObjectNode exceptions=(ObjectNode)requirement.path("exceptions");for(String id:new HashSet<>(exceptions.propertyNames()))if(!members.contains(id))exceptions.remove(id);
        }
        if(!transfers.isEmpty()) {
            JsonNode teacher=request.path("changes").path("teacherId");if(!"value".equals(teacher.path("mode").asString("")))throw problem("RULE_TRANSFER_TEACHER_REQUIRED","Choose an explicit replacement teacher before selecting teacher-scoped room-rule transfers.");
            for(String id:transfers) {
                JsonNode row=find(definition.path("roomAssignments"),id);if(!(row instanceof ObjectNode rule)||!rule.has("teacherId"))throw problem("INVALID_RULE_TRANSFER","Select existing teacher-scoped room rules only.");
                boolean matches=false;for(JsonNode lesson:before)if(rule.path("teacherId").equals(lesson.path("teacherId"))&&rule.path("subjectId").equals(lesson.path("subjectId")))matches=true;
                if(!matches)throw problem("INVALID_RULE_TRANSFER","The selected rule did not match any selected lesson before reassignment.");rulesBefore.add(rule.deepCopy());rule.set("teacherId",teacher.path("value").deepCopy());rulesAfter.add(rule.deepCopy());
            }
        }
        ((ObjectNode)draft.path("editingContext")).set("requirements",context);((ObjectNode)draft.path("editingContext")).set("authoredLocks",authored);
        entities.refreshTeachingIssues(draft);draft.put("draftRevision",DefinitionDraftService.revision(definition));return new Operation(document,before,after,rulesBefore,rulesAfter);
    }
    private static JsonNode find(JsonNode rows,String id){for(JsonNode row:rows)if(id.equals(row.path("id").asString("")))return row;return null;}
    private static boolean validId(String id){return id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");}
    private static boolean contains(JsonNode rows,String id){for(JsonNode row:rows)if(id.equals(row.asString("")))return true;return false;}
    private static Set<String> values(JsonNode rows){Set<String> result=new LinkedHashSet<>();for(JsonNode row:rows)result.add(row.asString(""));return result;}
    private static Set<String> selections(JsonNode rows){if(!rows.isArray())throw problem("INVALID_SELECTION","Select an explicit unique identifier array.");Set<String> result=new LinkedHashSet<>();for(JsonNode row:rows)if(!row.isTextual()||!validId(row.asString(""))||!result.add(row.asString("")))throw problem("INVALID_SELECTION","Selections must contain unique valid identities.");return result;}
    private ArrayNode array(Collection<String> values){ArrayNode rows=json.createArrayNode();values.forEach(rows::add);return rows;}
    private static String token(WorkspaceAggregate current,JsonNode request){ObjectNode doc=((ObjectNode)current.document()).deepCopy();doc.put("previewVersion",current.version());doc.set("occurrenceOperation",request);return DefinitionDraftService.revision(doc);}
    private static void requireDraft(WorkspaceAggregate current){if(current.state()!=WorkspaceState.DEFINITION_DRAFT)throw problem("INVALID_WORKSPACE_TRANSITION","Occurrence editing requires an open definition draft. No selected value was changed.");}
    private static WorkspaceProblem problem(String code,String message){return new WorkspaceProblem(HttpStatus.CONFLICT,code,message);}
    private record Operation(ObjectNode document,ArrayNode before,ArrayNode after,ArrayNode rulesBefore,ArrayNode rulesAfter){}
}
