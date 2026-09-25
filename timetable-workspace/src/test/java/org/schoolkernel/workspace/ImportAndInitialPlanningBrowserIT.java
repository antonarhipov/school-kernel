package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Download;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class ImportAndInitialPlanningBrowserIT extends WorkbenchBrowserSupport {

    @TempDir
    Path files;

    @Test
    @DisplayName("UC-1 browser journey: administrator imports a definition and sees its supplied school name")
    void importsInitialDefinition() {
        workbench.open().awaitText("Empty workspace");
        workbench.setFiles("#definition", ROOT.resolve("examples/initial-school.json"));
        workbench.click("#json-import button[type=submit]");

        String rendered = workbench.awaitText("Initial draft", Workbench.SOLVE);
        assertTrue(rendered.contains("Demo School"));
        assertFalse(rendered.contains("School name unavailable"));
    }

    @Test
    @DisplayName("Timetable UX polish UC-1 ext 1a: missing accepted lesson name is refused before Current and leaves workspace unchanged")
    void refusesUnmappableAcceptedMetadata() throws Exception {
        ObjectNode accepted = fixtures.validAcceptedDocument();
        ObjectNode definition = ((ObjectNode) accepted.path("acceptedBaseline").path("definition")).deepCopy();
        ((ObjectNode) definition.path("lessons").get(0)).remove("displayName");
        Path definitionFile = Files.write(files.resolve("missing-lesson-name.json"), JSON.writeValueAsBytes(definition));
        Path resultFile = Files.write(files.resolve("matching-result.json"),
                JSON.writeValueAsBytes(accepted.path("acceptedBaseline").path("result")));
        String before = storedDocument();

        workbench.open().awaitText("Empty workspace");
        workbench.setFiles("#definition", definitionFile);
        workbench.setFiles("#result", resultFile);
        workbench.click("#json-import button[type=submit]");

        String rejected = workbench.awaitText("Import verification failed", Workbench.SOLVE);
        assertTrue(rejected.contains("Empty workspace"));
        assertFalse(rejected.contains("Accepted baseline"));
        workbench.expectNot("document.querySelector('#workbench-inspector, #workbench-modes, [data-lesson-id]')",
                "invalid accepted pair cannot reach a Current lesson or inspector");
        assertEquals(before, storedDocument(), "refused accepted pair must not modify workspace document or version");
        assertEquals("EMPTY", storedLifecycle());
    }

    @Test
    @DisplayName("UC-2 browser journey: administrator creates, reviews, confirms, and opens the first accepted timetable")
    void plansReviewsAndAcceptsInitialTimetable() {
        workbench.open().awaitText("Empty workspace");
        workbench.setFiles("#definition", ROOT.resolve("examples/initial-school.json"));
        workbench.click("#json-import button[type=submit]");
        String initial = workbench.awaitText("Create 30-second proposal", Workbench.SOLVE);
        assertTrue(initial.contains("Lessons\n2"));
        assertTrue(initial.contains("No accepted timetable"));

        workbench.click("#start-plan");
        String proposal = workbench.awaitText("Initial proposal · feasible", Workbench.SOLVE);
        assertTrue(proposal.contains("No timetable is accepted yet"));
        assertTrue(proposal.contains("Execution limit\nPT30S"));
        assertTrue(proposal.contains("Termination reason"));
        assertTrue(proposal.contains("Timetable details"));

        workbench.check("#confirm-accept");
        workbench.click("#accept-proposal");
        String accepted = workbench.awaitText("Accepted baseline", Workbench.SOLVE);
        assertTrue(accepted.contains("Demo School"));
        assertTrue(accepted.contains("Current · accepted"));
        assertTrue(accepted.contains("Timetable details"));
    }

    @Test
    @DisplayName("UC-8 browser journey: administrator sees exact accepted identity and receives a verified ZIP")
    void exportsAcceptedBaseline() throws Exception {
        storeAccepted(fixtures.validAcceptedDocument());
        String before = storedDocument();
        JsonNode expected = fixtures.validAcceptedDocument().path("acceptedBaseline");

        workbench.open().awaitText("Accepted baseline · current timetable");
        workbench.click("#utilities > summary");
        String rendered = workbench.awaitText("Download verified accepted bundle");
        assertTrue(rendered.contains("Export accepted baseline"));
        assertTrue(rendered.contains("Demo School"));
        assertTrue(rendered.contains(expected.path("result").path("inputRevision").stringValue()));
        assertTrue(rendered.contains(expected.path("result").path("timetableRevision").stringValue()));

        Download download = workbench.page().waitForDownload(() -> workbench.click("#export-accepted"));
        assertEquals("accepted-baseline.zip", download.suggestedFilename());
        byte[] archive = Files.readAllBytes(download.path());
        assertArrayEquals(new byte[] { 'P', 'K' }, Arrays.copyOf(archive, 2), "the downloaded bundle is a ZIP archive");

        APIResponse response = workbench.page().request().get("http://localhost:" + port + "/api/accepted/export");
        assertEquals(200, response.status());
        assertEquals("application/zip", response.headers().get("content-type"));
        assertTrue(response.headers().get("content-disposition").contains("accepted-baseline.zip"));
        assertEquals(before, storedDocument(), "UC-8 browser export must preserve accepted state exactly");
    }

    @Test
    @DisplayName("UC-3 and RULE-19: one GET returns the complete accepted display snapshot without mutation")
    void returnsCompleteAcceptedSnapshotWithoutMutation() {
        ObjectNode document = fixtures.acceptedDocument(false);
        storeAccepted(document);
        String before = storedDocument();

        APIResponse response = workbench.page().request().get("http://localhost:" + port + "/api/workspace");

        assertEquals(200, response.status());
        assertEquals("\"ws-7\"", response.headers().get("etag"));
        JsonNode snapshot = JSON.readTree(response.text());
        assertEquals("ACCEPTED_BASELINE", snapshot.path("state").stringValue());
        assertTrue(snapshot.path("acceptedTimetable").booleanValue());
        assertEquals(document, snapshot.path("workspace"));
        assertEquals(before, storedDocument());
    }
}
