package org.schoolkernel.workspace;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Atomic occurrence operations; editing intent stays outside the native kernel definition. */
@Service
public class DefinitionRequirementService {
    static final List<String> SHARED = List.of("teacherId", "requiredRoomCapabilityIds", "preferredRoomIds", "undesirablePeriodIds", "periodLock", "roomLock");
    private final WorkspaceStore repository;
    private final ObjectMapper json;
    private final DefinitionEntityService entities;
    public DefinitionRequirementService(WorkspaceStore repository, ObjectMapper json, DefinitionEntityService entities) {
        this.repository=repository;this.json=json;this.entities=entities;
    }

    public ObjectNode view() {
        WorkspaceAggregate current=repository.load();requireDraft(current);
        ObjectNode result=json.createObjectNode().put("version",current.version());
        ObjectNode draft=(ObjectNode)current.document().path("definitionDraft");
        ArrayNode rows=result.putArray("requirements");
        ObjectNode requirements=context(draft);
        for (var entry:requirements.properties()) {
            ObjectNode row=(ObjectNode)entry.getValue().deepCopy();
            ArrayNode occurrences=row.putArray("occurrences");
            for(JsonNode lesson:draft.path("definition").path("lessons"))if(entry.getKey().equals(lesson.path("seriesId").asString(""))) occurrences.add(lesson);
            row.set("sourcePlacements", placements(draft,occurrences));rows.add(row);
        }
        ArrayNode ungrouped=result.putArray("ungrouped");
        for(JsonNode lesson:draft.path("definition").path("lessons"))if(!lesson.has("seriesId"))ungrouped.add(lesson);
        return result;
    }

    public ObjectNode preview(String ifMatch, JsonNode request) {
        WorkspaceAggregate current=repository.load();ImportService.requireMatchingVersion(ifMatch,current);requireDraft(current);
        Operation operation=operation(current,request);
        ObjectNode result=json.createObjectNode().put("version",current.version()).put("token",token(current,request));
        result.set("previousRequirement",context((ObjectNode)current.document().path("definitionDraft")).path(request.path("seriesId").asString("")));
        result.set("requirement",operation.requirement());result.set("before",operation.before());result.set("after",operation.after());
        result.set("removed",operation.removed());result.set("added",operation.added());result.set("overwrittenExceptions",operation.overwritten());
        result.set("issues",operation.document().path("definitionDraft").path("issues"));
        result.set("sourcePlacements",placements((ObjectNode)current.document().path("definitionDraft"),operation.before()));return result;
    }

