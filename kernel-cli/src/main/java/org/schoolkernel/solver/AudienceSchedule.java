package org.schoolkernel.solver;

import java.util.*;
import org.schoolkernel.domain.*;
import tools.jackson.databind.JsonNode;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;

/** Exact actual-session model. Attendance projections never duplicate resource accounting. */
public final class AudienceSchedule {
    public record Session(String id,String classId,String subjectId,String audienceId,String portionId,int size,
            Set<String> blockIds,JsonNode plan,JsonNode portion,JsonNode legacy) {
        public boolean automatic(){return portion!=null&&portion.path("teacherPolicy").path("mode").asString().equals("automatic");}
    }
    public record Attendance(String classId,AudienceDefinition.Block block) {public String key(){return org.schoolkernel.contract.JsonSupport.mapper().writeValueAsString(List.of(classId,block.id()));}}
    public final JsonNode document;
    public final SchoolDefinition resources;
    public final Map<String,Session> sessions=new LinkedHashMap<>();
    public final Map<String,Attendance> attendance=new LinkedHashMap<>();
    public final Map<String,JsonNode> assignments=new LinkedHashMap<>();
    public final List<ValidationError> errors=new ArrayList<>();
    public final List<PeriodValue> periods;
    public final List<RoomValue> rooms;
    public final Map<String,SchoolDefinition.Cohort> classes=new LinkedHashMap<>();
    public final Map<String,SchoolDefinition.Subject> subjects=new LinkedHashMap<>();
    public final Map<String,SchoolDefinition.Teacher> teachers=new LinkedHashMap<>();
    public AudienceSchedule(JsonNode document,SchoolDefinition resources){
        this.document=document;this.resources=resources;
        resources.cohorts().forEach(c->classes.put(c.id(),c));resources.subjects().forEach(s->subjects.put(s.id(),s));resources.teachers().forEach(t->teachers.put(t.id(),t));
        periods=resources.periods().stream().map(p->new PeriodValue(p.id(),p.weekday(),p.order(),resources.reservedPeriodIds().contains(p.id()))).toList();
        rooms=resources.rooms().stream().map(r->new RoomValue(r.id(),r.capacity(),r.capabilityIds(),r.availablePeriodIds())).toList();
        for(JsonNode c:document.path("cohorts")){
            var used=AudienceDefinition.usedSplits(document,c.path("id").asString());
            for(var b:AudienceDefinition.blocks(c,used,errors)){var a=new Attendance(c.path("id").asString(),b);attendance.put(a.key(),a);}
        }
        for(JsonNode l:document.path("lessons")){
            String c=l.path("cohortId").asString();var blocks=blocks(c,"class:"+c);
            sessions.put(l.path("id").asString(),new Session(l.path("id").asString(),c,l.path("subjectId").asString(),"class:"+c,"legacy:"+l.path("id").asString(),classes.get(c).size(),blocks,null,null,l));
        }
        for(JsonNode p:document.path("audiencePlans"))for(JsonNode q:p.path("portions")){
            String c=p.path("classId").asString(),audience=q.path("audienceId").asString();var blocks=blocks(c,audience);int size=blocks.stream().map(attendance::get).mapToInt(a->a.block().size()).sum();
            for(String id:AudienceDefinition.strings(q.path("sessionIds")))sessions.put(id,new Session(id,c,p.path("subjectId").asString(),audience,q.path("id").asString(),size,blocks,p,q,null));
        }
    }
    private Set<String> blocks(String classId,String audience){var result=new LinkedHashSet<String>();for(var a:attendance.values())if(a.classId().equals(classId)&&(audience.equals("class:"+classId)||a.block().groupIds().contains(audience)))result.add(a.key());return Set.copyOf(result);}
    public void assign(JsonNode rows){
        if(!rows.isArray()){error("/assignments",List.of(),"Expected a complete assignment array");return;}
        for(JsonNode row:rows){String id=row.path("lessonId").asString();Session s=sessions.get(id);
            if(s==null||assignments.containsKey(id)){error("/assignments",List.of(id),"Unknown or duplicated session identity");continue;}
            var required=new HashSet<>(List.of("lessonId","subjectId","cohortId","teacherId","periodId","roomId"));if(s.portion()!=null)required.addAll(List.of("audienceId","allocationId"));
            var permitted=new HashSet<>(required);permitted.addAll(List.of("audienceId","allocationId"));
            boolean shape=row.isObject()&&required.stream().allMatch(k->row.path(k).isTextual());for(var entry:row.properties())if(!permitted.contains(entry.getKey())||!entry.getValue().isTextual())shape=false;
            if(!shape){error("/assignments",List.of(id),"Every actual session requires exact subject, class, audience, weekly allocation, teacher, period and room identities");continue;}
            if(!s.classId().equals(row.path("cohortId").asString())||!s.subjectId().equals(row.path("subjectId").asString())||row.has("audienceId")&&!s.audienceId().equals(row.path("audienceId").asString()))error("/assignments",List.of(id),"Assignment audience/subject/class differs from its stable required session");
            if(!teachers.containsKey(row.path("teacherId").asString())||periods.stream().noneMatch(p->p.id().equals(row.path("periodId").asString()))||rooms.stream().noneMatch(r->r.id().equals(row.path("roomId").asString())))error("/assignments",List.of(id),"Unknown teacher, period or room identity");
            assignments.put(id,row.deepCopy());
        }
        for(String id:sessions.keySet())if(!assignments.containsKey(id))error("/assignments",List.of(id),"Required session is missing from the complete timetable");
    }
    public void error(String path,List<String> ids,String message){errors.add(new ValidationError(path,ids,message));}
    public List<PlanningLesson> projection(String block){
        var result=new ArrayList<PlanningLesson>();
        for(Session s:sessions.values())if(block==null||s.blockIds().contains(block)){
            JsonNode a=assignments.get(s.id());if(a==null)continue;
            var c=classes.get(s.classId());var t=teachers.get(a.path("teacherId").asString());if(t==null)continue;
            var required=s.legacy()!=null?set(s.legacy().path("requiredRoomCapabilityIds")):set(s.plan().path("requiredRoomCapabilityIds"));
            var preferred=s.legacy()!=null?set(s.legacy().path("preferredRoomIds")):Set.<String>of();
            if(preferred.isEmpty()&&c.homeRoomId()!=null){var home=rooms.stream().filter(r->r.id().equals(c.homeRoomId())).findFirst().orElse(null);if(home!=null&&home.capacity()>=s.size()&&home.capabilityIds().containsAll(required))preferred=Set.of(home.id());}
            JsonNode lock=null;for(JsonNode l:document.path("sessionLocks"))if(l.path("sessionId").asString().equals(s.id()))lock=l;
            String periodLock=s.legacy()!=null?optional(s.legacy(),"periodLock"):lock==null?null:optional(lock,"periodId");
            String roomLock=s.legacy()!=null?optional(s.legacy(),"roomLock"):s.portion().has("fixedRoomId")?s.portion().path("fixedRoomId").asString():lock==null?null:optional(lock,"roomId");
            var lesson=new PlanningLesson(s.id(),s.subjectId(),block==null?s.classId():block,s.size(),t.id(),s.legacy()!=null?optional(s.legacy(),"seriesId"):s.plan().path("id").asString(),t.availablePeriodIds(),t.undesirablePeriodIds(),c.availablePeriodIds(),c.undesirablePeriodIds(),s.legacy()!=null?set(s.legacy().path("undesirablePeriodIds")):Set.of(),required,preferred,periodLock,roomLock,null,null,periods,c.maxDailyLessonSpread(),c.maxDailyGaps(),PlacementRules.of(c,subjects.get(s.subjectId())));
            var allowed=s.portion()!=null?AudienceScheduling.compatibleRooms(document,s.plan(),s.portion(),t.id()).stream().map(r->r.path("id").asString()).collect(java.util.stream.Collectors.toSet()):legacyRooms(s,t.id());
            lesson.setRoomAssignment(allowed,List.of("audience-resource-policy"));
            if(block!=null)lesson.setConsecutivePolicy(policy(s,block));
            lesson.setPeriod(periods.stream().filter(p->p.id().equals(a.path("periodId").asString())).findFirst().orElse(null));lesson.setRoom(rooms.stream().filter(r->r.id().equals(a.path("roomId").asString())).findFirst().orElse(null));result.add(lesson);
        }
        return result;
    }
    private Set<String> legacyRooms(Session s,String teacher){
        var allowed=new HashSet<String>();for(var r:resources.rooms())allowed.add(r.id());
        for(var rule:resources.roomAssignments())if(rule.subjectId().equals(s.subjectId())&&(rule.teacherId()==null||rule.teacherId().equals(teacher)))allowed.retainAll(rule.useHomeRoom()?classes.get(s.classId()).homeRoomId()==null?Set.of():Set.of(classes.get(s.classId()).homeRoomId()):rule.allowedRoomIds());return allowed;
    }
    public ConsecutivePolicy policy(Session session,String block){
        if(session.legacy()!=null)return ConsecutivePolicy.of(resources,session.classId(),session.subjectId());
        var p=session.plan();JsonNode selected=null,whole=null;
        for(JsonNode q:p.path("portions"))if(q.path("audienceId").asString().equals("class:"+session.classId()))whole=q;else if(attendance.get(block).block().groupIds().contains(q.path("audienceId").asString()))selected=q;
        JsonNode rules=selected!=null&&selected.has("blockRules")?selected.path("blockRules"):whole!=null&&whole.has("blockRules")?whole.path("blockRules"):p.path("blockRules");
        var blocks=new ArrayList<SchoolDefinition.ConsecutiveBlock>();for(JsonNode r:rules)blocks.add(new SchoolDefinition.ConsecutiveBlock(r.path("id").asString(),block,session.subjectId(),r.path("length").asInt(),r.path("operator").asString(),r.path("count").asInt()));
        return new ConsecutivePolicy(block,session.subjectId(),resources.periods().stream().sorted(Comparator.comparing(SchoolDefinition.Period::weekday).thenComparingInt(SchoolDefinition.Period::order)).toList(),blocks);
    }
    public SchoolSchedule schedule(List<PlanningLesson> lessons){return new SchoolSchedule(periods,rooms,lessons,ConstraintWeightOverrides.none());}
    private static Set<String> set(JsonNode rows){return new HashSet<>(AudienceDefinition.strings(rows));}
    private static String optional(JsonNode row,String field){return row.has(field)?row.path(field).asString():null;}
}
