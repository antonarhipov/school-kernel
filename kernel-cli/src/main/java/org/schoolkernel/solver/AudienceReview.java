package org.schoolkernel.solver;

import java.util.*;
import org.schoolkernel.domain.*;
import tools.jackson.databind.JsonNode;

/** Independent complete-assignment review. Does not search or change supplied assignments. */
public final class AudienceReview {
    public record Conflict(String constraintId,long matchCount,List<List<String>> examples,List<String> lessonIds) {}
    public record Gap(String classId,String blockId,int studentCount,String day,List<String> periodIds,long studentPeriods) {}
    public record Workload(String teacherId,int actualSessions,Integer referenceWeeklyMaximum,boolean exceedsReference) {}
    public record Evaluation(boolean valid,Map<String,Long> hard,Map<String,Long> soft,List<Conflict> conflicts,
            long internalGapStudentPeriods,long coverageStudentPeriods,long ordinaryPreferencePenalty,List<Gap> gaps,
            List<Workload> workloads,List<AudienceCoordination.Match> coordinationMatches) {}
    private static final Set<String> RESOURCE_HARD=Set.of("hard.teacher-period","hard.room-period","hard.teacher-availability","hard.room-availability","hard.room-capacity","hard.room-capability","hard.period-lock","hard.room-lock","hard.room-assignment","hard.cohort-home-room","hard.reserved-period");
    private static final Set<String> ATTENDANCE_HARD=Set.of("hard.cohort-period","hard.cohort-availability","hard.cohort-late-start","hard.subject-day-edge","hard.subject-daily-limit","hard.subject-reserved-limit","hard.cohort-daily-spread","hard.consecutive-block");
    private static final Set<String> RESOURCE_SOFT=Set.of("soft.teacher-gap","soft.undesirable-period","soft.non-preferred-room");
    private static final Set<String> ATTENDANCE_SOFT=Set.of("soft.cohort-late-start","soft.cohort-week-balance","soft.series-same-day");
    public Evaluation evaluate(AudienceSchedule model){return evaluate(model,null);}
    public Evaluation evaluate(AudienceSchedule model,JsonNode assertedMatches){
        if(!model.errors.isEmpty())throw new IllegalArgumentException("Assignments must be structurally complete before audience review");
        var hard=new TreeMap<String,Long>();KernelCatalog.hardConstraintIds().forEach(id->hard.put(id,0L));
        for(String id:List.of("hard.teacher-policy","hard.audience-gap-free","hard.coordination","hard.session-lock"))hard.put(id,0L);
        var soft=new TreeMap<String,Long>();KernelCatalog.softConstraintIds().forEach(id->soft.put(id,0L));soft.put("soft.parallel-coverage",0L);
        var conflicts=new ArrayList<Conflict>();var gaps=new ArrayList<Gap>();var classLessons=new HashMap<String,List<PlanningLesson>>();
        var actual=model.projection(null);var global=new ScheduleEvaluator().evaluate(model.schedule(actual),model.resources.softWeights());
        include(model,actual,global,RESOURCE_HARD,RESOURCE_SOFT,1,hard,soft,conflicts);
        checkTeachers(model,hard,conflicts);checkLocks(model,hard,conflicts);
        long gapCost=0,coverage=0;
        for(var attendance:model.attendance.values()){
            var lessons=model.projection(attendance.key());classLessons.put(attendance.key(),lessons);
            var evaluation=new ScheduleEvaluator().evaluate(model.schedule(lessons),model.resources.softWeights());
            include(model,lessons,evaluation,ATTENDANCE_HARD,ATTENDANCE_SOFT,attendance.block().size(),hard,soft,conflicts);
            var byDay=new LinkedHashMap<String,List<Gap>>();
            for(Gap gap:gaps(model,attendance,lessons)){gaps.add(gap);gapCost=Math.addExact(gapCost,gap.studentPeriods());byDay.computeIfAbsent(gap.day(),ignored->new ArrayList<>()).add(gap);}
            var c=model.classes.get(attendance.classId());
            for(var day:byDay.entrySet()){
                long count=day.getValue().stream().mapToLong(g->g.periodIds().size()).sum();var ids=lessons.stream().filter(l->l.getPeriod().weekday().name().equals(day.getKey())).map(PlanningLesson::getId).toList();
                if(count>c.maxDailyGaps())add("hard.cohort-gap",Math.multiplyExact(count-c.maxDailyGaps(),attendance.block().size()),List.of(attendance.classId(),attendance.block().id(),day.getKey(),"Internal gaps="+count+"; maximum="+c.maxDailyGaps()),ids,hard,conflicts);
                for(JsonNode r:model.document.path("coordinationRules"))if(r.path("type").asString().equals("GAP_FREE")&&r.path("classId").asString().equals(attendance.classId())&&(!r.has("days")||AudienceDefinition.strings(r.path("days")).contains(day.getKey())))add("hard.audience-gap-free",Math.multiplyExact(count,attendance.block().size()),List.of(r.path("id").asString(),attendance.block().id(),day.getKey(),"Required zero internal gaps; observed="+count),ids,hard,conflicts);
            }
            var classPeriods=new HashSet<String>();for(var s:model.sessions.values())if(s.classId().equals(attendance.classId()))classPeriods.add(model.assignments.get(s.id()).path("periodId").asString());
            var occupied=new HashSet<String>();lessons.forEach(l->occupied.add(l.getPeriod().id()));classPeriods.removeAll(occupied);coverage=Math.addExact(coverage,Math.multiplyExact((long)classPeriods.size(),attendance.block().size()));
        }
        soft.put("soft.cohort-gap",gapCost);soft.put("soft.parallel-coverage",coverage);
        long ordinary=coverage;for(var entry:model.resources.softWeights().entrySet())if(!entry.getKey().equals("soft.cohort-gap"))ordinary=Math.addExact(ordinary,Math.multiplyExact(entry.getValue(),soft.getOrDefault(entry.getKey(),0L)));
        var coordination=new AudienceCoordination().review(model,classLessons,assertedMatches);
        for(var failure:coordination.conflicts())add("hard.coordination",1,List.of(failure.ruleId(),failure.message()),failure.lessonIds(),hard,conflicts);
        var workloads=new ArrayList<Workload>();
        for(var teacher:model.teachers.values()){
            int count=(int)model.assignments.values().stream().filter(a->a.path("teacherId").asString().equals(teacher.id())).count();var raw=AudienceDefinition.index(model.document.path("teachers")).get(teacher.id());Integer maximum=raw.has("referenceWeeklyMaximum")?raw.path("referenceWeeklyMaximum").asInt():null;workloads.add(new Workload(teacher.id(),count,maximum,maximum!=null&&count>maximum));
        }
        return new Evaluation(hard.values().stream().allMatch(n->n==0),Map.copyOf(hard),Map.copyOf(soft),List.copyOf(conflicts),gapCost,coverage,ordinary,List.copyOf(gaps),List.copyOf(workloads),coordination.matches());
    }
    private static void include(AudienceSchedule model,List<PlanningLesson> lessons,ScheduleEvaluator.Evaluation evaluation,Set<String> hardIds,Set<String> softIds,long factor,Map<String,Long> hard,Map<String,Long> soft,List<Conflict> conflicts){
        for(String id:softIds)soft.merge(id,Math.multiplyExact(evaluation.softMatchCounts().getOrDefault(id,0L),factor),Math::addExact);
        for(var diagnostic:HardConstraintDiagnostics.from(model.schedule(lessons),evaluation))if(hardIds.contains(diagnostic.constraintId())){
            long count=Math.multiplyExact(diagnostic.matchCount(),factor);hard.merge(diagnostic.constraintId(),count,Math::addExact);var affected=new TreeSet<String>();for(var example:diagnostic.examples())for(String id:example)if(model.sessions.containsKey(id))affected.add(id);if(affected.isEmpty())lessons.forEach(l->affected.add(l.getId()));conflicts.add(new Conflict(diagnostic.constraintId(),count,diagnostic.examples(),List.copyOf(affected)));
        }
    }
    private static void add(String id,long count,List<String> example,List<String> lessons,Map<String,Long> hard,List<Conflict> conflicts){hard.merge(id,count,Math::addExact);conflicts.add(new Conflict(id,count,List.of(example),List.copyOf(lessons)));}
    private static void checkTeachers(AudienceSchedule m,Map<String,Long> hard,List<Conflict> conflicts){
        var byPortion=new LinkedHashMap<String,List<AudienceSchedule.Session>>();
        for(var s:m.sessions.values())if(s.legacy()!=null){var a=m.assignments.get(s.id());if(!s.legacy().path("teacherId").equals(a.path("teacherId"))||a.has("allocationId")&&!a.path("allocationId").asString().equals(s.portionId()))add("hard.teacher-policy",1,List.of(s.id(),"Legacy fixed teacher/allocation cannot change without explicit policy adoption"),List.of(s.id()),hard,conflicts);}
        else byPortion.computeIfAbsent(s.portionId(),ignored->new ArrayList<>()).add(s);
        for(var entry:byPortion.entrySet()){
            var sample=entry.getValue().getFirst();var policy=sample.portion().path("teacherPolicy");String mode=policy.path("mode").asString();var chosen=new HashSet<String>();var shares=new HashMap<String,Integer>();
            for(var s:entry.getValue()){
                var a=m.assignments.get(s.id());String teacher=a.path("teacherId").asString(),allocation=a.path("allocationId").asString();chosen.add(teacher);
                if(!AudienceDefinition.candidates(m.document,s.plan(),s.portion()).contains(teacher))add("hard.teacher-policy",1,List.of(s.id(),teacher,"Chosen teacher is outside qualification, class eligibility or narrowed candidates"),List.of(s.id()),hard,conflicts);
                if(mode.equals("fixed")&&!teacher.equals(policy.path("fixedTeacherId").asString()))add("hard.teacher-policy",1,List.of(s.id(),teacher,"Hard fixed teacher differs"),List.of(s.id()),hard,conflicts);
                if(mode.equals("sharing")){
                    JsonNode share=AudienceDefinition.index(policy.path("shares")).get(allocation);
                    if(share==null||!share.path("teacherId").asString().equals(teacher))add("hard.teacher-policy",1,List.of(s.id(),allocation,"Chosen sharing allocation is missing or fixed to another teacher"),List.of(s.id()),hard,conflicts);else shares.merge(allocation,1,Integer::sum);
                }else if(!allocation.equals(entry.getKey()))add("hard.teacher-policy",1,List.of(s.id(),allocation,"Weekly allocation identity differs from the declared audience portion"),List.of(s.id()),hard,conflicts);
            }
            var ids=entry.getValue().stream().map(AudienceSchedule.Session::id).toList();
            if(mode.equals("automatic")&&chosen.size()!=1)add("hard.teacher-policy",1,List.of(entry.getKey(),"Automatic weekly continuity requires one teacher; observed="+chosen),ids,hard,conflicts);
            if(mode.equals("sharing"))for(JsonNode share:policy.path("shares"))if(shares.getOrDefault(share.path("id").asString(),0)!=share.path("count").asInt())add("hard.teacher-policy",1,List.of(share.path("id").asString(),"Fixed sharing count expected="+share.path("count").asInt()+"; observed="+shares.getOrDefault(share.path("id").asString(),0)),ids,hard,conflicts);
        }
    }
    private static void checkLocks(AudienceSchedule m,Map<String,Long> hard,List<Conflict> conflicts){for(JsonNode lock:m.document.path("sessionLocks")){String id=lock.path("sessionId").asString();var a=m.assignments.get(id);if(lock.has("periodId")&&!lock.path("periodId").equals(a.path("periodId"))||lock.has("roomId")&&!lock.path("roomId").equals(a.path("roomId")))add("hard.session-lock",1,List.of(id,"Explicit session lock differs from candidate assignment"),List.of(id),hard,conflicts);}}
    private static List<Gap> gaps(AudienceSchedule m,AudienceSchedule.Attendance attendance,List<PlanningLesson> lessons){
        var occupied=new HashSet<String>();lessons.forEach(l->occupied.add(l.getPeriod().id()));var c=m.classes.get(attendance.classId());var raw=AudienceDefinition.index(m.document.path("periods"));var result=new ArrayList<Gap>();var part=new ArrayList<SchoolDefinition.Period>();String day="";
        for(var p:m.resources.periods().stream().sorted(Comparator.comparing(SchoolDefinition.Period::weekday).thenComparingInt(SchoolDefinition.Period::order)).toList()){
            String current=p.weekday().name();if(!current.equals(day)||!c.availablePeriodIds().contains(p.id())){gapPart(attendance,day,part,occupied,result);part.clear();}day=current;
            if(c.availablePeriodIds().contains(p.id())&&(!m.resources.reservedPeriodIds().contains(p.id())||occupied.contains(p.id())))part.add(p);
            if(raw.get(p.id()).path("gapBoundaryAfter").asBoolean()){gapPart(attendance,day,part,occupied,result);part.clear();}
        }
        gapPart(attendance,day,part,occupied,result);return result;
    }
    private static void gapPart(AudienceSchedule.Attendance a,String day,List<SchoolDefinition.Period> part,Set<String> occupied,List<Gap> result){
        int first=-1,last=-1;for(int i=0;i<part.size();i++)if(occupied.contains(part.get(i).id())){if(first<0)first=i;last=i;}if(first<0||last<=first)return;
        var empty=new ArrayList<String>();for(int i=first+1;i<last;i++)if(!occupied.contains(part.get(i).id()))empty.add(part.get(i).id());if(!empty.isEmpty())result.add(new Gap(a.classId(),a.block().id(),a.block().size(),day,List.copyOf(empty),Math.multiplyExact((long)empty.size(),a.block().size())));
    }
}
