package org.schoolkernel.contract;

import java.util.*;
import org.schoolkernel.solver.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.*;

/** Public version-two result with actual assignments and independently recomputed accounting. */
public final class AudienceResultFactory {
    public ObjectNode feasible(AudienceSchedule m,AudienceReview.Evaluation e,String revision,String correlation,long elapsed,String termination){
        return feasible(m,e,revision,correlation,elapsed,termination,null,null);
    }
    public ObjectNode feasible(AudienceSchedule m,AudienceReview.Evaluation e,String revision,String correlation,long elapsed,String termination,JsonNode context,AudienceComparison.Comparison comparison){
        if(!e.valid())throw new IllegalArgumentException("Cannot publish an invalid audience timetable");
        var result=JsonSupport.mapper().createObjectNode().put("schemaVersion",2).put("status","FEASIBLE").put("kernelVersion",KernelMetadata.version()).put("correlationId",correlation).put("elapsedTimeMs",elapsed).put("schoolId",m.resources.schoolId()).put("catalogVersion",11).put("inputRevision",revision).put("seed",0).put("terminationReason",termination);
        result.putObject("limit").put("type","NONE");var weights=new TreeMap<>(m.resources.softWeights());weights.put("soft.parallel-coverage",1L);weights.put("soft.cohort-gap",1L);result.set("effectiveSoftWeights",JsonSupport.mapper().valueToTree(weights));
        var assignments=result.putObject("timetable").putArray("assignments");m.assignments.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->assignments.add(entry.getValue().deepCopy()));
        result.put("timetableRevision",new RevisionService().timetableRevision(2,m.resources.schoolId(),revision,assignments));
        var score=result.putObject("score").put("teacherAllocationChanges",comparison==null?0:comparison.teacherAllocationChanges()).put("periodMoves",comparison==null?0:comparison.periodMoves()).put("roomOnlyMoves",comparison==null?0:comparison.roomOnlyMoves()).put("internalGapStudentPeriods",e.internalGapStudentPeriods()).put("ordinaryPreferencePenalty",e.ordinaryPreferencePenalty());
        var breakdown=score.putArray("constraintBreakdown");for(var weight:weights.entrySet())breakdown.addObject().put("constraintId",weight.getKey()).put("category",weight.getKey().equals("soft.cohort-gap")?"STUDENT_GAP":"ORDINARY_PREFERENCE").put("effectiveWeight",weight.getValue()).put("matchCount",e.soft().getOrDefault(weight.getKey(),0L)).put("aggregatePenalty",Math.multiplyExact(weight.getValue(),e.soft().getOrDefault(weight.getKey(),0L)));
        var allocations=result.putArray("weeklyTeacherAllocations");var groups=new TreeMap<String,List<String>>();
        for(var s:m.sessions.values()){var a=m.assignments.get(s.id());String id=a.has("allocationId")?a.path("allocationId").asString():s.portionId();groups.computeIfAbsent(id,ignored->new ArrayList<>()).add(s.id());}
        for(var entry:groups.entrySet()){
            entry.getValue().sort(String::compareTo);var s=m.sessions.get(entry.getValue().getFirst());var a=m.assignments.get(s.id());
            var allocation=allocations.addObject().put("id",entry.getKey()).put("portionId",s.portionId()).put("classId",s.classId()).put("subjectId",s.subjectId()).put("audienceId",s.audienceId()).put("mode",s.portion()==null?"fixed":s.portion().path("teacherPolicy").path("mode").asString()).put("teacherId",a.path("teacherId").asString());allocation.set("sessionIds",JsonSupport.mapper().valueToTree(entry.getValue()));
        }
        result.set("coordinationMatches",JsonSupport.mapper().valueToTree(e.coordinationMatches()));
        var quality=result.putObject("quality").put("coverageStudentPeriods",e.coverageStudentPeriods()).put("gapDetailCount",e.gaps().size()).put("gapDetailsTruncated",e.gaps().size()>1000);
        quality.set("gaps",JsonSupport.mapper().valueToTree(e.gaps().stream().limit(1000).toList()));quality.set("teacherWorkload",JsonSupport.mapper().valueToTree(e.workloads()));if(context!=null){result.set("repairContext",context.deepCopy());result.set("changeReport",comparison.report().deepCopy());}return result;
    }
    public ObjectNode review(AudienceSchedule m,AudienceReview.Evaluation e,String revision,String correlation,long elapsed,JsonNode asserted){
        return review(m,e,revision,correlation,elapsed,asserted,null,null);
    }
    public ObjectNode review(AudienceSchedule m,AudienceReview.Evaluation e,String revision,String correlation,long elapsed,JsonNode asserted,JsonNode context,AudienceComparison.Comparison comparison){
        var result=new VerificationResultFactory().verifiedInitial(correlation,elapsed,m.resources.schoolId(),11,revision);result.put("status","REVIEWED");result.remove("mode");result.put("valid",e.valid());result.set("hardMatchCounts",JsonSupport.mapper().valueToTree(e.hard()));result.set("softMatchCounts",JsonSupport.mapper().valueToTree(e.soft()));result.set("conflicts",JsonSupport.mapper().valueToTree(e.conflicts().stream().limit(1000).toList()));result.put("truncated",e.conflicts().size()>1000);
        if(e.valid()){
            var checked=feasible(m,e,revision,correlation,elapsed,"MANUAL_REVIEW",context,comparison);if(asserted!=null&&asserted.has("coordinationMatches"))checked.set("coordinationMatches",asserted.path("coordinationMatches").deepCopy());result.set("result",checked);
            // These describe the source search, not a new search by this stateless reviewer.
            if(asserted!=null)for(String field:List.of("seed","limit","terminationReason","searchOutcome"))if(asserted.has(field))checked.set(field,asserted.path(field).deepCopy());
            boolean mismatch=asserted!=null&&asserted.has("score")&&(!AudiencePairValidator.equivalent(asserted.path("score"),checked.path("score"))||!AudiencePairValidator.equivalent(asserted.path("quality"),checked.path("quality")));result.put("assertedQualityMismatch",mismatch);if(mismatch){result.set("assertedScore",asserted.path("score").deepCopy());result.set("assertedQuality",asserted.path("quality").deepCopy());}
        }
        return result;
    }
}
