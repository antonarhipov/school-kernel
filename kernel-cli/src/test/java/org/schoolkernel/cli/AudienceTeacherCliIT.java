package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.*;

class AudienceTeacherCliIT {
    @TempDir Path temporary;
    ObjectNode fixture(){return (ObjectNode)JsonSupport.mapper().readTree(Path.of("src/test/resources/fixtures/audience-teachers.json"));}
    JsonNode verify(ObjectNode definition,int expected,String run)throws Exception{
        Path input=temporary.resolve(run+".json"),output=temporary.resolve(run+"-verification.json"),log=temporary.resolve(run+".log");
        byte[] bytes=JsonSupport.mapper().writeValueAsBytes(definition);Files.write(input,bytes);
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-jar","target/school-kernel.jar","verify","--definition",input.toString(),"--output",output.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        assertTrue(process.waitFor(20,TimeUnit.SECONDS));assertEquals(expected,process.exitValue(),Files.readString(log));assertArrayEquals(bytes,Files.readAllBytes(input));return JsonSupport.mapper().readTree(output);
    }
    @Test void acceptsPublishedEligibilityContinuityFixedAndSharingInputWithoutApplyingReferenceLoadLimits()throws Exception{
        ObjectNode d=fixture();var result=verify(d,0,"supported");assertEquals("VERIFIED",result.path("status").asString());assertEquals(11,result.path("catalogVersion").asInt());
        assertEquals(List.of(3,1,1),java.util.stream.StreamSupport.stream(d.path("audiencePlans").get(0).path("portions").spliterator(),false).map(p->p.path("sessionIds").size()).toList());
        assertEquals(0,d.path("teachers").get(0).path("referenceWeeklyMaximum").asInt());
    }
    @Test void rejectsEmptyIneligibleSharingAndDuplicateIdentityPoliciesAtThePublishedBoundary()throws Exception{
        List<Consumer<ObjectNode>> mutations=List.of(
            d->((ObjectNode)d.path("audiencePlans").get(0).path("portions").get(2).path("teacherPolicy")).putArray("candidateTeacherIds"),
            d->((ObjectNode)d.path("teachers").get(0).path("eligibleClassIdsBySubject")).putArray("s"),
            d->((ObjectNode)d.path("audiencePlans").get(0).path("portions").get(0).path("teacherPolicy").path("shares").get(0)).put("count",3),
            d->((ArrayNode)d.path("audiencePlans").get(0).path("portions").get(2).path("sessionIds")).set(0,d.path("audiencePlans").get(0).path("portions").get(1).path("sessionIds").get(0)));
        List<String> reasons=List.of("no eligible teacher","not eligible","sharing total 4","Duplicate stable session");
        for(int i=0;i<mutations.size();i++){ObjectNode d=fixture();mutations.get(i).accept(d);var result=verify(d,2,"invalid-"+i);assertEquals("INVALID_INPUT",result.path("status").asString());assertTrue(result.path("validationReport").toString().contains(reasons.get(i)),result.toString());assertFalse(result.has("timetable"));}
    }
    @Test void retainsCountedControlsAndRejectsUnrepresentableOccurrencesRoomsAndDurations() throws Exception {
        ObjectNode d=fixture();ObjectNode p=(ObjectNode)d.path("audiencePlans").get(0),q=(ObjectNode)p.path("portions").get(0);
        p.putArray("blockRules").addObject().put("id","double").put("length",2).put("count",1).put("operator","EXACTLY");
        String room=d.path("rooms").get(0).path("id").asString();p.putArray("allowedRoomIds").add(room);q.put("fixedRoomId",room);
        ObjectNode rule=d.putArray("coordinationRules").addObject();rule.put("id","allowed").put("classId",p.path("classId").asString()).put("type","ALLOWED").put("count",1);
        rule.putArray("activities").addObject().put("portionId",q.path("id").asString()).put("unit","BLOCK").put("blockRuleId","double");
        rule.putArray("periodIds").add(d.path("periods").get(0).path("id").asString());
        assertEquals("VERIFIED",verify(d,0,"coordination-supported").path("status").asString());
        rule.put("count",2);assertTrue(verify(d,2,"coordination-count").toString().contains("distinct rounds exceeds"));rule.put("count",1);
        q.putArray("allowedRoomIds");assertTrue(verify(d,2,"coordination-rooms").toString().contains("Empty room intersection"));q.remove("allowedRoomIds");
        rule.put("type","FIXED").put("day",d.path("periods").get(0).path("weekday").asString()).put("periodId",d.path("periods").get(d.path("periods").size()-1).path("id").asString());
        assertTrue(verify(d,2,"coordination-duration").toString().contains("complete selected duration"));
    }

    @Test void checksActualAudienceCapacityAndContinuousTeacherAvailabilityWithoutMutatingInput() throws Exception {
        ObjectNode d=fixture();for(JsonNode room:d.path("rooms"))((ObjectNode)room).put("capacity",13);
        assertTrue(verify(d,2,"audience-capacity").toString().contains("no compatible room"));
        d=fixture();for(JsonNode teacher:d.path("teachers"))((ObjectNode)teacher).putArray("availablePeriodIds");
        assertTrue(verify(d,2,"audience-availability").toString().contains("compatible available teaching periods"));
    }

}
