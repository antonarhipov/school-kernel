package org.schoolkernel.domain;

import java.util.*;
import org.schoolkernel.contract.JsonSupport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Version-two audience and weekly teacher-policy semantics, without assignment search. */
public final class AudienceDefinition {
    private AudienceDefinition() {}
    public static boolean enabled(JsonNode definition) { return definition.path("schemaVersion").asInt() == 2; }
    public static ObjectNode legacyResources(JsonNode original) {
        ObjectNode d=(ObjectNode)original.deepCopy();d.put("schemaVersion",1);d.put("catalogVersion",10);d.remove("audiencePlans");d.remove("coordinationRules");d.remove("sessionLocks");for(JsonNode period:d.path("periods"))((ObjectNode)period).remove("gapBoundaryAfter");
        for(JsonNode c:d.path("cohorts")){var row=(ObjectNode)c;row.remove("splits");row.remove("relationships");if(!row.has("maxDailyGaps"))row.put("maxDailyGaps",Integer.MAX_VALUE);}
        for(JsonNode t:d.path("teachers")){((ObjectNode)t).remove("eligibleClassIdsBySubject");((ObjectNode)t).remove("referenceWeeklyMaximum");}
        return d;
    }
    public record Block(String id,int size,Set<String> groupIds) {}
    public static Map<String,JsonNode> index(JsonNode rows){var result=new LinkedHashMap<String,JsonNode>();for(JsonNode row:rows)result.put(row.path("id").asString(),row);return result;}
    public static List<String> strings(JsonNode rows){var result=new ArrayList<String>();for(JsonNode row:rows)result.add(row.asString());return result;}
    private static ValidationError error(String path,String message){return new ValidationError(path,List.of(),message);}
    private static boolean nonnegative(JsonNode n){return n.isIntegralNumber()&&n.canConvertToInt()&&n.asInt()>=0;}
    public static List<ValidationError> validate(JsonNode definition){
        var errors=new ArrayList<ValidationError>();var classes=index(definition.path("cohorts"));var subjects=index(definition.path("subjects"));var teachers=index(definition.path("teachers"));
        var sessionIds=new HashSet<String>();for(JsonNode l:definition.path("lessons"))sessionIds.add(l.path("id").asString());
        var planIds=new HashSet<String>();var allocationIds=new HashSet<String>();for(JsonNode legacy:definition.path("lessons"))allocationIds.add("legacy:"+legacy.path("id").asString());var classSubjects=new HashSet<String>();
        for(JsonNode l:definition.path("lessons"))classSubjects.add(l.path("cohortId").asString()+"\u0000"+l.path("subjectId").asString());
        for(JsonNode teacher:definition.path("teachers"))for(var entry:teacher.path("eligibleClassIdsBySubject").properties()){
            if(!subjects.containsKey(entry.getKey()))errors.add(error("/teachers/"+teacher.path("id").asString(),"Unknown eligibility subject "+entry.getKey()));
            for(String id:strings(entry.getValue()))if(!classes.containsKey(id))errors.add(error("/teachers/"+teacher.path("id").asString(),"Unknown eligible class "+id));
        }
        for(JsonNode p:definition.path("audiencePlans")){
            String path="/audiencePlans/"+p.path("id").asString(),classId=p.path("classId").asString(),subjectId=p.path("subjectId").asString();
            if(!planIds.add(p.path("id").asString()))errors.add(error(path,"Duplicate teaching-plan identity"));
            JsonNode c=classes.get(classId);if(c==null||!subjects.containsKey(subjectId)){errors.add(error(path,"Unknown class or subject"));continue;}
            if(!classSubjects.add(classId+"\u0000"+subjectId))errors.add(error(path,"Duplicate class/subject demand, including fixed legacy teaching"));
            String delivery=p.path("delivery").asString();int total=p.path("weeklyTotal").asInt();int whole=delivery.equals("whole")?total:delivery.equals("grouped")?0:p.path("wholeClassCount").asInt();
            var expected=new LinkedHashMap<String,Integer>();if(whole>0)expected.put("class:"+classId,whole);
            if(!delivery.equals("whole")){
                JsonNode split=index(c.path("splits")).get(p.path("splitId").asString());
                if(split==null){errors.add(error(path,"Missing class split"));continue;}
                for(JsonNode g:split.path("groups")){
                    String id=g.path("id").asString();int count=p.path("overrides").has(id)?p.path("overrides").path(id).asInt():total;
                    if(count<whole)errors.add(error(path,"Whole-class contribution "+whole+" exceeds group "+id+" total "+count));
                    else if(count>whole)expected.put(id,count-whole);
                }
                for(var entry:p.path("overrides").properties())if(!index(split.path("groups")).containsKey(entry.getKey()))errors.add(error(path,"Unresolved group override "+entry.getKey()));
            } else if(!p.path("overrides").isEmpty())errors.add(error(path,"Whole-class teaching retains inactive group overrides; correct them explicitly"));
            var seen=new HashSet<String>();
            for(JsonNode portion:p.path("portions")){
                String audience=portion.path("audienceId").asString();
                if(!allocationIds.add(portion.path("id").asString()))errors.add(error(path,"Duplicate allocation identity"));
                if(!seen.add(audience)||!expected.containsKey(audience)||expected.get(audience)!=portion.path("sessionIds").size())errors.add(error(path,"Exact required session count differs for audience "+audience));
                for(String id:strings(portion.path("sessionIds")))if(!sessionIds.add(id))errors.add(error(path,"Duplicate stable session identity "+id));
                var policy=portion.path("teacherPolicy");var candidates=candidates(definition,p,portion);
                if(candidates.isEmpty())errors.add(error(path,"Audience "+audience+" has no eligible teacher candidates"));
                for(String id:strings(p.path("candidateTeacherIds")))if(!eligible(definition,p,id))errors.add(error(path,"Ineligible class-subject candidate "+id));
                for(String id:strings(policy.path("candidateTeacherIds")))if(!eligible(definition,p,id)||p.has("candidateTeacherIds")&&!strings(p.path("candidateTeacherIds")).contains(id))errors.add(error(path,"Narrowed teacher candidate is outside inherited eligibility: "+id));
                if(policy.path("mode").asString().equals("fixed")&&!candidates.contains(policy.path("fixedTeacherId").asString()))errors.add(error(path,"Fixed teacher is not eligible for this audience"));
                if(policy.path("mode").asString().equals("sharing")){
                    long count=0;var shares=new HashSet<String>();
                    for(JsonNode share:policy.path("shares")){
                        if(!shares.add(share.path("id").asString())||!allocationIds.add(share.path("id").asString()))errors.add(error(path,"Duplicate teacher-sharing portion identity"));
                        if(!nonnegative(share.path("count"))||share.path("count").asInt()==0)errors.add(error(path,"Teacher-fixed portions require positive counts"));else count+=share.path("count").asInt();
                        if(!candidates.contains(share.path("teacherId").asString()))errors.add(error(path,"Sharing teacher is not eligible"));
                    }
                    if(count!=portion.path("sessionIds").size())errors.add(error(path,"Teacher sharing total "+count+" differs from required sessions "+portion.path("sessionIds").size()));
                }
            }
            for(String audience:expected.keySet())if(!seen.contains(audience))errors.add(error(path,"Required audience sessions missing: "+audience));
        }
        for(JsonNode c:definition.path("cohorts")){
            var ids=new HashSet<String>();
            for(JsonNode s:c.path("splits")){
                if(!ids.add(s.path("id").asString()))errors.add(error("/cohorts","Duplicate split identity"));
                long size=0;for(JsonNode g:s.path("groups")){
                    if(!ids.add(g.path("id").asString())||g.path("size").asInt()<=0||g.path("displayName").asString().isBlank()||g.path("id").asString().startsWith("class:"))errors.add(error("/cohorts","Invalid or duplicate group in "+s.path("id").asString()));size+=g.path("size").asInt();
                }
                if(s.path("groupCount").asInt()<2||s.path("groupCount").asInt()>128||s.path("groupCount").asInt()!=s.path("groups").size()||size!=c.path("size").asInt())errors.add(error("/cohorts","Split "+s.path("id").asString()+" does not partition its class with 2–128 groups"));
                for(String subject:strings(s.path("subjectIds")))if(!subjects.containsKey(subject))errors.add(error("/cohorts","Unknown reused subject "+subject));
            }
            var used=usedSplits(definition,c.path("id").asString());
            blocks(c,used,errors);
        }
        var locks=new HashSet<String>();var periods=index(definition.path("periods"));var rooms=index(definition.path("rooms"));
        for(JsonNode lock:definition.path("sessionLocks")){
            String id=lock.path("sessionId").asString();
            if(!sessionIds.contains(id)||!locks.add(id)||lock.has("periodId")&&!periods.containsKey(lock.path("periodId").asString())||lock.has("roomId")&&!rooms.containsKey(lock.path("roomId").asString()))errors.add(error("/sessionLocks","Unknown or repeated session/period/room lock identity"));
        }
        errors.addAll(AudienceScheduling.validate(definition));
        return errors;
    }
    public static Set<String> usedSplits(JsonNode definition,String classId){
        var used=new LinkedHashSet<String>();
        for(JsonNode p:definition.path("audiencePlans"))if(p.path("classId").asString().equals(classId))
            for(JsonNode q:p.path("portions"))if(!q.path("audienceId").asString().equals("class:"+classId)&&!q.path("sessionIds").isEmpty())used.add(p.path("splitId").asString());
        return used;
    }
    public static List<Block> blocks(JsonNode c,Set<String> used,List<ValidationError> errors){
        String path="/cohorts/"+c.path("id").asString()+"/relationships";var splits=index(c.path("splits"));var result=new ArrayList<Block>();
        if(used.isEmpty())return List.of(new Block("class:"+c.path("id").asString(),c.path("size").asInt(),Set.of()));
        if(used.stream().anyMatch(id->!splits.containsKey(id))){errors.add(error(path,"Used split is missing"));return result;}
        if(used.size()==1){for(JsonNode g:splits.get(used.iterator().next()).path("groups"))result.add(new Block(g.path("id").asString(),g.path("size").asInt(),Set.of(g.path("id").asString())));return result;}
        JsonNode r=c.path("relationships");var dimensions=strings(r.path("splitIds"));
        if(!dimensions.containsAll(used)||dimensions.stream().anyMatch(id->!splits.containsKey(id))||new HashSet<>(dimensions).size()!=dimensions.size()){
            errors.add(error(path,"Missing or unresolved combined split relationships; reuse a split, declare nesting or provide joint counts"));return result;
        }
        if(r.path("mode").asString().equals("nested")){
            JsonNode base=splits.get(r.path("constituentSplitId").asString());
            if(base==null||!dimensions.contains(r.path("constituentSplitId").asString())){errors.add(error(path,"Missing nested constituent split"));return result;}
            for(JsonNode child:base.path("groups")){
                var ids=new TreeSet<String>();ids.add(child.path("id").asString());
                for(String sid:dimensions)if(!sid.equals(base.path("id").asString())){
                    var matches=new ArrayList<String>();for(JsonNode group:splits.get(sid).path("groups"))if(strings(r.path("unions").path(group.path("id").asString())).contains(child.path("id").asString()))matches.add(group.path("id").asString());
                    if(matches.size()!=1)errors.add(error(path,"Constituent "+child.path("id").asString()+" belongs to "+matches.size()+" groups in split "+sid));else ids.add(matches.getFirst());
                }
                result.add(new Block(JsonSupport.mapper().writeValueAsString(ids),child.path("size").asInt(),Set.copyOf(ids)));
            }
            for(String sid:dimensions)if(!sid.equals(base.path("id").asString()))for(JsonNode group:splits.get(sid).path("groups")){
                var members=strings(r.path("unions").path(group.path("id").asString()));var known=index(base.path("groups"));
                if(new HashSet<>(members).size()!=members.size()||!known.keySet().containsAll(members))errors.add(error(path,"Repeated or missing nested constituent reference"));
                long total=members.stream().filter(known::containsKey).mapToLong(id->known.get(id).path("size").asInt()).sum();if(total!=group.path("size").asInt())errors.add(error(path,"Nested group "+group.path("id").asString()+" total "+total+" differs from declared size"));
            }
        }else if(r.path("mode").asString().equals("joint")){
            long combinations=1;for(String sid:dimensions){if(combinations>4096L/Math.max(1,splits.get(sid).path("groups").size())){errors.add(error(path,"Joint relationship exceeds 4096 combined intersections"));return result;}combinations*=splits.get(sid).path("groups").size();}
            var seen=new HashSet<Set<String>>();
            for(JsonNode row:r.path("intersections")){
                var coordinates=strings(row.path("groupIds"));var ids=new TreeSet<>(coordinates);
                boolean shape=coordinates.size()==dimensions.size()&&dimensions.stream().allMatch(sid->index(splits.get(sid).path("groups")).keySet().stream().filter(ids::contains).count()==1);
                if(!shape||!seen.add(Set.copyOf(ids))||!nonnegative(row.path("count")))errors.add(error(path,"Duplicate, invalid or unresolved combined intersection "+ids));
                else if(row.path("count").asInt()>0)result.add(new Block(JsonSupport.mapper().writeValueAsString(ids),row.path("count").asInt(),Set.copyOf(ids)));
            }
            if(seen.size()!=combinations)errors.add(error(path,"Every combined intersection requires an explicit count, including zero; pairwise margins cannot determine joint attendance"));
        }else errors.add(error(path,"Missing relationship mode"));
        for(String sid:dimensions)for(JsonNode group:splits.get(sid).path("groups")){
            long margin=result.stream().filter(b->b.groupIds().contains(group.path("id").asString())).mapToLong(Block::size).sum();
            if(margin!=group.path("size").asInt())errors.add(error(path,"Group "+group.path("id").asString()+" margin "+margin+" differs from declared size"));
        }
        if(result.stream().mapToLong(Block::size).sum()!=c.path("size").asInt())errors.add(error(path,"Anonymous blocks do not partition the class exactly once"));
        return result;
    }
    public static boolean eligible(JsonNode definition,JsonNode plan,String teacherId){
        JsonNode t=index(definition.path("teachers")).get(teacherId),c=index(definition.path("cohorts")).get(plan.path("classId").asString()),s=index(definition.path("subjects")).get(plan.path("subjectId").asString());
        if(t==null||c==null||s==null)return false;
        boolean curator=s.path("curatorLesson").asBoolean()&&c.path("curatorTeacherId").asString().equals(teacherId);
        return (!s.path("curatorLesson").asBoolean()||curator)&&(curator||strings(t.path("qualifiedSubjectIds")).contains(plan.path("subjectId").asString()))
            &&strings(t.path("eligibleClassIdsBySubject").path(plan.path("subjectId").asString())).contains(plan.path("classId").asString());
    }
    public static List<String> candidates(JsonNode definition,JsonNode plan,JsonNode portion){
        var choices=new ArrayList<String>();for(JsonNode t:definition.path("teachers")){String id=t.path("id").asString();if(eligible(definition,plan,id)
            &&(!plan.has("candidateTeacherIds")||strings(plan.path("candidateTeacherIds")).contains(id))
            &&(!portion.path("teacherPolicy").has("candidateTeacherIds")||strings(portion.path("teacherPolicy").path("candidateTeacherIds")).contains(id)))choices.add(id);}
        return choices;
    }
}
