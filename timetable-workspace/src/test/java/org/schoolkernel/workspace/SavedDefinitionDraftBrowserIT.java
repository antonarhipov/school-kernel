package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class SavedDefinitionDraftBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void inspectsAcceptedTimetableAndPreservesSavedDraftAcrossReloadAndNarrowView() {
        ObjectNode document = fixtures.validAcceptedDocument();
        document.putObject("definitionDraft").set("definition",
                document.path("acceptedBaseline").path("definition").deepCopy());
        storeDefinitionDraft(document);
        String before = storedDocument();

        String rendered = workbench.open().awaitText("Saved school-definition draft");
        assertTrue(rendered.contains("Demo School"));
        assertTrue(rendered.contains("Your saved school-definition changes are preserved."));
        workbench.expectNot("document.querySelector('#open-repair-setup, #start-manual-draft')");
        workbench.page().locator("#inspect-authoring-current").click();
        workbench.selectLesson(document.path("acceptedBaseline").path("result")
                .path("timetable").path("assignments").get(0).path("lessonId").stringValue());
        workbench.expect("document.querySelector('#edit-period').disabled");
        workbench.expect("document.querySelector('#edit-room').disabled");
        workbench.expect("document.querySelector('#edit-teacher').disabled");

        APIResponse response = workbench.page().request().get("http://localhost:" + port + "/api/workspace");
        assertEquals(200, response.status());
        JsonNode snapshot = JSON.readTree(response.text());
        assertEquals("DEFINITION_DRAFT", snapshot.path("state").stringValue());
        assertTrue(snapshot.path("acceptedTimetable").booleanValue());
        assertEquals(document, snapshot.path("workspace"));
        assertEquals("\"ws-7\"", response.headers().get("etag"));

        JsonNode csrf = JSON.readTree(workbench.page().request()
                .get("http://localhost:" + port + "/api/csrf").text());
        APIResponse mutation = workbench.page().request().post("http://localhost:" + port + "/api/manual-draft",
                RequestOptions.create()
                        .setHeader("Origin", "http://localhost:" + port)
                        .setHeader("If-Match", "\"ws-7\"")
                        .setHeader(csrf.path("headerName").stringValue(), csrf.path("token").stringValue()));
        assertEquals(409, mutation.status(), "inspection must not turn saved definition edits into manual edits");

        workbench.page().setViewportSize(390, 844);
        workbench.awaitText("Saved school-definition draft");
        workbench.page().reload();
        workbench.awaitText("Your saved school-definition changes are preserved.");
        assertEquals("DEFINITION_DRAFT", storedLifecycle());
        assertEquals(before, storedDocument(), "loading and inspecting must preserve the saved draft and version");
    }

    @Test
    void loadsSavedInitialDefinitionDraftWithoutClaimingAnAcceptedTimetable() {
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "demo-school").put("displayName", "Demo School");
        document.putObject("definitionDraft").set("definition",
                JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile()));
        storeDefinitionDraft(document);
        String before = storedDocument();

        String rendered = workbench.open().awaitText("Saved school-definition draft");
        assertTrue(rendered.contains("No accepted timetable"));
        assertTrue(rendered.contains("Demo School"));
        assertFalse(rendered.contains("Workspace unavailable"));

        APIResponse response = workbench.page().request().get("http://localhost:" + port + "/api/workspace");
        assertEquals(200, response.status());
        assertFalse(JSON.readTree(response.text()).path("acceptedTimetable").booleanValue());
        assertEquals(before, storedDocument());
    }

    @Test
    void exportsAcceptedBaselineWithoutPublishingOrDiscardingSavedDefinitionChanges() {
        ObjectNode document = fixtures.validAcceptedDocument();
        ObjectNode draftDefinition = ((ObjectNode) document.path("acceptedBaseline").path("definition")).deepCopy();
        draftDefinition.put("displayName", "Unpublished school name");
        document.putObject("definitionDraft").set("definition", draftDefinition);
        storeDefinitionDraft(document);
        String before = storedDocument();

        APIResponse response = workbench.page().request().get("http://localhost:" + port + "/api/accepted/export");

        assertEquals(200, response.status());
        ImportDocuments exported = new SafeImportReader().readArchive(response.body());
        assertEquals(document.path("acceptedBaseline").path("definition"), exported.definition());
        assertEquals(document.path("acceptedBaseline").path("result"), exported.result());
        assertEquals(before, storedDocument());
        assertEquals("DEFINITION_DRAFT", storedLifecycle());
    }

    private void storeDefinitionDraft(ObjectNode document) {
        jdbc.sql("""
                UPDATE workspace_aggregate SET lifecycle_state='DEFINITION_DRAFT', version=7,
                active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                """).param("document", JSON.writeValueAsString(document)).update();
    }
}
