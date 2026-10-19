package org.schoolkernel.solver;

import java.util.*;
import org.schoolkernel.contract.JsonSupport;
import org.schoolkernel.domain.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.*;

/** Bounded finite search over actual sessions. Only independently reviewed complete leaves compete. */
public final class AudienceSolver {
    public record Outcome(ArrayNode assignments,String terminationReason,long visitedNodes,long completeCandidates,
            long reviewedCandidates,long unestablishedReviews,boolean searchExhausted,List<AudienceReview.Conflict> diagnostics) {}
    private record TeacherOption(String teacher,String allocation,List<String> periods,List<String> rooms) {}
    private record Choice(String teacher,String allocation,String period,String room) {}
    private record Domain(AudienceSchedule.Session session,List<TeacherOption> teachers,long size) {
        Choice at(long index){
            for(var teacher:teachers){long count=(long)teacher.periods().size()*teacher.rooms().size();
                if(index<count)return new Choice(teacher.teacher(),teacher.allocation(),teacher.periods().get((int)(index/teacher.rooms().size())),teacher.rooms().get((int)(index%teacher.rooms().size())));index-=count;
            }throw new IndexOutOfBoundsException();
        }
    }
    private static final class BudgetEnd extends RuntimeException {}
    private final AudienceSchedule model,baseline;
    private final SolverAdapter.ExecutionControls controls;
    private final long deadline;
    private final Random random;
    private final Map<String,JsonNode> periodRows,roomRows;
    private final Map<String,String> comparableTeachers=new HashMap<>();
    private final Map<String,JsonNode> comparableSessions=new HashMap<>();
    private long nodes,complete,reviewed,unestablished;
    private String termination="SEARCH_EXHAUSTED";
    private ArrayNode best;
    private long[] bestScore;
    private List<AudienceReview.Conflict> diagnostics=List.of();
    private final Map<String,Long> pruned=new TreeMap<>();
    private final Map<String,List<List<String>>> pruneExamples=new TreeMap<>();

