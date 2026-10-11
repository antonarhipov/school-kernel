package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class DefinitionDiscardBrowserIT extends WorkbenchBrowserSupport {
    @org.springframework.beans.factory.annotation.Autowired KernelVerifier verifier;
    @Test void exactRestorationForAllThreeSourcesIncludingPreUpgradeCatalog() {
        workbench.open();
        for (String state : java.util.List.of("EMPTY", "INITIAL_DRAFT", "ACCEPTED_BASELINE")) {
            ObjectNode source = state.equals("EMPTY") ? JSON.createObjectNode() : fixtures.validAcceptedDocument();
            if (state.equals("INITIAL_DRAFT")) {
                ObjectNode initial = JSON.createObjectNode(); initial.set("initialDefinition", source.path("acceptedBaseline").path("definition").deepCopy());
                initial.put("definitionRevision", "exact-initial-revision"); initial.set("school", source.path("school")); source = initial;
            }
            if (!state.equals("EMPTY")) source.putObject("authoringContext").put("literal", "<school> Õ");
            store(state, source); workbench.reload(); open();
            JsonNode recorded = storedWorkspaceDocument().path("definitionDraft").path("sourceDocument").deepCopy();
            assertEquals(source, recorded);
            if (state.equals("ACCEPTED_BASELINE")) {
                assertTrue(source.path("acceptedBaseline").path("definition").path("catalogVersion").intValue() < 8);
                assertEquals(8, storedWorkspaceDocument().path("definitionDraft").path("definition").path("catalogVersion").intValue());
            }
            workbench.page().locator("button[data-section='School']").click();
            workbench.page().locator("#school-display-name").fill("Authored name");
            workbench.page().locator("#save-school-identity").click(); workbench.awaitText("Saved — Your saved");
            workbench.page().locator("#discard-authoring").click(); workbench.awaitText("Restore recorded source: " + state);
            if (state.equals("EMPTY")) workbench.awaitText("newly created school definition will be removed");
            workbench.page().locator("#confirm-discard-authoring").click();
            workbench.expect("!document.querySelector('#discard-authoring-dialog') && !document.querySelector('#discard-authoring')");
            assertEquals(state, storedLifecycle()); assertEquals(source, storedWorkspaceDocument());
            workbench.reload(); assertEquals(source, storedWorkspaceDocument());
        }
    }
    @Test void cancelResizeStaleAccessAndStorageRefusalsPreserveAllWork() throws Exception {
        store("INITIAL_DRAFT", initial()); workbench.open(); open();
        workbench.page().locator("button[data-section='School']").click();
        workbench.page().locator("#school-display-name").fill("Unsaved literal <img> Õ");
        String before = storedDocument();
        workbench.page().locator("#discard-authoring").click(); workbench.page().locator("#cancel-discard-authoring").click();
        assertEquals("Unsaved literal <img> Õ", workbench.page().locator("#school-display-name").inputValue()); assertEquals(before, storedDocument());
        workbench.page().locator("#discard-authoring").click(); workbench.viewport(390,844);
        workbench.expect("document.querySelector('#confirm-discard-authoring').disabled && document.documentElement.scrollWidth <= innerWidth");
        workbench.page().locator("#cancel-discard-authoring").click(); assertEquals(before,storedDocument());
        workbench.viewport(1280,800); workbench.expect("!document.querySelector('#discard-authoring').disabled");
        assertEquals("Unsaved literal <img> Õ",workbench.page().locator("#school-display-name").inputValue());
        assertEquals(400, command("{}", null, true).status()); assertEquals(before,storedDocument());
        assertEquals(403, command("{\"confirmed\":true}", null, false).status()); assertEquals(before,storedDocument());
        workbench.page().locator("#discard-authoring").click();
        jdbc.sql("UPDATE workspace_aggregate SET version=version+1 WHERE workspace_id=1").update(); String newer=storedDocument();
        workbench.page().locator("#confirm-discard-authoring").click(); workbench.awaitText("Authoring was not discarded"); assertEquals(newer,storedDocument());
        assertEquals("Unsaved literal <img> Õ",workbench.page().locator("#school-display-name").inputValue());
        workbench.page().locator("#cancel-discard-authoring").click(); workbench.reload();
        workbench.page().locator("#discard-authoring").click();
        withRejectedWorkspaceUpdates("NEW.lifecycle_state='INITIAL_DRAFT'", () -> {
            workbench.page().locator("#confirm-discard-authoring").click(); workbench.awaitText("Authoring was not discarded"); assertEquals(newer,storedDocument());
        });
        workbench.page().locator("#confirm-discard-authoring").click(); workbench.awaitText("Initial draft");
        assertEquals(initial(),storedWorkspaceDocument());
    }
    @Test void invalidLifecycleAndSeededRepairRequireSeparateReturnWithoutMutation() {
        workbench.open(); ObjectNode doc=fixtures.validAcceptedDocument();
        doc.putObject("repairDraft").putObject("retainedAuthoring").put("sourceState","ACCEPTED_BASELINE");
        store("REPAIR_DRAFT",doc);String before=storedDocument();var response=command("{\"confirmed\":true}",null,true);
        assertEquals(409,response.status());assertTrue(response.text().contains("Return to authoring first"));assertEquals(before,storedDocument());
        store("INITIAL_DRAFT",initial());before=storedDocument();assertEquals(409,command("{\"confirmed\":true}",null,true).status());assertEquals(before,storedDocument());
    }

    @Test void legacyPreservedSourcesCanBeCancelledAndExplicitlyRestoredWithoutAdoptingAuthoredValues() {
        workbench.open();
        for (String state : java.util.List.of("INITIAL_DRAFT", "ACCEPTED_BASELINE")) {
            ObjectNode source = state.equals("INITIAL_DRAFT") ? initial() : fixtures.validAcceptedDocument();
            source.putObject("authoringContext").put("retained", "exact source defaults");
            ObjectNode legacy = legacy(state, source);
            store("DEFINITION_DRAFT", legacy);
            workbench.reload();
            workbench.page().locator("button[data-section='School']").click();
            workbench.page().locator("#school-display-name").fill("Unsaved <literal> Õ");
            String before = storedDocument();
            workbench.page().locator("#discard-authoring").click();
            workbench.awaitText("Restore recorded source: " + state);
            workbench.awaitText("This older draft retains its source in the workspace");
            workbench.page().locator("#cancel-discard-authoring").click();
            assertEquals("Unsaved <literal> Õ", workbench.page().locator("#school-display-name").inputValue());
            assertEquals(before, storedDocument());
            workbench.page().locator("#discard-authoring").click();
            workbench.page().locator("#confirm-discard-authoring").click();
            workbench.expectNot("document.querySelector('#discard-authoring-dialog, #discard-authoring')");
            assertEquals(state, storedLifecycle());
            assertEquals(source, storedWorkspaceDocument(), "remove only legacy authoring; preserve exact source and provenance");
            workbench.reload();
            assertEquals(source, storedWorkspaceDocument());
        }
    }

    @Test void missingNullAndMismatchedSourcesReportRefusalWithoutConsoleErrorsOrDataLoss() {
        workbench.open();
        for (String failure : java.util.List.of("missing", "null", "array", "revision", "catalog", "source-definition", "source-name", "competing-draft")) {
            ObjectNode source = fixtures.validAcceptedDocument();
            ObjectNode document = legacy("ACCEPTED_BASELINE", source);
            ObjectNode draft = (ObjectNode) document.path("definitionDraft");
            switch (failure) {
                case "missing" -> draft.remove("sourceDefinitionRevision");
                case "null" -> draft.putNull("sourceDocument");
                case "array" -> draft.putArray("sourceDocument");
                case "revision" -> draft.put("sourceDefinitionRevision", "sha256:" + "0".repeat(64));
                case "catalog" -> draft.put("sourceCatalogVersion", 9);
                case "source-definition" -> document.withObject("acceptedBaseline").withObject("definition").put("displayName", "Changed source");
                case "source-name" -> document.withObject("school").put("displayName", "Authored source name");
                case "competing-draft" -> document.putObject("manualDraft");
            }
            store("DEFINITION_DRAFT", document);
            String before = storedDocument();
            workbench.reload();
            workbench.page().locator("#discard-authoring").click();
            workbench.page().locator("#confirm-discard-authoring").click();
            workbench.awaitText("Authoring was not discarded");
            assertEquals(before, storedDocument(), failure + " must preserve the complete saved workspace/version");
            var response = command("{\"confirmed\":true}", null, true);
            assertEquals(409, response.status());
            assertTrue(response.text().contains("INVALID_AUTHORING_SOURCE"));
            assertEquals(before, storedDocument());
            workbench.page().locator("#cancel-discard-authoring").click();
        }
    }

    private ObjectNode legacy(String state, ObjectNode source) {
        JsonNode definition = state.equals("INITIAL_DRAFT") ? source.path("initialDefinition") : source.path("acceptedBaseline").path("definition");
        var originalLessons = new java.util.ArrayList<JsonNode>();
        definition.path("lessons").forEach(originalLessons::add);
        java.util.Collections.reverse(originalLessons);
        var reordered = ((ObjectNode) definition).putArray("lessons");
        originalLessons.forEach(reordered::add);
        String revision = verifier.verify(new ImportDocuments(definition, null, null,
                ImportDocuments.ImportMode.INITIAL_DEFINITION)).definitionRevision();
        source.put("definitionRevision", revision);
        ObjectNode document = source.deepCopy();
        ObjectNode draft = document.putObject("definitionDraft");
        draft.put("sourceState", state).put("sourceDefinitionRevision", revision)
                .put("sourceCatalogVersion", definition.path("catalogVersion").intValue());
        ObjectNode authored = (ObjectNode) definition.deepCopy();
        authored.put("displayName", "Authored <img> Õ");
        draft.set("definition", authored);
        draft.put("draftRevision", DefinitionDraftService.revision(authored));
        draft.putArray("issues");
        return document;
    }
    private ObjectNode initial(){ObjectNode doc=JSON.createObjectNode();doc.set("initialDefinition",fixtures.validAcceptedDocument().path("acceptedBaseline").path("definition"));return doc;}
    private void open(){workbench.page().locator("#open-authoring").click();workbench.page().locator("#confirm-authoring").click();workbench.awaitText("Saved school-definition draft");}
    private void store(String state,JsonNode doc){jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state,version=version+1,active_run_id=NULL,document=CAST(:doc AS jsonb) WHERE workspace_id=1").param("state",state).param("doc",JSON.writeValueAsString(doc)).update();}
    private com.microsoft.playwright.APIResponse command(String data,String version,boolean authorized){String base="http://localhost:"+port;JsonNode csrf=JSON.readTree(workbench.page().request().get(base+"/api/csrf").text());var opts=RequestOptions.create().setHeader("Content-Type","application/json").setHeader("Origin",base).setHeader("If-Match",version==null?"\"ws-"+storedVersion()+"\"":version).setData(data);if(authorized)opts.setHeader(csrf.path("headerName").asString(),csrf.path("token").asString());return workbench.page().request().delete(base+"/api/definition-draft",opts);}
}
