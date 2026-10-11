package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-6 class fields, linked rules and source/Current fidelity at the real administrator boundary. */
class DefinitionCohortsBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void allTwelveFieldsDefaultsNoLimitStableIdentityAndRestart() {
        create();String opened=storedDocument();name("Õäš Class");assertEquals("oas-class",workbench.page().locator("#class-id").inputValue());workbench.page().locator("#class-id").fill("Class.ID_9");number("size","25");
        number("maxDailyLessonSpread","0");mode("maxDailyGaps","unlimited");number("latestStartSlot","2");number("preferredLatestStartSlot","3");number("dailyLessonSpreadLimit","0");
        link("curatorTeacherId","unresolved-teacher");link("homeRoomId","unresolved-room");mode("availablePeriodIds","explicit");mode("undesirablePeriodIds","explicit");assertEquals(opened,storedDocument());save();
        ObjectNode expected=JSON.createObjectNode().put("id","Class.ID_9").put("displayName","Õäš Class").put("size",25).put("maxDailyLessonSpread",0).put("maxDailyGaps",2147483647)
                .put("latestStartSlot",2).put("preferredLatestStartSlot",3).put("dailyLessonSpreadLimit",0).put("curatorTeacherId","unresolved-teacher").put("homeRoomId","unresolved-room");
        expected.putArray("availablePeriodIds");expected.putArray("undesirablePeriodIds");assertEquals(expected,cohort());assertEquals(4,draft().path("issues").size());assertNotNull(workbench.page().locator("#class-id").getAttribute("readonly"));
        for(String field:List.of("maxDailyLessonSpread","maxDailyGaps","latestStartSlot","preferredLatestStartSlot","dailyLessonSpreadLimit","curatorTeacherId","homeRoomId","availablePeriodIds","undesirablePeriodIds"))mode(field,"default");name("Renamed class");save();
        expected=JSON.createObjectNode().put("id","Class.ID_9").put("displayName","Renamed class").put("size",25);assertEquals(expected,cohort());assertEquals(2,draft().path("issues").size());workbench.awaitText("Default: 0");
        String saved=storedDocument();name("Working only");workbench.reload();classes();assertEquals(saved,storedDocument());assertEquals("Renamed class",workbench.page().locator("#class-displayName").inputValue());
        try(var restarted=startRestartableWorkspace()){int restartPort=((org.springframework.boot.web.server.context.WebServerApplicationContext)restarted).getWebServer().getPort();workbench.open(restartPort);classes();assertEquals(saved,storedDocument());assertEquals(expected,cohort());}
    }

    @Test
    void invalidSupportedValuesAndUnavailableCatalogControls() {
        create();name(" ");number("size","0");number("maxDailyLessonSpread","bad");number("maxDailyGaps","-1");number("latestStartSlot","0");number("preferredLatestStartSlot","2147483648");number("dailyLessonSpreadLimit","-1");
        link("curatorTeacherId","missing");link("homeRoomId","BAD ID");mode("availablePeriodIds","explicit");workbench.page().locator("#class-availablePeriodIds-extra").fill("unknown, unknown");save();
        assertEquals(0,cohort().path("size").intValue());assertEquals(2147483648L,cohort().path("preferredLatestStartSlot").longValue());assertEquals("bad",cohort().path("maxDailyLessonSpread").asString());assertEquals(13,draft().path("issues").size());
        assertTrue(draft().path("issues").toString().contains("Duplicate reference"));assertTrue(draft().path("issues").toString().contains("Unresolved reference"));
        ObjectNode original=((ObjectNode)cohort()).deepCopy();
        ObjectNode typed=original.deepCopy().put("size",false).put("availablePeriodIds",false).put("curatorTeacherId",false);
        typed.putArray("homeRoomId").add("unknown");typed.putNull("undesirablePeriodIds");
        APIResponse typedResponse=command(original.path("id").asString(),typed,null);assertEquals(200,typedResponse.status(),typedResponse.text());assertEquals(typed,cohort());
        for(String field:List.of("size","availablePeriodIds","curatorTeacherId","homeRoomId","undesirablePeriodIds"))assertTrue(draft().path("issues").toString().contains("\"field\":\""+field+"\""));
        assertEquals(200,command(original.path("id").asString(),original,null).status());
        ObjectNode document=(ObjectNode)storedWorkspaceDocument(),definition=(ObjectNode)document.path("definitionDraft").path("definition");definition.put("catalogVersion",1);ObjectNode item=(ObjectNode)definition.path("cohorts").get(0);item.remove(List.of("maxDailyLessonSpread","maxDailyGaps","latestStartSlot","preferredLatestStartSlot","dailyLessonSpreadLimit","curatorTeacherId","homeRoomId"));store("DEFINITION_DRAFT",document);workbench.reload();classes();
        for(String field:List.of("maxDailyLessonSpread","maxDailyGaps","latestStartSlot","preferredLatestStartSlot","dailyLessonSpreadLimit","curatorTeacherId","homeRoomId"))assertTrue(workbench.page().locator("#class-"+field+"-mode").isDisabled());
        workbench.awaitText("Requires catalog 4");workbench.awaitText("Requires catalog 5");workbench.awaitText("Requires catalog 6");workbench.awaitText("Requires catalog 7");workbench.awaitText("Requires catalog 8");
        String saved=storedDocument();for(String field:List.of("maxDailyLessonSpread","maxDailyGaps","latestStartSlot","preferredLatestStartSlot","dailyLessonSpreadLimit","curatorTeacherId","homeRoomId")){assertEquals(409,command(item.path("id").asString(),item.deepCopy().put(field,1),null).status());assertEquals(saved,storedDocument());}
    }

    @Test
    void curatorHomeRoomPoliciesAndExactSourceCurrentSurviveClassChanges() throws Exception {
        for(boolean accepted:List.of(false,true)) {
            ObjectNode source=fixtures.validAcceptedDocument();if(accepted)storeAccepted(source);else{ObjectNode initial=JSON.createObjectNode();initial.set("initialDefinition",source.path("acceptedBaseline").path("definition"));source=initial;store("INITIAL_DRAFT",source);}
            workbench.open();open();classes();ObjectNode opened=(ObjectNode)storedWorkspaceDocument(),definition=(ObjectNode)opened.path("definitionDraft").path("definition");definition.put("catalogVersion",9);
            ((ObjectNode)definition.path("subjects").get(0)).put("curatorLesson",true);ObjectNode lesson=(ObjectNode)definition.path("lessons").get(0);lesson.put("roomLock","room-101");lesson.putArray("requiredRoomCapabilityIds").add("lab");
            definition.putArray("roomAssignments").addObject().put("id","home-policy").put("subjectId","math").put("useHomeRoom",true);
            definition.withArray("roomAssignments").addObject().put("id","allowed-policy").put("subjectId","math").putArray("allowedRoomIds").add("room-101");
            store("DEFINITION_DRAFT",opened);workbench.reload();classes();JsonNode exactOthers=definition.deepCopy();number("size","31");link("curatorTeacherId","teacher-alex");link("homeRoomId","room-102");save();
            ObjectNode expected=((ObjectNode)exactOthers).deepCopy();((ObjectNode)expected.path("cohorts").get(0)).put("size",31).put("curatorTeacherId","teacher-alex").put("homeRoomId","room-102");assertEquals(expected,definition());
            assertEquals(source,draft().path("sourceDocument"));for(var entry:source.properties())assertEquals(entry.getValue(),storedWorkspaceDocument().path(entry.getKey()));
            String issues=draft().path("issues").toString();assertTrue(issues.contains("room capacity"));assertTrue(issues.contains("required room capabilities"));assertTrue(issues.contains("no room in common"));workbench.awaitText("Matched room policy home-policy");
            number("size","24");link("homeRoomId","room-101");save();assertTrue(draft().path("issues").isEmpty());assertEquals(exactOthers.path("lessons"),definition().path("lessons"));assertEquals(exactOthers.path("roomAssignments"),definition().path("roomAssignments"));
            mode("homeRoomId","default");save();assertFalse(cohort().has("homeRoomId"));assertTrue(draft().path("issues").toString().contains("requires this class to declare a home room"));
            link("homeRoomId","room-101");link("curatorTeacherId","other-teacher");save();assertTrue(draft().path("issues").toString().contains("configured curator"));assertEquals(exactOthers.path("teachers"),definition().path("teachers"));assertEquals(source,draft().path("sourceDocument"));
        }
    }

    @Test
    void loadRoomAndCapabilityShortagesAreAdvisoriesDistinctFromLocks() {
        ObjectNode source=fixtures.validAcceptedDocument();storeAccepted(source);workbench.open();open();classes();ObjectNode doc=(ObjectNode)storedWorkspaceDocument(),definition=(ObjectNode)doc.path("definitionDraft").path("definition");definition.put("catalogVersion",9);
        for(JsonNode room:definition.path("rooms"))((ObjectNode)room).putArray("capabilityIds");store("DEFINITION_DRAFT",doc);workbench.reload();classes();number("size","100");mode("availablePeriodIds","explicit");save();
        assertEquals(4,draft().path("issues").size());int teacherAdvisories=0;for(JsonNode issue:draft().path("issues")){assertEquals("ADVISORY",issue.path("severity").asString());if("Teacher".equals(issue.path("ownerType").asString()))teacherAdvisories++;}assertEquals(1,teacherAdvisories,"Complete teaching diagnostics additionally report the teacher load against the class availability bound.");
        assertTrue(draft().path("issues").toString().contains("eligible-period upper bound 0"));assertTrue(draft().path("issues").toString().contains("No configured room can hold"));assertTrue(draft().path("issues").toString().contains("required capability combination"));
        assertEquals(source.path("acceptedBaseline").path("definition").path("lessons"),definition().path("lessons"));
        doc=(ObjectNode)storedWorkspaceDocument();((ObjectNode)doc.path("definitionDraft").path("definition").path("lessons").get(0)).put("periodLock","mon-1");store("DEFINITION_DRAFT",doc);workbench.reload();classes();save();assertTrue(draft().path("issues").toString().contains("outside this class"));assertTrue(draft().path("issues").toString().contains("BLOCKING"));
    }

    @Test
    void responsiveUnsavedChoicesFailuresAndStaleReview() throws Exception {
        create();name("First");number("size","24");save();String saved=storedDocument();name("Working retained");
        for(int width:List.of(1600,1280,390,700)){workbench.viewport(width,844);workbench.expect("document.documentElement.scrollWidth <= innerWidth");assertEquals(saved,storedDocument());if(width<=700){assertEquals(0,workbench.page().locator("#class-form").count());workbench.awaitText("First");}}
        workbench.viewport(1280,800);assertEquals("Working retained",workbench.page().locator("#class-displayName").inputValue());nav("Review");workbench.page().locator("#school-stay").click();assertEquals(saved,storedDocument());nav("Review");workbench.page().locator("#school-discard-working").click();classes();
        name("Save on leaving");withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'DEFINITION_DRAFT'",()->{nav("Review");workbench.page().locator("#school-save-continue").click();workbench.awaitText("No change was committed");assertEquals(saved,storedDocument());assertEquals("Save on leaving",workbench.page().locator("#class-displayName").inputValue());});
        nav("Review");workbench.page().locator("#school-save-continue").click();workbench.expect("document.querySelector('#authoring-section h3')?.textContent === 'Review'");classes();name("Stale working");ObjectNode newer=((ObjectNode)cohort()).deepCopy().put("preferredLatestStartSlot",4);assertEquals(200,command("first",newer,null).status());String other=storedDocument();clickSave();workbench.awaitText("No change was committed");assertEquals(other,storedDocument());workbench.page().locator("#class-refresh").click();workbench.expect("document.querySelector('#class-preferredLatestStartSlot')?.value === '4'");assertEquals("Stale working",workbench.page().locator("#class-displayName").inputValue());save();assertEquals(4,cohort().path("preferredLatestStartSlot").intValue());
        number("size","27");newer=((ObjectNode)cohort()).deepCopy().put("size",28);assertEquals(200,command("first",newer,null).status());other=storedDocument();clickSave();workbench.awaitText("No change was committed");workbench.page().locator("#class-refresh").click();workbench.awaitText("Newer saved fields conflict");assertTrue(workbench.page().locator("#save-class").isDisabled());assertEquals(other,storedDocument());workbench.page().locator("#class-keep-conflicts").click();save();assertEquals(27,cohort().path("size").intValue());
        name("Transport retained");String beforeTransport=storedDocument();workbench.page().route("**/api/definition-draft/cohorts",route->route.abort());clickSave();workbench.awaitText("No change was committed");assertEquals(beforeTransport,storedDocument());assertEquals("Transport retained",workbench.page().locator("#class-displayName").inputValue());workbench.page().unroute("**/api/definition-draft/cohorts");
    }

    @Test
    void identityStateAccessAndSystemManagedFieldsRefuseAtomically() {
        create();name("Existing");number("size","24");save();String saved=storedDocument();ObjectNode item=((ObjectNode)cohort()).deepCopy();
        assertEquals(409,command("",item,null).status());assertEquals(saved,storedDocument());assertEquals(409,command("existing",item.deepCopy().put("id","renamed"),null).status());assertEquals(saved,storedDocument());assertEquals(422,command("",item.deepCopy().put("id","invalid id"),null).status());assertEquals(saved,storedDocument());assertEquals(400,command("existing",item.deepCopy().put("catalogVersion",9),null).status());assertEquals(saved,storedDocument());
        ObjectNode document=(ObjectNode)storedWorkspaceDocument();((ObjectNode)document.path("definitionDraft")).withArray("removedIdentities").addObject().put("ownerType","Cohort").put("id","reserved");store("DEFINITION_DRAFT",document);saved=storedDocument();assertEquals(409,command("",item.deepCopy().put("id","reserved"),null).status());assertEquals(saved,storedDocument());
        APIResponse csrf=workbench.page().request().post(base()+"/api/definition-draft/cohorts",RequestOptions.create().setData(request("existing",item)).setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match","\"ws-"+storedVersion()+"\""));assertEquals(403,csrf.status());assertEquals(saved,storedDocument());
        for(WorkspaceState state:WorkspaceState.values()){if(state==WorkspaceState.DEFINITION_DRAFT)continue;store(state.name(),storedWorkspaceDocument());saved=storedDocument();assertEquals(409,command("existing",item,null).status());assertEquals(saved,storedDocument());}
    }
    private void create(){workbench.open();open();classes();}
    private void open(){workbench.page().locator("#open-authoring").click();workbench.page().locator("#confirm-authoring").click();workbench.awaitText("Saved school-definition draft");}
    private void nav(String section){workbench.page().locator("button[data-section='"+section+"']").click();}
    private void classes(){nav("Classes");workbench.awaitText("Class name");}
    private void name(String value){workbench.page().locator("#class-displayName").fill(value);}
    private void mode(String field,String value){workbench.page().locator("#class-"+field+"-mode").selectOption(value);}
    private void number(String field,String value){if(!field.equals("size"))mode(field,"value");workbench.page().locator("#class-"+field).fill(value);}
    private void link(String field,String value){mode(field,"value");workbench.page().locator("#class-"+field).fill(value);}
    private void clickSave(){workbench.page().locator("#save-class").click();}
    private void save(){long before=storedVersion();clickSave();workbench.expect("document.querySelector('#school-save-status')?.textContent.startsWith('Saved —')");assertEquals(before+1,storedVersion());}
    private JsonNode draft(){return storedWorkspaceDocument().path("definitionDraft");}
    private JsonNode definition(){return draft().path("definition");}
    private JsonNode cohort(){return definition().path("cohorts").get(0);}
    private String base(){return "http://localhost:"+port;}
    private String request(String original,JsonNode item){ObjectNode request=JSON.createObjectNode().put("originalId",original);request.set("item",item);return JSON.writeValueAsString(request);}
    private APIResponse command(String original,JsonNode item,String version){JsonNode csrf=JSON.readTree(workbench.page().request().get(base()+"/api/csrf").text());return workbench.page().request().post(base()+"/api/definition-draft/cohorts",RequestOptions.create().setData(request(original,item)).setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match",version==null?"\"ws-"+storedVersion()+"\"":version).setHeader(csrf.path("headerName").asString(),csrf.path("token").asString()));}
    private void store(String state,JsonNode document){jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state, version=version+1, active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1").param("state",state).param("document",JSON.writeValueAsString(document)).update();}
}