    public AudienceSolver(AudienceSchedule model,AudienceSchedule baseline,SolverAdapter.ExecutionControls controls){
        this.model=model;this.baseline=baseline;this.controls=controls;random=new Random(controls.seed());
        deadline=controls.timeLimit()==null?Long.MAX_VALUE:System.nanoTime()+controls.timeLimit().toNanos();
        periodRows=AudienceDefinition.index(model.document.path("periods"));roomRows=AudienceDefinition.index(model.document.path("rooms"));
        if(baseline!=null)for(var s:model.sessions.values()){
            var before=baseline.sessions.get(s.id());
            if(before!=null&&AudienceComparison.comparable(baseline,before,model,s))comparableSessions.put(s.id(),baseline.assignments.get(s.id()));
            if(s.automatic())for(var old:baseline.sessions.values())if(old.automatic()&&old.portionId().equals(s.portionId())&&AudienceComparison.sameAudience(baseline,old,model,s)){
                comparableTeachers.put(s.portionId(),baseline.assignments.get(old.id()).path("teacherId").asString());break;
            }
        }
    }
    public Outcome solve(){
        boolean exhausted=false;
        try{
            var domains=new ArrayList<Domain>();for(var session:model.sessions.values()){checkTime();var domain=domain(session);domains.add(domain);if(domain.size()==0)reject("hard.session-lock",List.of(session.id(),"Hard availability/lock/room requirements leave no compatible actual-session placement"));}
            domains.sort(Comparator.comparingLong(Domain::size).thenComparing(d->d.session().portionId()).thenComparing(d->d.session().id()));
            // Iterative search avoids a session-count-dependent Java stack limit.
            var cursors=new long[domains.size()];int depth=0;
            if(domains.isEmpty()){consider();termination="EMPTY_PROBLEM";exhausted=true;}
            else while(depth>=0){
                checkTime();
                if(depth==domains.size()){
                    consider();depth--;model.assignments.remove(domains.get(depth).session().id());continue;
                }
                var domain=domains.get(depth);
                if(cursors[depth]>=domain.size()){
                    cursors[depth]=0;depth--;if(depth>=0)model.assignments.remove(domains.get(depth).session().id());continue;
                }
                tick();Choice choice=domain.at(cursors[depth]++);
                if(!compatible(domain.session(),choice))continue;
                model.assignments.put(domain.session().id(),assignment(domain.session(),choice));
                if(bestScore!=null&&compare(lowerBound(),bestScore)>0){model.assignments.remove(domain.session().id());continue;}
                depth++;
            }
            if(!domains.isEmpty())exhausted=unestablished==0;
        }catch(BudgetEnd ended){/* Best complete checked candidate remains publishable. */}
        finally{model.assignments.clear();}
        if(best==null&&diagnostics.isEmpty())diagnostics=pruned.entrySet().stream().map(e->new AudienceReview.Conflict(e.getKey(),e.getValue(),List.copyOf(pruneExamples.get(e.getKey())),List.of())).toList();
        return new Outcome(best,termination,nodes,complete,reviewed,unestablished,exhausted,diagnostics);
    }
    private void checkTime(){
        if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("Audience search cancelled");
        if(System.nanoTime()>=deadline){termination="TIME_LIMIT";throw new BudgetEnd();}
    }
    private void tick(){checkTime();if(controls.stepLimit()!=null&&nodes>=controls.stepLimit()){termination="STEP_LIMIT";throw new BudgetEnd();}nodes++;}
    private Domain domain(AudienceSchedule.Session s){
        var options=new ArrayList<TeacherOption>();var choices=new ArrayList<String>();
        if(s.legacy()!=null){String teacher=s.legacy().path("teacherId").asString();choices.add(teacher);}
        else{
            var policy=s.portion().path("teacherPolicy");String mode=policy.path("mode").asString();
            if(mode.equals("sharing"))for(JsonNode share:policy.path("shares"))choices.add(share.path("id").asString());
            else if(mode.equals("fixed"))choices.add(policy.path("fixedTeacherId").asString());
            else choices.addAll(AudienceDefinition.candidates(model.document,s.plan(),s.portion()));
        }
        Collections.shuffle(choices,random);String preferred=comparableTeachers.get(s.portionId());if(preferred!=null&&choices.remove(preferred))choices.addFirst(preferred);
        long size=0;
        for(String key:choices){
            checkTime();String teacher=key,allocation=s.portionId();
            if(s.portion()!=null&&s.portion().path("teacherPolicy").path("mode").asString().equals("sharing")){
                var share=AudienceDefinition.index(s.portion().path("teacherPolicy").path("shares")).get(key);teacher=share.path("teacherId").asString();allocation=key;
            }
            var t=model.teachers.get(teacher);var c=model.classes.get(s.classId());var subject=model.subjects.get(s.subjectId());
            var rooms=new ArrayList<String>();
            if(s.portion()!=null)for(JsonNode r:AudienceScheduling.compatibleRooms(model.document,s.plan(),s.portion(),teacher))rooms.add(r.path("id").asString());
            else for(var r:model.resources.rooms())if(r.capacity()>=s.size()&&r.capabilityIds().containsAll(new HashSet<>(AudienceDefinition.strings(s.legacy().path("requiredRoomCapabilityIds")))))rooms.add(r.id());
            var periods=new ArrayList<String>();
            for(var p:model.resources.periods())if(t.availablePeriodIds().contains(p.id())&&c.availablePeriodIds().contains(p.id())&&(subject.reservedPeriodsAllowed()||!model.resources.reservedPeriodIds().contains(p.id()))&&allowedStart(s,p.id()))periods.add(p.id());
            String periodLock=s.legacy()==null?null:optional(s.legacy(),"periodLock"),roomLock=s.legacy()==null?null:optional(s.legacy(),"roomLock");
            for(JsonNode lock:model.document.path("sessionLocks"))if(lock.path("sessionId").asString().equals(s.id())){
                if(lock.has("periodId"))periodLock=lock.path("periodId").asString();if(lock.has("roomId"))roomLock=lock.path("roomId").asString();
            }
            if(periodLock!=null)periods.retainAll(List.of(periodLock));if(roomLock!=null)rooms.retainAll(List.of(roomLock));
            // Prefer baseline values, then declared grid/room order. Seed only breaks equivalent teacher choices.
            var old=comparableSessions.get(s.id());if(old!=null){first(periods,old.path("periodId").asString());first(rooms,old.path("roomId").asString());}
            if(!periods.isEmpty()&&!rooms.isEmpty()){options.add(new TeacherOption(teacher,allocation,List.copyOf(periods),List.copyOf(rooms)));size=Math.addExact(size,(long)periods.size()*rooms.size());}
        }
        return new Domain(s,List.copyOf(options),size);
    }
    private boolean allowedStart(AudienceSchedule.Session s,String period){
        if(s.portion()==null||s.portion().path("sessionIds").size()!=1)return true;
        for(JsonNode rule:model.document.path("coordinationRules"))for(JsonNode ref:rule.path("activities"))if(ref.path("portionId").asString().equals(s.portionId())&&ref.path("unit").asString().equals("SESSION")){
            String type=rule.path("type").asString();if(type.equals("FIXED")&&!rule.path("periodId").asString().equals(period))return false;
            if(type.equals("ALLOWED")&&(rule.has("periodIds")&&!AudienceDefinition.strings(rule.path("periodIds")).contains(period)||rule.has("days")&&!AudienceDefinition.strings(rule.path("days")).contains(periodRows.get(period).path("weekday").asString())))return false;
        }return true;
    }
    private boolean compatible(AudienceSchedule.Session s,Choice choice){
        if(!AudienceScheduling.available(roomRows.get(choice.room()),choice.period()))return reject("hard.room-availability",List.of(s.id(),choice.room(),choice.period()));
        int shareCount=0;var subject=model.subjects.get(s.subjectId());String day=periodRows.get(choice.period()).path("weekday").asString();
        for(var entry:model.assignments.entrySet()){
            var other=model.sessions.get(entry.getKey());var a=entry.getValue();boolean overlaps=!Collections.disjoint(s.blockIds(),other.blockIds());
            if(a.path("periodId").asString().equals(choice.period())){
                if(a.path("teacherId").asString().equals(choice.teacher()))return reject("hard.teacher-period",List.of(s.id(),other.id(),choice.teacher(),choice.period()));
                if(a.path("roomId").asString().equals(choice.room()))return reject("hard.room-period",List.of(s.id(),other.id(),choice.room(),choice.period()));
                if(overlaps)return reject("hard.cohort-period",List.of(s.id(),other.id(),s.audienceId(),other.audienceId(),choice.period()));
            }
            if(s.portionId().equals(other.portionId())){
                if(s.automatic()&&!a.path("teacherId").asString().equals(choice.teacher()))return reject("hard.teacher-policy",List.of(s.portionId(),s.id(),other.id(),"Weekly teacher continuity"));
                if(a.path("allocationId").asString().equals(choice.allocation()))shareCount++;
            }
        }
        // Partial monotone bounds are per attending block, not sums across disjoint groups.
        for(String block:s.blockIds()){
            int daily=0,reserved=0;
            for(var entry:model.assignments.entrySet()){
                var other=model.sessions.get(entry.getKey());if(!other.blockIds().contains(block)||!other.subjectId().equals(s.subjectId()))continue;
                String period=entry.getValue().path("periodId").asString();if(periodRows.get(period).path("weekday").asString().equals(day))daily++;if(model.resources.reservedPeriodIds().contains(period))reserved++;
            }
            if(daily>=subject.maxDailyLessonsPerCohort())return reject("hard.subject-daily-limit",List.of(s.id(),s.subjectId(),block,day));
            if(model.resources.reservedPeriodIds().contains(choice.period())&&reserved>=subject.maxWeeklyReservedLessonsPerCohort())return reject("hard.subject-reserved-limit",List.of(s.id(),s.subjectId(),block,choice.period()));
            if(forbiddenRun(s,choice,block))return reject("hard.consecutive-block",List.of(s.id(),s.subjectId(),block,"Partial run exceeds every declared exact length"));
        }
        if(s.portion()!=null&&s.portion().path("teacherPolicy").path("mode").asString().equals("sharing")){
            var share=AudienceDefinition.index(s.portion().path("teacherPolicy").path("shares")).get(choice.allocation());if(shareCount>=share.path("count").asInt())return reject("hard.teacher-policy",List.of(s.id(),choice.allocation(),"Declared sharing count exhausted"));
        }
        return singletonCoordination(s,choice);
    }
    private boolean forbiddenRun(AudienceSchedule.Session s,Choice choice,String block){
        var policy=model.policy(s,block);int maximum=policy.rules().stream().mapToInt(SchoolDefinition.ConsecutiveBlock::length).max().orElse(1);
        var occupied=new HashSet<String>();occupied.add(choice.period());
        for(var entry:model.assignments.entrySet()){var other=model.sessions.get(entry.getKey());if(other.subjectId().equals(s.subjectId())&&other.blockIds().contains(block))occupied.add(entry.getValue().path("periodId").asString());}
        int run=0;SchoolDefinition.Period previous=null;
        for(var period:policy.grid()){
            if(previous==null||previous.weekday()!=period.weekday()||!previous.adjacentToNext()||!occupied.contains(period.id()))run=0;
            if(occupied.contains(period.id())){run++;if(run>maximum)return true;}previous=period;
        }return false;
    }
    private boolean singletonCoordination(AudienceSchedule.Session s,Choice choice){
        if(s.portion()==null||s.portion().path("sessionIds").size()!=1)return true;
        for(JsonNode rule:model.document.path("coordinationRules")){
            String type=rule.path("type").asString();if(!Set.of("START_TOGETHER","SAME_DAY","DIFFERENT_DAYS").contains(type))continue;
            boolean selected=false;for(JsonNode ref:rule.path("activities"))if(ref.path("portionId").asString().equals(s.portionId())&&ref.path("unit").asString().equals("SESSION"))selected=true;
            if(!selected)continue;
            for(JsonNode ref:rule.path("activities"))for(var other:model.sessions.values())if(other.portionId().equals(ref.path("portionId").asString())&&other.portion().path("sessionIds").size()==1&&ref.path("unit").asString().equals("SESSION")&&model.assignments.containsKey(other.id())){
                var a=model.assignments.get(other.id());boolean sameDay=periodRows.get(a.path("periodId").asString()).path("weekday").equals(periodRows.get(choice.period()).path("weekday"));
                if(type.equals("START_TOGETHER")&&!a.path("periodId").asString().equals(choice.period())||type.equals("SAME_DAY")&&!sameDay||type.equals("DIFFERENT_DAYS")&&sameDay)return false;
            }
        }return true;
    }
    private long[] lowerBound(){
        long teachers=0,periods=0,rooms=0;var seen=new HashSet<String>();
        for(var entry:model.assignments.entrySet()){
            var s=model.sessions.get(entry.getKey());var a=entry.getValue();String teacher=comparableTeachers.get(s.portionId());if(teacher!=null&&seen.add(s.portionId())&&!teacher.equals(a.path("teacherId").asString()))teachers++;
            var old=comparableSessions.get(s.id());if(old!=null){if(!old.path("periodId").equals(a.path("periodId")))periods++;else if(!old.path("roomId").equals(a.path("roomId")))rooms++;}
        }return new long[]{teachers,periods,rooms,0,0};
    }
    private void consider(){
        checkTime();complete++;
        try{
            var evaluation=new AudienceReview().evaluate(model);reviewed++;if(!evaluation.valid()){diagnostics=evaluation.conflicts();return;}
            var score=lowerBound();score[3]=evaluation.internalGapStudentPeriods();score[4]=evaluation.ordinaryPreferencePenalty();
            if(bestScore==null||compare(score,bestScore)<0){bestScore=score;best=JsonSupport.mapper().createArrayNode();model.assignments.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->best.add(e.getValue().deepCopy()));}
        }catch(AudienceCoordination.ReviewLimit limit){unestablished++;}
    }
    public static int compare(long[] a,long[] b){for(int i=0;i<a.length;i++){int result=Long.compare(a[i],b[i]);if(result!=0)return result;}return 0;}
    private static ObjectNode assignment(AudienceSchedule.Session s,Choice c){
        var row=JsonSupport.mapper().createObjectNode().put("lessonId",s.id()).put("subjectId",s.subjectId()).put("cohortId",s.classId()).put("teacherId",c.teacher()).put("periodId",c.period()).put("roomId",c.room());
        if(s.portion()!=null)row.put("audienceId",s.audienceId()).put("allocationId",c.allocation());return row;
    }
    private static String optional(JsonNode node,String field){return node.has(field)?node.path(field).asString():null;}
    private static void first(List<String> values,String preferred){if(values.remove(preferred))values.addFirst(preferred);}
    private boolean reject(String constraint,List<String> example){pruned.merge(constraint,1L,Math::addExact);var examples=pruneExamples.computeIfAbsent(constraint,ignored->new ArrayList<>());if(examples.size()<5)examples.add(example);return false;}
}
