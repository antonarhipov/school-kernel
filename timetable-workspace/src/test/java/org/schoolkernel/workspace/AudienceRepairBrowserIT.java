package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.time.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.*;

class AudienceRepairBrowserIT extends WorkbenchBrowserSupport {
    @Test void repairsWeeklyTeachersWithExactPinsAndAtomicAcceptance() throws Exception {
        var h=new DemoPlanningBrowserIT();h.output=Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-13/"+System.currentTimeMillis()).toAbsolutePath();Files.createDirectories(h.output);h.base="http://localhost:"+port;
        Page p=workbench.page();
        try {
            JsonNode pair=JSON.readTree(Files.readString(Path.of("src/test/resources/student-groups/inspection-pairs.json"))).path("nested");
            ObjectNode definition=(ObjectNode)pair.path("definition").deepCopy();
            ObjectNode spare=(ObjectNode)definition.path("teachers").get(0).deepCopy();spare.put("id","TE2").put("displayName","Replacement English teacher");((ArrayNode)definition.path("teachers")).add(spare);
            ((ArrayNode)definition.path("audiencePlans").get(0).path("candidateTeacherIds")).add("TE2");
            ObjectNode supplied=JSON.createObjectNode();supplied.putObject("timetable").set("assignments",pair.path("assignments"));
            JsonNode checked=verifier.review(new ImportDocuments(definition,supplied,null,ImportDocuments.ImportMode.ACCEPTED_BASELINE),Duration.ofSeconds(30),ignored->{},()->{});
            assertTrue(checked.path("valid").asBoolean(),checked.toPrettyString());
            ObjectNode document=JSON.createObjectNode();document.putObject("school").put("id",definition.path("schoolId").asString()).put("displayName","Nested repair");
            ObjectNode baseline=document.putObject("acceptedBaseline");baseline.set("definition",definition);baseline.set("result",checked.path("result"));baseline.set("manifest",new ManifestService().validatedOrGenerated(new ImportDocuments(definition,checked.path("result"),null,ImportDocuments.ImportMode.ACCEPTED_BASELINE)));
            storeAccepted(document);JsonNode original=storedWorkspaceDocument().path("acceptedBaseline").deepCopy();workbench.open().awaitText("Start a protected repair");
            workbench.openRepairSetup();workbench.repairResource("TEACHER","TE");
            for(JsonNode period:definition.path("periods")) if(!period.path("nonTeaching").asBoolean())workbench.checkRepairPeriod(period.path("id").asString());
            workbench.submitRepair();workbench.awaitText("Repair draft · not current");
            for(JsonNode a:pair.path("assignments"))workbench.pin(a.path("lessonId").asString(),true,true);
            assertTrue(storedWorkspaceDocument().path("repairDraft").path("readyToSolve").asBoolean());
            assertEquals(original,storedWorkspaceDocument().path("acceptedBaseline"));
            h.record("UC-13 steps1–3: nested baseline retained; English teacher unavailable all week; explicit session time/room attempt pins remain compatible with automatic substitution",p,"01-draft");
            workbench.click("#solve-draft");workbench.awaitText("Repair proposal · feasible",Duration.ofSeconds(90));
            JsonNode proposal=storedWorkspaceDocument().path("proposal").deepCopy();
            assertEquals(2,proposal.path("changeCounts").path("teacherChanges").asInt());
            assertEquals(2,proposal.path("review").path("changedLessons").size());
            assertThat(p.locator("#audience-repair-review")).containsText("Replacement English teacher");
            assertThat(p.locator("#proposal-context")).containsText("Kernel priority: hard validity; automatic weekly teacher stability; period stability; room-only stability; internal student gaps; ordinary preferences.");
            assertTrue(p.locator(".audience-cell").count()>0,"Repair comparison uses compact audience cells");
            assertEquals(original,storedWorkspaceDocument().path("acceptedBaseline"));
            assertEquals(6,proposal.path("definition").path("sessionLocks").size());
            h.record("UC-13 steps4–5: real replan produces two weekly allocation replacements; old/new teachers, every affected session, unchanged allocations and quality/workload are visible before acceptance",p,"02-proposal");
            workbench.check("#confirm-repair-accept");String before=storedDocument();
            withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'ACCEPTED_BASELINE'",()->{workbench.click("#accept-repair");workbench.awaitText("Current did not advance");assertEquals(before,storedDocument());});
            h.record("UC-13 6b: injected durable acceptance failure preserves exact baseline/draft/proposal and retry eligibility",p,"03-refused");
            workbench.check("#confirm-repair-accept");workbench.click("#accept-repair");p.waitForCondition(()->"ACCEPTED_BASELINE".equals(storedLifecycle()));
            assertEquals(proposal.path("definition"),storedWorkspaceDocument().path("acceptedBaseline").path("definition"));assertEquals(proposal.path("result"),storedWorkspaceDocument().path("acceptedBaseline").path("result"));
            for(JsonNode lock:storedWorkspaceDocument().path("acceptedBaseline").path("manifest").path("locks")){assertEquals("ATTEMPT_SCOPED",lock.path("periodLockOrigin").asString());assertEquals("ATTEMPT_SCOPED",lock.path("roomLockOrigin").asString());}
            workbench.open().awaitText("Start a protected repair");h.record("UC-13 steps6–7: reverified exact pair accepted atomically and reloaded; pin provenance remains attempt-scoped",p,"04-accepted");
            workbench.startRepair("ROOM","ER","1-2");
            var drafts=new RepairDraftService(null,null,JSON);assertTrue(drafts.compiledDefinition(storedWorkspaceDocument()).path("sessionLocks").isEmpty(),"Prior attempt pins do not become persistent");
            workbench.discardDraft();p.waitForCondition(()->"ACCEPTED_BASELINE".equals(storedLifecycle()));
            // Fresh run from same original, discard its proposal and prove stale acceptance is refused.
            storeAccepted(document);workbench.open().awaitText("Start a protected repair");workbench.startRepair("TEACHER","TE","1-2");
            for(JsonNode a:pair.path("assignments"))workbench.pin(a.path("lessonId").asString(),true,true);
            workbench.click("#solve-draft");workbench.awaitText("Repair proposal · feasible",Duration.ofSeconds(90));
            workbench.click("#discard-proposal");workbench.awaitText("Repair draft · not current");assertEquals(original,storedWorkspaceDocument().path("acceptedBaseline"));
            h.record("UC-13 5a: explicit proposal discard removes acceptance eligibility, preserves accepted pair and durable intent",p,"05-discard");
            // A fresh workspace process restores the durable draft and invalidates old proposal eligibility.
            workbench.click("#solve-draft");workbench.awaitText("Repair proposal · feasible",Duration.ofSeconds(90));
            long oldVersion=storedVersion();JsonNode retainedDraft=storedWorkspaceDocument().path("repairDraft").deepCopy();
            try(var restarted=startRestartableWorkspace()) {
                workbench.open(restarted.getEnvironment().getProperty("local.server.port",Integer.class)).awaitText("Repair draft · not current");
                assertEquals(original,storedWorkspaceDocument().path("acceptedBaseline"));assertEquals(retainedDraft,storedWorkspaceDocument().path("repairDraft"));assertFalse(storedWorkspaceDocument().has("proposal"));
                Object stale=p.evaluate("""
                  async version=>{const c=await(await fetch('/api/csrf')).json();return(await fetch('/api/proposal/accept',{method:'POST',headers:{[c.headerName]:c.token,'If-Match':'"ws-'+version+'"'}})).status;}
                """,Long.toString(oldVersion));assertEquals(412,((Number)stale).intValue());
                h.record("UC-13 6a: fresh application startup restores durable draft, removes old proposal eligibility; stale acceptance request returns412 without accepted/draft mutation",p,"06-restart-stale");
            }
            workbench.open().awaitText("Repair draft · not current");
            // Fixed teacher remains hard, even though the spare teacher is eligible.
            for(JsonNode portion:definition.path("audiencePlans").get(0).path("portions")) {
                ((ObjectNode)portion).putObject("teacherPolicy").put("mode","fixed").put("fixedTeacherId","TE");
            }
            JsonNode fixedReview=verifier.review(new ImportDocuments(definition,supplied,null,ImportDocuments.ImportMode.ACCEPTED_BASELINE),Duration.ofSeconds(30),ignored->{},()->{});assertTrue(fixedReview.path("valid").asBoolean());
            baseline.set("result",fixedReview.path("result"));baseline.set("manifest",new ManifestService().validatedOrGenerated(new ImportDocuments(definition,fixedReview.path("result"),null,ImportDocuments.ImportMode.ACCEPTED_BASELINE)));
            storeAccepted(document);workbench.open().awaitText("Start a protected repair");workbench.startRepair("TEACHER","TE","1-2");workbench.pin("e1",true,false);
            p.waitForCondition(()->!storedWorkspaceDocument().path("repairDraft").path("readyToSolve").asBoolean());
            assertFalse(storedWorkspaceDocument().path("repairDraft").path("readyToSolve").asBoolean());assertThat(p.locator("#solve-draft")).isDisabled();
            h.record("UC-13 2a/G2: fixed teacher unavailable at an explicitly pinned period blocks repair despite eligible replacement; policy, baseline and draft retained",p,"06-fixed-refusal");
            Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
        }catch(Throwable f){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+f);h.record("FAIL "+f,p,"failure");throw f;}
        finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-13 repair browser journey\n\n"+Instant.now()+"\n"+h.base+"\nReal Chrome, isolated PostgreSQL; actual CLI review/replan/verify; no operator data.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nHarness closes browser and disposable database.\n");}
    }
}
