package org.schoolkernel.solver;

import java.util.*;
import org.schoolkernel.contract.JsonSupport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Stable anonymous audience identity comparison; no label/position/time matching. */
public final class AudienceComparison {
    public record Comparison(long teacherAllocationChanges,long periodMoves,long roomOnlyMoves,ObjectNode report) {}
    public Comparison compare(AudienceSchedule old,AudienceSchedule current){
        var report=JsonSupport.mapper().createObjectNode();for(String key:List.of("additions","cancellations","teacherChanges","forcedMoves","periodMoves","roomOnlyMoves","unmatchedChanges"))report.putArray(key);
        long teacherChanges=0,periodMoves=0,roomMoves=0;var oldPortions=portions(old);var newPortions=portions(current);
        for(var entry:newPortions.entrySet()){
            var before=oldPortions.get(entry.getKey());var after=entry.getValue();
            if(before==null)continue;
            if(before.automatic()&&after.automatic()&&sameAudience(old,before,current,after)){
                String oldTeacher=old.assignments.get(before.id()).path("teacherId").asString(),newTeacher=current.assignments.get(after.id()).path("teacherId").asString();
                if(!oldTeacher.equals(newTeacher)){teacherChanges++;var change=report.withArray("teacherChanges").addObject().put("allocationId",entry.getKey()).put("classId",after.classId()).put("subjectId",after.subjectId()).put("audienceId",after.audienceId()).put("oldTeacherId",oldTeacher).put("newTeacherId",newTeacher);change.set("sessionIds",JsonSupport.mapper().valueToTree(current.sessions.values().stream().filter(s->s.portionId().equals(entry.getKey())).map(AudienceSchedule.Session::id).sorted().toList()));}
            }
        }
        for(var s:current.sessions.values()){
            var before=old.sessions.get(s.id());if(before==null){report.withArray("additions").addObject().put("lessonId",s.id());continue;}
            if(!sameAudience(old,before,current,s)||!samePortion(before,s)){
                report.withArray("unmatchedChanges").addObject().put("lessonId",s.id()).put("reason",!sameAudience(old,before,current,s)?"Audience, subject or count changed":"Teaching portion was replaced");continue;
            }
            var a=old.assignments.get(s.id());var b=current.assignments.get(s.id());boolean period=!a.path("periodId").equals(b.path("periodId")),room=!a.path("roomId").equals(b.path("roomId"));
            if(period){periodMoves++;report.withArray("periodMoves").addObject().put("lessonId",s.id()).put("oldPeriodId",a.path("periodId").asString()).put("newPeriodId",b.path("periodId").asString()).put("oldRoomId",a.path("roomId").asString()).put("newRoomId",b.path("roomId").asString());}
            else if(room){roomMoves++;report.withArray("roomOnlyMoves").addObject().put("lessonId",s.id()).put("oldRoomId",a.path("roomId").asString()).put("newRoomId",b.path("roomId").asString());}
        }
        for(String id:old.sessions.keySet())if(!current.sessions.containsKey(id))report.withArray("cancellations").addObject().put("lessonId",id);
        return new Comparison(teacherChanges,periodMoves,roomMoves,report);
    }
    private static Map<String,AudienceSchedule.Session> portions(AudienceSchedule m){var result=new LinkedHashMap<String,AudienceSchedule.Session>();for(var s:m.sessions.values())result.putIfAbsent(s.portionId(),s);return result;}
    private static boolean samePortion(AudienceSchedule.Session before,AudienceSchedule.Session after){
        if(before.portionId().equals(after.portionId()))return true;
        if(before.legacy()!=null&&after.plan()!=null)for(JsonNode occurrence:after.plan().path("sourceFixedPlan").path("occurrences"))if(occurrence.path("id").asString().equals(before.id()))return true;return false;
    }
    public static boolean comparable(AudienceSchedule old,AudienceSchedule.Session before,AudienceSchedule current,AudienceSchedule.Session after){
        return samePortion(before,after)&&sameAudience(old,before,current,after);
    }
    public static boolean sameAudience(AudienceSchedule old,AudienceSchedule.Session before,AudienceSchedule current,AudienceSchedule.Session after){
        if(!before.classId().equals(after.classId())||!before.subjectId().equals(after.subjectId())||!before.audienceId().equals(after.audienceId())||before.size()!=after.size())return false;
        if(before.audienceId().equals("class:"+before.classId()))return true;
        var common=new HashSet<String>();for(var a:old.attendance.values())if(a.classId().equals(before.classId()))common.addAll(a.block().groupIds());var now=new HashSet<String>();for(var a:current.attendance.values())if(a.classId().equals(after.classId()))now.addAll(a.block().groupIds());common.retainAll(now);
        return composition(old,before,common).equals(composition(current,after,common));
    }
    private static Map<List<String>,Long> composition(AudienceSchedule model,AudienceSchedule.Session session,Set<String> common){
        var result=new HashMap<List<String>,Long>();
        for(String key:session.blockIds()){
            var block=model.attendance.get(key).block();var signature=block.groupIds().stream().filter(common::contains).sorted().toList();
            result.merge(signature,(long)block.size(),Math::addExact);
        }
        return result;
    }
}
