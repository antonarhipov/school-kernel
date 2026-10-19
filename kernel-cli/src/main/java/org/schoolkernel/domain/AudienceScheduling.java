package org.schoolkernel.domain;

import java.util.*;
import tools.jackson.databind.JsonNode;

/** Static hard-control checks shared by all version-two entry points. */
public final class AudienceScheduling {
    private AudienceScheduling() {}
    public record Activity(JsonNode plan, JsonNode portion, List<JsonNode> blocks) {}
    public static Map<String,Activity> activities(JsonNode d) {
        var result=new LinkedHashMap<String,Activity>();
        for(JsonNode p:d.path("audiencePlans"))for(JsonNode q:p.path("portions")) {
            var blocks=new ArrayList<JsonNode>();for(JsonNode b:q.has("blockRules")?q.path("blockRules"):p.path("blockRules"))blocks.add(b);
            result.put(q.path("id").asString(),new Activity(p,q,List.copyOf(blocks)));
        }
        return result;
    }
    private static void add(List<ValidationError> errors,String path,String message){errors.add(new ValidationError(path,List.of(),message));}
    public static List<ValidationError> validate(JsonNode d) {
        var errors=new ArrayList<ValidationError>();var activities=activities(d);var periods=AudienceDefinition.index(d.path("periods"));var rooms=AudienceDefinition.index(d.path("rooms"));var classes=AudienceDefinition.index(d.path("cohorts"));var subjects=AudienceDefinition.index(d.path("subjects"));
        var days=new HashSet<String>();for(JsonNode period:periods.values())days.add(period.path("weekday").asString());
        for(var entry:activities.entrySet()) {
            var a=entry.getValue();var p=a.plan();var q=a.portion();String path="/audiencePlans/"+p.path("id").asString()+"/portions/"+entry.getKey();
            long consumed=0;var lengths=new HashSet<Integer>();var blockIds=new HashSet<String>();
            for(JsonNode b:a.blocks()) {
                int length=b.path("length").asInt(),count=b.path("count").asInt();
                if(length<2||count<1||!Set.of("AT_LEAST","EXACTLY").contains(b.path("operator").asString()))add(errors,path,"Required blocks need exact length >=2, positive weekly frequency and At least/Exactly");
                if(!lengths.add(length)||!blockIds.add(b.path("id").asString()))add(errors,path,"Duplicate consecutive length or block identity");
                consumed+=(long)length*count;
                if(length>=2&&q.path("teacherPolicy").path("mode").asString().equals("sharing")&&!p.path("delivery").asString().equals("mixed")){
                    var totals=new HashMap<String,Long>();for(JsonNode share:q.path("teacherPolicy").path("shares"))totals.merge(share.path("teacherId").asString(),share.path("count").asLong(),Long::sum);
                    if(totals.values().stream().mapToLong(total->total/length).sum()<count)add(errors,path,"Fixed teacher portions cannot provide complete required consecutive blocks");
                }
                var subject=subjects.get(p.path("subjectId").asString());if(subject!=null&&subject.has("maxDailyLessonsPerCohort")&&subject.path("maxDailyLessonsPerCohort").asInt()<length)add(errors,path,"Subject daily maximum is below required block length");
            }
            int whole=p.path("delivery").asString().equals("whole")?p.path("weeklyTotal").asInt():p.path("delivery").asString().equals("grouped")?0:p.path("wholeClassCount").asInt();
            String audience=q.path("audienceId").asString();int received=audience.equals("class:"+p.path("classId").asString())?whole:p.path("overrides").path(audience).asInt(p.path("weeklyTotal").asInt());
            if(consumed>received)add(errors,path,"Required blocks consume "+consumed+" periods, exceeding audience teaching "+received);
            var candidates=AudienceDefinition.candidates(d,p,q);var policy=q.path("teacherPolicy");
            var selected=new LinkedHashSet<>(candidates);
            if(policy.path("mode").asString().equals("fixed"))selected=new LinkedHashSet<>(List.of(policy.path("fixedTeacherId").asString()));
            if(policy.path("mode").asString().equals("sharing")){selected.clear();for(JsonNode share:policy.path("shares"))selected.add(share.path("teacherId").asString());}
            var options=new HashMap<String,Set<String>>();var allSlots=new HashSet<String>();boolean anyRoom=false;
            for(String teacher:selected)if(candidates.contains(teacher)){
                var suitable=compatibleRooms(d,p,q,teacher);anyRoom|=!suitable.isEmpty();var slots=new HashSet<String>();
                JsonNode t=AudienceDefinition.index(d.path("teachers")).get(teacher),cohort=classes.get(p.path("classId").asString()),subject=subjects.get(p.path("subjectId").asString());
                if(t!=null&&cohort!=null&&subject!=null)for(JsonNode period:periods.values()){
                    String id=period.path("id").asString();
                    if(available(cohort,id)&&available(t,id)&&(subject.path("reservedPeriodsAllowed").asBoolean()||!AudienceDefinition.strings(d.path("reservedPeriodIds")).contains(id))&&suitable.stream().anyMatch(room->available(room,id)))slots.add(id);
                }
                options.put(teacher,slots);allSlots.addAll(slots);
            }
            if(!anyRoom)add(errors,path,"Audience has no compatible room for its actual size, capabilities and teacher/room restrictions");
            if(allSlots.size()<q.path("sessionIds").size())add(errors,path,"Audience sessions exceed compatible available teaching periods");
            if(policy.path("mode").asString().equals("automatic")&&options.values().stream().noneMatch(slots->slots.size()>=q.path("sessionIds").size()))add(errors,path,"No eligible teacher can provide the continuous weekly allocation");
            if(policy.path("mode").asString().equals("sharing"))for(JsonNode share:policy.path("shares"))if(options.getOrDefault(share.path("teacherId").asString(),Set.of()).size()<share.path("count").asInt())add(errors,path,"Fixed teacher-sharing portion exceeds compatible available periods");
            var inherited=p.has("allowedRoomIds")?AudienceDefinition.strings(p.path("allowedRoomIds")):new ArrayList<>(rooms.keySet());
            var allowed=new HashSet<>(inherited);if(q.has("allowedRoomIds")){var narrowed=AudienceDefinition.strings(q.path("allowedRoomIds"));if(!inherited.containsAll(narrowed))add(errors,path,"Narrowed rooms are outside inherited restrictions");allowed.retainAll(narrowed);}
            if(!rooms.keySet().containsAll(inherited))add(errors,path,"Unknown allowed room reference");
            if(allowed.isEmpty()||q.has("fixedRoomId")&&!allowed.contains(q.path("fixedRoomId").asString()))add(errors,path,"Empty room intersection or fixed room outside inherited restrictions");
        }
        for(JsonNode p:d.path("audiencePlans")) {
            var capabilities=AudienceDefinition.strings(p.path("requiredRoomCapabilityIds"));
            if(new HashSet<>(capabilities).size()!=capabilities.size()||capabilities.stream().anyMatch(id->!id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")))add(errors,"/audiencePlans/"+p.path("id").asString(),"Required capabilities need distinct valid identifiers");
        }
        var ruleIds=new HashSet<String>();
        for(JsonNode r:d.path("coordinationRules")) {
            String path="/coordinationRules/"+r.path("id").asString(),type=r.path("type").asString();int count=r.path("count").asInt();
            if(!ruleIds.add(r.path("id").asString()))add(errors,path,"Duplicate hard coordination identity");
            if(!classes.containsKey(r.path("classId").asString()))add(errors,path,"Unknown coordination class");
            for(String field:List.of("days","periodIds"))if(r.has(field)){
                var values=AudienceDefinition.strings(r.path(field));var known=field.equals("days")?days:periods.keySet();
                if(values.isEmpty()||!known.containsAll(values)||new HashSet<>(values).size()!=values.size())add(errors,path,"Explicit "+field+" must contain nonempty distinct known values");
            }
            if(type.equals("GAP_FREE"))continue;
            if(count<1)add(errors,path,"Coordination count must be a positive integer");
            if(r.path("activities").isEmpty()||Set.of("START_TOGETHER","SAME_DAY","DIFFERENT_DAYS","COVERAGE").contains(type)&&r.path("activities").size()<2)add(errors,path,"Select sufficient teaching activities for this control");
            var selected=new HashSet<String>();var selectedClasses=new HashSet<String>();
            for(JsonNode ref:r.path("activities")) {
                String id=ref.path("portionId").asString();var a=activities.get(id);
                if(!selected.add(id))add(errors,path,"An activity is selected more than once");
                if(a==null){add(errors,path,"Missing teaching activity "+id);continue;}
                selectedClasses.add(a.plan().path("classId").asString());
                JsonNode block=null;int available=a.portion().path("sessionIds").size(),length=1;
                if(ref.path("unit").asString().equals("BLOCK")) {
                    block=a.blocks().stream().filter(b->b.path("id").asString().equals(ref.path("blockRuleId").asString())).findFirst().orElse(null);
                    if(block==null){add(errors,path,"Missing required block "+ref.path("blockRuleId").asString());continue;}
                    available=block.path("count").asInt();length=block.path("length").asInt();
                } else if(!ref.path("unit").asString().equals("SESSION"))add(errors,path,"Unknown activity unit");
                if(count>available)add(errors,path,"Requested "+count+" distinct rounds exceeds "+available+" available activity occurrences for "+id);
                if(type.equals("FIXED")) {
                    JsonNode start=periods.get(r.path("periodId").asString());
                    if(start==null||!start.path("weekday").asString().equals(r.path("day").asString())||!fits(periods.values(),start,length))add(errors,path,"Fixed slot cannot hold the complete selected duration");
                }
            }
            if(type.equals("FIXED")&&count!=1)add(errors,path,"Fixed-slot control selects one occurrence; repeating fixed slots require separate controls");
            if(type.equals("DIFFERENT_DAYS")&&selected.size()>days.size())add(errors,path,"Different-days participants exceed available weekdays");
            if(type.equals("COVERAGE")&&(selectedClasses.size()!=1||!selectedClasses.contains(r.path("classId").asString())))add(errors,path,"Strict coverage requires activities of its selected parent class");
        }
        var forced=new HashMap<String,JsonNode>();
        for(JsonNode r:d.path("coordinationRules"))if(r.path("type").asString().equals("FIXED")&&r.path("count").asInt()==1)for(JsonNode ref:r.path("activities"))if(occurrenceCount(activities,ref)==1){
            String key=activityKey(ref);JsonNode prior=forced.put(key,r);
            if(prior!=null&&(!prior.path("day").equals(r.path("day"))||!prior.path("periodId").equals(r.path("periodId"))))add(errors,"/coordinationRules/"+r.path("id").asString(),"The only available occurrence is fixed to contradictory slots");
        }
        for(JsonNode r:d.path("coordinationRules")){
            String path="/coordinationRules/"+r.path("id").asString(),type=r.path("type").asString();var fixed=new ArrayList<JsonNode>();
            for(JsonNode ref:r.path("activities")){
                JsonNode f=forced.get(activityKey(ref));fixed.add(f);
                if(type.equals("ALLOWED")&&r.path("count").asInt()==occurrenceCount(activities,ref)&&f!=null&&(r.has("days")&&!AudienceDefinition.strings(r.path("days")).contains(f.path("day").asString())||r.has("periodIds")&&!AudienceDefinition.strings(r.path("periodIds")).contains(f.path("periodId").asString())))add(errors,path,"The required allowed set excludes a fixed occurrence");
            }
            if(r.path("count").asInt()==1&&fixed.size()>1&&fixed.stream().allMatch(Objects::nonNull)){
                long distinctDays=fixed.stream().map(f->f.path("day").asString()).distinct().count(),distinctStarts=fixed.stream().map(f->f.path("periodId").asString()).distinct().count();
                if(type.equals("START_TOGETHER")&&distinctStarts>1||type.equals("SAME_DAY")&&distinctDays>1||type.equals("DIFFERENT_DAYS")&&distinctDays!=fixed.size())add(errors,path,"Coordination contradicts selected fixed starts or weekdays");
            }
        }
        return errors;
    }
    public static boolean available(JsonNode resource,String period){return !resource.has("availablePeriodIds")||AudienceDefinition.strings(resource.path("availablePeriodIds")).contains(period);}
    public static List<JsonNode> compatibleRooms(JsonNode d,JsonNode p,JsonNode q,String teacher){
        var classes=AudienceDefinition.index(d.path("cohorts"));JsonNode c=classes.get(p.path("classId").asString());if(c==null)return List.of();
        JsonNode subject=AudienceDefinition.index(d.path("subjects")).get(p.path("subjectId").asString());if(subject==null)return List.of();
        int size=c.path("size").asInt();if(!q.path("audienceId").asString().equals("class:"+p.path("classId").asString())){
            size=0;for(JsonNode split:c.path("splits"))for(JsonNode group:split.path("groups"))if(group.path("id").equals(q.path("audienceId")))size=group.path("size").asInt();
        }
        var result=new ArrayList<JsonNode>();
        for(JsonNode room:d.path("rooms")){
            String id=room.path("id").asString();
            if(room.path("capacity").asInt()<size||!AudienceDefinition.strings(room.path("capabilityIds")).containsAll(AudienceDefinition.strings(p.path("requiredRoomCapabilityIds")))
                ||p.has("allowedRoomIds")&&!AudienceDefinition.strings(p.path("allowedRoomIds")).contains(id)||q.has("allowedRoomIds")&&!AudienceDefinition.strings(q.path("allowedRoomIds")).contains(id)
                ||q.has("fixedRoomId")&&!q.path("fixedRoomId").asString().equals(id)||subject.path("curatorLesson").asBoolean()&&c.has("homeRoomId")&&!c.path("homeRoomId").asString().equals(id))continue;
            boolean allowed=true;
            for(JsonNode rule:d.path("roomAssignments"))if(rule.path("subjectId").equals(p.path("subjectId"))&&(!rule.has("teacherId")||rule.path("teacherId").asString().equals(teacher))) {
                if(rule.path("useHomeRoom").asBoolean()?!c.path("homeRoomId").asString().equals(id):!AudienceDefinition.strings(rule.path("allowedRoomIds")).contains(id)){allowed=false;break;}
            }
            if(allowed)result.add(room);
        }
        return result;
    }
    private static String activityKey(JsonNode ref){return ref.path("portionId").asString()+"/"+ref.path("unit").asString()+"/"+ref.path("blockRuleId").asString();}
    private static int occurrenceCount(Map<String,Activity> activities,JsonNode ref){
        var a=activities.get(ref.path("portionId").asString());if(a==null)return 0;
        if(ref.path("unit").asString().equals("SESSION"))return a.portion().path("sessionIds").size();
        return a.blocks().stream().filter(b->b.path("id").asString().equals(ref.path("blockRuleId").asString())).mapToInt(b->b.path("count").asInt()).findFirst().orElse(0);
    }
    public static boolean fits(Collection<JsonNode> periods,JsonNode start,int length) {
        var day=periods.stream().filter(p->p.path("weekday").asString().equals(start.path("weekday").asString())).sorted(Comparator.comparingInt(p->p.path("order").asInt())).toList();int index=day.indexOf(start);
        if(index<0||length<1||index+length>day.size())return false;
        for(int i=index;i<index+length-1;i++)if(!day.get(i).path("adjacentToNext").asBoolean())return false;return true;
    }
}
