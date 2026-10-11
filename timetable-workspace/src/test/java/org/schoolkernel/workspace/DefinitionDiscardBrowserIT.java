package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class DefinitionDiscardBrowserIT extends WorkbenchBrowserSupport {
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
    private ObjectNode initial(){ObjectNode doc=JSON.createObjectNode();doc.set("initialDefinition",fixtures.validAcceptedDocument().path("acceptedBaseline").path("definition"));return doc;}
    private void open(){workbench.page().locator("#open-authoring").click();workbench.page().locator("#confirm-authoring").click();workbench.awaitText("Saved school-definition draft");}
    private void store(String state,JsonNode doc){jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state,version=version+1,active_run_id=NULL,document=CAST(:doc AS jsonb) WHERE workspace_id=1").param("state",state).param("doc",JSON.writeValueAsString(doc)).update();}
    private com.microsoft.playwright.APIResponse command(String data,String version,boolean authorized){String base="http://localhost:"+port;JsonNode csrf=JSON.readTree(workbench.page().request().get(base+"/api/csrf").text());var opts=RequestOptions.create().setHeader("Content-Type","application/json").setHeader("Origin",base).setHeader("If-Match",version==null?"\"ws-"+storedVersion()+"\"":version).setData(data);if(authorized)opts.setHeader(csrf.path("headerName").asString(),csrf.path("token").asString());return workbench.page().request().delete(base+"/api/definition-draft",opts);}
}
