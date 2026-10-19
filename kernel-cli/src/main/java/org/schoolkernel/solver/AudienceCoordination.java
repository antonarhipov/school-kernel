package org.schoolkernel.solver;

import java.util.*;
import org.schoolkernel.domain.AudienceDefinition;
import tools.jackson.databind.JsonNode;

/** Bounded independent reconstruction of distinct coordination rounds from actual placements. */
public final class AudienceCoordination {
    public record Occurrence(String id,List<String> sessionIds,List<String> periodIds,String startPeriodId,String day) {}
    public record Round(List<Occurrence> occurrences) {}
    public record Match(String ruleId,String type,List<Round> rounds) {}
    public record Failure(String ruleId,String message,List<String> lessonIds) {}
    public record Review(List<Match> matches,List<Failure> conflicts) {}
    public static final class ReviewLimit extends RuntimeException {public ReviewLimit(){super("Coordination review exceeded its bounded matching budget; validity is unestablished. Supplied assignments were preserved.");}}
    private long nodes,deadline;
    public Review review(AudienceSchedule m,Map<String,List<PlanningLesson>> byBlock,JsonNode certificate){
        nodes=0;deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(3);var matches=new ArrayList<Match>();var failures=new ArrayList<Failure>();
        if(certificate!=null){var ids=new HashSet<String>();var known=new HashSet<String>();for(JsonNode r:m.document.path("coordinationRules"))known.add(r.path("id").asString());for(JsonNode match:certificate)if(!ids.add(match.path("ruleId").asString())||!known.contains(match.path("ruleId").asString()))failures.add(new Failure(match.path("ruleId").asString(),"Unknown or repeated coordination match identity",List.of()));}
        for(JsonNode rule:m.document.path("coordinationRules")){
            JsonNode asserted=null;if(certificate!=null)for(JsonNode match:certificate)if(match.path("ruleId").equals(rule.path("id")))asserted=match;
            if(certificate!=null&&(asserted==null||!asserted.path("type").equals(rule.path("type")))){failures.add(new Failure(rule.path("id").asString(),"Missing or mismatched coordination certificate",List.of()));continue;}
            if(rule.path("type").asString().equals("GAP_FREE")){if(asserted!=null&&!asserted.path("rounds").isEmpty())failures.add(new Failure(rule.path("id").asString(),"Gap-free control has no counted rounds",List.of()));matches.add(new Match(rule.path("id").asString(),"GAP_FREE",List.of()));continue;}
            var choices=new ArrayList<List<Occurrence>>();var selectedPlans=new HashSet<String>();var lessonIds=new LinkedHashSet<String>();
            for(JsonNode ref:rule.path("activities")){
                String portionId=ref.path("portionId").asString();var activity=m.sessions.values().stream().filter(s->s.portionId().equals(portionId)).findFirst().orElse(null);
                if(activity==null){choices.add(List.of());continue;}selectedPlans.add(activity.plan().path("id").asString());for(var s:m.sessions.values())if(s.portionId().equals(portionId))lessonIds.add(s.id());
                choices.add(ref.path("unit").asString().equals("BLOCK")?blocks(m,activity,ref,byBlock):singles(m,activity));
            }
            var used=new ArrayList<Set<String>>();choices.forEach(c->used.add(new HashSet<>()));var rounds=new ArrayList<Round>();
            boolean matched;
            if(asserted!=null)matched=certificate(m,rule,choices,selectedPlans,used,rounds,asserted);
            else {if((long)rule.path("count").asInt()*(choices.size()+1)>512)throw new ReviewLimit();matched=chooseRounds(m,rule,choices,selectedPlans,used,rounds,0,-1);}
            if(matched)matches.add(new Match(rule.path("id").asString(),rule.path("type").asString(),List.copyOf(rounds)));
            else failures.add(new Failure(rule.path("id").asString(),"Required "+rule.path("count").asInt()+" distinct "+rule.path("type").asString()+" rounds; no complete matching satisfies actual starts, durations and selected-plan coverage. Available occurrences="+choices.stream().map(List::size).toList(),List.copyOf(lessonIds)));
        }
        return new Review(List.copyOf(matches),List.copyOf(failures));
    }
    private static boolean certificate(AudienceSchedule m,JsonNode rule,List<List<Occurrence>> choices,Set<String> plans,List<Set<String>> used,List<Round> rounds,JsonNode asserted){
        if(asserted.path("rounds").size()!=rule.path("count").asInt())return false;
        for(JsonNode round:asserted.path("rounds")){
            if(round.path("occurrences").size()!=choices.size())return false;var selected=new ArrayList<Occurrence>();int index=0;
            for(JsonNode raw:round.path("occurrences")){
                var found=choices.get(index).stream().filter(o->org.schoolkernel.contract.JsonSupport.mapper().valueToTree(o).equals(raw)).findFirst().orElse(null);
                if(found==null||!used.get(index).add(found.id()))return false;selected.add(found);index++;
            }
            if(!satisfies(m,rule,selected,plans))return false;rounds.add(new Round(List.copyOf(selected)));
        }
        return true;
    }
    private void tick(){if(++nodes>Long.getLong("school.kernel.maxCoordinationReviewNodes",250_000L)||(nodes%256==0&&System.nanoTime()>deadline))throw new ReviewLimit();}
    private boolean chooseRounds(AudienceSchedule m,JsonNode rule,List<List<Occurrence>> choices,Set<String> plans,List<Set<String>> used,List<Round> rounds,int round,int after){
        tick();if(round==rule.path("count").asInt())return true;
        if(choices.isEmpty())return false;
        for(int i=0;i<choices.size();i++)if(choices.get(i).size()-used.get(i).size()<rule.path("count").asInt()-round)return false;
        return chooseParticipants(m,rule,choices,plans,used,rounds,round,after,0,new ArrayList<>(),-1);
    }
    private boolean chooseParticipants(AudienceSchedule m,JsonNode rule,List<List<Occurrence>> choices,Set<String> plans,List<Set<String>> used,List<Round> rounds,int round,int after,int participant,List<Occurrence> selected,int firstIndex){
        tick();if(participant==choices.size()){
            if(!satisfies(m,rule,selected,plans))return false;
            for(int i=0;i<selected.size();i++)used.get(i).add(selected.get(i).id());rounds.add(new Round(List.copyOf(selected)));
            if(chooseRounds(m,rule,choices,plans,used,rounds,round+1,firstIndex))return true;
            rounds.removeLast();for(int i=0;i<selected.size();i++)used.get(i).remove(selected.get(i).id());return false;
        }
        for(int index=0;index<choices.get(participant).size();index++){
            var occurrence=choices.get(participant).get(index);if(used.get(participant).contains(occurrence.id())||participant==0&&index<=after)continue;
            selected.add(occurrence);if(partial(rule,selected)&&chooseParticipants(m,rule,choices,plans,used,rounds,round,after,participant+1,selected,participant==0?index:firstIndex))return true;selected.removeLast();
        }
        return false;
    }
    private static boolean partial(JsonNode r,List<Occurrence> selected){
        String type=r.path("type").asString();
        if(type.equals("START_TOGETHER"))return selected.stream().map(Occurrence::startPeriodId).distinct().count()==1;
        if(type.equals("SAME_DAY"))return selected.stream().map(Occurrence::day).distinct().count()==1;
        if(type.equals("DIFFERENT_DAYS"))return selected.stream().map(Occurrence::day).distinct().count()==selected.size();
        if(type.equals("FIXED"))return selected.stream().allMatch(o->o.startPeriodId().equals(r.path("periodId").asString())&&o.day().equals(r.path("day").asString()));
        if(type.equals("ALLOWED"))return selected.stream().allMatch(o->(!r.has("days")||AudienceDefinition.strings(r.path("days")).contains(o.day()))&&(!r.has("periodIds")||AudienceDefinition.strings(r.path("periodIds")).contains(o.startPeriodId())));
        return true;
    }
    private static boolean satisfies(AudienceSchedule m,JsonNode r,List<Occurrence> selected,Set<String> plans){
        if(!partial(r,selected))return false;if(!r.path("type").asString().equals("COVERAGE"))return true;
        var occupied=new HashSet<String>();selected.forEach(o->occupied.addAll(o.periodIds()));String classId=r.path("classId").asString();
        for(String period:occupied)for(var a:m.attendance.values())if(a.classId().equals(classId)){
            long count=m.sessions.values().stream().filter(s->s.plan()!=null&&plans.contains(s.plan().path("id").asString())&&s.blockIds().contains(a.key())&&m.assignments.get(s.id()).path("periodId").asString().equals(period)).count();if(count!=1)return false;
        }
        return true;
    }
    private static List<Occurrence> singles(AudienceSchedule m,AudienceSchedule.Session activity){
        var result=new ArrayList<Occurrence>();for(var s:m.sessions.values())if(s.portionId().equals(activity.portionId())){
            var a=m.assignments.get(s.id());var period=m.periods.stream().filter(p->p.id().equals(a.path("periodId").asString())).findFirst().orElseThrow();result.add(new Occurrence(s.id(),List.of(s.id()),List.of(period.id()),period.id(),period.weekday().name()));
        }
        result.sort(Comparator.comparing(Occurrence::id));return List.copyOf(result);
    }
    private static List<Occurrence> blocks(AudienceSchedule m,AudienceSchedule.Session activity,JsonNode ref,Map<String,List<PlanningLesson>> byBlock){
        JsonNode rule=null;for(JsonNode b:activity.portion().has("blockRules")?activity.portion().path("blockRules"):activity.plan().path("blockRules"))if(b.path("id").asString().equals(ref.path("blockRuleId").asString()))rule=b;if(rule==null)return List.of();int length=rule.path("length").asInt();
        var occurrences=new TreeMap<String,Occurrence>();var present=new HashMap<String,Set<String>>();
        for(String block:activity.blockIds()){
            var placed=new HashMap<String,List<PlanningLesson>>();for(var l:byBlock.getOrDefault(block,List.of()))if(l.getSubjectId().equals(activity.subjectId()))placed.computeIfAbsent(l.getPeriod().id(),ignored->new ArrayList<>()).add(l);
            var runs=new ArrayList<List<PlanningLesson>>();var run=new ArrayList<PlanningLesson>();org.schoolkernel.domain.SchoolDefinition.Period previous=null;
            for(var p:m.resources.periods().stream().sorted(Comparator.comparing(org.schoolkernel.domain.SchoolDefinition.Period::weekday).thenComparingInt(org.schoolkernel.domain.SchoolDefinition.Period::order)).toList()){
                var at=placed.getOrDefault(p.id(),List.of());boolean adjacent=previous!=null&&previous.weekday()==p.weekday()&&previous.adjacentToNext();if(!adjacent||at.size()!=1){if(!run.isEmpty())runs.add(List.copyOf(run));run.clear();}if(at.size()==1)run.add(at.getFirst());previous=p;
            }if(!run.isEmpty())runs.add(List.copyOf(run));
            for(var r:runs)if(r.size()==length&&r.stream().allMatch(l->l.getTeacherId().equals(r.getFirst().getTeacherId())&&l.getRoom().id().equals(r.getFirst().getRoom().id()))){
                var ids=r.stream().map(PlanningLesson::getId).toList();String key=String.join("/",ids);var periods=r.stream().map(l->l.getPeriod().id()).toList();occurrences.putIfAbsent(key,new Occurrence(key,ids,periods,periods.getFirst(),r.getFirst().getPeriod().weekday().name()));present.computeIfAbsent(key,ignored->new HashSet<>()).add(block);
            }
        }
        return occurrences.entrySet().stream().filter(e->present.get(e.getKey()).containsAll(activity.blockIds())).map(Map.Entry::getValue).toList();
    }
}
