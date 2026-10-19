package org.schoolkernel.contract;

import java.util.*;
import org.schoolkernel.domain.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

/** Exact pair/provenance and declared weekly allocation certificate checks. Quality is recomputed separately. */
public final class AudiencePairValidator {
    public List<ValidationError> envelope(JsonNode definition,String revision,JsonNode result){
        var errors=new ArrayList<>(new AudienceResultSchemaValidator().validate(result));if(!errors.isEmpty())return errors;
        if(!result.path("status").asString().equals("FEASIBLE")||!result.path("schoolId").equals(definition.path("schoolId"))||!result.path("inputRevision").asString().equals(revision))errors.add(error("/inputRevision","Candidate must be a complete feasible result paired with this exact school and definition revision"));
        String actual=new RevisionService().timetableRevision(2,definition.path("schoolId").asString(),revision,(ArrayNode)result.path("timetable").path("assignments"));
        if(!actual.equals(result.path("timetableRevision").asString()))errors.add(error("/timetableRevision","Result revision does not match its exact actual assignments"));return errors;
    }
    public List<ValidationError> allocations(JsonNode asserted,JsonNode computed){
        var errors=new ArrayList<ValidationError>();var expected=AudienceDefinition.index(computed.path("weeklyTeacherAllocations"));var seen=new HashSet<String>();
        for(JsonNode allocation:asserted.path("weeklyTeacherAllocations")){
            String id=allocation.path("id").asString();var original=expected.get(id);boolean valid=original!=null&&seen.add(id);
            if(valid){for(String key:List.of("portionId","classId","subjectId","audienceId","mode","teacherId"))if(!allocation.path(key).equals(original.path(key)))valid=false;
                var supplied=AudienceDefinition.strings(allocation.path("sessionIds"));var sessions=AudienceDefinition.strings(original.path("sessionIds"));if(supplied.size()!=sessions.size()||!new HashSet<>(supplied).equals(new HashSet<>(sessions)))valid=false;
            }
            if(!valid)errors.add(error("/weeklyTeacherAllocations/"+id,"Weekly allocation identities, fixed/automatic policy, chosen teacher or exact sessions differ from assignments and definition"));
        }
        if(!seen.containsAll(expected.keySet()))errors.add(error("/weeklyTeacherAllocations","A selected weekly allocation is missing"));return errors;
    }
    public static boolean equivalent(JsonNode left,JsonNode right){
        if(left.isNumber()&&right.isNumber())return left.decimalValue().compareTo(right.decimalValue())==0;
        if(left.isObject()&&right.isObject()){if(left.size()!=right.size())return false;for(var entry:left.properties())if(!right.has(entry.getKey())||!equivalent(entry.getValue(),right.path(entry.getKey())))return false;return true;}
        if(left.isArray()&&right.isArray()){if(left.size()!=right.size())return false;for(int i=0;i<left.size();i++)if(!equivalent(left.get(i),right.get(i)))return false;return true;}return left.equals(right);
    }
    private static ValidationError error(String path,String message){return new ValidationError(path,List.of(),message);}
}
