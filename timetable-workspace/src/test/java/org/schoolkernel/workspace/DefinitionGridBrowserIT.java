package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-3 main, extensions, atomicity and exact fidelity at the local administrator boundary. */
class DefinitionGridBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void uc3MainGeneratedRowsIndividualAdjustmentExplicitReservationAndRestart() {
        create(); String opened = storedDocument();
        workbench.page().locator("#grid-bell-start-1").fill("09:00");
        workbench.page().locator("#grid-bell-end-1").fill("09:45");
        workbench.page().locator("#grid-bell-start-2").fill("10:00");
        workbench.page().locator("#grid-bell-end-2").fill("10:40");
        workbench.page().locator("#generate-grid").click();
        for (String day : List.of("monday", "tuesday", "wednesday", "thursday", "friday")) field(day+"-2", "endTime").fill("10:45");
        assertEquals(opened, storedDocument());
        review(); assertEquals(opened, storedDocument()); save();
        JsonNode grid = definition().path("periods");
        var expected = JSON.createArrayNode();
        for (String day : List.of("monday", "tuesday", "wednesday", "thursday", "friday")) for (int slot = 1; slot <= 2; slot++) {
            expected.addObject().put("id", day+"-"+slot).put("displayName", Character.toUpperCase(day.charAt(0))+day.substring(1)+" "+slot)
                    .put("weekday", day.toUpperCase()).put("order", slot).put("startTime", slot==1 ? "09:00" : "10:00").put("endTime", slot==1 ? "09:45" : "10:45");
        }
        assertEquals(expected, grid); assertFalse(definition().has("reservedPeriodIds"));
        assertEquals(2, draft().path("issues").size(), "only school identity remains incomplete");
        // Reservation is explicit and unrelated to name, order or time.
        field("monday-1", "displayName").fill("Õppetund <b>Reserved-looking name</b>");
        workbench.page().locator(".grid-period-row[data-period-id='monday-2'] input[data-field=reserved]").check(); review(); save();
        assertEquals(JSON.readTree("[\"monday-2\"]"), definition().path("reservedPeriodIds"));
        assertEquals(0, workbench.page().locator(".grid-period-row b").count());
        String durable = storedDocument(); workbench.reload(); grid(); assertEquals(durable, storedDocument());
        try (var restarted = startRestartableWorkspace()) {
            int restartedPort = ((org.springframework.boot.web.server.context.WebServerApplicationContext) restarted).getWebServer().getPort();
            workbench.open(restartedPort); grid(); assertEquals(durable, storedDocument());
            assertEquals("Õppetund <b>Reserved-looking name</b>", field("monday-1", "displayName").inputValue());
            assertEquals("10:45", field("friday-2", "endTime").inputValue());
        }
    }

    @Test
    void uc3InvalidRowsCompleteDiagnosticsCancellationAndResponsiveUnsavedNavigation() {
        create(); workbench.page().locator("#generate-grid").click(); review(); save(); String saved = storedDocument();
        field("monday-1", "order").fill("2"); field("monday-1", "startTime").fill("09:00"); field("monday-1", "endTime").fill("10:00");
        field("monday-2", "startTime").fill("09:30"); field("monday-2", "endTime").fill("10:30");
        review(); workbench.awaitText("Duplicate weekday/order"); workbench.awaitText("Clock interval overlaps"); assertEquals(saved, storedDocument());
        ObjectNode conflicts = gridRequest();
        ((ObjectNode) conflicts.path("periods").get(0)).put("order", 2).put("startTime", "09:00").put("endTime", "10:00");
        ((ObjectNode) conflicts.path("periods").get(1)).put("startTime", "09:30").put("endTime", "10:30");
        JsonNode complete = JSON.readTree(command("/preview", conflicts, null).text());
        var expectedIssues = JSON.createArrayNode();
        for (String id : List.of("monday-1", "monday-2")) {
            String other = id.equals("monday-1") ? "monday-2" : "monday-1";
            expectedIssues.addObject().put("severity", "BLOCKING").put("ownerType", "Period").put("ownerId", id).put("field", "order")
                    .put("message", "Duplicate weekday/order with "+other+".").putArray("relatedIds").add(other);
            expectedIssues.addObject().put("severity", "BLOCKING").put("ownerType", "Period").put("ownerId", id).put("field", "startTime")
                    .put("message", "Clock interval overlaps "+other+".").putArray("relatedIds").add(other);
        }
        var actualIssues = JSON.createArrayNode();
        for (JsonNode issue : complete.path("issues")) if ("Period".equals(issue.path("ownerType").stringValue())) actualIssues.add(issue);
        assertEquals(expectedIssues, actualIssues); assertEquals(saved, storedDocument());
        workbench.page().locator("#cancel-grid").click(); assertEquals(saved, storedDocument()); assertEquals("1", field("monday-1", "order").inputValue());
        field("monday-1", "order").fill("bad order"); field("monday-1", "startTime").fill("25:00");
        field("monday-2", "startTime").fill("09:00:00"); field("monday-2", "endTime").fill("08:00");
        for (int[] viewport : List.of(new int[]{1600,900}, new int[]{1280,800}, new int[]{390,844}, new int[]{700,844})) {
            workbench.viewport(viewport[0], viewport[1]); workbench.expect("document.documentElement.scrollWidth <= innerWidth"); assertEquals(saved, storedDocument());
            if (viewport[0] <= 700) { assertEquals(0, workbench.page().locator(".grid-period-row input, .grid-period-row select, #save-grid, #generate-grid").count()); workbench.awaitText("Read-only inspection"); }
        }
        workbench.viewport(1280,800); assertEquals("bad order", field("monday-1", "order").inputValue());
        workbench.page().locator("button[data-section=Review]").click(); workbench.page().locator("#school-stay").click(); assertEquals("bad order", field("monday-1", "order").inputValue());
        review(); save(); assertEquals("bad order", definition().path("periods").get(0).path("order").stringValue());
        assertTrue(draft().path("issues").toString().contains("Supply both start and end")); assertTrue(draft().path("issues").toString().contains("positive duration"));
        assertEquals(saved.substring(saved.indexOf(':')+1).contains("bad order"), false);
        String invalid = storedDocument(); workbench.reload(); grid(); assertEquals("bad order", field("monday-1", "order").inputValue()); assertEquals(invalid, storedDocument());
        field("monday-1", "displayName").fill("Discard me"); workbench.page().locator("button[data-section=Review]").click(); workbench.page().locator("#school-discard-working").click();
        assertEquals("Review", workbench.page().locator("#authoring-section h3").textContent()); assertEquals(invalid, storedDocument());
        grid(); field("monday-1", "displayName").fill("Save and continue");
        workbench.page().locator("button[data-section=Review]").click(); workbench.page().locator("#school-save-continue").click();
        workbench.expect("document.querySelector('#authoring-section h3')?.textContent === 'Review'");
        assertEquals("Save and continue", definition().path("periods").get(0).path("displayName").stringValue());
    }

    @Test
    void uc3ExistingGridAdditionsAvailabilityExactSourceAndSameIdTimingImpact() throws Exception {
        ObjectNode source = fixtures.validAcceptedDocument(); storeAccepted(source); workbench.open(); open(); grid();
        JsonNode before = definition().deepCopy();
        // Source includes real accepted placements; no assignments are materialized into lesson defaults.
        String periodId = source.path("acceptedBaseline").path("result").path("timetable").path("assignments").get(0).path("periodId").stringValue();
        field(periodId, "startTime").fill("08:50:00"); field(periodId, "endTime").fill("09:35:00");
        review(); workbench.awaitText("Teaching-time impacts"); save();
        JsonNode impacts = draft().path("gridTimingImpacts");
        var expectedIds = new java.util.TreeSet<String>();
        for (JsonNode a : source.path("acceptedBaseline").path("result").path("timetable").path("assignments")) if (a.path("periodId").stringValue().equals(periodId)) expectedIds.add(a.path("lessonId").stringValue());
        var actualIds = new java.util.TreeSet<String>();
        for (JsonNode i : impacts) { actualIds.add(i.path("lessonId").stringValue()); assertEquals("08:50:00", i.path("after").path("startTime").stringValue()); assertEquals(periodId, i.path("periodId").stringValue()); }
        assertEquals(expectedIds, actualIds);
        JsonNode originalPeriods = definition().path("periods").deepCopy();
        workbench.page().locator("details").filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText("Add generated slots")).first().locator("summary").click();
        for (String day : List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")) workbench.page().locator("#grid-count-"+day).fill(day.equals("saturday") ? "1" : "0");
        workbench.page().locator("#grid-count-saturday").dispatchEvent("change"); workbench.page().locator("#generate-grid").click(); review();
        workbench.awaitText("availability"); save();
        assertEquals(originalPeriods.size()+1, definition().path("periods").size());
        assertEquals(JSON.createObjectNode().put("id", "saturday-1").put("displayName", "Saturday 1").put("weekday", "SATURDAY").put("order", 1), definition().path("periods").get(originalPeriods.size()));
        for (int i=0;i<originalPeriods.size();i++) assertEquals(originalPeriods.get(i), definition().path("periods").get(i));
        for (String key : List.of("subjects", "teachers", "cohorts", "rooms", "lessons", "softConstraintOverrides")) assertEquals(before.path(key), definition().path(key));
        assertEquals(source, draft().path("sourceDocument")); assertEquals(source.path("acceptedBaseline"), storedWorkspaceDocument().path("acceptedBaseline"));
        APIResponse export = workbench.page().request().get(base()+"/api/accepted/export"); assertEquals(200, export.status());
        assertEquals(source.path("acceptedBaseline").path("definition"), new SafeImportReader().readArchive(export.body()).definition());
    }

    @Test
    void uc3StorageStaleTransportAndAccessFailuresRetainRows() throws Exception {
        create(); workbench.page().locator("#generate-grid").click(); review(); String saved = storedDocument();
        withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'DEFINITION_DRAFT'", () -> { clickSave(); workbench.awaitText("No change was committed"); assertEquals(saved, storedDocument()); assertEquals(10, workbench.page().locator("#authoring-section > .grid-period-rows .grid-period-row").count()); });
        // Real concurrent tab advances the saved revision, leaving this tab's reviewed rows stale.
        assertEquals(200, command("", JSON.createObjectNode().set("periods", JSON.createArrayNode()), null).status()); String newer = storedDocument();
        review(); workbench.awaitText("No change was committed"); assertEquals(newer, storedDocument());
        workbench.page().locator("#grid-refresh").click(); workbench.awaitText("Unsaved —"); review(); save();
        field("monday-1", "displayName").fill("Working retry"); review(); newer=storedDocument(); final String durable = newer;
        workbench.page().route("**/api/definition-draft/grid", route -> route.abort()); clickSave(); workbench.awaitText("No change was committed"); assertEquals(durable, storedDocument());
        assertEquals("Working retry", field("monday-1", "displayName").inputValue()); workbench.page().unroute("**/api/definition-draft/grid");
        review(); workbench.page().route("**/api/definition-draft/grid", route -> {
            var headers=new java.util.HashMap<>(route.request().headers()); headers.keySet().removeIf(k -> k.equalsIgnoreCase("X-CSRF-TOKEN"));
            route.resume(new com.microsoft.playwright.Route.ResumeOptions().setHeaders(headers));
        }); clickSave(); workbench.awaitText("No change was committed"); assertEquals(durable, storedDocument());
        workbench.page().unroute("**/api/definition-draft/grid"); review(); save();
    }

    @Test
    void uc3IdentityRemovalReservationAndLifecycleRefusalsAreAtomic() {
        create(); workbench.page().locator("#generate-grid").click(); review(); save();
        ObjectNode valid = gridRequest(); String before = storedDocument();
        ObjectNode deletion=valid.deepCopy(); ((tools.jackson.databind.node.ArrayNode) deletion.path("periods")).remove(0);
        assertEquals(409, command("", deletion, null).status()); assertEquals(before, storedDocument());
        ObjectNode duplicate=valid.deepCopy(); ((ObjectNode) duplicate.path("periods").get(1)).put("id", "monday-1");
        assertEquals(422, command("", duplicate, null).status()); assertEquals(before, storedDocument());
        ObjectNode unknown=valid.deepCopy(); ((ObjectNode) unknown.path("periods").get(0)).put("unknown", "no");
        assertEquals(422, command("", unknown, null).status()); assertEquals(before, storedDocument());
        ObjectNode badReservation=valid.deepCopy(); badReservation.putArray("reservedPeriodIds").add("unknown-slot").add("unknown-slot");
        assertEquals(200, command("", badReservation, null).status()); assertEquals(badReservation.path("reservedPeriodIds"), definition().path("reservedPeriodIds"));
        assertTrue(draft().path("issues").toString().contains("unique declared period IDs"));
        for (WorkspaceState state : WorkspaceState.values()) if (state != WorkspaceState.DEFINITION_DRAFT) {
            jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state, version=version+1 WHERE workspace_id=1").param("state", state.name()).update(); before=storedDocument();
            for (String path : List.of("", "/preview")) { APIResponse response=command(path, valid, null); assertEquals(409,response.status()); assertTrue(response.text().contains(state.name())); assertEquals(before, storedDocument()); }
        }
    }
    @Test
    void uc3AddedSlotPreviewNamesAllAndSelectedResourcesAndReservesRemovedIds() {
        create();
        // Isolated resource-availability variant; no teacher/resource editor is implemented early.
        ObjectNode document = (ObjectNode) storedWorkspaceDocument();
        ObjectNode definition = (ObjectNode) document.path("definitionDraft").path("definition");
        for (String collection : List.of("teachers", "cohorts", "rooms")) {
            var rows = definition.withArray(collection);
            rows.addObject().put("id", collection+"-all").put("displayName", collection+" All");
            rows.addObject().put("id", collection+"-selected").put("displayName", collection+" Selected").putArray("availablePeriodIds");
        }
        ((ObjectNode) document.path("definitionDraft")).withArray("removedIdentities").addObject().put("ownerType", "Period").put("id", "reserved-period");
        jdbc.sql("UPDATE workspace_aggregate SET version=version+1, document=CAST(:document AS jsonb) WHERE workspace_id=1")
                .param("document", JSON.writeValueAsString(document)).update();
        workbench.reload(); grid();
        String unchanged=storedDocument();
        ObjectNode request=JSON.createObjectNode(); request.putArray("periods").addObject().put("id", "added-period").put("displayName", "Added")
                .put("weekday", "SATURDAY").put("order", 1);
        JsonNode preview=JSON.readTree(command("/preview",request,null).text());
        var expected=JSON.createArrayNode();
        for(String collection:List.of("teachers","cohorts","rooms")) {
            expected.addObject().put("periodId","added-period").put("ownerType",collection).put("ownerId",collection+"-all").put("displayName",collection+" All").put("effect","Included in all-period availability");
            expected.addObject().put("periodId","added-period").put("ownerType",collection).put("ownerId",collection+"-selected").put("displayName",collection+" Selected").put("effect","Excluded from explicit selected availability");
        }
        assertEquals(expected,preview.path("availabilityEffects")); assertEquals(unchanged,storedDocument());
        assertEquals(200,command("",request,null).status());
        for(String collection:List.of("teachers","cohorts","rooms")) assertEquals(definition.path(collection),definition().path(collection));
        ObjectNode reserved=request.deepCopy(); reserved.withArray("periods").addObject().put("id","reserved-period").put("displayName","Removed ID")
                .put("weekday","SATURDAY").put("order",2);
        unchanged=storedDocument(); assertEquals(409,command("",reserved,null).status()); assertEquals(unchanged,storedDocument());
    }

    @Test
    void uc3F1StaleRefreshPreservesUntouchedNewerRowsAndRequiresConflictChoice() {
        create(); workbench.page().locator("#generate-grid").click(); review(); save();
        var second = newTab(); second.open(); second.page().locator("button[data-section=Grid]").click();
        field("monday-1", "displayName").fill("My Monday name");
        second.page().getByRole(com.microsoft.playwright.options.AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("tuesday-1 displayName").setExact(true)).fill("Their Tuesday name");
        second.page().locator("#preview-grid").click(); second.page().locator("#save-grid").click();
        second.expect("!document.querySelector('#grid-preview')");
        String newer = storedDocument(); review(); workbench.awaitText("No change was committed"); assertEquals(newer,storedDocument());
        workbench.page().locator("#grid-refresh").click();
        workbench.expect("document.querySelector('[aria-label=\"tuesday-1 displayName\"]')?.value === 'Their Tuesday name'");
        assertEquals("My Monday name",field("monday-1","displayName").inputValue());
        assertEquals("Their Tuesday name",field("tuesday-1","displayName").inputValue());
        review(); save();
        assertEquals("My Monday name",definition().path("periods").get(0).path("displayName").stringValue());
        assertEquals("Their Tuesday name",definition().path("periods").get(2).path("displayName").stringValue());
        // Both tabs change the same field: refresh must keep both values visible and require an explicit choice.
        second.reload(); second.page().locator("button[data-section=Grid]").click();
        field("monday-1","displayName").fill("My conflicting Monday");
        second.page().getByRole(com.microsoft.playwright.options.AriaRole.TEXTBOX,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("monday-1 displayName").setExact(true)).fill("Their conflicting Monday");
        second.page().locator("#preview-grid").click(); second.page().locator("#save-grid").click(); second.expect("!document.querySelector('#grid-preview')");
        newer=storedDocument(); review(); workbench.awaitText("No change was committed"); workbench.page().locator("#grid-refresh").click();
        workbench.awaitText("Newer saved fields conflict"); workbench.awaitText("Their conflicting Monday"); workbench.awaitText("My conflicting Monday");
        review(); workbench.awaitText("Resolve the newer saved field conflicts"); assertEquals(newer,storedDocument());
        workbench.page().locator("#grid-use-latest").click(); assertEquals("Their conflicting Monday",field("monday-1","displayName").inputValue());
        review(); save(); assertEquals("Their conflicting Monday",definition().path("periods").get(0).path("displayName").stringValue());
    }

    private void create() { workbench.open(); open(); grid(); }
    private void open() { workbench.page().locator("#open-authoring").click(); workbench.page().locator("#confirm-authoring").click(); workbench.awaitText("Saved school-definition draft"); }
    private void grid() { workbench.page().locator("button[data-section=Grid]").click(); workbench.awaitText("Periods are explicit"); }
    private com.microsoft.playwright.Locator field(String id,String field) { return workbench.page().locator("#authoring-section > .grid-period-rows .grid-period-row[data-period-id='"+id+"'] [data-field='"+field+"']"); }
    private void review() { workbench.page().locator("#preview-grid").click(); workbench.expect("document.querySelector('#grid-preview') || document.querySelector('#grid-save-error')"); }
    private void clickSave() { workbench.page().locator("#save-grid").click(); }
    private void save() { clickSave(); workbench.expect("!document.querySelector('#grid-preview') && !document.querySelector('#grid-save-error')"); }
    private JsonNode definition() { return draft().path("definition"); }
    private JsonNode draft() { return storedWorkspaceDocument().path("definitionDraft"); }
    private ObjectNode gridRequest() { ObjectNode body=JSON.createObjectNode(); body.set("periods",definition().path("periods").deepCopy()); if(definition().has("reservedPeriodIds")) body.set("reservedPeriodIds",definition().path("reservedPeriodIds").deepCopy()); return body; }
    private String base() { return "http://localhost:"+port; }
    private APIResponse command(String suffix, JsonNode body,String version) {
        JsonNode csrf=JSON.readTree(workbench.page().request().get(base()+"/api/csrf").text());
        return workbench.page().request().post(base()+"/api/definition-draft/grid"+suffix, RequestOptions.create().setData(JSON.writeValueAsString(body))
                .setHeader("Content-Type","application/json").setHeader("Origin",base()).setHeader("If-Match",version==null?"\"ws-"+storedVersion()+"\"":version)
                .setHeader(csrf.path("headerName").stringValue(),csrf.path("token").stringValue()));
    }
}
