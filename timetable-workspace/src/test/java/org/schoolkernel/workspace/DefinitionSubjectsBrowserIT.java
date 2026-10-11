package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Subject authoring at the actual local HTTP/browser and durable aggregate boundaries. */
class DefinitionSubjectsBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void explicitValuesOmissionsIdentityReloadAndRestart() {
        create(); String before=storedDocument(); name("Õäš <b>Math</b>");
        assertEquals("oas-b-math-b",workbench.page().locator("#subject-id").inputValue());
        assertEquals(before,storedDocument());
        workbench.page().locator("#subject-id").fill("Math.ID_9");
        mode("reservedPeriodsAllowed","true"); number("maxWeeklyReservedLessonsPerCohort","2147483647");
        mode("dayEdgeOnly","false"); number("maxDailyLessonsPerCohort","2"); mode("curatorLesson","true"); save();
        ObjectNode expected=JSON.createObjectNode().put("id","Math.ID_9").put("displayName","Õäš <b>Math</b>")
                .put("reservedPeriodsAllowed",true).put("maxWeeklyReservedLessonsPerCohort",2147483647)
                .put("dayEdgeOnly",false).put("maxDailyLessonsPerCohort",2).put("curatorLesson",true);
        assertEquals(expected,subject()); assertEquals(0,workbench.page().locator("#subject-effects b").count());
        assertNotNull(workbench.page().locator("#subject-id").getAttribute("readonly"));
        name("Renamed subject"); mode("curatorLesson","default"); mode("maxWeeklyReservedLessonsPerCohort","default"); save();
        expected.put("displayName","Renamed subject");expected.remove("curatorLesson");expected.remove("maxWeeklyReservedLessonsPerCohort");assertEquals(expected,subject());
        String saved=storedDocument(); name("Working only"); workbench.reload(); subjects(); assertEquals("Renamed subject",workbench.page().locator("#subject-displayName").inputValue());assertEquals(saved,storedDocument());
        try(var restarted=startRestartableWorkspace()) {
            int restartedPort=((org.springframework.boot.web.server.context.WebServerApplicationContext)restarted).getWebServer().getPort();
            workbench.open(restartedPort);subjects();assertEquals(expected,subject());assertEquals(saved,storedDocument());
        }
    }

    @Test
    void incompleteSupportedFieldsAndCatalogUnavailableRefusal() {
        create();name(" ");number("maxWeeklyReservedLessonsPerCohort","0");number("maxDailyLessonsPerCohort","bad");save();
        assertEquals(" ",subject().path("displayName").stringValue());assertEquals(0,subject().path("maxWeeklyReservedLessonsPerCohort").intValue());assertEquals("bad",subject().path("maxDailyLessonsPerCohort").stringValue());
        JsonNode issues=draft().path("issues"); assertEquals(6,issues.size());
        assertTrue(issues.toString().contains("explicit permission"));workbench.awaitText("explicit permission");
        name("Corrected");mode("reservedPeriodsAllowed","true");number("maxWeeklyReservedLessonsPerCohort","1");mode("maxDailyLessonsPerCohort","default");save();assertEquals(2,draft().path("issues").size());
        ObjectNode document=(ObjectNode)storedWorkspaceDocument();((ObjectNode)document.path("definitionDraft").path("definition")).put("catalogVersion",1);
        ((ObjectNode)document.path("definitionDraft").path("definition").path("subjects").get(0)).remove(List.of("reservedPeriodsAllowed","maxWeeklyReservedLessonsPerCohort"));
        store("DEFINITION_DRAFT",document);workbench.reload();subjects();workbench.awaitText("Requires catalog 6");workbench.awaitText("Requires catalog 8");
        assertTrue(workbench.page().locator("#subject-curatorLesson-mode").isDisabled());
        String saved=storedDocument();ObjectNode item=((ObjectNode)subject()).deepCopy().put("curatorLesson",true);
        assertEquals(409,command(item.path("id").asString(),item,null).status());assertEquals(saved,storedDocument());
    }

    @Test
    void unsavedNavigationFailureResizeAndSubjectSelectionSaveContinue() throws Exception {
        create();name("First");save();String saved=storedDocument();name("Retained working subject");
        for(int width:List.of(1600,1280,390,700)) {
            workbench.viewport(width,844);workbench.expect("document.documentElement.scrollWidth <= innerWidth");assertEquals(saved,storedDocument());
            if(width<=700) {assertEquals(0,workbench.page().locator("#subject-form").count());workbench.awaitText("First");}
        }
        workbench.viewport(1280,800);assertEquals("Retained working subject",workbench.page().locator("#subject-displayName").inputValue());
        nav("Review");workbench.page().locator("#school-stay").click();assertEquals(saved,storedDocument());
        nav("Review");workbench.page().locator("#school-discard-working").click();subjects();assertEquals("First",workbench.page().locator("#subject-displayName").inputValue());
        name("Save on leaving");
        withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'DEFINITION_DRAFT'",()-> {nav("Review");workbench.page().locator("#school-save-continue").click();workbench.awaitText("No change was committed");assertEquals(saved,storedDocument());assertEquals("Save on leaving",workbench.page().locator("#subject-displayName").inputValue());});
        nav("Review");workbench.page().locator("#school-save-continue").click();workbench.expect("document.querySelector('#authoring-section h3')?.textContent === 'Review'");
        assertEquals("Save on leaving",subject().path("displayName").asString());subjects();name("Rename before addition");
        workbench.page().locator("#add-subject").click();workbench.page().locator("#school-save-continue").click();workbench.expect("document.querySelector('#subject-displayName')?.value === ''");
        assertEquals("Rename before addition",subject().path("displayName").asString());name("Second");save();assertEquals(2,definition().path("subjects").size());assertEquals("Rename before addition",subject().path("displayName").asString());
    }

    @Test
    void staleMergesUntouchedFieldsAndRequiresExplicitConflictChoice() {
        create();name("Original");save();name("Working name");
        ObjectNode newer=((ObjectNode)subject()).deepCopy().put("dayEdgeOnly",true);assertEquals(200,command("original",newer,null).status());
        String saved=storedDocument();clickSave();workbench.awaitText("No change was committed");assertEquals(saved,storedDocument());
        workbench.page().locator("#subject-refresh").click();workbench.expect("document.querySelector('#subject-dayEdgeOnly-mode')?.value === 'true'");
        assertEquals("Working name",workbench.page().locator("#subject-displayName").inputValue());save();assertEquals(true,subject().path("dayEdgeOnly").booleanValue());
        name("My conflicting name");newer=((ObjectNode)subject()).deepCopy().put("displayName","Other tab name");assertEquals(200,command("original",newer,null).status());saved=storedDocument();clickSave();workbench.awaitText("No change was committed");
        workbench.page().locator("#subject-refresh").click();workbench.awaitText("Newer saved fields conflict");assertTrue(workbench.page().locator("#save-subject").isDisabled());assertEquals(saved,storedDocument());
        workbench.page().locator("#subject-use-latest").click();assertEquals("Other tab name",workbench.page().locator("#subject-displayName").inputValue());save();
        name("My reviewed name");newer=((ObjectNode)subject()).deepCopy().put("displayName","Another name");assertEquals(200,command("original",newer,null).status());clickSave();workbench.awaitText("No change was committed");workbench.page().locator("#subject-refresh").click();workbench.awaitText("Newer saved fields conflict");
        workbench.page().locator("#subject-keep-conflicts").click();save();assertEquals("My reviewed name",subject().path("displayName").asString());
    }

    @Test
    void sourceCurrentQualificationsLessonsPoliciesAndCuratorExceptionStayExact() throws Exception {
        for(boolean accepted:List.of(false,true)) {
            ObjectNode source=fixtures.validAcceptedDocument();
            if(accepted) storeAccepted(source); else {ObjectNode initial=JSON.createObjectNode();initial.set("initialDefinition",source.path("acceptedBaseline").path("definition"));source=initial;store("INITIAL_DRAFT",source);}
            workbench.open();open();subjects();ObjectNode opened=(ObjectNode)storedWorkspaceDocument(),definition=(ObjectNode)opened.path("definitionDraft").path("definition");
            definition.put("catalogVersion",9);
            ObjectNode subject=(ObjectNode)definition.path("subjects").get(0);String id=subject.path("id").asString();
            ObjectNode cohort=(ObjectNode)definition.path("cohorts").get(0);cohort.put("curatorTeacherId","teacher-alex").put("homeRoomId","room-102");
            ((ObjectNode)definition.path("teachers").get(0)).putArray("qualifiedSubjectIds");
            definition.putArray("roomAssignments").addObject().put("id","subject-room-policy").put("subjectId",id).putArray("allowedRoomIds").add("room-102");
            store("DEFINITION_DRAFT",opened);workbench.reload();subjects();
            JsonNode exactDefinition=definition.deepCopy();mode("curatorLesson","true");save();
            ObjectNode expected=(ObjectNode)exactDefinition.deepCopy();((ObjectNode)expected.path("subjects").get(0)).put("curatorLesson",true);assertEquals(expected,definition());
            assertEquals(source,draft().path("sourceDocument"));for(var entry:source.properties())assertEquals(entry.getValue(),storedWorkspaceDocument().path(entry.getKey()));
            assertFalse(draft().path("issues").toString().contains("not qualified"));workbench.awaitText("Room policy subject-room-policy");
            // Curator change makes the lesson invalid, but subject save never silently reassigns it.
            opened=(ObjectNode)storedWorkspaceDocument();((ObjectNode)opened.path("definitionDraft").path("definition").path("cohorts").get(0)).put("curatorTeacherId","other-teacher");
            store("DEFINITION_DRAFT",opened);workbench.reload();subjects();name("Curator subject renamed");save();assertTrue(draft().path("issues").toString().contains("configured curator"));
            assertEquals(expected.path("lessons"),definition().path("lessons"));assertEquals(expected.path("teachers"),definition().path("teachers"));assertEquals(expected.path("roomAssignments"),definition().path("roomAssignments"));
            mode("curatorLesson","false");save();assertTrue(draft().path("issues").toString().contains("not qualified"));
        }
    }

    @Test
    void identityStateAccessTransportAndStorageRefusalsAreAtomic() {
        create();name("Existing");save();String saved=storedDocument();ObjectNode item=JSON.createObjectNode().put("id","existing").put("displayName","Collision");
        assertEquals(409,command("",item,null).status());assertEquals(saved,storedDocument());
        assertEquals(409,command("existing",item.deepCopy().put("id","renamed"),null).status());assertEquals(saved,storedDocument());
        assertEquals(400,command("existing",item.deepCopy().put("catalogVersion",9),null).status());assertEquals(saved,storedDocument());
        assertEquals(422,command("",item.deepCopy().put("id","invalid id"),null).status());assertEquals(saved,storedDocument());
        ObjectNode doc=(ObjectNode)storedWorkspaceDocument();((ObjectNode)doc.path("definitionDraft")).withArray("removedIdentities").addObject().put("ownerType","Subject").put("id","reserved");store("DEFINITION_DRAFT",doc);saved=storedDocument();
        assertEquals(409,command("",item.deepCopy().put("id","reserved"),null).status());assertEquals(saved,storedDocument());
        workbench.reload();subjects();workbench.page().locator("#add-subject").click();name("Existing");assertEquals("existing-2",workbench.page().locator("#subject-id").inputValue());
        name("Transport working");workbench.page().route("**/api/definition-draft/subjects",route->route.abort());clickSave();workbench.awaitText("No change was committed");assertEquals(saved,storedDocument());assertEquals("Transport working",workbench.page().locator("#subject-displayName").inputValue());workbench.page().unroute("**/api/definition-draft/subjects");
        APIResponse csrf=workbench.page().request().post(base()+"/api/definition-draft/subjects",RequestOptions.create().setData(request("",item)).setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match","\"ws-"+storedVersion()+"\""));assertEquals(403,csrf.status());assertEquals(saved,storedDocument());
        for(WorkspaceState state:WorkspaceState.values()) {if(state==WorkspaceState.DEFINITION_DRAFT)continue;store(state.name(),storedWorkspaceDocument());saved=storedDocument();assertEquals(409,command("",item,null).status());assertEquals(saved,storedDocument());}
    }
    private JsonNode definition(){return draft().path("definition");}
    private JsonNode draft(){return storedWorkspaceDocument().path("definitionDraft");}
    private JsonNode subject(){return definition().path("subjects").get(0);}
    private void create(){workbench.open();open();subjects();}
    private void open(){workbench.page().locator("#open-authoring").click();workbench.page().locator("#confirm-authoring").click();workbench.awaitText("Saved school-definition draft");}
    private void nav(String section){workbench.page().locator("button[data-section='"+section+"']").click();}
    private void subjects(){nav("Subjects");workbench.awaitText("Subject name");}
    private void name(String value){workbench.page().locator("#subject-displayName").fill(value);}
    private void mode(String field,String value){workbench.page().locator("#subject-"+field+"-mode").selectOption(value);}
    private void number(String field,String value){mode(field,"value");workbench.page().locator("#subject-"+field).fill(value);}
    private void clickSave(){workbench.page().locator("#save-subject").click();}
    private void save(){clickSave();workbench.expect("document.querySelector('#school-save-status')?.textContent.startsWith('Saved —')");}
    private String base(){return "http://localhost:"+port;}
    private String request(String original,JsonNode item){ObjectNode request=JSON.createObjectNode().put("originalId",original);request.set("item",item);return JSON.writeValueAsString(request);}
    private APIResponse command(String original,JsonNode item,String version){JsonNode csrf=JSON.readTree(workbench.page().request().get(base()+"/api/csrf").text());return workbench.page().request().post(base()+"/api/definition-draft/subjects",RequestOptions.create().setData(request(original,item)).setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match",version==null?"\"ws-"+storedVersion()+"\"":version).setHeader(csrf.path("headerName").asString(),csrf.path("token").asString()));}
    private void store(String state,JsonNode document){jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state, version=version+1, active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1").param("state",state).param("document",JSON.writeValueAsString(document)).update();}
}
