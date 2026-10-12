package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Presentation regressions exercise real forms and compare the durable source and workspace by value. */
class DefinitionAuthoringUxBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void searchesPagesAndDuplicateNamesPreserveWorkingFormsAndExplicitSelection() {
        ObjectNode source = fixtures.validAcceptedDocument();
        var subjects = ((ObjectNode) source.path("acceptedBaseline").path("definition")).withArray("subjects");
        subjects.addObject().put("id", "history-a").put("displayName", "Ajalugu");
        subjects.addObject().put("id", "history-b").put("displayName", "Ajalugu");
        subjects.addObject().put("id", "oas-proof").put("displayName", "Õäš proof");
        for (int i = 1; i <= 35; i++) subjects.addObject().put("id", "subject-" + i).put("displayName", "Subject " + i);
        storeAccepted(source); open(); nav("Subjects");
        assertEquals(25, page().locator(".definition-record-choice").count());
        page().locator("#subjects-search").fill("AJALUGU");
        assertEquals(2, page().locator(".definition-record-choice").count());
        page().locator("[data-subject-id='history-a']").click();
        assertEquals("true", page().locator("[data-subject-id='history-a']").getAttribute("aria-pressed"));
        String saved = storedDocument();
        page().locator("#subject-displayName").fill("Retained <literal> Õ");
        page().locator("#subjects-search").fill("no matches");
        assertEquals("Retained <literal> Õ", page().locator("#subject-displayName").inputValue());
        assertTrue(page().locator(".definition-browser-count").innerText().contains("outside"));
        assertEquals(saved, storedDocument());
        page().locator("#subjects-search").fill("history-");
        page().locator("[data-subject-id='history-b']").click(); page().locator("#school-stay").click();
        assertEquals("history-a", page().locator("#subject-id").inputValue());
        page().locator("[data-subject-id='history-b']").click(); page().locator("#school-save-continue").click();
        workbench.expect("document.querySelector('#subject-id')?.value === 'history-b'");
        assertEquals("history-", page().locator("#subjects-search").inputValue());
        assertEquals("Retained <literal> Õ", subject("history-a").path("displayName").asString());
        assertEquals(source.path("acceptedBaseline"), storedWorkspaceDocument().path("acceptedBaseline"));
        page().locator("#subjects-search").fill("OAS"); page().locator("#subjects-search").press("ArrowDown"); page().keyboard().press("Enter");
        assertEquals("oas-proof", page().locator("#subject-id").inputValue());
        page().locator("#subjects-search").fill(""); page().locator(".definition-browser-pager button").last().click();
        nav("Teachers"); nav("Subjects"); assertTrue(page().locator(".definition-browser-pager").innerText().contains("Page 2"));
        page().locator("#subject-displayName").fill("Unsaved across inspection"); saved = storedDocument();
        for (int width : List.of(700, 390, 1280, 1600)) {
            workbench.viewport(width, 844); workbench.expect("document.documentElement.scrollWidth <= innerWidth");
            if (width <= 700) assertEquals(0, page().locator("#subject-form").count());
            else assertEquals("Unsaved across inspection", page().locator("#subject-displayName").inputValue());
            assertEquals(saved, storedDocument());
        }
    }

    @Test
    void namePickersGridsAndFieldIssuesPreserveIdsAndOptionalPresence() {
        ObjectNode source = fixtures.validAcceptedDocument(); storeAccepted(source); open(); nav("Classes");
        page().locator("#class-curatorTeacherId-mode").selectOption("value");
        page().locator("#class-curatorTeacherId-picker").fill("Alex");
        page().locator("#class-curatorTeacherId-options button").first().click();
        assertEquals("teacher-alex", page().locator("#class-curatorTeacherId").inputValue());
        assertEquals("Alex", page().locator("#class-curatorTeacherId-picker").inputValue());
        page().locator("#class-homeRoomId-mode").selectOption("value"); page().locator("#class-homeRoomId-picker").fill("101");
        page().locator("#class-homeRoomId-options button").first().click();
        assertEquals("room-101", page().locator("#class-homeRoomId").inputValue());
        page().locator("#class-size").fill("invalid count"); page().locator("#save-class").click();
        workbench.expect("document.querySelector('#class-size')?.getAttribute('aria-invalid') === 'true'");
        assertEquals("invalid count", draft().path("definition").path("cohorts").get(0).path("size").asString());
        assertEquals("teacher-alex", draft().path("definition").path("cohorts").get(0).path("curatorTeacherId").asString());
        page().locator(".definition-editor-issues a[href='#class-size']").first().click();
        assertEquals("class-size", page().evaluate("document.activeElement.id"));
        page().locator("#class-size").fill("25"); page().locator("#save-class").click(); workbench.expect("!document.querySelector('#class-size')?.hasAttribute('aria-invalid')");
        nav("Rooms"); assertNull(page().locator("#room-availability").getAttribute("open"));
        page().locator("#room-availability > summary").click(); assertTrue(page().locator(".definition-week-grid").count() > 0);
        page().locator("#room-availablePeriodIds-mode").focus();
        page().locator("#room-availablePeriodIds-mode").selectOption("explicit");
        workbench.expect("document.activeElement.id === 'room-availablePeriodIds-mode'");
        assertNotNull(page().locator("#room-availability").getAttribute("open"));
        page().locator("input[data-field='availablePeriodIds'][data-reference-id='mon-1']").check(); page().locator("#save-room").click();
        workbench.expect("document.querySelector('#school-save-status')?.textContent.startsWith('Saved —')");
        assertEquals(JSON.readTree("[\"mon-1\"]"), draft().path("definition").path("rooms").get(0).path("availablePeriodIds"));
        page().locator("#room-availablePeriodIds-mode").selectOption("default"); page().locator("#save-room").click();
        workbench.expect("document.querySelector('#school-save-status')?.textContent.startsWith('Saved —')");
        assertFalse(draft().path("definition").path("rooms").get(0).has("availablePeriodIds"));
        assertEquals(source.path("acceptedBaseline"), storedWorkspaceDocument().path("acceptedBaseline"));
        assertEquals(source, draft().path("sourceDocument"));
        assertEquals("sticky", page().locator(".definition-editor-actions").evaluate("node => getComputedStyle(node).position"));
        nav("Weekly requirements"); page().locator("#requirement-form").waitFor();
        assertEquals(6, page().locator(".definition-record-table th").count());
        assertEquals("Year 7A", page().locator("#requirement-cohortId-picker").inputValue());
        assertEquals("cohort-7a", page().locator("#requirement-cohortId").inputValue());
        assertNull(page().locator(".authoring-draft-details").getAttribute("open"));
    }

    private Page page() { return workbench.page(); }
    private void open() { workbench.open(); page().locator("#open-authoring").click(); page().locator("#confirm-authoring").click(); page().locator("#discard-authoring").waitFor(); }
    private void nav(String section) { page().locator("button[data-section='" + section + "']").click(); }
    private JsonNode draft() { return storedWorkspaceDocument().path("definitionDraft"); }
    private JsonNode subject(String id) { for (JsonNode row : draft().path("definition").path("subjects")) if (row.path("id").asString().equals(id)) return row; fail("Missing subject"); return null; }
}
