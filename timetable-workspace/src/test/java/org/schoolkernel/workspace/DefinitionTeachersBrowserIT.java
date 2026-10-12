package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-5 contract against real browser controls, HTTP guards and exact durable JSON. */
class DefinitionTeachersBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void explicitSelectionsOmittedVersusEmptyAndRestart() {
        create(); name("Õäš Teacher <b>A</b>"); assertEquals("oas-teacher-b-a-b",workbench.page().locator("#teacher-id").inputValue());
        workbench.page().locator("#teacher-id").fill("Teacher.ID_9");
        workbench.page().locator("#teacher-qualifiedSubjectIds-extra").fill("unresolved");
        mode("availablePeriodIds","explicit");mode("undesirablePeriodIds","explicit");String before=storedDocument();
        assertEquals(before,storedDocument()); save();
        ObjectNode expected=JSON.createObjectNode().put("id","Teacher.ID_9").put("displayName","Õäš Teacher <b>A</b>");
        expected.putArray("qualifiedSubjectIds").add("unresolved");expected.putArray("availablePeriodIds");expected.putArray("undesirablePeriodIds");
        assertEquals(expected,teacher());workbench.awaitText("Unresolved reference: unresolved");assertEquals(0,workbench.page().locator("#teacher-effects b").count());
        workbench.page().locator("#teacher-qualifiedSubjectIds-extra").fill("");mode("availablePeriodIds","default");mode("undesirablePeriodIds","default");name("Renamed teacher");save();
        expected.putArray("qualifiedSubjectIds");expected.remove("availablePeriodIds");expected.remove("undesirablePeriodIds");expected.put("displayName","Renamed teacher");assertEquals(expected,teacher());
        assertEquals(2,draft().path("issues").size());assertNotNull(workbench.page().locator("#teacher-id").getAttribute("readonly"));
        String saved=storedDocument();name("Working only");workbench.reload();teachers();assertEquals("Renamed teacher",workbench.page().locator("#teacher-displayName").inputValue());assertEquals(saved,storedDocument());
        try(var restarted=startRestartableWorkspace()) {int restartedPort=((org.springframework.boot.web.server.context.WebServerApplicationContext)restarted).getWebServer().getPort();workbench.open(restartedPort);teachers();assertEquals(saved,storedDocument());assertEquals(expected,teacher());}
    }

    @Test
    void qualificationAvailabilityCuratorAndAllRelatedReferencesPreserveSourceAndCurrent() throws Exception {
        for(boolean accepted:List.of(false,true)) {
            ObjectNode source=fixtures.validAcceptedDocument();
            if(accepted)storeAccepted(source);else {ObjectNode initial=JSON.createObjectNode();initial.set("initialDefinition",source.path("acceptedBaseline").path("definition"));source=initial;store("INITIAL_DRAFT",source);}
            workbench.open();open();teachers();ObjectNode opened=(ObjectNode)storedWorkspaceDocument();ObjectNode definition=(ObjectNode)opened.path("definitionDraft").path("definition");
            definition.put("catalogVersion",9);definition.putArray("reservedPeriodIds").add("mon-2");
            ((ObjectNode)definition.path("lessons").get(0)).put("periodLock","mon-1");
            JsonNode exactOthers=definition.deepCopy();store("DEFINITION_DRAFT",opened);workbench.reload();teachers();workbench.awaitText("Reserved");
            select("qualifiedSubjectIds","math",false);mode("availablePeriodIds","explicit");select("availablePeriodIds","mon-2",true);mode("undesirablePeriodIds","explicit");select("undesirablePeriodIds","mon-1",true);save();
            ObjectNode expected=((ObjectNode)exactOthers).deepCopy();ObjectNode teacher=(ObjectNode)expected.path("teachers").get(0);
            teacher.putArray("qualifiedSubjectIds").add("science");teacher.putArray("availablePeriodIds").add("mon-2");teacher.putArray("undesirablePeriodIds").add("mon-1");
            assertEquals(expected,definition());assertEquals(source,draft().path("sourceDocument"));for(var entry:source.properties())assertEquals(entry.getValue(),storedWorkspaceDocument().path(entry.getKey()));
            assertTrue(draft().path("issues").toString().contains("not qualified"));assertTrue(draft().path("issues").toString().contains("outside this teacher"));workbench.awaitText("lesson-math-1");
            // Explicit curator exception: empty ordinary qualifications are not implicitly expanded.
            opened=(ObjectNode)storedWorkspaceDocument();definition=(ObjectNode)opened.path("definitionDraft").path("definition");
            ((ObjectNode)definition.path("subjects").get(0)).put("curatorLesson",true);((ObjectNode)definition.path("cohorts").get(0)).put("curatorTeacherId","teacher-alex");
            store("DEFINITION_DRAFT",opened);workbench.reload();teachers();mode("availablePeriodIds","default");save();
            assertFalse(draft().path("issues").toString().contains("not qualified"));assertFalse(draft().path("issues").toString().contains("outside this teacher"));assertEquals(JSON.readTree("[\"science\"]"),teacher().path("qualifiedSubjectIds"));
            opened=(ObjectNode)storedWorkspaceDocument();((ObjectNode)opened.path("definitionDraft").path("definition").path("cohorts").get(0)).put("curatorTeacherId","another-teacher");store("DEFINITION_DRAFT",opened);workbench.reload();teachers();save();assertTrue(draft().path("issues").toString().contains("configured curator"));
            assertEquals(exactOthers.path("lessons"),definition().path("lessons"));assertEquals(exactOthers.path("rooms"),definition().path("rooms"));assertEquals(source,draft().path("sourceDocument"));
        }
    }

    @Test
    void invalidReferenceArraysSaveCompleteBlockersAndCorrectWithoutCascade() {
        create();name(" ");workbench.page().locator("#teacher-qualifiedSubjectIds-extra").fill("missing-subject, missing-subject, BAD ID");
        mode("availablePeriodIds","explicit");workbench.page().locator("#teacher-availablePeriodIds-extra").fill("missing-period");mode("undesirablePeriodIds","explicit");workbench.page().locator("#teacher-undesirablePeriodIds-extra").fill("unknown-preference");save();
        assertEquals(JSON.readTree("[\"missing-subject\",\"missing-subject\",\"BAD ID\"]"),teacher().path("qualifiedSubjectIds"));
        assertEquals(8,draft().path("issues").size());workbench.awaitText("Duplicate reference");workbench.awaitText("Invalid reference");workbench.awaitText("Unresolved reference: missing-period");
        ObjectNode item=((ObjectNode)teacher()).deepCopy();item.put("qualifiedSubjectIds","invalid array");item.put("availablePeriodIds",false);item.putNull("undesirablePeriodIds");assertEquals(200,command(item.path("id").asString(),item,null).status());
        assertEquals(item,teacher());assertEquals(6,draft().path("issues").size());
        workbench.reload();teachers();workbench.awaitText("Saved invalid selection");name("Corrected teacher");workbench.page().locator("#teacher-qualifiedSubjectIds-extra").fill(" ");mode("availablePeriodIds","default");mode("undesirablePeriodIds","default");save();
        assertEquals(JSON.readTree("[]"),teacher().path("qualifiedSubjectIds"));assertEquals(2,draft().path("issues").size());
    }

    @Test
    void unsavedChoicesResponsiveFailuresAndSelectionSaveContinue() throws Exception {
        create();name("First");save();String saved=storedDocument();name("Working retained");
        for(int width:List.of(1600,1280,390,700)){workbench.viewport(width,844);workbench.expect("document.documentElement.scrollWidth <= innerWidth");assertEquals(saved,storedDocument());if(width<=700){assertEquals(0,workbench.page().locator("#teacher-form").count());workbench.awaitText("First");}}
        workbench.viewport(1280,800);assertEquals("Working retained",workbench.page().locator("#teacher-displayName").inputValue());nav("Review");workbench.page().locator("#school-stay").click();assertEquals(saved,storedDocument());
        nav("Review");workbench.page().locator("#school-discard-working").click();teachers();assertEquals("First",workbench.page().locator("#teacher-displayName").inputValue());
        name("Save on leaving");withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'DEFINITION_DRAFT'",()->{nav("Review");workbench.page().locator("#school-save-continue").click();workbench.awaitText("No change was committed");assertEquals(saved,storedDocument());assertEquals("Save on leaving",workbench.page().locator("#teacher-displayName").inputValue());});
        nav("Review");workbench.page().locator("#school-save-continue").click();workbench.expect("document.querySelector('#authoring-section h3')?.textContent === 'Review'");assertEquals("Save on leaving",teacher().path("displayName").asString());
        teachers();name("Rename before addition");workbench.page().locator("#add-teacher").click();workbench.page().locator("#school-save-continue").click();workbench.expect("document.querySelector('#teacher-displayName')?.value === ''");assertEquals("Rename before addition",teacher().path("displayName").asString());name("Second");save();assertEquals(2,definition().path("teachers").size());
        name("Transport retained");String transportSaved=storedDocument();workbench.page().route("**/api/definition-draft/teachers",route->route.abort());clickSave();workbench.awaitText("No change was committed");assertEquals(transportSaved,storedDocument());assertEquals("Transport retained",workbench.page().locator("#teacher-displayName").inputValue());workbench.page().unroute("**/api/definition-draft/teachers");
    }

    @Test
    void staleConflictIdentityAndAccessChecksHaveNoPartialMutation() {
        create();name("Original");save();name("Working name");ObjectNode newer=((ObjectNode)teacher()).deepCopy();newer.putArray("undesirablePeriodIds");assertEquals(200,command("original",newer,null).status());String saved=storedDocument();
        clickSave();workbench.awaitText("No change was committed");assertEquals(saved,storedDocument());workbench.page().locator("#teacher-refresh").click();workbench.expect("document.querySelector('#teacher-undesirablePeriodIds-mode')?.value === 'explicit'");assertEquals("Working name",workbench.page().locator("#teacher-displayName").inputValue());save();assertTrue(teacher().has("undesirablePeriodIds"));
        name("My conflicting name");newer=((ObjectNode)teacher()).deepCopy().put("displayName","Newer name");assertEquals(200,command("original",newer,null).status());saved=storedDocument();clickSave();workbench.awaitText("No change was committed");workbench.page().locator("#teacher-refresh").click();workbench.awaitText("Newer saved fields conflict");assertTrue(workbench.page().locator("#save-teacher").isDisabled());assertEquals(saved,storedDocument());
        workbench.page().locator("#teacher-use-latest").click();save();assertEquals("Newer name",teacher().path("displayName").asString());saved=storedDocument();
        ObjectNode item=((ObjectNode)teacher()).deepCopy();assertEquals(409,command("",item,null).status());assertEquals(saved,storedDocument());assertEquals(409,command("original",item.deepCopy().put("id","renamed"),null).status());assertEquals(saved,storedDocument());
        assertEquals(422,command("",item.deepCopy().put("id","invalid id"),null).status());assertEquals(saved,storedDocument());assertEquals(400,command("original",item.deepCopy().put("catalogVersion",9),null).status());assertEquals(saved,storedDocument());
        ObjectNode document=(ObjectNode)storedWorkspaceDocument();((ObjectNode)document.path("definitionDraft")).withArray("removedIdentities").addObject().put("ownerType","Teacher").put("id","reserved");store("DEFINITION_DRAFT",document);saved=storedDocument();assertEquals(409,command("",item.deepCopy().put("id","reserved"),null).status());assertEquals(saved,storedDocument());
        APIResponse csrf=workbench.page().request().post(base()+"/api/definition-draft/teachers",RequestOptions.create().setData(request("original",item)).setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match","\"ws-"+storedVersion()+"\""));assertEquals(403,csrf.status());assertEquals(saved,storedDocument());
        for(WorkspaceState state:WorkspaceState.values()){if(state==WorkspaceState.DEFINITION_DRAFT)continue;store(state.name(),storedWorkspaceDocument());saved=storedDocument();assertEquals(409,command("original",item,null).status());assertEquals(saved,storedDocument());}
    }
    private void create(){workbench.open();open();teachers();}
    private void open(){workbench.page().locator("#open-authoring").click();workbench.page().locator("#confirm-authoring").click();workbench.awaitText("Saved school-definition draft");}
    private void nav(String section){workbench.page().locator("button[data-section='"+section+"']").click();}
    private void expandForm() {
        var groups = workbench.page().locator("#teacher-form details");
        for (int i = 0; i < groups.count(); i++) if (groups.nth(i).getAttribute("open") == null) groups.nth(i).locator(":scope > summary").click();
    }
    private void teachers(){nav("Teachers");workbench.awaitText("Teacher name"); expandForm(); }
    private void name(String value){workbench.page().locator("#teacher-displayName").fill(value);}
    private void mode(String field,String value){ expandForm(); workbench.page().locator("#teacher-"+field+"-mode").selectOption(value); expandForm(); }
    private void select(String field,String id,boolean value){workbench.page().locator("#teacher-form input[data-field='"+field+"'][data-reference-id='"+id+"']").setChecked(value);}
    private void clickSave(){workbench.page().locator("#save-teacher").click();}
    private void save(){long before=storedVersion();clickSave();workbench.expect("document.querySelector('#school-save-status')?.textContent.startsWith('Saved —')");assertEquals(before+1,storedVersion());}
    private JsonNode draft(){return storedWorkspaceDocument().path("definitionDraft");}
    private JsonNode definition(){return draft().path("definition");}
    private JsonNode teacher(){return definition().path("teachers").get(0);}
    private String base(){return "http://localhost:"+port;}
    private String request(String original,JsonNode item){ObjectNode request=JSON.createObjectNode().put("originalId",original);request.set("item",item);return JSON.writeValueAsString(request);}
    private APIResponse command(String original,JsonNode item,String version){JsonNode csrf=JSON.readTree(workbench.page().request().get(base()+"/api/csrf").text());return workbench.page().request().post(base()+"/api/definition-draft/teachers",RequestOptions.create().setData(request(original,item)).setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match",version==null?"\"ws-"+storedVersion()+"\"":version).setHeader(csrf.path("headerName").asString(),csrf.path("token").asString()));}
    private void store(String state,JsonNode document){jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state, version=version+1, active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1").param("state",state).param("document",JSON.writeValueAsString(document)).update();}
}
