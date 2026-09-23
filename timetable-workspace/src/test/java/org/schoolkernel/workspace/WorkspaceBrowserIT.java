package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = WorkspaceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkspaceBrowserIT {
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("browser_workspace")
            .withUsername("workspace")
            .withPassword("workspace");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("workspace.kernel-executable", () -> ROOT.resolve("school-kernel").toString());
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ProposalReviewService reviews;

    @Autowired
    KernelVerifier verifier;

    @TempDir
    Path browserProfile;

    private Process browser;

    @BeforeEach
    void reset() {
        jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state='EMPTY', version=0, active_run_id=NULL, document='{}'::jsonb
                        WHERE workspace_id=1
                        """)
                .update();
    }

    @AfterEach
    void stopBrowser() throws Exception {
        if (browser != null) {
            browser.destroy();
            if (!browser.waitFor(5, TimeUnit.SECONDS)) {
                browser.destroyForcibly();
            }
        }
    }

    @Test
    @DisplayName("UC-1 browser journey: administrator imports a definition and sees its supplied school name")
    void importsInitialDefinitionInRealBrowser() throws Exception {
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("DOM.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Empty workspace", Duration.ofSeconds(15));

            int rootNode = cdp.command("DOM.getDocument", JSON.createObjectNode())
                    .path("result").path("root").path("nodeId").intValue();
            ObjectNode query = JSON.createObjectNode();
            query.put("nodeId", rootNode);
            query.put("selector", "#definition");
            int inputNode = cdp.command("DOM.querySelector", query)
                    .path("result").path("nodeId").intValue();
            ObjectNode files = JSON.createObjectNode();
            files.put("nodeId", inputNode);
            files.putArray("files").add(ROOT.resolve("examples/initial-school.json").toString());
            cdp.command("DOM.setFileInputFiles", files);
            cdp.evaluate("document.querySelector('#json-import button').click()");

            String rendered = cdp.awaitText("Initial draft", Duration.ofSeconds(30));
            assertTrue(rendered.contains("Demo School"));
            assertFalse(rendered.contains("School name unavailable"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("Timetable UX polish UC-1 ext 1a: missing accepted lesson name is refused before Current and leaves workspace unchanged")
    void refusesUnmappableAcceptedMetadataInRealBrowser() throws Exception {
        ObjectNode accepted = validAcceptedDocument();
        ObjectNode definition = ((ObjectNode) accepted.path("acceptedBaseline").path("definition")).deepCopy();
        ((ObjectNode) definition.path("lessons").get(0)).remove("displayName");
        Path definitionFile = browserProfile.resolve("missing-lesson-name.json");
        Path resultFile = browserProfile.resolve("matching-result.json");
        Files.write(definitionFile, JSON.writeValueAsBytes(definition));
        Files.write(resultFile, JSON.writeValueAsBytes(accepted.path("acceptedBaseline").path("result")));
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("DOM.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Empty workspace", Duration.ofSeconds(15));
            int rootNode = cdp.command("DOM.getDocument", JSON.createObjectNode())
                    .path("result").path("root").path("nodeId").intValue();
            for (String[] input : new String[][] { { "#definition", definitionFile.toString() }, { "#result", resultFile.toString() } }) {
                ObjectNode query = JSON.createObjectNode().put("nodeId", rootNode).put("selector", input[0]);
                int inputNode = cdp.command("DOM.querySelector", query).path("result").path("nodeId").intValue();
                ObjectNode files = JSON.createObjectNode().put("nodeId", inputNode);
                files.putArray("files").add(input[1]);
                cdp.command("DOM.setFileInputFiles", files);
            }
            cdp.evaluate("document.querySelector('#json-import button').click()");
            String rejected = cdp.awaitText("Import verification failed", Duration.ofSeconds(30));
            assertTrue(rejected.contains("Empty workspace"));
            assertFalse(rejected.contains("Accepted baseline"));
            assertFalse(cdp.evaluateValue("Boolean(document.querySelector('#workbench-inspector, #workbench-modes, [data-lesson-id]'))")
                    .path("result").path("result").path("value").booleanValue(), "invalid accepted pair cannot reach a Current lesson or inspector");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "refused accepted pair must not modify workspace document or version");
        assertEquals("EMPTY", jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single());
    }

    @Test
    @DisplayName("UC-2 browser journey: administrator creates, reviews, confirms, and opens the first accepted timetable")
    void plansReviewsAndAcceptsInitialTimetableInRealBrowser() throws Exception {
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("DOM.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Empty workspace", Duration.ofSeconds(15));

            int rootNode = cdp.command("DOM.getDocument", JSON.createObjectNode())
                    .path("result").path("root").path("nodeId").intValue();
            ObjectNode query = JSON.createObjectNode();
            query.put("nodeId", rootNode);
            query.put("selector", "#definition");
            int inputNode = cdp.command("DOM.querySelector", query)
                    .path("result").path("nodeId").intValue();
            ObjectNode files = JSON.createObjectNode();
            files.put("nodeId", inputNode);
            files.putArray("files").add(ROOT.resolve("examples/initial-school.json").toString());
            cdp.command("DOM.setFileInputFiles", files);
            cdp.evaluate("document.querySelector('#json-import button').click()");
            String initial = cdp.awaitText("Create 30-second proposal", Duration.ofSeconds(30));
            assertTrue(initial.contains("Lessons\n2"));
            assertTrue(initial.contains("No accepted timetable"));

            cdp.evaluate("document.querySelector('#start-plan').click()");
            String proposal = cdp.awaitText("Initial proposal · feasible", Duration.ofSeconds(45));
            assertTrue(proposal.contains("No timetable is accepted yet"));
            assertTrue(proposal.contains("Execution limit\nPT30S"));
            assertTrue(proposal.contains("Termination reason"));
            assertTrue(proposal.contains("Timetable details"));

            cdp.evaluate("document.querySelector('#confirm-accept').click(); document.querySelector('#accept-proposal').click()");
            String accepted = cdp.awaitText("Accepted baseline", Duration.ofSeconds(30));
            assertTrue(accepted.contains("Accepted timetable · Demo School"));
            assertTrue(accepted.contains("current accepted timetable"));
            assertTrue(accepted.contains("Timetable details"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("UC-8 browser journey: administrator sees exact accepted identity and receives a verified ZIP")
    void exportsAcceptedBaselineInRealBrowser() throws Exception {
        storeAccepted(validAcceptedDocument());
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Accepted baseline · current timetable", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('#utilities summary').click()");
            String rendered = cdp.awaitText("Download verified accepted bundle", Duration.ofSeconds(15));
            JsonNode expected = validAcceptedDocument().path("acceptedBaseline");
            assertTrue(rendered.contains("Export accepted baseline"));
            assertTrue(rendered.contains("Demo School"));
            assertTrue(rendered.contains(expected.path("result").path("inputRevision").stringValue()));
            assertTrue(rendered.contains(expected.path("result").path("timetableRevision").stringValue()));
            JsonNode response = cdp.evaluateValue("""
                    fetch('/api/accepted/export').then(async response => ({
                      status: response.status,
                      type: response.headers.get('Content-Type'),
                      disposition: response.headers.get('Content-Disposition'),
                      size: (await response.arrayBuffer()).byteLength
                    }))
                    """).path("result").path("result").path("value");
            assertEquals(200, response.path("status").intValue());
            assertEquals("application/zip", response.path("type").stringValue());
            assertTrue(response.path("disposition").stringValue().contains("accepted-baseline.zip"));
            assertTrue(response.path("size").intValue() > 0);
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-8 browser export must preserve accepted state exactly");
    }

    @Test
    @DisplayName("Timetable inspection regression: accepted whole-school inspection remains local, narrow-safe, and immutable")
    void inspectsAcceptedWholeSchoolTimetableInRealBrowser() throws Exception {
        ObjectNode accepted = acceptedDocument(false);
        storeAccepted(accepted);
        String before = storedDocument();

        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            String complete = cdp.awaitText("Complete school population", Duration.ofSeconds(15));
            assertTrue(complete.contains("Accepted baseline · current timetable"));
            assertTrue(complete.contains("Mathematics"));
            assertTrue(complete.contains("Room 102"));
            assertTrue(complete.contains("Empty"));
            assertTrue(complete.contains("Showing 1 of 1 classes"));
            assertTrue(cdp.evaluateValue("Boolean(document.querySelector('.week-matrix'))")
                    .path("result").path("result").path("value").booleanValue());

            cdp.evaluate("document.querySelector('[data-range=\"DAY\"]').click()");
            String day = cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            assertTrue(day.contains("Alex"));
            assertTrue(cdp.evaluateValue("Boolean(document.querySelector('.matrix:not(.week-matrix)'))")
                    .path("result").path("result").path("value").booleanValue());
            assertTrue(cdp.evaluateValue("localStorage.getItem('school-kernel.inspection.v1.opaque-school-id')")
                    .path("result").path("result").path("value").stringValue().contains("\"range\":\"DAY\""));
            cdp.evaluate("document.querySelector('[data-range=\"WEEK\"]').click()");
            cdp.awaitText("Complete recurring Week", Duration.ofSeconds(5));

            cdp.evaluate("document.querySelector('#lesson-search').focus()");
            cdp.command("Input.insertText", object("text", "Science"));
            String searched = cdp.awaitText("Search matches: 1", Duration.ofSeconds(5));
            assertTrue(searched.contains("Complete school population"));
            assertTrue(cdp.evaluateValue("Boolean(document.querySelector('.lesson-cell.search-match'))")
                    .path("result").path("result").path("value").booleanValue());
            assertTrue(cdp.evaluateValue("[...document.querySelectorAll('[data-lesson-id]')].every(button => !button.hidden)")
                    .path("result").path("result").path("value").booleanValue());

            cdp.evaluate("document.querySelector('#lesson-search').value='not-present'; document.querySelector('#lesson-search').dispatchEvent(new Event('input',{bubbles:true}))");
            String noMatch = cdp.awaitText("Search matches: 0", Duration.ofSeconds(5));
            assertTrue(noMatch.contains("Complete school population"));
            assertFalse(noMatch.contains("This narrowed view is empty"));
            cdp.evaluate("document.querySelector('#reset-view').click()");
            cdp.awaitText("Complete school population", Duration.ofSeconds(5));

            cdp.evaluate("document.querySelector('[data-lesson-id=\"lesson-math-1\"]').focus()");
            cdp.pressKey(" ", "Space");
            String details = cdp.awaitText("Accepted assignment", Duration.ofSeconds(5));
            assertTrue(details.contains("Mathematics 1"));
            assertTrue(details.contains("Year 7A"));
            cdp.evaluate("document.querySelector('.lesson-panel details').open=true");
            assertTrue(cdp.awaitText("kernel term cohort", Duration.ofSeconds(5)).contains("lesson-math-1"));

            cdp.evaluate("document.querySelector('[data-open-focus=\"teacherId\"]').focus()");
            cdp.pressKey(" ", "Space");
            String focused = cdp.awaitText("Teacher schedule · Alex", Duration.ofSeconds(5));
            assertTrue(focused.contains("Monday"));
            assertTrue(focused.contains("Return to whole-school matrix"));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            assertTrue(cdp.evaluateValue("document.querySelector('.workspace-card').classList.contains('compact-density') && !document.querySelector('[data-density]')")
                    .path("result").path("result").path("value").booleanValue());

            ObjectNode metrics = JSON.createObjectNode().put("width", 390).put("height", 844)
                    .put("deviceScaleFactor", 1).put("mobile", true);
            cdp.command("Emulation.setDeviceMetricsOverride", metrics);
            String narrow = cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(5));
            assertTrue(narrow.contains("Class schedule · Year 7A"));
            assertFalse(narrow.contains("Accept as current"));
            assertFalse(cdp.evaluateValue("Boolean(document.querySelector('.matrix-wrap'))")
                    .path("result").path("result").path("value").booleanValue());
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-3 G6 inspection must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("Timetable UX polish UC-1: scale Current workbench retains Week/Day, focus, selection, inspector, and Utilities through failed export and narrow view")
    void inspectsPolishedCurrentWorkbenchInRealBrowser() throws Exception {
        ObjectNode document = scaleDocument();
        String acceptedRevision = document.path("acceptedBaseline").path("result").path("timetableRevision")
                .asText(document.path("timetableRevision").stringValue());
        storeAccepted(document);
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            String current = cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            assertTrue(current.contains("Scale School"));
            assertTrue(current.contains("Accepted baseline · current timetable"));
            assertEquals("Accepted revision: " + acceptedRevision, cdp.evaluateValue("document.querySelector('.accepted-heading .revision')?.textContent")
                    .path("result").path("result").path("value").stringValue(), "header must identify the accepted revision");
            assertTrue(cdp.evaluateValue("document.querySelector('#workbench-modes [data-mode=\"CURRENT\"]')?.getAttribute('aria-pressed') === 'true' && document.querySelectorAll('#workbench-modes [data-mode]').length === 1")
                    .path("result").path("result").path("value").booleanValue(), "only Current is available in accepted state");
            assertTrue(cdp.evaluateValue("Boolean(document.querySelector('#accepted-view .week-matrix')) && document.querySelectorAll('#accepted-view [data-lesson-id]').length === 1000")
                    .path("result").path("result").path("value").booleanValue(), "complete whole-school Week must represent every scale lesson");

            cdp.evaluate("document.querySelector('[data-range=DAY]').click()");
            String day = cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            assertTrue(day.contains("Complete school population"));
            assertTrue(cdp.evaluateValue("document.querySelectorAll('#accepted-view .matrix:not(.week-matrix) [data-lesson-id]').length === 204")
                    .path("result").path("result").path("value").booleanValue(), "Monday has 17 groups of 12 assignments");
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click()");
            cdp.awaitText("Complete recurring Week", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#subject-investigation').value='subject-0'; document.querySelector('#subject-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(cdp.awaitText("Subject matches: 60", Duration.ofSeconds(5)).contains("Complete school population"));
            cdp.evaluate("document.querySelector('#subject-investigation').value=''; document.querySelector('#subject-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.evaluate("document.querySelector('#cohort-filter').value='cohort-0'; document.querySelector('#cohort-filter').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.awaitText("Filtered whole-school matrix", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-0]').click()");
            String details = cdp.awaitText("Accepted assignment", Duration.ofSeconds(5));
            assertTrue(details.contains("Declared lesson 0"));
            assertTrue(cdp.evaluateValue("document.querySelector('#lesson-details-host')?.contains(document.querySelector('#lesson-panel-title')) && document.querySelector('#workbench-inspector')?.contains(document.querySelector('#lesson-panel-title')) && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 0'")
                    .path("result").path("result").path("value").booleanValue(), "exact selection belongs to the side inspector");
            cdp.evaluate("document.querySelector('#lesson-details-host details').open=true");
            String acceptedFields = cdp.evaluateValue("document.querySelector('#lesson-details-host').innerText")
                    .path("result").path("result").path("value").stringValue();
            for (String field : new String[] { "Subject 0", "Class 0", "Teacher 0", "Declared period 0", "Room 0", "lesson-0" }) {
                assertTrue(acceptedFields.contains(field), field);
            }

            cdp.evaluate("document.querySelector('[data-open-focus=cohortId]').click()");
            cdp.awaitText("Class schedule · Class 0", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            assertTrue(cdp.awaitText("Filtered whole-school matrix", Duration.ofSeconds(5)).contains("Class: Class 0"));
            assertTrue(cdp.evaluateValue("document.querySelector('#cohort-filter').value === 'cohort-0' && document.querySelector('[data-range=WEEK]').getAttribute('aria-pressed') === 'true' && document.querySelector('[data-lesson-id=lesson-0]').getAttribute('aria-pressed') === 'true'")
                    .path("result").path("result").path("value").booleanValue(), "focused return restores range, filter, and selection");

            JsonNode open = cdp.evaluateValue("({width:document.querySelector('.canvas-region').getBoundingClientRect().width, inspector:document.querySelector('#workbench-inspector').getBoundingClientRect().width, requests:performance.getEntriesByType('resource').filter(entry => entry.name.endsWith('/api/workspace')).length})")
                    .path("result").path("result").path("value");
            assertTrue(open.path("inspector").doubleValue() > 200, "open inspector must occupy a fixed desktop column");
            cdp.evaluate("document.querySelector('#toggle-inspector').click()");
            JsonNode collapsed = cdp.evaluateValue("({width:document.querySelector('.canvas-region').getBoundingClientRect().width, selected:document.querySelector('[data-lesson-id=lesson-0]').getAttribute('aria-pressed'), summary:document.querySelector('#inspector-summary')?.innerText, requests:performance.getEntriesByType('resource').filter(entry => entry.name.endsWith('/api/workspace')).length})")
                    .path("result").path("result").path("value");
            assertTrue(collapsed.path("width").doubleValue() > open.path("width").doubleValue(), "collapse expands the canvas");
            assertEquals("true", collapsed.path("selected").stringValue());
            assertTrue(collapsed.path("summary").stringValue().contains("Declared lesson 0"), "collapsed inspector retains a visible selection summary");
            assertEquals(open.path("requests").intValue(), collapsed.path("requests").intValue(), "collapse must not reload workspace");
            cdp.evaluate("document.querySelector('#reopen-inspector').click()");
            assertTrue(cdp.evaluateValue("document.querySelector('#workbench-inspector').getBoundingClientRect().width === " + open.path("inspector").doubleValue() + " && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 0'")
                    .path("result").path("result").path("value").booleanValue(), "reopen restores the same width and exact selection");

            assertTrue(cdp.evaluateValue("!document.querySelector('#utilities').open && !document.querySelector('#export-accepted')?.getClientRects().length")
                    .path("result").path("result").path("value").booleanValue(), "export starts inside the closed Utilities disclosure");
            cdp.evaluate("document.querySelector('#utilities summary').click()");
            String utilities = cdp.awaitText("Download verified accepted bundle", Duration.ofSeconds(5));
            assertTrue(utilities.contains("Export accepted baseline"));
            assertTrue(cdp.evaluateValue("document.querySelector('#utilities').contains(document.querySelector('#export-accepted')) && !/Discard|Cancel run|Accept as current|Revise|Import/.test(document.querySelector('#utilities').innerText)")
                    .path("result").path("result").path("value").booleanValue(), "Utilities contains export, not lifecycle or import actions");
            cdp.command("Network.enable", JSON.createObjectNode());
            cdp.command("Network.setBlockedURLs", JSON.createObjectNode().set("urls",
                    JSON.createArrayNode().add("*/api/accepted/export*")));
            cdp.evaluate("document.querySelector('#export-accepted').click()");
            assertTrue(cdp.awaitText("No accepted bundle was produced", Duration.ofSeconds(15)).contains("Declared lesson 0"));
            assertTrue(cdp.evaluateValue("document.querySelector('#workbench-modes [data-mode=\"CURRENT\"]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#cohort-filter').value === 'cohort-0' && document.querySelector('[data-lesson-id=lesson-0]').getAttribute('aria-pressed') === 'true'")
                    .path("result").path("result").path("value").booleanValue(), "failed export retains Current, filter, and selection");
            assertEquals(before, storedDocument(), "failed export cannot change the accepted bundle or workspace version");

            ObjectNode narrowMetrics = JSON.createObjectNode().put("width", 390).put("height", 844)
                    .put("deviceScaleFactor", 1).put("mobile", true);
            cdp.command("Emulation.setDeviceMetricsOverride", narrowMetrics);
            String narrow = cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(5));
            assertTrue(narrow.contains("Accepted baseline"));
            assertFalse(narrow.contains("Complete school population"));
            assertFalse(cdp.evaluateValue("Boolean(document.querySelector('.matrix-wrap, #start-repair-form, #apply-pin, #cancel-run, #accept-repair'))")
                    .path("result").path("result").path("value").booleanValue(), "narrow view must not expose desktop canvas or mutation controls");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-1 workbench presentation and failed export must preserve exact durable state");
    }

    @Test
    @DisplayName("UC-2 main/extensions/G1-G8/RULE-14: real browser traces exact subject teaching and teacher load without mutation")
    void tracesSubjectTeachingAndTeacherLoadInRealBrowser() throws Exception {
        storeAccepted(acceptedDocument(false));
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Complete school population", Duration.ofSeconds(15));

            cdp.evaluate("""
                    const subject=document.querySelector('#subject-investigation');
                    subject.value='math'; subject.dispatchEvent(new Event('change',{bubbles:true}));
                    """);
            String subjectOnly = cdp.awaitText("Subject matches: 1", Duration.ofSeconds(5));
            assertTrue(subjectOnly.contains("Teacher matches: 0"));
            assertTrue(cdp.evaluateValue("[...document.querySelectorAll('[data-lesson-id]')].every(button => !button.hidden)")
                    .path("result").path("result").path("value").booleanValue(), "subject highlighting must retain nonmatches");
            assertTrue(cdp.evaluateValue("document.querySelector('[data-lesson-id=\"lesson-math-1\"]').classList.contains('subject-match')")
                    .path("result").path("result").path("value").booleanValue());

            cdp.evaluate("""
                    const teacher=document.querySelector('#teacher-investigation');
                    teacher.value='teacher-alex'; teacher.dispatchEvent(new Event('change',{bubbles:true}));
                    """);
            String combined = cdp.awaitText("Dual matches: 1", Duration.ofSeconds(5));
            assertTrue(combined.contains("Teacher matches: 2"));
            assertTrue(combined.contains("Teacher load by period · Alex"));
            assertTrue(combined.contains("Monday 1\nAssigned"));
            assertTrue(combined.contains("Monday 2\nAssigned"));
            assertTrue(combined.contains("Monday 3\nUnavailable"));
            assertTrue(combined.contains("Tuesday 1\nAvailable · unassigned"));
            assertTrue(cdp.evaluateValue("document.querySelector('[data-lesson-id=\"lesson-math-1\"]').classList.contains('dual-match')")
                    .path("result").path("result").path("value").booleanValue());

            cdp.evaluate("document.querySelector('[data-lesson-id=\"lesson-science-1\"]').click(); document.querySelector('#subject-only').click()");
            String selectionCleared = cdp.awaitText("outside the active filters", Duration.ofSeconds(5));
            assertTrue(selectionCleared.contains("Subject: Mathematics"));
            assertFalse(cdp.evaluateValue("Boolean(document.querySelector('#lesson-details-host .lesson-panel'))")
                    .path("result").path("result").path("value").booleanValue());
            assertTrue(cdp.evaluateValue("document.querySelector('[data-lesson-id=\"lesson-science-1\"]').hidden")
                    .path("result").path("result").path("value").booleanValue());

            cdp.evaluate("document.querySelector('#teacher-only').click()");
            String filtered = cdp.awaitText("Represented lessons: 1", Duration.ofSeconds(5));
            assertTrue(filtered.contains("Teacher matches: 1"), "filtered totals must count only represented lessons");
            assertTrue(filtered.contains("Dual matches: 1"));
            cdp.evaluate("document.querySelector('[data-range=\"DAY\"]').click()");
            String day = cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            assertTrue(day.contains("Subject matches: 1"));
            assertTrue(day.contains("Teacher matches: 1"));

            cdp.evaluate("document.querySelector('#clear-subject').click()");
            String teacherRetained = cdp.awaitText("Teacher matches: 2", Duration.ofSeconds(5));
            assertTrue(teacherRetained.contains("Subject matches: 0"));
            assertTrue(cdp.evaluateValue("document.querySelector('#teacher-investigation').value")
                    .path("result").path("result").path("value").stringValue().equals("teacher-alex"));

            cdp.evaluate("""
                    const weekday=document.querySelector('#weekday');
                    weekday.value='TUESDAY'; weekday.dispatchEvent(new Event('change',{bubbles:true}));
                    """);
            cdp.awaitText("Day · Tuesday", Duration.ofSeconds(5));
            cdp.evaluate("""
                    const subject=document.querySelector('#subject-investigation');
                    subject.value='math'; subject.dispatchEvent(new Event('change',{bubbles:true}));
                    """);
            cdp.awaitText("Subject matches: 0", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#subject-only').click()");
            String emptyIntersection = cdp.awaitText("No lessons match the active filters", Duration.ofSeconds(5));
            assertTrue(emptyIntersection.contains("Subject matches: 0"));
            assertTrue(emptyIntersection.contains("Teacher matches: 0"));

            cdp.evaluate("document.querySelector('#clear-subject').click()");
            String noTeacherAssignments = cdp.awaitText("Teacher matches: 0", Duration.ofSeconds(5));
            assertTrue(noTeacherAssignments.contains("Teacher load by period · Alex"));
            assertTrue(noTeacherAssignments.contains("Available · unassigned"));

            cdp.evaluate("document.querySelector('#clear-teacher').click(); document.querySelector('#reset-view').click()");
            String cleared = cdp.awaitText("No active filters", Duration.ofSeconds(5));
            assertTrue(cleared.contains("Subject matches: 0"));
            assertTrue(cleared.contains("Teacher matches: 0"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-2 investigation must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-2 G3/G4/G8/RULE-7/8/16: scale browser compares represented IDs and explicit/omitted teacher availability")
    void tracesScaleInvestigationAndOmittedAvailabilityInRealBrowser() throws Exception {
        ObjectNode document = investigationScaleDocument();
        JsonNode definition = document.path("acceptedBaseline").path("definition");
        JsonNode assignments = document.path("acceptedBaseline").path("result").path("timetable").path("assignments");
        KernelVerifier.Verification verified = verifier.verify(new ImportDocuments(definition,
                document.path("acceptedBaseline").path("result"), null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        assertEquals(document.path("definitionRevision").stringValue(), verified.definitionRevision());
        assertEquals(document.path("timetableRevision").stringValue(), verified.timetableRevision());
        assertTrue(definition.path("cohorts").size() >= 60);
        assertTrue(definition.path("teachers").size() >= 100);
        assertTrue(definition.path("rooms").size() >= 100);
        assertTrue(assignments.size() >= 900 && assignments.size() <= 1_100);
        assertEquals(60, definition.path("periods").size());
        assertTrue(definition.path("subjects").get(0).path("displayName").stringValue().length() > 50);
        assertTrue(definition.path("teachers").get(16).path("displayName").stringValue().length() > 50);
        assertTrue(definition.path("cohorts").get(16).path("displayName").stringValue().length() > 50);
        assertTrue(definition.path("rooms").get(16).path("displayName").stringValue().length() > 50);
        assertEquals("subject-0", definition.path("lessons").get(960).path("subjectId").stringValue());
        assertEquals("subject-0", assignments.get(960).path("subjectId").stringValue());
        assertEquals("teacher-16", assignments.get(960).path("teacherId").stringValue());
        assertEquals(41, definition.path("teachers").get(16).path("availablePeriodIds").size());
        assertEquals("period-40", definition.path("teachers").get(16).path("availablePeriodIds").get(40).stringValue());
        assertFalse(definition.path("teachers").get(17).has("availablePeriodIds"));
        Set<String> lessonIds = new HashSet<>();
        for (JsonNode assignment : assignments) lessonIds.add(assignment.path("lessonId").stringValue());
        assertEquals(assignments.size(), lessonIds.size(), "every generated assignment has one unique declared lesson ID");
        assertEquals(definition.path("lessons").size(), lessonIds.size());
        for (JsonNode assignment : assignments) {
            String id = assignment.path("lessonId").stringValue();
            int ordinal = Integer.parseInt(id.substring("lesson-".length()));
            assertEquals(definition.path("lessons").get(ordinal).path("id"), assignment.path("lessonId"));
            assertEquals(definition.path("lessons").get(ordinal).path("subjectId"), assignment.path("subjectId"));
            assertEquals(definition.path("lessons").get(ordinal).path("cohortId"), assignment.path("cohortId"));
            assertEquals(definition.path("lessons").get(ordinal).path("teacherId"), assignment.path("teacherId"));
        }
        assertEquals("cohort-16", assignments.get(960).path("cohortId").stringValue());
        assertEquals("room-16", assignments.get(960).path("roomId").stringValue());
        assertEquals("period-0", assignments.get(960).path("periodId").stringValue());
        assertEquals("period-39", assignments.get(999).path("periodId").stringValue());
        Set<String> occupiedTeachers = new HashSet<>(), occupiedCohorts = new HashSet<>(), occupiedRooms = new HashSet<>();
        for (JsonNode assignment : assignments) {
            occupiedTeachers.add(assignment.path("teacherId").stringValue());
            occupiedCohorts.add(assignment.path("cohortId").stringValue());
            occupiedRooms.add(assignment.path("roomId").stringValue());
        }
        assertTrue(occupiedTeachers.contains("teacher-0"));
        assertFalse(occupiedTeachers.contains("teacher-17"));
        assertTrue(occupiedCohorts.contains("cohort-0"));
        assertFalse(occupiedCohorts.contains("cohort-59"));
        assertTrue(occupiedRooms.contains("room-0"));
        assertFalse(occupiedRooms.contains("room-99"));

        storeAccepted(document);
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            String complete = cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            assertTrue(complete.contains("Accepted baseline · current timetable"));

            cdp.evaluate("document.querySelector('#subject-investigation').value='subject-0'; document.querySelector('#subject-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertInvestigationPopulation(cdp, document, null, "subject-0", null, false, false);
            cdp.evaluate("document.querySelector('#teacher-investigation').value='teacher-16'; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertInvestigationPopulation(cdp, document, null, "subject-0", "teacher-16", false, false);
            assertTeacherRibbon(cdp, document, "teacher-16", null);
            assertTrue(cdp.evaluateValue("document.querySelector('[data-lesson-id=lesson-960]').classList.contains('dual-match')")
                    .path("result").path("result").path("value").booleanValue());

            cdp.evaluate("document.querySelector('#subject-only').click()");
            assertInvestigationPopulation(cdp, document, null, "subject-0", "teacher-16", true, false);
            cdp.evaluate("document.querySelector('#teacher-only').click()");
            assertInvestigationPopulation(cdp, document, null, "subject-0", "teacher-16", true, true);
            cdp.evaluate("document.querySelector('[data-range=DAY]').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            assertInvestigationPopulation(cdp, document, "MONDAY", "subject-0", "teacher-16", true, true);
            assertTeacherRibbon(cdp, document, "teacher-16", "MONDAY");

            cdp.evaluate("document.querySelector('#clear-subject').click()");
            assertInvestigationPopulation(cdp, document, "MONDAY", null, "teacher-16", false, true);
            cdp.evaluate("document.querySelector('#clear-teacher').click()");
            assertInvestigationPopulation(cdp, document, "MONDAY", null, null, false, false);

            cdp.evaluate("document.querySelector('#subject-investigation').value='subject-19'; document.querySelector('#subject-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertInvestigationPopulation(cdp, document, "MONDAY", "subject-19", null, false, false);
            cdp.evaluate("document.querySelector('#subject-only').click()");
            assertInvestigationPopulation(cdp, document, "MONDAY", "subject-19", null, true, false);
            assertTrue(cdp.awaitText("This narrowed view is empty", Duration.ofSeconds(5)).contains("Subject: Subject 19"));
            cdp.evaluate("document.querySelector('#clear-subject').click()");

            cdp.evaluate("document.querySelector('#teacher-investigation').value='teacher-17'; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertInvestigationPopulation(cdp, document, "MONDAY", null, "teacher-17", false, false);
            assertTeacherRibbon(cdp, document, "teacher-17", "MONDAY");
            cdp.evaluate("document.querySelector('#teacher-only').click()");
            assertInvestigationPopulation(cdp, document, "MONDAY", null, "teacher-17", false, true);
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click()");
            cdp.awaitText("Complete recurring Week", Duration.ofSeconds(5));
            assertInvestigationPopulation(cdp, document, null, null, "teacher-17", false, true);
            assertTeacherRibbon(cdp, document, "teacher-17", null);
            cdp.evaluate("document.querySelector('#clear-teacher').click(); document.querySelector('#reset-view').click()");
            assertInvestigationPopulation(cdp, document, null, null, null, false, false);
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-2 scale investigation must not mutate accepted workspace state");
    }

    private static void assertInvestigationPopulation(Cdp cdp, JsonNode document, String weekday, String subject,
                                                      String teacher, boolean subjectOnly, boolean teacherOnly) throws Exception {
        JsonNode baseline = document.path("acceptedBaseline");
        Set<String> periodIds = new HashSet<>();
        for (JsonNode period : baseline.path("definition").path("periods")) {
            if (weekday == null || weekday.equals(period.path("weekday").stringValue())) periodIds.add(period.path("id").stringValue());
        }
        Set<String> represented = new HashSet<>(), subjectMatches = new HashSet<>(), teacherMatches = new HashSet<>(), dualMatches = new HashSet<>();
        for (JsonNode assignment : baseline.path("result").path("timetable").path("assignments")) {
            if (!periodIds.contains(assignment.path("periodId").stringValue())) continue;
            boolean matchesSubject = subject != null && subject.equals(assignment.path("subjectId").stringValue());
            boolean matchesTeacher = teacher != null && teacher.equals(assignment.path("teacherId").stringValue());
            if (subjectOnly && !matchesSubject || teacherOnly && !matchesTeacher) continue;
            String id = assignment.path("lessonId").stringValue();
            represented.add(id);
            if (matchesSubject) subjectMatches.add(id);
            if (matchesTeacher) teacherMatches.add(id);
            if (matchesSubject && matchesTeacher) dualMatches.add(id);
        }
        String rendered = cdp.awaitText("Represented lessons: " + represented.size(), Duration.ofSeconds(5));
        assertTrue(rendered.contains("Subject matches: " + subjectMatches.size()));
        assertTrue(rendered.contains("Teacher matches: " + teacherMatches.size()));
        assertTrue(rendered.contains("Dual matches: " + dualMatches.size()));
        JsonNode visible = cdp.evaluateValue("[...document.querySelectorAll('.lesson-cell[data-lesson-id]:not([hidden])')].map(tile => tile.dataset.lessonId)")
                .path("result").path("result").path("value");
        Set<String> visibleIds = new HashSet<>();
        for (JsonNode id : visible) visibleIds.add(id.stringValue());
        assertEquals(represented, visibleIds, "the displayed population must equal the independently filtered accepted IDs");
        assertEquals(visible.size(), visibleIds.size(), "no lesson may be counted twice in the represented population");
    }

    private static void assertTeacherRibbon(Cdp cdp, JsonNode document, String teacher, String weekday) throws Exception {
        JsonNode baseline = document.path("acceptedBaseline");
        JsonNode definition = baseline.path("definition");
        JsonNode availability = null;
        for (JsonNode candidate : definition.path("teachers")) {
            if (teacher.equals(candidate.path("id").stringValue())) availability = candidate.path("availablePeriodIds");
        }
        JsonNode slots = cdp.evaluateValue("[...document.querySelectorAll('.teacher-ribbon li')].map(li => ({period:li.querySelector('strong').textContent, state:li.querySelector('span').textContent}))")
                .path("result").path("result").path("value");
        int index = 0;
        for (JsonNode period : definition.path("periods")) {
            if (weekday != null && !weekday.equals(period.path("weekday").stringValue())) continue;
            boolean assigned = false, available = availability == null || availability.isMissingNode();
            for (JsonNode assignment : baseline.path("result").path("timetable").path("assignments")) {
                if (teacher.equals(assignment.path("teacherId").stringValue()) && period.path("id").equals(assignment.path("periodId"))) assigned = true;
            }
            if (!available) for (JsonNode allowed : availability) {
                if (allowed.equals(period.path("id"))) available = true;
            }
            assertEquals(period.path("displayName").stringValue(), slots.get(index).path("period").stringValue());
            assertEquals(assigned ? "Assigned" : available ? "Available · unassigned" : "Unavailable",
                    slots.get(index).path("state").stringValue(), period.path("id").stringValue());
            index++;
        }
        assertEquals(index, slots.size());
    }

    @Test
    @DisplayName("UC-3 main/extensions/G1-G8/RULE-15: real browser highlights search, narrows explicitly, and returns from focused accepted schedules")
    void narrowsAndFocusesAcceptedTimetableInRealBrowser() throws Exception {
        storeAccepted(scaleDocument());
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));

            cdp.evaluate("document.querySelector('#lesson-search').value='Subject 0'; document.querySelector('#lesson-search').dispatchEvent(new Event('input',{bubbles:true}))");
            String search = cdp.awaitText("Search matches: 60", Duration.ofSeconds(5));
            assertTrue(search.contains("Complete school population"));
            assertTrue(cdp.evaluateValue("document.querySelectorAll('.lesson-cell.search-match').length === 60 && [...document.querySelectorAll('[data-lesson-id]')].every(button => !button.hidden)")
                    .path("result").path("result").path("value").booleanValue(), "search must highlight without narrowing");

            cdp.evaluate("document.querySelector('#lesson-search').value='not present'; document.querySelector('#lesson-search').dispatchEvent(new Event('input',{bubbles:true}))");
            String emptySearch = cdp.awaitText("Search matches: 0", Duration.ofSeconds(5));
            assertTrue(emptySearch.contains("Complete school population"));
            assertFalse(emptySearch.contains("This narrowed view is empty"));
            cdp.evaluate("document.querySelector('#reset-view').click()");
            cdp.awaitText("No active filters", Duration.ofSeconds(5));

            cdp.evaluate("document.querySelector('[data-range=DAY]').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            cdp.evaluate("""
                    (() => {
                    const choose=(id,value) => { const control=document.querySelector(id); control.value=value; control.dispatchEvent(new Event('change',{bubbles:true})); };
                    choose('#cohort-filter','cohort-0');
                    choose('#teacher-filter','teacher-0');
                    choose('#room-filter','room-0');
                    choose('#period-focus','period-0');
                    })()
                    """);
            String narrowed = cdp.awaitText("Represented lessons: 1", Duration.ofSeconds(5));
            assertTrue(narrowed.contains("Filtered whole-school matrix"));
            assertTrue(narrowed.contains("Class: Class 0"));
            assertTrue(narrowed.contains("Teacher: Teacher 0"));
            assertTrue(narrowed.contains("Room: Room 0"));
            assertTrue(narrowed.contains("Period: Declared period 0"));

            cdp.evaluate("document.querySelector('#room-filter').value='room-1'; document.querySelector('#room-filter').dispatchEvent(new Event('change',{bubbles:true}))");
            String emptyFiltered = cdp.awaitText("This narrowed view is empty", Duration.ofSeconds(5));
            assertTrue(emptyFiltered.contains("Filtered whole-school matrix"));
            assertTrue(emptyFiltered.contains("Reset view"));
            cdp.evaluate("document.querySelector('#reset-empty').click()");
            cdp.awaitText("Complete school population", Duration.ofSeconds(5));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-3 search and narrowing must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-3 focused schedules/G4-G8/RULE-15: real browser returns from class, teacher, room, empty, and narrow agendas")
    void opensFocusedAcceptedSchedulesInRealBrowser() throws Exception {
        storeAccepted(scaleDocument());
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            cdp.evaluate("document.querySelector('[data-range=DAY]').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            cdp.evaluate("""
                    (() => {
                    const choose=(id,value) => { const control=document.querySelector(id); control.value=value; control.dispatchEvent(new Event('change',{bubbles:true})); };
                    choose('#cohort-filter','cohort-0');
                    choose('#teacher-filter','teacher-0');
                    choose('#room-filter','room-0');
                    })()
                    """);
            cdp.awaitText("Represented lessons: 12", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-0]').click()");
            String details = cdp.awaitText("Accepted assignment", Duration.ofSeconds(5));
            assertTrue(details.contains("Declared lesson 0"));
            assertTrue(details.contains("Class 0"));

            cdp.evaluate("document.querySelector('[data-open-focus=cohortId]').click()");
            assertTrue(cdp.awaitText("Class schedule · Class 0", Duration.ofSeconds(5)).contains("Monday"));
            cdp.evaluate("document.querySelector('[data-focus-type=teacherId]').click()");
            cdp.awaitText("Teacher schedule · Teacher 0", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('[data-focus-type=roomId]').click()");
            cdp.awaitText("Room schedule · Room 0", Duration.ofSeconds(5));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-3 focused schedules must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-3 focused extensions/G4-G8/RULE-15: real browser returns from empty and narrow room agendas")
    void returnsFromEmptyAndNarrowFocusedSchedulesInRealBrowser() throws Exception {
        storeAccepted(scaleDocument());
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            cdp.evaluate("document.querySelector('[data-range=DAY]').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            cdp.evaluate("""
                    (() => {
                    const choose=(id,value) => { const control=document.querySelector(id); control.value=value; control.dispatchEvent(new Event('change',{bubbles:true})); };
                    choose('#cohort-filter','cohort-0');
                    choose('#teacher-filter','teacher-0');
                    choose('#room-filter','room-0');
                    })()
                    """);
            cdp.awaitText("Represented lessons: 12", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('[data-open-focus=roomId]').click()");
            cdp.evaluate("document.querySelector('#focus-entity').value='room-99'; document.querySelector('#focus-entity').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(cdp.awaitText("No accepted lessons are scheduled for this selection.", Duration.ofSeconds(5)).contains("Room 99"));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            String returned = cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            assertTrue(returned.contains("Filtered whole-school matrix"));
            assertTrue(cdp.evaluateValue("document.querySelector('#cohort-filter').value === 'cohort-0' && document.querySelector('#teacher-filter').value === 'teacher-0' && document.querySelector('#room-filter').value === 'room-0'")
                    .path("result").path("result").path("value").booleanValue(), "return must retain the whole-school context");

            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-3 empty focused inspection must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-3 extension 2a: an accepted school with no lessons retains declared classes and empty periods")
    void showsDeclaredEmptyAcceptedTimetableInRealBrowser() throws Exception {
        storeAccepted(acceptedDocument(true));
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            String rendered = cdp.awaitText("No accepted lessons are scheduled", Duration.ofSeconds(15));
            assertTrue(rendered.contains("Year 7A"));
            assertTrue(rendered.contains("Monday 1"));
            assertTrue(rendered.contains("Empty"));
            assertTrue(rendered.contains("Current · accepted"));
            assertTrue(cdp.evaluateValue("document.querySelector('#workbench-modes [data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && document.querySelectorAll('#accepted-view [data-lesson-id]').length === 0")
                    .path("result").path("result").path("value").booleanValue(), "empty accepted snapshot retains Current without inventing a lesson");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-1 extensions 2b, 5a, and 5b: invalid or blocked device preferences never alter accepted inspection")
    void handlesInvalidAndBlockedInspectionPreferencesInRealBrowser() throws Exception {
        storeAccepted(acceptedDocument(false));
        String before = storedDocument();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Complete recurring Week", Duration.ofSeconds(15));

            cdp.evaluate("localStorage.setItem('school-kernel.inspection.v1.opaque-school-id', '{bad')");
            cdp.command("Page.navigate", object("url", page));
            assertTrue(cdp.awaitText("Complete recurring Week", Duration.ofSeconds(5)).contains("Week"));

            for (String invalidPreference : java.util.List.of(
                    "JSON.stringify({version:2,range:'DAY',weekdayId:'MONDAY'})",
                    "JSON.stringify({version:1,range:'OTHER',weekdayId:'MONDAY'})",
                    "JSON.stringify({version:1,range:'DAY',weekdayId:'UNKNOWN'})",
                    "'x'.repeat(1025)")) {
                cdp.evaluate("localStorage.setItem('school-kernel.inspection.v1.opaque-school-id', " + invalidPreference + ")");
                cdp.command("Page.navigate", object("url", page));
                cdp.awaitText("Complete recurring Week", Duration.ofSeconds(5));
            }
            cdp.evaluate("localStorage.removeItem('school-kernel.inspection.v1.opaque-school-id'); localStorage.setItem('school-kernel.inspection.v1.other-school', JSON.stringify({version:1,range:'DAY',weekdayId:'TUESDAY'}))");
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Complete recurring Week", Duration.ofSeconds(5));

            cdp.evaluate("localStorage.setItem('school-kernel.inspection.v1.opaque-school-id', JSON.stringify({version:1,range:'DAY',weekdayId:'MONDAY'}))");
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            assertTrue(cdp.evaluateValue("Object.keys(JSON.parse(localStorage.getItem('school-kernel.inspection.v1.opaque-school-id'))).sort().join(',')")
                    .path("result").path("result").path("value").stringValue().equals("range,version,weekdayId"));
            cdp.evaluate("document.querySelector('[data-lesson-id=\"lesson-math-1\"]').click(); document.querySelector('#weekday').value='TUESDAY'; document.querySelector('#weekday').dispatchEvent(new Event('change',{bubbles:true}))");
            String exclusion = cdp.awaitText("outside the represented Day", Duration.ofSeconds(5));
            assertFalse(exclusion.contains("Accepted assignment"));
            assertFalse(cdp.evaluateValue("Boolean(document.querySelector('#lesson-details-host .lesson-panel'))")
                    .path("result").path("result").path("value").booleanValue());

            cdp.evaluate("Object.getPrototypeOf(localStorage).setItem = () => { throw new Error('blocked'); }; document.querySelector('[data-range=\"WEEK\"]').click()");
            assertTrue(cdp.awaitText("could not save the display preference", Duration.ofSeconds(5)).contains("Current · accepted"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-1 preference failures must not mutate accepted workspace state");
    }

    @Test
    @DisplayName("UC-3 and RULE-19: one GET returns the complete accepted display snapshot without mutation")
    void returnsCompleteAcceptedSnapshotWithoutMutation() throws Exception {
        ObjectNode document = acceptedDocument(false);
        storeAccepted(document);
        String before = storedDocument();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/workspace")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("\"ws-7\"", response.headers().firstValue("ETag").orElseThrow());
        JsonNode snapshot = JSON.readTree(response.body());
        assertEquals("ACCEPTED_BASELINE", snapshot.path("state").stringValue());
        assertTrue(snapshot.path("acceptedTimetable").booleanValue());
        assertEquals(document, snapshot.path("workspace"));
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-4 main/extensions/G4/RULE-19/20/24: real keyboard browser stages, pins, previews, resolves conflict, and discards safely")
    void preparesProtectedRepairDraftInRealBrowser() throws Exception {
        storeAccepted(acceptedDocument(false));
        JsonNode acceptedBefore = JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single()).path("acceptedBaseline").deepCopy();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(15));
            cdp.evaluate("""
                    document.querySelector('.repair-entry').open=true;
                    document.querySelector('[name=period][value="mon-1"]').checked=true;
                    document.querySelector('#start-repair-form').requestSubmit();
                    """);
            String draft = cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            assertTrue(draft.contains("Directly affected lessons\n1"));
            assertTrue(draft.contains("Accepted baseline remains current"));
            assertTrue(draft.contains("Mathematics 1") || draft.contains("Mathematics"));

            String durableDraft = storedDocument();
            cdp.evaluate("document.querySelector('[data-lesson-id=\"lesson-math-1\"]').click()");
            JsonNode requestsBeforeMode = cdp.evaluateValue("performance.getEntriesByType('resource').filter(entry => entry.name.endsWith('/api/workspace')).length")
                    .path("result").path("result").path("value");
            cdp.evaluate("document.querySelector('#workbench-modes [data-mode=CURRENT]').click()");
            assertTrue(cdp.awaitText("Complete school population", Duration.ofSeconds(5)).contains("Repair draft · not current · Current"));
            assertTrue(cdp.evaluateValue("document.querySelector('#workbench-modes [data-mode=CURRENT]').getAttribute('aria-pressed') === 'true' && document.querySelector('#accepted-view .week-matrix') !== null && document.querySelector('#workbench-inspector #lesson-panel-title')?.textContent === 'Mathematics 1' && !document.querySelector('#apply-pin')")
                    .path("result").path("result").path("value").booleanValue(), "Current from Draft shows exact accepted selection without draft mutation controls");
            cdp.evaluate("document.querySelector('#cohort-filter').value='cohort-7a'; document.querySelector('#cohort-filter').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.evaluate("document.querySelector('#workbench-modes [data-mode=DRAFT]').click()");
            cdp.awaitText("Bulk-protect accepted assignments", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#workbench-modes [data-mode=CURRENT]').click()");
            assertTrue(cdp.evaluateValue("document.querySelector('#cohort-filter').value === 'cohort-7a' && document.querySelector('[data-range=WEEK]').getAttribute('aria-pressed') === 'true' && document.querySelector('#workbench-inspector #lesson-panel-title')?.textContent === 'Mathematics 1'")
                    .path("result").path("result").path("value").booleanValue(), "mode round trip retains representable filter, Week, and selection");
            assertEquals(requestsBeforeMode.intValue(), cdp.evaluateValue("performance.getEntriesByType('resource').filter(entry => entry.name.endsWith('/api/workspace')).length")
                    .path("result").path("result").path("value").intValue(), "presentation switches do not reload the workspace");
            assertEquals(durableDraft, storedDocument(), "mode switches cannot mutate draft or accepted state");
            cdp.evaluate("document.querySelector('#workbench-modes [data-mode=DRAFT]').click()");

            ObjectNode narrowMetrics = JSON.createObjectNode().put("width", 390).put("height", 844)
                    .put("deviceScaleFactor", 1).put("mobile", true);
            cdp.command("Emulation.setDeviceMetricsOverride", narrowMetrics);
            String narrow = cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(5));
            assertFalse(narrow.contains("Apply selected pins"));
            assertFalse(narrow.contains("Discard repair draft"));
            assertFalse(cdp.evaluateValue("Boolean(document.querySelector('.matrix-wrap'))")
                    .path("result").path("result").path("value").booleanValue());
            ObjectNode desktopMetrics = JSON.createObjectNode().put("width", 1280).put("height", 800)
                    .put("deviceScaleFactor", 1).put("mobile", false);
            cdp.command("Emulation.setDeviceMetricsOverride", desktopMetrics);
            cdp.awaitText("Bulk-protect accepted assignments", Duration.ofSeconds(5));

            cdp.evaluate("document.querySelector('[data-lesson-id=\"lesson-math-1\"]').focus()");
            cdp.pressKey(" ", "Space");
            cdp.awaitText("Protect accepted assignment dimensions", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#apply-pin').focus()");
            cdp.pressKey(" ", "Space");
            String conflict = cdp.awaitText("Resolve blocking conflicts before solving", Duration.ofSeconds(10));
            assertTrue(conflict.contains("Blocking conflict"));

            cdp.evaluate("document.querySelector('[data-lesson-id=\"lesson-math-1\"]').click()");
            cdp.evaluate("document.querySelector('#remove-pin').click()");
            cdp.awaitText("Draft is durably saved with no blocking conflict", Duration.ofSeconds(10));

            cdp.evaluate("document.querySelector('#preview-bulk').click()");
            String preview = cdp.awaitText("Bulk pin preview · no changes applied yet", Duration.ofSeconds(10));
            assertTrue(preview.contains("1 lesson in this immutable snapshot"));
            cdp.evaluate("document.querySelector('#confirm-bulk').click()");
            cdp.awaitText("Confirmed bulk snapshot · 1 lesson", Duration.ofSeconds(10));
            cdp.evaluate("document.querySelector('[data-undo-bulk]').click()");
            cdp.awaitText("Attempt-scoped pins\n0", Duration.ofSeconds(10));

            cdp.evaluate("document.querySelector('#confirm-discard-draft').click()");
            cdp.evaluate("document.querySelector('#discard-draft').click()");
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(10));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        JsonNode after = JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single());
        assertEquals("ACCEPTED_BASELINE", jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1").query(String.class).single());
        assertEquals(acceptedBefore, after.path("acceptedBaseline"));
        assertFalse(after.has("repairDraft"));
    }

    @Test
    @DisplayName("UC-7 main/7a/G1-G4/RULE-24: real browser accepts a protected teacher repair then a directly parented room repair")
    void generatesRepairProposalInRealBrowser() throws Exception {
        ObjectNode document = validAcceptedDocument();
        JsonNode acceptedBefore = document.path("acceptedBaseline").deepCopy();
        storeAccepted(document);
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(15));
            cdp.evaluate("""
                    document.querySelector('.repair-entry').open=true;
                    document.querySelector('[name=period][value="mon-1"]').checked=true;
                    document.querySelector('#start-repair-form').requestSubmit();
                    """);
            cdp.awaitText("Generate 30-second repair proposal", Duration.ofSeconds(15));
            cdp.evaluate("""
                    document.querySelector('[data-lesson-id="lesson-science-1"]').click();
                    document.querySelector('[name=lesson-dimension][value="PERIOD"]').checked=false;
                    document.querySelector('[name=lesson-dimension][value="ROOM"]').checked=true;
                    document.querySelector('#apply-pin').click();
                    """);
            String protectedDraft = cdp.awaitText("Accepted room pinned", Duration.ofSeconds(10));
            assertTrue(protectedDraft.contains("Attempt-scoped pins\n1"));
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            String solving = cdp.awaitText("Repair generation · running", Duration.ofSeconds(10));
            assertTrue(solving.contains("Accepted baseline remains current"));
            assertTrue(solving.contains("Search lessons"));
            assertFalse(solving.contains("Apply selected pins"));

            String proposal = cdp.awaitText("Repair proposal · feasible", Duration.ofSeconds(50));
            assertTrue(proposal.contains("Accepted baseline remains current"));
            assertTrue(proposal.contains("period stability, then room-only stability"));
            assertTrue(proposal.contains("Execution limit\nPT30S"));
            assertTrue(proposal.contains("Period moves"));
            assertTrue(proposal.contains("Unique changed lessons"));
            assertTrue(proposal.contains("Direct effects of your intent"));
            assertTrue(proposal.contains("Solver ripple effects"));
            assertTrue(proposal.contains("Additions\n0"));
            assertTrue(proposal.contains("Cancellations\n0"));
            assertTrue(proposal.contains("Old assignment"));
            assertTrue(proposal.contains("Proposed assignment"));
            jdbc.sql("UPDATE workspace_aggregate SET document = document #- '{proposal,review}' WHERE workspace_id=1")
                    .update();
            cdp.command("Page.navigate", object("url", page));
            String restoredLegacyProposal = cdp.awaitText("Repair proposal · feasible", Duration.ofSeconds(15));
            assertTrue(restoredLegacyProposal.contains("Unique changed lessons"));
            assertTrue(restoredLegacyProposal.contains("Old assignment"));
            assertTrue(restoredLegacyProposal.contains("Proposed assignment"));
            cdp.evaluate("document.querySelector('#confirm-repair-accept').focus()");
            cdp.pressKey(" ", "Space");
            cdp.evaluate("document.querySelector('#accept-repair').focus()");
            cdp.pressKey(" ", "Space");
            String accepted = cdp.awaitText("Accepted baseline · current timetable", Duration.ofSeconds(15));
            assertTrue(accepted.contains("Start a protected repair"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }

        JsonNode firstAccepted = JSON.readTree(jdbc.sql(
                        "SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single()).path("acceptedBaseline").deepCopy();
        assertTrue(firstAccepted.path("manifest").path("locks").valueStream()
                .anyMatch(lock -> "lesson-science-1".equals(lock.path("lessonId").stringValue())
                        && "ATTEMPT_SCOPED".equals(lock.path("roomLockOrigin").stringValue())));

        String laterTarget = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(laterTarget).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Accepted baseline · current timetable", Duration.ofSeconds(15));
            cdp.evaluate("""
                    document.querySelector('.repair-entry').open=true;
                    const type = document.querySelector('#repair-resource-type');
                    type.value='ROOM';
                    type.dispatchEvent(new Event('change', {bubbles:true}));
                    document.querySelector('#repair-resource').value='room-102';
                    document.querySelector('[name=period][value="mon-1"]').checked=true;
                    document.querySelector('#start-repair-form').requestSubmit();
                    """);
            String roomDraft = cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            assertTrue(roomDraft.contains("Accepted baseline remains current"));
            assertTrue(roomDraft.contains("Directly affected lessons\n0"));
            assertTrue(roomDraft.contains("Attempt-scoped pins\n0"));
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            String secondProposal = cdp.awaitText("Repair proposal · feasible", Duration.ofSeconds(50));
            assertTrue(secondProposal.contains("Accepted baseline remains current"));
            assertTrue(secondProposal.contains("Direct effects of your intent"));
            assertTrue(secondProposal.contains("Solver ripple effects"));
            cdp.evaluate("document.querySelector('#confirm-repair-accept').click(); document.querySelector('#accept-repair').click()");
            String secondAccepted = cdp.awaitText("Accepted baseline · current timetable", Duration.ofSeconds(15));
            assertTrue(secondAccepted.contains("Start a protected repair"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        JsonNode stored = JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single());
        assertEquals("ACCEPTED_BASELINE", jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single());
        assertFalse(acceptedBefore.equals(stored.path("acceptedBaseline")));
        assertEquals("FEASIBLE", stored.path("acceptedBaseline").path("result").path("status").stringValue());
        assertTrue(stored.path("acceptedBaseline").path("manifest").path("locks").isEmpty());
        assertFalse(stored.has("proposal"));
        assertFalse(stored.has("repairDraft"));
    }

    @Test
    @DisplayName("UC-3 G5 and RULE-25: target-scale post-load interactions record diagnostic p95 evidence")
    void measuresTargetScaleInspectionInteractionsInRealBrowser() throws Exception {
        ObjectNode document = scaleDocument();
        JsonNode definition = document.path("acceptedBaseline").path("definition");
        assertEquals(1_000, definition.path("lessons").size());
        assertEquals(100, definition.path("teachers").size());
        assertEquals(60, definition.path("cohorts").size());
        assertEquals(100, definition.path("rooms").size());
        assertEquals(60, definition.path("periods").size());
        storeAccepted(document);
        String before = storedDocument();

        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            cdp.evaluate("const matrix=document.querySelector('.matrix-wrap'); matrix.scrollTop=matrix.scrollHeight; document.querySelector('[data-lesson-id=\"lesson-999\"]').click()");
            String offViewport = cdp.awaitText("Accepted assignment", Duration.ofSeconds(5));
            assertTrue(offViewport.contains("Declared lesson 999"));
            assertTrue(cdp.evaluateValue("document.querySelector('.matrix-wrap').scrollTop > 0")
                    .path("result").path("result").path("value").booleanValue());
            cdp.evaluate("document.querySelector('[data-range=\"DAY\"]').click()");
            cdp.awaitText("Day · Thursday", Duration.ofSeconds(5));

            JsonNode search = measured(cdp, """
                    const element=document.querySelector('#lesson-search');
                    element.value=i%2===0?'Subject 1':'';
                    element.dispatchEvent(new Event('input',{bubbles:true}));
                    """);
            JsonNode filters = measured(cdp, """
                    const element=document.querySelector('#teacher-filter');
                    element.value=i%2===0?'teacher-0':'';
                    element.dispatchEvent(new Event('change',{bubbles:true}));
                    """);
            cdp.evaluate("document.querySelector('#reset-view').click()");
            cdp.evaluate("document.querySelector('#weekday').value='TUESDAY'; document.querySelector('#weekday').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.awaitText("Day · Tuesday", Duration.ofSeconds(5));
            JsonNode selections = measured(cdp, """
                    const element=document.querySelector('[data-lesson-id]');
                    element.click();
                    """);

            recordPerformance("search", search, 250.0);
            recordPerformance("filter", filters, 250.0);
            recordPerformance("selection", selections, 250.0);
            System.out.printf("UC-3 scale samples search=%s filter=%s selection=%s; solver time excluded%n",
                    search, filters, selections);
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument());
    }

    @Test
    @DisplayName("UC-4 G6 and RULE-25: target-scale persisted pin feedback records diagnostic p95 evidence")
    void measuresTargetScalePinFeedbackInRealBrowser() throws Exception {
        storeAccepted(scaleDocument());
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            cdp.evaluate("""
                    document.querySelector('.repair-entry').open=true;
                    document.querySelector('#repair-resource-type').value='TEACHER';
                    document.querySelector('#repair-resource-type').dispatchEvent(new Event('change',{bubbles:true}));
                    document.querySelector('#repair-resource').value='teacher-99';
                    document.querySelector('[name=period][value="period-59"]').checked=true;
                    document.querySelector('#start-repair-form').requestSubmit();
                    """);
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            JsonNode samples = cdp.evaluateValue("""
                    (async () => {
                      const samples=[];
                      for(let i=0;i<20;i++) {
                        document.querySelector('[data-lesson-id="lesson-0"]').click();
                        await new Promise(resolve => requestAnimationFrame(resolve));
                        const applying=i%2===0;
                        const started=performance.now();
                        document.querySelector(applying?'#apply-pin':'#remove-pin').click();
                        const expected=applying?'Accepted period pinned':'Unpinned · kernel stability ordering applies';
                        while(!document.body.innerText.includes(expected)) await new Promise(resolve => setTimeout(resolve,2));
                        samples.push(Number((performance.now()-started).toFixed(3)));
                      }
                      return samples;
                    })()
                    """).path("result").path("result").path("value");
            recordPerformance("pin feedback", samples, 250.0);
            System.out.printf("UC-4 scale pin-feedback samples=%s; solver time excluded%n", samples);
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        JsonNode stored = JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single());
        assertEquals(1_000, stored.path("acceptedBaseline").path("result").path("timetable").path("assignments").size());
        assertEquals("REPAIR_DRAFT", jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1").query(String.class).single());
    }

    @Test
    @DisplayName("UC-6 G5 and RULE-25: target-scale proposal impact review records diagnostic timing evidence")
    void measuresTargetScaleProposalReviewOpeningInRealBrowser() throws Exception {
        ObjectNode document = scaleProposalDocument();
        jdbc.sql("""
                UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=10,
                active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                """).param("document", JSON.writeValueAsString(document)).update();
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + port + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                        + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                                .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString()).body();
        try (Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue())) {
            cdp.command("Page.enable", JSON.createObjectNode());
            cdp.command("Runtime.enable", JSON.createObjectNode());
            cdp.command("Page.navigate", object("url", page));
            String rendered;
            try {
                rendered = cdp.awaitText("Unique changed lessons", Duration.ofSeconds(20));
            } catch (AssertionError failure) {
                throw new AssertionError(failure.getMessage() + "\nConsole: " + cdp.errors()
                        + "\nRendered: " + cdp.evaluateValue("document.body.innerText"), failure);
            }
            assertTrue(rendered.contains("100"));
            assertTrue(rendered.contains("Direct effects of your intent: 50"));
            assertTrue(rendered.contains("Solver ripple effects: 50"));
            assertTrue(rendered.contains("By class"));
            cdp.evaluate("document.querySelector('#show-unchanged').click()");
            assertTrue(cdp.awaitText("Accepted and unchanged · not included in change totals", Duration.ofSeconds(5))
                    .contains("Accepted assignment"));
            cdp.evaluate("document.querySelector('[data-review-context]').click()");
            cdp.awaitText("Review context", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#return-review').click()");
            cdp.awaitText("Proposal impact review", Duration.ofSeconds(5));
            double openingMs = cdp.evaluateValue("window.__workspaceProposalReviewMs")
                    .path("result").path("result").path("value").doubleValue();
            assertTrue(Double.isFinite(openingMs) && openingMs >= 0.0,
                    "proposal review timing must be a finite non-negative value");
            System.out.printf("UC-6 scale proposal-review opening=%.3f ms; solver time excluded%n", openingMs);
            System.out.printf("proposal review reference=1000.0 ms; diagnostic only; exceeded=%s%n",
                    openingMs >= 1_000.0);
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    private static JsonNode measured(Cdp cdp, String operation) throws Exception {
        return cdp.evaluateValue("""
                (async () => {
                  const samples=[];
                  for(let i=0;i<20;i++) {
                    await new Promise(resolve => requestAnimationFrame(resolve));
                    const started=performance.now();
                    %s
                    samples.push(Number((performance.now()-started).toFixed(3)));
                  }
                  return samples;
                })()
                """.formatted(operation)).path("result").path("result").path("value");
    }

    private static void recordPerformance(String interaction, JsonNode samples, double referenceMs) {
        assertEquals(20, samples.size(), interaction + " must retain all raw timing samples");
        double[] ordered = new double[samples.size()];
        for (int index = 0; index < samples.size(); index++) ordered[index] = samples.get(index).doubleValue();
        java.util.Arrays.sort(ordered);
        double p95 = ordered[(int) Math.ceil(ordered.length * 0.95) - 1];
        assertTrue(Double.isFinite(p95) && p95 >= 0.0,
                interaction + " p95 must be a finite non-negative value");
        System.out.printf("%s p95=%.3f ms; reference=%.1f ms; diagnostic only; exceeded=%s%n",
                interaction, p95, referenceMs, p95 >= referenceMs);
    }

    private ObjectNode acceptedDocument(boolean empty) throws Exception {
        ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile());
        definition.withArray("periods").addObject().put("id", "tue-1").put("displayName", "Tuesday 1")
                .put("weekday", "TUESDAY").put("order", 1);
        ((ObjectNode) definition.path("teachers").get(0)).putArray("availablePeriodIds").add("mon-1").add("tue-1");
        if (empty) definition.putArray("lessons");
        ObjectNode result = JSON.createObjectNode();
        result.put("status", "FEASIBLE");
        var assignments = result.putObject("timetable").putArray("assignments");
        if (!empty) {
            assignments.add(assignment("lesson-math-1", "math", "cohort-7a", "teacher-alex", "mon-1", "room-102"));
            assignments.add(assignment("lesson-science-1", "science", "cohort-7a", "teacher-alex", "mon-2", "room-101"));
        }
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "opaque-school-id").put("displayName", "Demo School");
        document.put("definitionRevision", "sha256:definition");
        document.put("timetableRevision", "sha256:timetable");
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition);
        baseline.set("result", result);
        baseline.putObject("manifest").put("manifestVersion", 1);
        return document;
    }

    private ObjectNode validAcceptedDocument() throws Exception {
        JsonNode definition = JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile());
        JsonNode result = JSON.readTree(Path.of("src/test/resources/uc5-accepted-result.json").toFile());
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "demo-school").put("displayName", "Demo School");
        document.put("definitionRevision", result.path("inputRevision").stringValue());
        document.put("timetableRevision", result.path("timetableRevision").stringValue());
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition);
        baseline.set("result", result);
        ObjectNode manifest = baseline.putObject("manifest");
        manifest.put("manifestVersion", 1).put("definitionSchemaVersion", 1).put("resultSchemaVersion", 1)
                .put("catalogVersion", 1).put("schoolId", "demo-school")
                .put("inputRevision", result.path("inputRevision").stringValue())
                .put("timetableRevision", result.path("timetableRevision").stringValue()).putArray("locks");
        return document;
    }

    private ObjectNode scaleDocument() {
        ObjectNode definition = JSON.createObjectNode();
        definition.put("schemaVersion", 1).put("catalogVersion", 1).put("schoolId", "opaque-scale-school")
                .put("displayName", "Scale School");
        var subjects = definition.putArray("subjects");
        for (int i = 0; i < 20; i++) subjects.addObject().put("id", "subject-" + i).put("displayName", "Subject " + i);
        var teachers = definition.putArray("teachers");
        for (int i = 0; i < 100; i++) teachers.addObject().put("id", "teacher-" + i).put("displayName", "Teacher " + i)
                .putArray("qualifiedSubjectIds").add("subject-" + (i % 20));
        var cohorts = definition.putArray("cohorts");
        for (int i = 0; i < 60; i++) cohorts.addObject().put("id", "cohort-" + i).put("displayName", "Class " + i).put("size", 25);
        var rooms = definition.putArray("rooms");
        for (int i = 0; i < 100; i++) rooms.addObject().put("id", "room-" + i).put("displayName", "Room " + i).put("capacity", 30).putArray("capabilityIds");
        String[] weekdays = { "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" };
        var periods = definition.putArray("periods");
        for (int i = 0; i < 60; i++) periods.addObject().put("id", "period-" + i)
                .put("displayName", "Declared period " + i).put("weekday", weekdays[i / 12]).put("order", i % 12 + 1);
        var lessons = definition.putArray("lessons");
        ObjectNode result = JSON.createObjectNode().put("status", "FEASIBLE");
        var assignments = result.putObject("timetable").putArray("assignments");
        for (int i = 0; i < 1_000; i++) {
            int group = i / 60;
            String subject = "subject-" + (group % 20);
            String cohort = "cohort-" + group;
            String teacher = "teacher-" + group;
            String room = "room-" + group;
            String lesson = "lesson-" + i;
            lessons.addObject().put("id", lesson).put("displayName", "Declared lesson " + i).put("subjectId", subject)
                    .put("cohortId", cohort).put("teacherId", teacher);
            assignments.add(assignment(lesson, subject, cohort, teacher, "period-" + (i % 60), room));
        }
        ObjectNode document = JSON.createObjectNode();
        document.putObject("school").put("id", "opaque-scale-school").put("displayName", "Scale School");
        document.put("definitionRevision", "sha256:scale-definition").put("timetableRevision", "sha256:scale-timetable");
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", definition); baseline.set("result", result); baseline.putObject("manifest").put("manifestVersion", 1);
        return document;
    }

    private ObjectNode investigationScaleDocument() throws Exception {
        ObjectNode document = scaleDocument();
        JsonNode baseline = document.path("acceptedBaseline");
        JsonNode definition = baseline.path("definition");
        ((ObjectNode) definition.path("subjects").get(0)).put("displayName", "Subject Zero with a deliberately long authoritative display name for timetable tiles");
        ((ObjectNode) definition.path("teachers").get(16)).put("displayName", "Teacher Sixteen with a deliberately long authoritative display name for timetable tiles");
        ((ObjectNode) definition.path("cohorts").get(16)).put("displayName", "Class Sixteen with a deliberately long authoritative display name for timetable tiles");
        ((ObjectNode) definition.path("rooms").get(16)).put("displayName", "Room Sixteen with a deliberately long authoritative display name for timetable tiles");
        ObjectNode selectedTeacher = (ObjectNode) definition.path("teachers").get(16);
        selectedTeacher.withArray("qualifiedSubjectIds").add("subject-0");
        var available = selectedTeacher.putArray("availablePeriodIds");
        for (int i = 0; i <= 40; i++) available.add("period-" + i);
        ((ObjectNode) definition.path("lessons").get(960)).put("subjectId", "subject-0");
        ((ObjectNode) baseline.path("result").path("timetable").path("assignments").get(960)).put("subjectId", "subject-0");
        String revision = verifier.verify(new ImportDocuments(definition, null, null,
                ImportDocuments.ImportMode.INITIAL_DEFINITION)).definitionRevision();
        ObjectNode result = (ObjectNode) JSON.readTree(Path.of("src/test/resources/uc5-accepted-result.json").toFile());
        result.put("schoolId", "opaque-scale-school").put("inputRevision", revision).put("correlationId", "uc2-scale-generated");
        result.set("timetable", baseline.path("result").path("timetable").deepCopy());
        var orderedAssignments = new ArrayList<JsonNode>();
        result.path("timetable").path("assignments").forEach(orderedAssignments::add);
        orderedAssignments.sort(Comparator.comparing(item -> item.path("lessonId").stringValue()));
        ObjectNode scope = JSON.createObjectNode().put("schemaVersion", 1).put("schoolId", "opaque-scale-school")
                .put("inputRevision", revision);
        var ordered = scope.putArray("assignments");
        orderedAssignments.forEach(ordered::add);
        String timetableRevision = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(CanonicalJson.bytes(scope)));
        result.put("timetableRevision", timetableRevision);
        ((ObjectNode) document).put("definitionRevision", revision).put("timetableRevision", timetableRevision);
        ((ObjectNode) baseline).set("result", result);
        ObjectNode manifest = (ObjectNode) baseline.path("manifest");
        manifest.put("definitionSchemaVersion", 1).put("resultSchemaVersion", 1).put("catalogVersion", 1)
                .put("schoolId", "opaque-scale-school").put("inputRevision", revision)
                .put("timetableRevision", timetableRevision).putArray("locks");
        return document;
    }

    private ObjectNode scaleProposalDocument() {
        ObjectNode document = scaleDocument();
        ObjectNode draft = document.putObject("repairDraft");
        ObjectNode intent = draft.putObject("intent");
        intent.putArray("changes"); intent.putArray("pins"); intent.putArray("bulkActions");
        var direct = draft.putArray("directEffectLessonIds");
        for (int i = 0; i < 50; i++) direct.add("lesson-" + i);
        draft.put("intentRevision", "sha256:scale-intent").put("readyToSolve", true).putArray("conflicts");
        JsonNode baseline = document.path("acceptedBaseline");
        ObjectNode definition = (ObjectNode) baseline.path("definition").deepCopy();
        definition.put("basedOnRevision", "sha256:scale-definition");
        ObjectNode result = (ObjectNode) baseline.path("result").deepCopy();
        var report = result.putObject("changeReport");
        report.putArray("additions"); report.putArray("cancellations"); report.putArray("teacherChanges");
        report.putArray("forcedMoves"); var periodMoves = report.putArray("periodMoves"); report.putArray("roomOnlyMoves");
        for (int i = 0; i < 100; i++) {
            ObjectNode assignment = (ObjectNode) result.path("timetable").path("assignments").get(i);
            String oldPeriod = assignment.path("periodId").stringValue();
            String newPeriod = "period-" + ((i + 1) % 60);
            String room = assignment.path("roomId").stringValue();
            assignment.put("periodId", newPeriod);
            periodMoves.addObject().put("lessonId", "lesson-" + i).put("oldPeriodId", oldPeriod)
                    .put("newPeriodId", newPeriod).put("oldRoomId", room).put("newRoomId", room);
        }
        ObjectNode proposal = document.putObject("proposal");
        proposal.put("kind", "REPAIR").put("sourceWorkspaceVersion", 8)
                .put("acceptedTimetableRevision", "sha256:scale-timetable")
                .put("successorDefinitionRevision", "sha256:scale-successor")
                .put("intentRevision", "sha256:scale-intent").put("proposedTimetableRevision", "sha256:scale-proposed")
                .put("runId", java.util.UUID.randomUUID().toString()).put("limit", "PT30S")
                .put("terminationReason", "TIME_LIMIT").put("elapsedTimeMs", 30_000);
        proposal.set("definition", definition); proposal.set("result", result);
        ObjectNode counts = proposal.putObject("changeCounts");
        ProposalReviewService.CATEGORIES.forEach(category -> counts.put(category, report.path(category).size()));
        proposal.set("review", reviews.create(baseline, draft, definition, result));
        return document;
    }

    private static ObjectNode assignment(String lesson, String subject, String cohort, String teacher, String period, String room) {
        return JSON.createObjectNode().put("lessonId", lesson).put("subjectId", subject).put("cohortId", cohort)
                .put("teacherId", teacher).put("periodId", period).put("roomId", room);
    }

    private void storeAccepted(ObjectNode document) throws Exception {
        jdbc.sql("""
                        UPDATE workspace_aggregate SET lifecycle_state='ACCEPTED_BASELINE', version=7,
                        active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                        """).param("document", JSON.writeValueAsString(document)).update();
    }

    private String storedDocument() {
        return jdbc.sql("SELECT version || ':' || document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single();
    }

    private int startBrowser() throws Exception {
        Path chrome = chromeBinary();
        browser = new ProcessBuilder(
                chrome.toString(),
                "--headless",
                "--disable-gpu",
                "--no-first-run",
                "--no-default-browser-check",
                "--remote-debugging-port=0",
                "--user-data-dir=" + browserProfile,
                "about:blank")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        Path activePort = browserProfile.resolve("DevToolsActivePort");
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (!Files.isRegularFile(activePort) && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(Files.isRegularFile(activePort), () -> "Chrome did not expose DevTools: " + browser.info());
        return Integer.parseInt(Files.readAllLines(activePort).get(0));
    }

    private static Path chromeBinary() {
        String configured = System.getenv("CHROME_BINARY");
        var candidates = configured == null
                ? java.util.List.of(
                        Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"),
                        Path.of("/usr/bin/google-chrome"),
                        Path.of("/usr/bin/chromium"),
                        Path.of("/usr/bin/chromium-browser"))
                : java.util.List.of(Path.of(configured));
        return candidates.stream().filter(Files::isExecutable).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "A real Chrome/Chromium binary is required; set CHROME_BINARY"));
    }

    private static ObjectNode object(String name, String value) {
        return JSON.createObjectNode().put(name, value);
    }

    private static final class Cdp implements WebSocket.Listener, AutoCloseable {
        private final AtomicLong sequence = new AtomicLong();
        private final Map<Long, CompletableFuture<JsonNode>> replies = new ConcurrentHashMap<>();
        private final CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();
        private final StringBuilder fragments = new StringBuilder();
        private final WebSocket socket;

        Cdp(String url) {
            socket = HttpClient.newHttpClient().newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI.create(url), this)
                    .join();
        }

        JsonNode command(String method, ObjectNode params) throws Exception {
            long id = sequence.incrementAndGet();
            CompletableFuture<JsonNode> reply = new CompletableFuture<>();
            replies.put(id, reply);
            ObjectNode request = JSON.createObjectNode();
            request.put("id", id);
            request.put("method", method);
            request.set("params", params);
            socket.sendText(JSON.writeValueAsString(request), true).join();
            JsonNode response = reply.get(15, TimeUnit.SECONDS);
            if (response.has("error")) {
                throw new IllegalStateException(response.path("error").toString());
            }
            return response;
        }

        void evaluate(String expression) throws Exception {
            ObjectNode params = object("expression", expression);
            params.put("awaitPromise", true);
            params.put("returnByValue", true);
            command("Runtime.evaluate", params);
        }

        JsonNode evaluateValue(String expression) throws Exception {
            ObjectNode params = object("expression", expression);
            params.put("awaitPromise", true);
            params.put("returnByValue", true);
            return command("Runtime.evaluate", params);
        }

        void pressKey(String key, String code) throws Exception {
            int virtualKey = "Enter".equals(key) ? 13 : 32;
            ObjectNode down = JSON.createObjectNode().put("type", "keyDown").put("key", key).put("code", code)
                    .put("windowsVirtualKeyCode", virtualKey).put("nativeVirtualKeyCode", virtualKey);
            command("Input.dispatchKeyEvent", down);
            ObjectNode up = JSON.createObjectNode().put("type", "keyUp").put("key", key).put("code", code)
                    .put("windowsVirtualKeyCode", virtualKey).put("nativeVirtualKeyCode", virtualKey);
            command("Input.dispatchKeyEvent", up);
        }

        String awaitText(String expected, Duration timeout) throws Exception {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                ObjectNode params = object("expression", "document.body?.innerText || ''");
                params.put("returnByValue", true);
                String text = command("Runtime.evaluate", params)
                        .path("result").path("result").path("value").stringValue();
                if (text != null && text.contains(expected)) {
                    return text;
                }
                Thread.sleep(100);
            }
            throw new AssertionError("Browser did not render: " + expected + "\nRendered: " + text());
        }

        String text() throws Exception {
            ObjectNode params = object("expression", "document.body?.innerText || ''");
            params.put("returnByValue", true);
            return command("Runtime.evaluate", params)
                    .path("result").path("result").path("value").stringValue();
        }

        CopyOnWriteArrayList<String> errors() {
            return errors;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onText(
                WebSocket webSocket, CharSequence data, boolean last) {
            fragments.append(data);
            if (last) {
                try {
                    JsonNode message = JSON.readTree(fragments.toString());
                    if (message.has("id")) {
                        CompletableFuture<JsonNode> reply = replies.remove(message.path("id").longValue());
                        if (reply != null) reply.complete(message);
                    } else if ("Runtime.exceptionThrown".equals(message.path("method").stringValue())) {
                        errors.add(message.toString());
                    }
                } catch (Exception exception) {
                    errors.add(exception.getMessage());
                } finally {
                    fragments.setLength(0);
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            errors.add(error.toString());
        }

        @Override
        public void close() {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }
}
