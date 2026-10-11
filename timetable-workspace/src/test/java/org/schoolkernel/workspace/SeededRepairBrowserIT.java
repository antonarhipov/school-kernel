package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class SeededRepairBrowserIT extends WorkbenchBrowserSupport {
    @Test void returnRestoresExactAuthoringAndDropsOnlyRepairIntentIncludingOnStorageFailure() throws Exception {
        seed();JsonNode source=storedWorkspaceDocument().path("acceptedBaseline").deepCopy(),retained=storedWorkspaceDocument().path("repairDraft").path("retainedAuthoring").deepCopy();
        assertEquals(200,command("PATCH","/api/repair-draft",JSON.readTree("{\"action\":\"PIN\",\"lessonId\":\"lesson-math-1\",\"dimensions\":[\"PERIOD\"]}")).status());
        String before=storedDocument();assertEquals(422,command("POST","/api/repair-draft/return-to-authoring",JSON.createObjectNode()).status());assertEquals(before,storedDocument());
        withRejectedWorkspaceUpdates("NEW.lifecycle_state='DEFINITION_DRAFT'",()->{assertEquals(503,command("POST","/api/repair-draft/return-to-authoring",JSON.createObjectNode().put("confirmed",true)).status());assertEquals(before,storedDocument());});
        workbench.reload();workbench.page().locator("#confirm-discard-draft").check();workbench.page().locator("#discard-draft").click();workbench.awaitText("Saved school-definition draft");
        assertEquals(retained,storedWorkspaceDocument().path("definitionDraft"));assertEquals(source,storedWorkspaceDocument().path("acceptedBaseline"));assertFalse(storedWorkspaceDocument().has("repairDraft"));workbench.reload();assertEquals(retained,storedWorkspaceDocument().path("definitionDraft"));
    }
    @Test void refusesRemovedPlacementAndPersistentLockConflictWithoutChangingStaging() {
        seed();ObjectNode doc=(ObjectNode)storedWorkspaceDocument(),definition=(ObjectNode)doc.path("repairDraft").path("seedDefinition");
        ObjectNode lesson=(ObjectNode)definition.path("lessons").get(0);String lessonId=lesson.path("id").asString();
        String acceptedPeriod="";for(JsonNode assignment:doc.path("acceptedBaseline").path("result").path("timetable").path("assignments"))if(lessonId.equals(assignment.path("lessonId").asString()))acceptedPeriod=assignment.path("periodId").asString();
        String other=definition.path("periods").get(0).path("id").asString();if(other.equals(acceptedPeriod))other=definition.path("periods").get(1).path("id").asString();lesson.put("periodLock",other);
        ((ObjectNode)doc.path("repairDraft").path("retainedAuthoring")).set("definition",definition.deepCopy());store(doc);String before=storedDocument();
        assertEquals(422,command("PATCH","/api/repair-draft",JSON.createObjectNode().put("action","PIN").put("lessonId",lessonId).set("dimensions",JSON.readTree("[\"PERIOD\"]"))).status());assertEquals(before,storedDocument());
        assertEquals(422,command("PATCH","/api/repair-draft",JSON.readTree("{\"action\":\"PIN\",\"lessonId\":\"new-lesson\",\"dimensions\":[\"PERIOD\"]}")).status());assertEquals(before,storedDocument());
    }
    @Test void cancelledRunAndRecoveryPreserveExactSeedContextPinsAndCurrent() {
        seed();JsonNode saved=storedWorkspaceDocument().deepCopy();APIResponse started=command("POST","/api/runs",JSON.createObjectNode());assertEquals(202,started.status());String id=storedWorkspaceDocument().path("run").path("id").asString();
        assertEquals(409,command("POST","/api/repair-draft/return-to-authoring",JSON.createObjectNode().put("confirmed",true)).status());assertEquals(saved.path("acceptedBaseline"),storedWorkspaceDocument().path("acceptedBaseline"));
        assertEquals(200,command("DELETE","/api/runs/"+id,JSON.createObjectNode()).status());assertEquals("REPAIR_DRAFT",storedLifecycle());assertEquals(saved.path("repairDraft"),storedWorkspaceDocument().path("repairDraft"));
        recovery.recoverInterruptedRun();workbench.reload();assertEquals(saved.path("repairDraft"),storedWorkspaceDocument().path("repairDraft"));
    }
    private void seed() {
        storeAccepted(fixtures.validAcceptedDocument());workbench.open();
        workbench.page().locator("#open-authoring").click();workbench.page().locator("#confirm-authoring").click();workbench.awaitText("Saved school-definition draft");
        ObjectNode doc=(ObjectNode)storedWorkspaceDocument();doc.withObject("definitionDraft").withObject("definition").put("displayName","Seeded successor");store(doc);workbench.reload();workbench.page().locator("button[data-section='Review']").click();workbench.page().locator("#finish-definition").click();workbench.page().locator("#confirm-definition-finish").click();workbench.awaitText("Repair draft");assertEquals("REPAIR_DRAFT",storedLifecycle());
    }
    private void store(JsonNode doc){jdbc.sql("UPDATE workspace_aggregate SET version=version+1,document=CAST(:doc AS jsonb) WHERE workspace_id=1").param("doc",JSON.writeValueAsString(doc)).update();}
    private APIResponse command(String method,String route,JsonNode body){String base="http://localhost:"+port;JsonNode csrf=JSON.readTree(workbench.page().request().get(base+"/api/csrf").text());return workbench.page().request().fetch(base+route,RequestOptions.create().setMethod(method).setHeader("Content-Type","application/json").setHeader("Origin",base).setHeader(csrf.path("headerName").asString(),csrf.path("token").asString()).setHeader("If-Match","\"ws-"+storedVersion()+"\"").setData(JSON.writeValueAsString(body)));}
}
