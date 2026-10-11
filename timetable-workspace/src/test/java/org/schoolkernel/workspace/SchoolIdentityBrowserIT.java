package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-2 contract at the local administrator's real HTTP/browser boundary. */
class SchoolIdentityBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void uc2MainSuggestionExplicitSaveStableIdentityAndDurableRestart() {
        create();
        String opened = storedDocument();
        name("Õäš  Kool!");
        assertEquals("oas-kool", workbench.page().locator("#school-identifier").inputValue());
        workbench.awaitText("Unsaved —"); assertEquals(opened, storedDocument());
        save();
        JsonNode document = storedWorkspaceDocument();
        ObjectNode expected = JSON.createObjectNode().put("schemaVersion", 1).put("catalogVersion", 9)
                .put("schoolId", "oas-kool").put("displayName", "Õäš  Kool!");
        for (String key : List.of("subjects", "teachers", "cohorts", "rooms", "periods", "lessons")) expected.putArray(key);
        assertEquals(expected, document.path("definitionDraft").path("definition"));
        assertTrue(document.path("definitionDraft").path("issues").isEmpty());
        assertEquals(JSON.createObjectNode(), document.path("definitionDraft").path("sourceDocument"));
        assertEquals("DEFINITION_DRAFT", storedLifecycle());
        assertTrue(workbench.page().locator("#school-identifier").getAttribute("readonly") != null);
        String revision = document.path("definitionDraft").path("draftRevision").stringValue();
        assertTrue(revision.matches("sha256:[0-9a-f]{64}"));
        name(" Õpetajate <img src=x onerror=alert(1)> Kool "); save();
        assertEquals("oas-kool", definition().path("schoolId").stringValue());
        assertEquals(" Õpetajate <img src=x onerror=alert(1)> Kool ", definition().path("displayName").stringValue());
        assertEquals(0, workbench.page().locator(".definition-authoring img").count());
        assertNotEquals(revision, storedWorkspaceDocument().path("definitionDraft").path("draftRevision").stringValue());
        String saved = storedDocument();
        name("Only working text"); workbench.reload(); school();
        assertEquals(" Õpetajate <img src=x onerror=alert(1)> Kool ", workbench.page().locator("#school-display-name").inputValue());
        assertEquals(saved, storedDocument());
        try (var context = startRestartableWorkspace()) {
            int restartedPort = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context).getWebServer().getPort();
            workbench.open(restartedPort).awaitText("Saved school-definition draft"); school();
            assertEquals("oas-kool", workbench.page().locator("#school-identifier").inputValue());
            assertEquals(" Õpetajate <img src=x onerror=alert(1)> Kool ", workbench.page().locator("#school-display-name").inputValue());
            assertEquals(saved, storedDocument());
        }
    }

    @Test
    void uc2AdvancedOverrideInvalidIdentityCollisionAndBlankName() {
        create(); name("A school");
        workbench.page().locator("#school-identifier").fill("INVALID ID");
        String before = storedDocument(); clickSave(); workbench.awaitText("No change was committed");
        assertEquals(before, storedDocument()); assertEquals("INVALID ID", workbench.page().locator("#school-identifier").inputValue());
        workbench.awaitText("Working values remain Unsaved");
        workbench.page().locator("#school-identifier").fill("Advanced.ID_9"); name(" "); save();
        assertEquals("Advanced.ID_9", definition().path("schoolId").stringValue());
        assertEquals(" ", definition().path("displayName").stringValue());
        JsonNode issue = storedWorkspaceDocument().path("definitionDraft").path("issues").get(0);
        ObjectNode expectedIssue = JSON.createObjectNode().put("severity", "BLOCKING").put("ownerType", "School")
                .put("ownerId", "Advanced.ID_9").put("field", "displayName").put("message", "Enter a nonblank school name.");
        expectedIssue.putArray("relatedIds"); assertEquals(expectedIssue, issue);
        workbench.awaitText("1 blocking issues");
        name("Completed name"); save(); assertTrue(storedWorkspaceDocument().path("definitionDraft").path("issues").isEmpty());
        before = storedDocument();
        APIResponse rename = command("{\"schoolId\":\"renamed\",\"displayName\":\"Changed\"}", null);
        assertEquals(409, rename.status()); assertTrue(rename.text().contains("SCHOOL_ID_IMMUTABLE")); assertEquals(before, storedDocument());
        // Labelled new-school lineage variant: a reserved school identity, with another collection's identical ID allowed.
        ObjectNode doc = (ObjectNode) storedWorkspaceDocument();
        ((ObjectNode) doc.path("definitionDraft").path("definition")).remove("schoolId");
        ((ObjectNode) doc.path("definitionDraft")).withArray("removedIdentities").addObject().put("ownerType", "School").put("id", "reserved");
        store("DEFINITION_DRAFT", doc); before = storedDocument();
        workbench.reload(); school(); name("Reserved");
        assertEquals("reserved-2", workbench.page().locator("#school-identifier").inputValue());
        name("💡"); assertEquals("school", workbench.page().locator("#school-identifier").inputValue());
        name("A".repeat(140)); assertEquals("a".repeat(128), workbench.page().locator("#school-identifier").inputValue());
        workbench.page().locator("#school-identifier").fill("reserved"); clickSave(); workbench.awaitText("reserved in this authoring lineage");
        assertEquals(before, storedDocument());
        assertEquals(409, command("{\"schoolId\":\"reserved\",\"displayName\":\"New\"}", null).status()); assertEquals(before, storedDocument());
        ((ObjectNode) doc.path("definitionDraft")).withArray("removedIdentities").addObject().put("ownerType", "Teacher").put("id", "shared");
        store("DEFINITION_DRAFT", doc);
        assertEquals(200, command("{\"schoolId\":\"shared\",\"displayName\":\"New\"}", null).status());
    }

    @Test
    void uc2UnsavedNavigationStayDiscardSaveFailureAndResponsiveLayouts() throws Exception {
        create(); String before = storedDocument(); name("Working Õ school");
        for (int[] size : List.of(new int[]{1600,900}, new int[]{1280,800})) {
            workbench.viewport(size[0], size[1]);
            assertEquals(true, workbench.page().evaluate("document.documentElement.scrollWidth <= innerWidth"));
            for (String selector : List.of("#school-display-name", "#school-identifier", "#save-school-identity", "#school-save-status")) {
                assertTrue(workbench.page().locator(selector).isVisible());
                assertEquals(true, workbench.page().locator(selector).evaluate("e => { const r=e.getBoundingClientRect(); return r.left>=0 && r.right<=innerWidth && r.top>=0 && r.bottom<=innerHeight; }"));
            }
            workbench.page().screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/uc2-school-" + size[0] + ".png")));
        }
        for (int width : List.of(390, 700)) {
            workbench.viewport(width,844); workbench.awaitText("Editing requires a wider view");
            assertEquals(0, workbench.page().locator("#school-identity-form").count());
            assertEquals(true, workbench.page().evaluate("document.documentElement.scrollWidth <= innerWidth"));
            assertEquals(before, storedDocument());
        }
        workbench.viewport(1280,800); assertEquals("Working Õ school", workbench.page().locator("#school-display-name").inputValue()); workbench.awaitText("Unsaved —");
        workbench.page().locator("button[data-section=Review]").click(); workbench.page().locator("#school-stay").click();
        assertEquals("School", workbench.page().locator("#authoring-section h3").textContent()); assertEquals(before, storedDocument());
        workbench.page().locator("button[data-section=Review]").click(); workbench.page().locator("#school-discard-working").click();
        school(); assertEquals("", workbench.page().locator("#school-display-name").inputValue()); assertEquals(before, storedDocument());
        name("Save on leaving");
        withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'DEFINITION_DRAFT'", () -> {
            workbench.page().locator("button[data-section=Review]").click(); workbench.page().locator("#school-save-continue").click();
            workbench.awaitText("No change was committed"); assertEquals("School", workbench.page().locator("#authoring-section h3").textContent());
            assertEquals("Save on leaving", workbench.page().locator("#school-display-name").inputValue()); assertEquals(before, storedDocument());
        });
        workbench.page().locator("button[data-section=Review]").click(); workbench.page().locator("#school-save-continue").click();
        workbench.awaitText("Save on leaving"); assertEquals("Review", workbench.page().locator("#authoring-section h3").textContent());
        assertEquals("Save on leaving", definition().path("displayName").stringValue()); school();
        assertEquals("Save on leaving", workbench.page().locator("#school-display-name").inputValue());
    }

    @Test
    void uc2StorageStaleVersionTransportAndAccessFailuresRetainWorkingValues() throws Exception {
        create(); name("Retry Õ name"); String before = storedDocument();
        withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'DEFINITION_DRAFT'", () -> {
            clickSave(); workbench.awaitText("No change was committed"); assertEquals(before, storedDocument());
            assertEquals("Retry Õ name", workbench.page().locator("#school-display-name").inputValue());
        });
        save(); name("Stale working name");
        assertEquals(200, command("{\"schoolId\":\"retry-o-name\",\"displayName\":\"Other tab saved\"}", null).status());
        String newer = storedDocument(); clickSave(); workbench.awaitText("No change was committed");
        assertEquals(newer, storedDocument()); assertEquals("Stale working name", workbench.page().locator("#school-display-name").inputValue());
        workbench.page().locator("#school-refresh").click(); workbench.awaitText("Other tab saved");
        assertEquals("Stale working name", workbench.page().locator("#school-display-name").inputValue()); save();
        name("Transport failure"); newer = storedDocument();
        workbench.page().route("**/api/definition-draft/school", route -> route.abort());
        clickSave(); workbench.awaitText("No change was committed"); assertEquals(newer, storedDocument());
        assertEquals("Transport failure", workbench.page().locator("#school-display-name").inputValue());
        workbench.page().unroute("**/api/definition-draft/school");
        // Remove the actual CSRF header from both attempts; production protection refuses the real form request.
        workbench.page().route("**/api/definition-draft/school", route -> {
            var headers = new java.util.HashMap<>(route.request().headers()); headers.keySet().removeIf(key -> key.equalsIgnoreCase("X-CSRF-TOKEN") || key.equalsIgnoreCase("X-XSRF-TOKEN"));
            route.resume(new com.microsoft.playwright.Route.ResumeOptions().setHeaders(headers));
        });
        clickSave(); workbench.awaitText("No change was committed"); assertEquals(newer, storedDocument());
        assertEquals("Transport failure", workbench.page().locator("#school-display-name").inputValue());
        workbench.page().unroute("**/api/definition-draft/school"); save();
        name("Working after state change"); JsonNode durable = storedWorkspaceDocument();
        store("INITIAL_DRAFT", durable); String changedState = storedDocument();
        clickSave(); workbench.awaitText("No change was committed");
        assertEquals("Working after state change", workbench.page().locator("#school-display-name").inputValue());
        assertEquals(changedState, storedDocument());
    }

    @Test
    void uc2InitialAndAcceptedSourceFidelityCurrentInspectionAndExport() throws Exception {
        for (boolean accepted : List.of(false, true)) {
            ObjectNode source = fixtures.validAcceptedDocument();
            if (accepted) storeAccepted(source);
            else { ObjectNode initial = JSON.createObjectNode(); initial.set("initialDefinition", source.path("acceptedBaseline").path("definition")); source = initial; store("INITIAL_DRAFT", source); }
            workbench.open(); open(); school();
            ObjectNode opened = (ObjectNode) storedWorkspaceDocument();
            ObjectNode draft = (ObjectNode) opened.path("definitionDraft");
            draft.withObject("editingContext").putObject("requirement").put("explicitException", true);
            draft.withArray("issues").addObject().put("severity", "BLOCKING").put("ownerType", "Teacher")
                    .put("ownerId", "teacher-alex").put("field", "qualifiedSubjectIds").put("message", "Retained issue");
            draft.withArray("removedIdentities").addObject().put("ownerType", "Teacher").put("id", "removed-teacher");
            store("DEFINITION_DRAFT", opened); workbench.reload(); school();
            ObjectNode expected = ((ObjectNode) opened.path("definitionDraft").path("definition")).deepCopy();
            name("Renamed Õ school"); save(); expected.put("displayName", "Renamed Õ school");
            assertEquals(expected, definition());
            JsonNode savedDraft = storedWorkspaceDocument().path("definitionDraft");
            assertEquals(draft.path("editingContext"), savedDraft.path("editingContext"));
            assertEquals(draft.path("removedIdentities"), savedDraft.path("removedIdentities"));
            assertEquals(draft.path("issues"), savedDraft.path("issues"));
            assertEquals(source, storedWorkspaceDocument().path("definitionDraft").path("sourceDocument"));
            for (var entry : source.properties()) assertEquals(entry.getValue(), storedWorkspaceDocument().path(entry.getKey()));
            if (accepted) {
                name("Save before Current"); workbench.page().locator("#inspect-authoring-current").click();
                workbench.page().locator("#school-save-continue").click(); workbench.awaitText("Current");
                workbench.page().locator("#resume-authoring").click(); school();
                assertEquals("Save before Current", workbench.page().locator("#school-display-name").inputValue());
                APIResponse response = workbench.page().request().get(base() + "/api/accepted/export"); assertEquals(200, response.status());
                var bundle = new SafeImportReader().readArchive(response.body());
                assertEquals(source.path("acceptedBaseline").path("definition"), bundle.definition());
                assertEquals(source.path("acceptedBaseline").path("result"), bundle.result());
            }
        }
    }

    @Test
    void uc2StateSecurityAndSystemManagedFieldRefusalsHaveNoSideEffects() {
        create(); String before = storedDocument(); String valid = "{\"schoolId\":\"School.ID\",\"displayName\":\"Name\"}";
        for (String invalid : List.of("", "-bad", "õschool", "a".repeat(129))) {
            assertEquals(422, command(JSON.writeValueAsString(JSON.createObjectNode().put("schoolId", invalid).put("displayName", "Name")), null).status()); assertEquals(before, storedDocument());
        }
        for (String field : List.of("schemaVersion", "catalogVersion", "basedOnRevision", "draftRevision", "subjects")) {
            assertEquals(400, command(valid.substring(0, valid.length()-1) + ",\"" + field + "\":9}", null).status()); assertEquals(before, storedDocument());
        }
        APIResponse csrf = workbench.page().request().post(base() + "/api/definition-draft/school", RequestOptions.create().setHeader("Origin", base()).setHeader("If-Match", "\"ws-" + storedVersion() + "\"").setData(valid));
        assertEquals(403, csrf.status()); assertEquals(before, storedDocument());
        for (WorkspaceState state : WorkspaceState.values()) {
            if (state == WorkspaceState.DEFINITION_DRAFT) continue;
            store(state.name(), storedWorkspaceDocument()); before = storedDocument();
            APIResponse refused = command(valid, null); assertEquals(409, refused.status()); assertTrue(refused.text().contains(state.name())); assertEquals(before, storedDocument());
        }
        APIResponse host = workbench.page().request().get(base()+"/api/workspace", RequestOptions.create().setHeader("Host", "outside.example"));
        assertEquals(403, host.status()); assertFalse(host.text().contains("definitionDraft"));
    }

    private void create() { workbench.open(); open(); }
    private void open() { workbench.page().locator("#open-authoring").click(); workbench.page().locator("#confirm-authoring").click(); workbench.awaitText("Saved school-definition draft"); }
    private void school() { workbench.page().locator("button[data-section=School]").click(); workbench.awaitText("School name"); }
    private void name(String value) { workbench.page().locator("#school-display-name").fill(value); }
    private void clickSave() { workbench.page().locator("#save-school-identity").click(); }
    private void save() { clickSave(); workbench.expect("document.querySelector('#school-save-status')?.textContent.startsWith('Saved —')"); }
    private JsonNode definition() { return storedWorkspaceDocument().path("definitionDraft").path("definition"); }
    private String base() { return "http://localhost:" + port; }
    private APIResponse command(String body, String version) {
        JsonNode csrf = JSON.readTree(workbench.page().request().get(base()+"/api/csrf").text());
        return workbench.page().request().post(base()+"/api/definition-draft/school", RequestOptions.create().setData(body).setHeader("Content-Type", "application/json")
                .setHeader("Origin", base()).setHeader("If-Match", version == null ? "\"ws-"+storedVersion()+"\"" : version)
                .setHeader(csrf.path("headerName").stringValue(), csrf.path("token").stringValue()));
    }
    private void store(String state, JsonNode document) {
        jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state, version=version+1, active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1")
                .param("state", state).param("document", JSON.writeValueAsString(document)).update();
    }
}
