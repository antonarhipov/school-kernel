package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-7 room fields and implications at the durable administrator boundary. */
class DefinitionRoomsBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void fiveFieldsOpenTokensZeroOmissionSelectionAndRestart() {
        create();name("Õäš Room");assertEquals("oas-room",page().locator("#room-id").inputValue());page().locator("#room-id").fill("Room.ID_9");capacity("0");tokens("lab, Extra.Token_9");mode("explicit");save();
        ObjectNode expected=JSON.createObjectNode().put("id","Room.ID_9").put("displayName","Õäš Room").put("capacity",0);expected.putArray("capabilityIds").add("lab").add("Extra.Token_9");expected.putArray("availablePeriodIds");assertEquals(expected,room());assertNotNull(page().locator("#room-id").getAttribute("readonly"));assertEquals(2,draft().path("issues").size());
        mode("default");page().locator("input[data-reference-id='lab']").uncheck();page().locator("input[data-reference-id='Extra.Token_9']").uncheck();name("Renamed room");save();expected=JSON.createObjectNode().put("id","Room.ID_9").put("displayName","Renamed room").put("capacity",0);expected.putArray("capabilityIds");assertEquals(expected,room());
        String saved=storedDocument();name("Canceled");page().locator("#cancel-room").click();assertEquals(saved,storedDocument());assertEquals("Renamed room",page().locator("#room-displayName").inputValue());
        page().locator("#add-room").click();name("Second");capacity("12");save();assertEquals("second",page().locator("#room-id").inputValue());assertEquals(2,definition().path("rooms").size());
        saved=storedDocument();try(var restarted=startRestartableWorkspace()){int restartPort=((org.springframework.boot.web.server.context.WebServerApplicationContext)restarted).getWebServer().getPort();workbench.open(restartPort);rooms();assertEquals(saved,storedDocument());assertEquals(expected,room());}
    }

    @Test
    void invalidSupportedValuesTypesAndAtomicIdentityAccessStateGuards() {
        create();name(" ");capacity("-1");tokens("BAD ID, lab, lab");mode("explicit");page().locator("#room-availablePeriodIds-extra").fill("unknown, unknown");save();assertEquals(-1,room().path("capacity").intValue());assertEquals(8,draft().path("issues").size());assertTrue(draft().path("issues").toString().contains("Duplicate capability token"));
        ObjectNode item=((ObjectNode)room()).deepCopy(),typed=item.deepCopy().put("capacity",false).put("availablePeriodIds",false);typed.putArray("capabilityIds").add(false).add("x");assertEquals(200,command(item.path("id").asString(),typed).status());assertEquals(typed,room());assertTrue(draft().path("issues").toString().contains("Invalid capability token"));
        assertEquals(200,command(item.path("id").asString(),item).status());String saved=storedDocument();String id=item.path("id").asString();assertEquals(409,command("",item).status());assertEquals(saved,storedDocument());assertEquals(409,command(id,item.deepCopy().put("id","changed")).status());assertEquals(saved,storedDocument());assertEquals(422,command("",item.deepCopy().put("id","bad id")).status());assertEquals(saved,storedDocument());assertEquals(400,command(id,item.deepCopy().put("homeRoomId","invented")).status());assertEquals(saved,storedDocument());
        ObjectNode document=(ObjectNode)storedWorkspaceDocument();((ObjectNode)document.path("definitionDraft")).withArray("removedIdentities").addObject().put("ownerType","Room").put("id","reserved");store("DEFINITION_DRAFT",document);saved=storedDocument();assertEquals(409,command("",item.deepCopy().put("id","reserved")).status());assertEquals(saved,storedDocument());
        APIResponse csrf=page().request().post(base()+"/api/definition-draft/rooms",RequestOptions.create().setData(request(id,item)).setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match","\"ws-"+storedVersion()+"\""));assertEquals(403,csrf.status());assertEquals(saved,storedDocument());
        for(WorkspaceState state:WorkspaceState.values()){if(state==WorkspaceState.DEFINITION_DRAFT)continue;store(state.name(),storedWorkspaceDocument());saved=storedDocument();assertEquals(409,command(id,item).status());assertEquals(saved,storedDocument());}
    }

    @Test
    void lastCapabilityAdvisoryPolicyLockAndHomeRoomConsequencesPreserveSources() {
        for(boolean accepted:List.of(false,true)) {
            ObjectNode source=fixtures.validAcceptedDocument();if(accepted)storeAccepted(source);else{ObjectNode initial=JSON.createObjectNode();initial.set("initialDefinition",source.path("acceptedBaseline").path("definition"));source=initial;store("INITIAL_DRAFT",source);}
            workbench.open();open();rooms();JsonNode original=definition().deepCopy();page().locator("input[data-reference-id='lab']").uncheck();save();assertTrue(draft().path("issues").toString().contains("No configured room supplies"));assertFalse(draft().path("issues").toString().contains("BLOCKING"));assertEquals(source,draft().path("sourceDocument"));for(var e:source.properties())assertEquals(e.getValue(),storedWorkspaceDocument().path(e.getKey()));for(String key:List.of("lessons","cohorts","teachers","subjects","periods"))assertEquals(original.path(key),definition().path(key));
            page().locator("input[data-reference-id='lab']").check();save();assertTrue(draft().path("issues").isEmpty());
            ObjectNode doc=(ObjectNode)storedWorkspaceDocument(),def=(ObjectNode)doc.path("definitionDraft").path("definition");def.put("catalogVersion",9);((ObjectNode)def.path("cohorts").get(0)).put("homeRoomId","room-101").put("curatorTeacherId","teacher-alex");((ObjectNode)def.path("subjects").get(0)).put("curatorLesson",true);((ObjectNode)def.path("lessons").get(0)).put("roomLock","room-101").put("periodLock","mon-1");((ObjectNode)def.path("lessons").get(0)).putArray("requiredRoomCapabilityIds").add("lab");def.putArray("roomAssignments").addObject().put("id","home-policy").put("subjectId","math").put("useHomeRoom",true);store("DEFINITION_DRAFT",doc);workbench.reload();rooms();JsonNode others=definition().deepCopy();
            capacity("0");page().locator("input[data-reference-id='lab']").uncheck();mode("explicit");save();String issues=draft().path("issues").toString();assertTrue(issues.contains("Curator home room contradicts"), issues);assertTrue(issues.contains("Persistent room lock has insufficient capacity"), issues);assertTrue(issues.contains("cannot supply required room capabilities"), issues);assertTrue(issues.contains("unavailable at the locked period"), issues);assertTrue(issues.contains("No matched policy room is available"), issues);assertTrue(issues.contains("Matched room policies have no room with sufficient"), issues);assertEquals(others.path("lessons"),definition().path("lessons"));assertEquals(others.path("cohorts"),definition().path("cohorts"));assertEquals(others.path("roomAssignments"),definition().path("roomAssignments"));workbench.awaitText("Class home room:");
            capacity("30");page().locator("input[data-reference-id='lab']").check();mode("default");save();assertTrue(draft().path("issues").isEmpty());assertEquals(source,draft().path("sourceDocument"));
        }
    }

    @Test
    void responsiveUnsavedStorageTransportAndThreeWayConflicts() throws Exception {
        create();name("First");capacity("24");save();String saved=storedDocument();name("Working retained");
        for(int width:List.of(1600,1280,390,700)){workbench.viewport(width,844);workbench.expect("document.documentElement.scrollWidth <= innerWidth");assertEquals(saved,storedDocument());if(width<=700){assertEquals(0,page().locator("#room-form").count());workbench.awaitText("First");}}
        workbench.viewport(1280,800);assertEquals("Working retained",page().locator("#room-displayName").inputValue());nav("Review");page().locator("#school-stay").click();nav("Review");page().locator("#school-discard-working").click();rooms();
        name("Save on leaving");withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'DEFINITION_DRAFT'",()->{nav("Review");page().locator("#school-save-continue").click();workbench.awaitText("No change was committed");assertEquals(saved,storedDocument());assertEquals("Save on leaving",page().locator("#room-displayName").inputValue());});
        nav("Review");page().locator("#school-save-continue").click();workbench.expect("document.querySelector('#authoring-section h3')?.textContent === 'Review'");rooms();name("Stale working");ObjectNode newer=((ObjectNode)room()).deepCopy().put("capacity",28);assertEquals(200,command("first",newer).status());String other=storedDocument();clickSave();workbench.awaitText("No change was committed");assertEquals(other,storedDocument());page().locator("#room-refresh").click();workbench.expect("document.querySelector('#room-capacity')?.value === '28'");assertEquals("Stale working",page().locator("#room-displayName").inputValue());save();assertEquals(28,room().path("capacity").intValue());
        capacity("27");newer=((ObjectNode)room()).deepCopy().put("capacity",29);assertEquals(200,command("first",newer).status());other=storedDocument();clickSave();workbench.awaitText("No change was committed");page().locator("#room-refresh").click();workbench.awaitText("Newer saved fields conflict");assertTrue(page().locator("#save-room").isDisabled());assertEquals(other,storedDocument());page().locator("#room-use-latest").click();save();assertEquals(29,room().path("capacity").intValue());
        name("Transport retained");String beforeTransport=storedDocument();page().route("**/api/definition-draft/rooms",route->route.abort());clickSave();workbench.awaitText("No change was committed");assertEquals(beforeTransport,storedDocument());assertEquals("Transport retained",page().locator("#room-displayName").inputValue());page().unroute("**/api/definition-draft/rooms");
    }

    private com.microsoft.playwright.Page page(){return workbench.page();}
    private void create(){workbench.open();open();rooms();}
    private void open(){page().locator("#open-authoring").click();page().locator("#confirm-authoring").click();workbench.awaitText("Saved school-definition draft");}
    private void nav(String section){page().locator("button[data-section='"+section+"']").click();}
    private void expandForm() {
        var groups = workbench.page().locator("#room-form details");
        for (int i = 0; i < groups.count(); i++) if (groups.nth(i).getAttribute("open") == null) groups.nth(i).locator(":scope > summary").click();
    }
    private void rooms(){nav("Rooms");workbench.awaitText("Room name"); expandForm(); }
    private void name(String value){page().locator("#room-displayName").fill(value);}
    private void capacity(String value){page().locator("#room-capacity").fill(value);}
    private void tokens(String value){page().locator("#room-capabilityIds-extra").fill(value);}
    private void mode(String value){ expandForm(); page().locator("#room-availablePeriodIds-mode").selectOption(value); expandForm(); }
    private void clickSave(){page().locator("#save-room").click();}
    private void save(){long before=storedVersion();clickSave();workbench.expect("document.querySelector('#school-save-status')?.textContent.startsWith('Saved —')");assertEquals(before+1,storedVersion());}
    private JsonNode draft(){return storedWorkspaceDocument().path("definitionDraft");}
    private JsonNode definition(){return draft().path("definition");}
    private JsonNode room(){return definition().path("rooms").get(0);}
    private String base(){return "http://localhost:"+port;}
    private String request(String original,JsonNode item){ObjectNode r=JSON.createObjectNode().put("originalId",original);r.set("item",item);return JSON.writeValueAsString(r);}
    private APIResponse command(String original,JsonNode item){JsonNode csrf=JSON.readTree(page().request().get(base()+"/api/csrf").text());return page().request().post(base()+"/api/definition-draft/rooms",RequestOptions.create().setData(request(original,item)).setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match","\"ws-"+storedVersion()+"\"").setHeader(csrf.path("headerName").asString(),csrf.path("token").asString()));}
    private void store(String state,JsonNode document){jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state, version=version+1, active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1").param("state",state).param("document",JSON.writeValueAsString(document)).update();}
}
