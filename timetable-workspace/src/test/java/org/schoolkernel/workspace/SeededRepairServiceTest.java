package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class SeededRepairServiceTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final RepairDraftService drafts = new RepairDraftService(null, null, JSON);

    private ObjectNode document() {
        ObjectNode definition = (ObjectNode) JSON.readTree("""
            {"schemaVersion":1,"catalogVersion":9,"schoolId":"s","displayName":"School",
             "subjects":[{"id":"s1","displayName":"Subject"}],
             "teachers":[{"id":"t1","displayName":"Teacher","qualifiedSubjectIds":["s1"]}],
             "cohorts":[{"id":"c1","displayName":"Class","size":2}],
             "rooms":[{"id":"r1","displayName":"Room","capacity":4,"capabilityIds":[]}],
             "periods":[{"id":"p1","displayName":"Period","weekday":"MONDAY","order":1,"startTime":"09:00","endTime":"09:45"}],
             "lessons":[{"id":"l1","displayName":"Lesson","teacherId":"t1","subjectId":"s1","cohortId":"c1"}]}
            """);
        ObjectNode source=JSON.createObjectNode();source.putObject("school").put("id","s").put("displayName","School");
        ObjectNode baseline=source.putObject("acceptedBaseline");baseline.set("definition",definition.deepCopy());
        ObjectNode result=baseline.putObject("result");result.put("schemaVersion",1);result.put("inputRevision","sha256:source");result.put("timetableRevision","sha256:timetable");
        result.putObject("timetable").putArray("assignments").addObject().put("lessonId","l1").put("subjectId","s1").put("teacherId","t1").put("cohortId","c1").put("periodId","p1").put("roomId","r1");
        baseline.putObject("manifest").putArray("locks");
        ObjectNode document=source.deepCopy(),draft=document.putObject("repairDraft");
        definition.put("basedOnRevision","sha256:source");draft.set("seedDefinition",definition);
        ObjectNode retained=draft.putObject("retainedAuthoring");retained.put("sourceState","ACCEPTED_BASELINE");retained.set("sourceDocument",source);retained.set("definition",definition.deepCopy());retained.putObject("editingContext").put("exact","context");
        ObjectNode intent=draft.putObject("intent");intent.putArray("changes");intent.putArray("pins");intent.putArray("bulkActions");return document;
    }
    @Test void compilesEntireSeedWithoutLosingOptionalPresenceOrReintroducingOldPins() {
        ObjectNode doc=document();ObjectNode seed=(ObjectNode)doc.path("repairDraft").path("seedDefinition");
        seed.put("displayName","Successor");((ObjectNode)seed.path("teachers").get(0)).putArray("undesirablePeriodIds");
        ((ObjectNode)seed.path("lessons").get(0)).put("periodLock","p1");
        ((ObjectNode)doc.path("repairDraft").path("retainedAuthoring")).set("definition",seed.deepCopy());
        ((ObjectNode)doc.path("acceptedBaseline").path("manifest")).withArray("locks").addObject().put("lessonId","l1").put("periodLockOrigin","ATTEMPT_SCOPED");
        ((ObjectNode)doc.path("repairDraft").path("retainedAuthoring").path("sourceDocument")).set("acceptedBaseline",doc.path("acceptedBaseline").deepCopy());
        drafts.refreshSavedDraft(doc);assertEquals(seed,drafts.compiledDefinition(doc));
        JsonNode manifest=new ManifestService().forAcceptedRepair(seed,doc.path("acceptedBaseline").path("result"),doc.path("acceptedBaseline").path("manifest"),doc.path("repairDraft"));
        assertEquals("PERSISTENT_POLICY",manifest.path("locks").get(0).path("periodLockOrigin").asString());
        assertTrue(seed.path("teachers").get(0).has("undesirablePeriodIds"));
    }
    @Test void seedContextSourceAndIntentAreBoundAndTamperingIsRefused() {
        ObjectNode doc=document();drafts.refreshSavedDraft(doc);String first=doc.path("repairDraft").path("intentRevision").asString();
        ((ObjectNode)doc.path("repairDraft").path("retainedAuthoring").path("editingContext")).put("exact","new context");drafts.refreshSavedDraft(doc);assertNotEquals(first,doc.path("repairDraft").path("intentRevision").asString());
        ((ObjectNode)doc.path("repairDraft").path("seedDefinition")).put("displayName","tampered");assertThrows(WorkspaceProblem.class,()->drafts.compiledDefinition(doc));
    }
    @Test void sameIdTimingImpactsAreSeparateFromKernelPeriodMovesAndNativeChangesAreComplete() {
        ObjectNode doc=document();ObjectNode successor=(ObjectNode)doc.path("repairDraft").path("seedDefinition");
        ((ObjectNode)successor.path("periods").get(0)).put("startTime","09:05").put("endTime","09:50");
        ((ObjectNode)successor.path("cohorts").get(0)).put("homeRoomId","r1");
        ObjectNode result=((ObjectNode)doc.path("acceptedBaseline").path("result")).deepCopy();ObjectNode report=result.putObject("changeReport");ProposalReviewService.CATEGORIES.forEach(report::putArray);
        JsonNode review=new ProposalReviewService(JSON).create(doc.path("acceptedBaseline"),doc.path("repairDraft"),successor,result);
        assertEquals(1,review.path("timingImpactCount").intValue());assertEquals(JSON.readTree("[\"l1\"]"),review.path("timingImpactLessonIds"));
        assertEquals(6,review.path("categories").size());review.path("categories").forEach(category->assertEquals(0,category.path("count").intValue()));
        JsonNode changed=review.path("changedLessons").get(0);assertEquals("09:00",changed.path("old").path("timing").path("startTime").asString());assertEquals("09:05",changed.path("proposed").path("timing").path("startTime").asString());
        assertTrue(review.path("definitionChanges").valueStream().anyMatch(change->"homeRoomId".equals(change.path("field").asString())));
    }
    @Test void addedPinsRemainTemporaryAndSeedPolicyAlwaysWinsProvenance() {
        ObjectNode doc=document(),draft=(ObjectNode)doc.path("repairDraft");ObjectNode pin=((ObjectNode)draft.path("intent")).withArray("pins").addObject().put("lessonId","l1");pin.putArray("periodSources").add("INDIVIDUAL");pin.putArray("roomSources");
        drafts.refreshSavedDraft(doc);ObjectNode compiled=drafts.compiledDefinition(doc);
        JsonNode manifest=new ManifestService().forAcceptedRepair(compiled,doc.path("acceptedBaseline").path("result"),doc.path("acceptedBaseline").path("manifest"),draft);
        assertEquals("ATTEMPT_SCOPED",manifest.path("locks").get(0).path("periodLockOrigin").asString());assertFalse(draft.path("seedDefinition").path("lessons").get(0).has("periodLock"));
    }
}
