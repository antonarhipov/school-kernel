package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.time.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class AudienceManualBrowserIT extends WorkbenchBrowserSupport {
    @Test void explicitIndividualCombinedAndWeeklyAllocationCorrections() throws Exception {
        var h=new DemoPlanningBrowserIT();h.output=Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-14/"+System.currentTimeMillis()+"-normal").toAbsolutePath();Files.createDirectories(h.output);h.base="http://localhost:"+port;Page p=workbench.page();
        try {
            JsonNode pair=JSON.readTree(Files.readString(Path.of("src/test/resources/student-groups/manual-pair.json")));ObjectNode supplied=JSON.createObjectNode();supplied.putObject("timetable").set("assignments",pair.path("assignments"));
            JsonNode checked=verifier.review(new ImportDocuments(pair.path("definition"),supplied,null,ImportDocuments.ImportMode.ACCEPTED_BASELINE),Duration.ofSeconds(30),ignored->{},()->{});assertTrue(checked.path("valid").asBoolean(),checked.toPrettyString());
            ObjectNode document=JSON.createObjectNode();document.putObject("school").put("id",pair.path("definition").path("schoolId").asString()).put("displayName","Manual audiences");ObjectNode baseline=document.putObject("acceptedBaseline");baseline.set("definition",pair.path("definition"));baseline.set("result",checked.path("result"));baseline.set("manifest",new ManifestService().validatedOrGenerated(new ImportDocuments(pair.path("definition"),checked.path("result"),null,ImportDocuments.ImportMode.ACCEPTED_BASELINE)));
            storeAccepted(document);workbench.open().awaitText("Start a protected repair");JsonNode source=storedWorkspaceDocument().path("acceptedBaseline").deepCopy();
            workbench.selectLesson("e1");p.locator("#edit-period").selectOption("0-4");assertThat(p.locator("#audience-manual-proposal")).containsText("h3");assertThat(p.locator("#audience-manual-proposal")).containsText("h4");assertEquals("ACCEPTED_BASELINE",storedLifecycle());
            p.locator("#cancel-combined-change").click();assertEquals(source,storedWorkspaceDocument().path("acceptedBaseline"));workbench.selectLesson("e1");p.locator("#edit-period").selectOption("0-4");p.locator("#apply-individual-change").click();
            workbench.awaitText("Manual draft");p.waitForCondition(()->storedWorkspaceDocument().path("manualDraft").path("conflicts").size()>0);
            JsonNode draft=storedWorkspaceDocument().path("manualDraft");assertEquals("0-4",assignment(draft.path("assignments"),"e1").path("periodId").asString());assertEquals("1-2",assignment(draft.path("assignments"),"h3").path("periodId").asString());assertThat(p.locator("#publish-manual-draft")).isDisabled();assertEquals(source,storedWorkspaceDocument().path("acceptedBaseline"));
            h.record("UC-14 steps1–4/3a/4a: selected composite lesson offers explicit three-session proposal; cancel preserves Current; individual provisional edit moves only e1, hard coordination conflict is visible and publication disabled",p,"01-invalid-individual");
            workbench.selectLesson("e1");p.locator("#revert-lesson-btn").click();p.waitForCondition(()->storedWorkspaceDocument().path("manualDraft").path("conflicts").isEmpty());
            workbench.selectLesson("e1");p.locator("#edit-period").selectOption("0-4");p.locator("#apply-combined-change").click();p.waitForCondition(()->"0-4".equals(assignment(storedWorkspaceDocument().path("manualDraft").path("assignments"),"h3").path("periodId").asString()));assertTrue(storedWorkspaceDocument().path("manualDraft").path("conflicts").isEmpty());
            workbench.selectLesson("e1");p.locator("#edit-teacher").selectOption("TE2");assertThat(p.locator("#audience-manual-proposal")).containsText("e1b");assertEquals("TE",assignment(storedWorkspaceDocument().path("manualDraft").path("assignments"),"e1").path("teacherId").asString());
            p.locator("#apply-combined-change").click();p.waitForCondition(()->"TE2".equals(assignment(storedWorkspaceDocument().path("manualDraft").path("assignments"),"e1b").path("teacherId").asString()));assertEquals("TE",assignment(storedWorkspaceDocument().path("manualDraft").path("assignments"),"e2").path("teacherId").asString());
            h.record("UC-14 3a/3b: confirmed combined round lists/moves e1+h3+h4; explicit automatic weekly teacher replacement lists e1/e1b and changes both, leaving E2 independent; full kernel review remains valid with permitted worse gaps",p,"02-valid-combined-allocation");
            String before=storedDocument();withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'ACCEPTED_BASELINE'",()->{p.locator("#publish-manual-draft").click();assertThat(p.locator("#manual-publish-status")).isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));assertEquals(before,storedDocument());});
            p.locator("#publish-manual-draft").click();p.waitForCondition(()->"ACCEPTED_BASELINE".equals(storedLifecycle()));JsonNode accepted=storedWorkspaceDocument().path("acceptedBaseline");assertEquals(source.path("definition"),accepted.path("definition"));assertEquals("TE2",assignment(accepted.path("result").path("timetable").path("assignments"),"e1b").path("teacherId").asString());assertTrue(accepted.path("result").path("score").path("internalGapStudentPeriods").asInt()>0);assertTrue(accepted.path("result").has("quality"));assertEquals(1,accepted.path("result").path("coordinationMatches").size());verifier.verify(new ImportDocuments(accepted.path("definition"),accepted.path("result"),null,ImportDocuments.ImportMode.ACCEPTED_BASELINE));
            workbench.open().awaitText("Start a protected repair");h.record("UC-14 steps5–6/6a: failed atomic publication preserves exact draft/source; retry freshly recomputes weekly allocation, matching, gaps and quality, verifies and durably publishes exact pair; reload succeeds",p,"03-published");
            workbench.selectLesson("e1");p.locator("#edit-room").selectOption("HR1");p.waitForCondition(()->"MANUAL_DRAFT".equals(storedLifecycle()));JsonNode kept=accepted.deepCopy();p.onceDialog(Dialog::accept);p.locator("#discard-manual-draft").click();p.waitForCondition(()->"ACCEPTED_BASELINE".equals(storedLifecycle()));assertEquals(kept,storedWorkspaceDocument().path("acceptedBaseline"));
            h.record("UC-14 4b: explicit discard abandons only invalid manual working copy, preserving published pair",p,"04-discard");Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
        }catch(Throwable f){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+f);h.record("FAIL "+f,p,"failure");throw f;}
        finally {Files.writeString(h.output.resolve("ledger.md"),"# UC-14 normal manual browser journey\n\n"+Instant.now()+"\n"+h.base+"\nReal Chrome, disposable PostgreSQL, real CLI review and verify.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nHarness cleanup preserves operator data.\n");}
    }
    static JsonNode assignment(JsonNode assignments,String id){return assignments.valueStream().filter(a->id.equals(a.path("lessonId").asString())).findFirst().orElseThrow();}
}