    @Transactional
    public WorkspaceAggregate save(String ifMatch, JsonNode envelope) {
        WorkspaceAggregate current=repository.load();long version=ImportService.requireMatchingVersion(ifMatch,current);requireDraft(current);
        if(!(envelope instanceof ObjectNode)||envelope.size()!=2||!envelope.has("request")||!envelope.path("token").isTextual()
                || !token(current,envelope.path("request")).equals(envelope.path("token").asString("")))
            throw problem("STALE_REQUIREMENT_PREVIEW","Review the exact current operation before saving. No change was committed.");
        Operation operation=operation(current,envelope.path("request"));
        if(repository.replace(version,WorkspaceState.DEFINITION_DRAFT,WorkspaceState.DEFINITION_DRAFT,operation.document()).isEmpty())
            throw new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED,"STALE_WORKSPACE_VERSION","The workspace changed. Re-review the operation; no change was committed.");
        return repository.load();
    }

    private Operation operation(WorkspaceAggregate current,JsonNode request) {
        if(!(request instanceof ObjectNode)||!Set.of("originalSeriesId","seriesId","cohortId","subjectId","count","changes","followers","applyToAllFields","removeIds").equals(request.propertyNames()))
            throw problem("INVALID_REQUIREMENT_OPERATION","Supply only the requirement identity, shared cohort/subject, count, changes, scopes and selected removals.");
        String id=request.path("seriesId").asString("");if(!validId(id))throw problem("INVALID_SERIES_ID","Choose a valid stable series identifier before saving.");
        if(!request.path("changes").isObject()||!request.path("followers").isObject()||!request.path("applyToAllFields").isArray()||!request.path("removeIds").isArray())throw problem("INVALID_REQUIREMENT_OPERATION","Changes and follower selections must be objects; scopes and removals must be arrays.");
        ObjectNode document=((ObjectNode)current.document()).deepCopy(),draft=(ObjectNode)document.path("definitionDraft"),definition=(ObjectNode)draft.path("definition");
        ObjectNode requirements=context(draft);boolean existing=requirements.has(id);
        String original=request.path("originalSeriesId").asString("");
        if(!request.path("originalSeriesId").isTextual())throw problem("INVALID_SERIES_ID","Supply the original series identity explicitly.");
        if(original.isEmpty()&&existing)throw problem("SERIES_ID_COLLISION","That series identifier already exists. Choose a fresh identity for a new requirement.");
        if(!original.isEmpty()&&(!original.equals(id)||!existing))throw problem("SERIES_ID_IMMUTABLE","The saved requirement identity is permanent. No occurrence was changed.");
        if(!existing && reserved(draft,"Series","seriesId").contains(id))throw problem("SERIES_ID_RESERVED","That series identity is reserved; restore source work explicitly where available.");
        ObjectNode requirement=existing?(ObjectNode)requirements.path(id).deepCopy():json.createObjectNode().put("seriesId",id).put("nextSequence",1);
        if(!existing){requirement.putObject("defaults");requirement.putObject("exceptions");requirement.putArray("mixedFields");}
        ObjectNode defaults=(ObjectNode)requirement.path("defaults"),exceptions=(ObjectNode)requirement.path("exceptions");
        Set<String> mixed=strings(requirement.path("mixedFields")),all=strings(request.path("applyToAllFields"));
        if(!SHARED.containsAll(all))throw problem("INVALID_SHARED_FIELD","Only supported shared lesson settings can be applied.");
        ArrayNode before=json.createArrayNode();Set<String> memberIds=new LinkedHashSet<>();
        for(JsonNode lesson:definition.path("lessons"))if(id.equals(lesson.path("seriesId").asString(""))){before.add(lesson.deepCopy());memberIds.add(lesson.path("id").asString(""));}
        for(var field:request.path("followers").properties())if(!SHARED.contains(field.getKey())||!field.getValue().isArray()||!memberIds.containsAll(strings(field.getValue())))throw problem("INVALID_FOLLOWER_SELECTION","Select only existing occurrences of this requirement.");
        Set<String> removals=strings(request.path("removeIds"));if(!memberIds.containsAll(removals))throw problem("INVALID_OCCURRENCE_REMOVAL","Choose only existing requirement occurrences for cancellation.");
        JsonNode count=request.path("count");boolean expandable=count.isIntegralNumber()&&count.canConvertToInt()&&count.intValue()>=0;
        if(expandable && removals.size()!=Math.max(0,before.size()-count.intValue()))throw problem("REMOVAL_SELECTION_REQUIRED","Select exactly "+Math.max(0,before.size()-count.intValue())+" occurrences to cancel. Working preview is retained.");
        if(!expandable&&!removals.isEmpty())throw problem("INVALID_COUNT_REMOVAL","An incomplete count cannot cancel occurrences. Keep all existing occurrences until the count is completed.");
        requirement.set("cohortId",request.path("cohortId").deepCopy());requirement.set("subjectId",request.path("subjectId").deepCopy());requirement.set("count",count.deepCopy());
        ArrayNode overwritten=json.createArrayNode();
        for(var change:request.path("changes").properties()) {
            String field=change.getKey();JsonNode setting=change.getValue();
            if(!SHARED.contains(field)||!setting.isObject()||setting.size()!=("value".equals(setting.path("mode").asString(""))?2:1)||!Set.of("mode","value").containsAll(setting.propertyNames())||!Set.of("omitted","value").contains(setting.path("mode").asString(""))||("value".equals(setting.path("mode").asString(""))&&!setting.has("value")))throw problem("INVALID_SHARED_FIELD","Choose an explicit supported shared value or omitted default.");
            if(mixed.contains(field)&&!request.path("followers").has(field)&&!all.contains(field))throw problem("MIXED_FOLLOWERS_REQUIRED","The imported "+field+" is Mixed. Explicitly select followers or Apply to all before establishing a default.");
            if("omitted".equals(setting.path("mode").asString("")))defaults.remove(field);else defaults.set(field,setting.path("value").deepCopy());mixed.remove(field);
            for(JsonNode lesson:before){String lessonId=lesson.path("id").asString("");if(removals.contains(lessonId))continue;Set<String> fields=strings(exceptions.path(lessonId));boolean wasException=fields.contains(field);
                boolean follows=all.contains(field)||(request.path("followers").has(field)?strings(request.path("followers").path(field)).contains(lessonId):!wasException);
                if(follows){if(wasException&&all.contains(field))overwritten.addObject().put("lessonId",lessonId).put("field",field).set("previous",lesson.path(field).deepCopy());fields.remove(field);}else fields.add(field);
                exceptions.set(lessonId,array(fields));
            }
        }
        // Scopes cannot secretly change exception intent without a reviewed shared-field change.
        for(var selection:request.path("followers").properties())if(!request.path("changes").has(selection.getKey()))throw problem("INVALID_FOLLOWER_SELECTION","A follower scope must accompany an explicit shared-setting choice.");
        for(String field:all)if(!request.path("changes").has(field))throw problem("INVALID_SHARED_FIELD","Apply to all must accompany an explicit shared-setting choice.");
        if(expandable && count.intValue()>before.size() && !mixed.isEmpty())throw problem("MIXED_DEFAULTS_REQUIRED","Establish explicit defaults and reviewed follower scopes for every Mixed setting before adding occurrences: "+String.join(", ",mixed)+".");
        requirement.set("mixedFields",array(mixed));
        ArrayNode after=json.createArrayNode(),removed=json.createArrayNode(),added=json.createArrayNode();
        if(!expandable){for(JsonNode lesson:before)after.add(lesson.deepCopy());}
        else {
            for(JsonNode row:before){String lessonId=row.path("id").asString("");if(removals.contains(lessonId)){removed.add(row);exceptions.remove(lessonId);continue;}
                ObjectNode lesson=(ObjectNode)row.deepCopy();lesson.set("cohortId",request.path("cohortId").deepCopy());lesson.set("subjectId",request.path("subjectId").deepCopy());
                for(String field:SHARED)if(!mixed.contains(field)&&!strings(exceptions.path(lessonId)).contains(field)){if(defaults.has(field))lesson.set(field,defaults.path(field).deepCopy());else lesson.remove(field);}after.add(lesson);
            }
            Set<String> occupied=reserved(draft,"Lesson","id");for(JsonNode lesson:definition.path("lessons"))occupied.add(lesson.path("id").asString(""));
            long sequence=requirement.path("nextSequence").asLong(1);
            while(after.size()<count.intValue()) {
                String lessonId;long number;
                do {number=sequence++;String suffix=String.format(java.util.Locale.ROOT,".%02d",number);lessonId=id.substring(0,Math.min(id.length(),128-suffix.length()))+suffix;}while(occupied.contains(lessonId));occupied.add(lessonId);
                ObjectNode lesson=json.createObjectNode().put("id",lessonId).put("seriesId",id).put("displayName",name(definition,"cohorts",request.path("cohortId").asString(""))+" "+name(definition,"subjects",request.path("subjectId").asString(""))+" "+number);
                lesson.set("cohortId",request.path("cohortId").deepCopy());lesson.set("subjectId",request.path("subjectId").deepCopy());for(var field:defaults.properties())lesson.set(field.getKey(),field.getValue().deepCopy());after.add(lesson);added.add(lesson.deepCopy());
            }
            requirement.put("nextSequence",sequence);
        }
        if(expandable){ArrayNode lessons=json.createArrayNode();Map<String,JsonNode> replacements=new HashMap<>();for(JsonNode lesson:after)replacements.put(lesson.path("id").asString(""),lesson);for(JsonNode lesson:definition.path("lessons")){if(!id.equals(lesson.path("seriesId").asString("")))lessons.add(lesson);else if(replacements.containsKey(lesson.path("id").asString("")))lessons.add(replacements.get(lesson.path("id").asString("")));}added.forEach(lessons::add);definition.set("lessons",lessons);}
        for(JsonNode lesson:removed)draft.withArray("removedIdentities").addObject().put("ownerType","Lesson").put("id",lesson.path("id").asString(""));
        // Explicit shared lock choices carry the same persistent-policy intent as individual choices.
        if(expandable){ObjectNode authored=draft.path("editingContext").path("authoredLocks").isObject()?(ObjectNode)draft.path("editingContext").path("authoredLocks").deepCopy():json.createObjectNode();Set<String> addedIds=new HashSet<>();for(JsonNode row:added)addedIds.add(row.path("id").asString(""));for(JsonNode row:after)for(String field:List.of("periodLock","roomLock")){String lessonId=row.path("id").asString("");if(addedIds.contains(lessonId)||request.path("changes").has(field)&&!strings(exceptions.path(lessonId)).contains(field)){ObjectNode locks=authored.withObject(lessonId);if(row.has(field))locks.set(field,row.path(field).deepCopy());else locks.remove(field);}}for(JsonNode row:removed)authored.remove(row.path("id").asString(""));((ObjectNode)draft.path("editingContext")).set("authoredLocks",authored);}
        requirements.set(id,requirement);((ObjectNode)draft.path("editingContext")).set("requirements",requirements);
        entities.refreshTeachingIssues(draft);
        draft.put("draftRevision",DefinitionDraftService.revision(definition));return new Operation(document,requirement,before,after,removed,added,overwritten);
    }

    /** Conservative inference only; absence is compared by value, not collapsed into explicit empty. */
    ObjectNode context(ObjectNode draft) {
        ObjectNode requirements=draft.path("editingContext").path("requirements").isObject()?(ObjectNode)draft.path("editingContext").path("requirements").deepCopy():json.createObjectNode();
        Map<String,List<JsonNode>> groups=new LinkedHashMap<>();for(JsonNode lesson:draft.path("definition").path("lessons"))if(lesson.path("seriesId").isTextual())groups.computeIfAbsent(lesson.path("seriesId").asString(""),k->new ArrayList<>()).add(lesson);
        for(var group:groups.entrySet())if(!requirements.has(group.getKey())){
            List<JsonNode> members=group.getValue();JsonNode first=members.getFirst();ObjectNode row=requirements.putObject(group.getKey()).put("seriesId",group.getKey()).put("count",members.size()).put("nextSequence",1);
            row.set("cohortId",first.path("cohortId").deepCopy());row.set("subjectId",first.path("subjectId").deepCopy());ObjectNode defaults=row.putObject("defaults"),exceptions=row.putObject("exceptions");ArrayNode mixed=row.putArray("mixedFields");
            for(String field:SHARED){boolean equal=members.stream().allMatch(lesson->lesson.path(field).equals(first.path(field)));if(equal){if(first.has(field))defaults.set(field,first.path(field).deepCopy());}else {mixed.add(field);for(JsonNode lesson:members)exceptions.withArray(lesson.path("id").asString("")).add(field);}}
        }
        return requirements;
    }
    private Set<String> reserved(ObjectNode draft,String type,String field) {
        Set<String> result=new HashSet<>();JsonNode source=draft.path("sourceDocument"),definition=source.has("initialDefinition")?source.path("initialDefinition"):source.path("acceptedBaseline").path("definition");
        for(JsonNode lesson:definition.path("lessons"))if(lesson.has(field))result.add(lesson.path(field).asString(""));
        for(JsonNode row:draft.path("removedIdentities"))if(type.equals(row.path("ownerType").asString("")))result.add(row.path("id").asString(""));return result;
    }
    private ArrayNode placements(ObjectNode draft,JsonNode members){Set<String> ids=new HashSet<>();for(JsonNode member:members)ids.add(member.path("id").asString(""));ArrayNode result=json.createArrayNode();for(JsonNode placement:draft.path("sourceDocument").path("acceptedBaseline").path("result").path("timetable").path("assignments"))if(ids.contains(placement.path("lessonId").asString("")))result.add(placement);return result;}
    private static Set<String> strings(JsonNode values){Set<String> result=new LinkedHashSet<>();for(JsonNode value:values){if(!value.isTextual()||!result.add(value.asString("")))throw problem("INVALID_OPERATION_SELECTION","Selections must contain unique text identifiers.");}return result;}
    private ArrayNode array(Collection<String> values){ArrayNode result=json.createArrayNode();values.forEach(result::add);return result;}
    private static JsonNode find(JsonNode rows,String id){for(JsonNode row:rows)if(id.equals(row.path("id").asString("")))return row;return null;}
    private static String name(JsonNode definition,String collection,String id){JsonNode item=find(definition.path(collection),id);return item==null?id:item.path("displayName").asString(id);}
    private static int countMembers(JsonNode definition,String id){int count=0;for(JsonNode lesson:definition.path("lessons"))if(id.equals(lesson.path("seriesId").asString("")))count++;return count;}
    private static boolean validId(String id){return id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");}
    private static String token(WorkspaceAggregate current,JsonNode request){ObjectNode value=((ObjectNode)current.document()).deepCopy();value.put("previewVersion",current.version());value.set("requirementOperation",request);return DefinitionDraftService.revision(value);}
    private static void requireDraft(WorkspaceAggregate current){if(current.state()!=WorkspaceState.DEFINITION_DRAFT)throw problem("INVALID_WORKSPACE_TRANSITION","Weekly requirements require an open definition draft. No change was committed.");}
    private static WorkspaceProblem problem(String code,String message){return new WorkspaceProblem(HttpStatus.CONFLICT,code,message);}
    private record Operation(ObjectNode document,ObjectNode requirement,ArrayNode before,ArrayNode after,ArrayNode removed,ArrayNode added,ArrayNode overwritten){}
}
