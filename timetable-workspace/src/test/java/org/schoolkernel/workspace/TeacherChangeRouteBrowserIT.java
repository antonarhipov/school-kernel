package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class TeacherChangeRouteBrowserIT extends WorkbenchBrowserSupport {
    @Test void acceptedRouteNoTeacherControlLiteralNarrowAndAuthoringEntry() {
        ObjectNode source=fixtures.validAcceptedDocument();source.withObject("school").put("displayName","<img src=x> Õ School");storeAccepted(source);workbench.open();workbench.awaitText("Author definition");
        String before=storedDocument();workbench.page().locator("#teacher-change-route summary").click();workbench.awaitText("Teaching responsibility belongs to the school definition");assertEquals(before,storedDocument());
        workbench.page().locator(".lesson-cell[data-lesson-id='lesson-math-1']").first().click();workbench.page().locator("#edit-period").waitFor();assertEquals(0,workbench.page().locator("#edit-teacher").count());assertEquals(before,storedDocument());
        workbench.viewport(390,844);workbench.expect("document.querySelector('.narrow-mode') && !document.querySelector('[data-teacher-authoring]') && document.documentElement.scrollWidth <= innerWidth");workbench.page().locator("#teacher-change-route summary").click();workbench.awaitText("Teaching responsibility belongs");assertEquals(0,workbench.page().locator("#state-card img").count());assertEquals(before,storedDocument());
        workbench.viewport(1440,900);workbench.expect("document.querySelector('[data-teacher-authoring]') !== null");if(workbench.page().locator("#workbench-inspector").isVisible())workbench.page().locator("#toggle-inspector").click();workbench.page().locator("#teacher-change-route summary").click();workbench.page().locator("#teacher-change-route [data-teacher-authoring]").click();assertEquals(before,storedDocument());workbench.page().locator("#confirm-authoring").click();workbench.awaitText("Saved school-definition draft");assertEquals(source,storedWorkspaceDocument().path("definitionDraft").path("sourceDocument"));
        workbench.page().locator("button[data-section='Weekly requirements']").click();workbench.awaitText("Weekly requirements");workbench.page().locator("button[data-section='Occurrence details']").click();workbench.awaitText("Occurrence details");assertEquals(source.path("acceptedBaseline"),storedWorkspaceDocument().path("acceptedBaseline"));
    }
    @Test void directTeacherFieldsAreRefusedOnBaselineAndManualIncludingNullAndSameValue() {
        storeAccepted(fixtures.validAcceptedDocument());workbench.open();
        for(String state:java.util.List.of("ACCEPTED_BASELINE","MANUAL_DRAFT")) {
            if(state.equals("MANUAL_DRAFT"))assertEquals(200,command("POST","/api/manual-draft","{}").status());
            for(String teacher:java.util.List.of("\"teacher-alex\"","\"other\"","null")) {
                String before=storedDocument();var response=command("PATCH","/api/manual-draft","{\"action\":\"REASSIGN_LESSON\",\"lessonId\":\"lesson-math-1\",\"periodId\":\"mon-3\",\"teacherId\":"+teacher+"}");assertEquals(422,response.status());assertTrue(response.text().contains("TEACHER_CHANGE_REQUIRES_AUTHORING"));assertEquals(before,storedDocument());
            }
        }
        workbench.reload();workbench.page().locator("#teacher-change-route summary").click();workbench.awaitText("MANUAL_DRAFT is active");assertEquals(0,workbench.page().locator("[data-teacher-authoring]").count());
    }
    @Test void savedLegacyMismatchBlocksPublicationEvenWithEmptyMetadataAndExplicitFullRevertWorks() {
        ObjectNode source=fixtures.validAcceptedDocument();source.putObject("authoringContext").put("exact","retained shared teaching intent");storeAccepted(source);workbench.open();assertEquals(200,command("POST","/api/manual-draft","{}").status());
        ObjectNode document=(ObjectNode)storedWorkspaceDocument(),assignment=(ObjectNode)document.path("manualDraft").path("assignments").get(0);JsonNode accepted=source.path("acceptedBaseline").path("result").path("timetable").path("assignments").get(0);String id=accepted.path("lessonId").asString();assignment.put("teacherId","legacy-teacher").put("periodId","mon-3").put("roomId","room-101");storeManual(document);String before=storedDocument();
        workbench.reload();workbench.page().locator("#teacher-change-route summary").click();workbench.awaitText("legacy teacher differences for lessons: "+id);assertTrue(workbench.page().locator("#publish-manual-draft").isDisabled());assertEquals(before,storedDocument());
        var refused=command("POST","/api/manual-draft/publish","{}");assertEquals(422,refused.status());assertTrue(refused.text().contains(id));assertEquals(before,storedDocument());
        workbench.page().locator(".lesson-cell[data-lesson-id='"+id+"']").first().click();workbench.page().locator("#revert-lesson-btn").click();workbench.expect("document.querySelector('#publish-manual-draft') && !document.querySelector('#publish-manual-draft').disabled");assertEquals(accepted,storedWorkspaceDocument().path("manualDraft").path("assignments").get(0));assertEquals(source.path("acceptedBaseline"),storedWorkspaceDocument().path("acceptedBaseline"));
        assertEquals(200,command("PATCH","/api/manual-draft","{\"action\":\"REASSIGN_LESSON\",\"lessonId\":\"lesson-science-1\",\"periodId\":\"mon-3\",\"roomId\":\"room-101\"}").status());assertEquals(200,command("POST","/api/manual-draft/publish","{}").status());assertEquals("ACCEPTED_BASELINE",storedLifecycle());assertEquals(source.path("authoringContext"),storedWorkspaceDocument().path("authoringContext"));assertEquals(source.path("acceptedBaseline").path("definition"),storedWorkspaceDocument().path("acceptedBaseline").path("definition"));
    }
    @Test void blockedLifecycleGuidanceNamesRoutesAndDoesNotMutate() {
        workbench.open();for(String state:java.util.List.of("REPAIR_DRAFT","SOLVING_REPAIR","REPAIR_PROPOSAL")){
            ObjectNode doc=state.equals("REPAIR_PROPOSAL")?fixtures.scaleProposalDocument():fixtures.scaleDocument();
            if(!state.equals("REPAIR_PROPOSAL")) { doc.putObject("repairDraft").set("intent", JSON.readTree("{\"changes\":[],\"pins\":[],\"bulkActions\":[]}")); doc.withObject("repairDraft").putArray("conflicts"); doc.withObject("repairDraft").putArray("directEffectLessonIds"); }
            if(state.equals("SOLVING_REPAIR")){ObjectNode run=doc.putObject("run");run.put("id","guidance-only").put("limit","PT1M").put("kind","REPAIR");}
            jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state,version=version+1,document=CAST(:doc AS jsonb) WHERE workspace_id=1").param("state",state).param("doc",JSON.writeValueAsString(doc)).update();String before=storedDocument();workbench.reload();workbench.page().locator("#teacher-change-route summary").click();workbench.awaitText(state+" is active");assertEquals(0,workbench.page().locator("[data-teacher-authoring]").count());assertEquals(before,storedDocument());var refused=command("PATCH","/api/manual-draft","{\"action\":\"REASSIGN_LESSON\",\"lessonId\":\"lesson-0\",\"teacherId\":\"teacher-1\"}");assertEquals(409,refused.status());assertTrue(refused.text().contains("TEACHER_CHANGE_REQUIRES_AUTHORING"));assertTrue(refused.text().contains(state+" is active"));assertEquals(before,storedDocument());
        }
    }
    private void storeManual(JsonNode doc){jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state='MANUAL_DRAFT',version=version+1,document=CAST(:doc AS jsonb) WHERE workspace_id=1").param("doc",JSON.writeValueAsString(doc)).update();}
    private com.microsoft.playwright.APIResponse command(String method,String path,String data){String base="http://localhost:"+port;JsonNode csrf=JSON.readTree(workbench.page().request().get(base+"/api/csrf").text());return workbench.page().request().fetch(base+path,RequestOptions.create().setMethod(method).setHeader("Origin",base).setHeader("Content-Type","application/json").setHeader(csrf.path("headerName").asString(),csrf.path("token").asString()).setHeader("If-Match","\"ws-"+storedVersion()+"\"").setData(data));}
}
