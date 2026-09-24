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
import java.util.Base64;
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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = {WorkspaceApplication.class, WorkspaceRepairPlanningIT.ProcessConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

    @Autowired
    WorkspaceRecovery recovery;

    @Autowired
    WorkspaceRepairPlanningIT.SwitchingRepairProcessLauncher processes;

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
        processes.reset();
    }

    @AfterEach
    void stopBrowser() throws Exception {
        processes.reset();
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
            assertTrue(accepted.contains("Demo School"));
            assertTrue(accepted.contains("Current · accepted"));
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
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1600)
                    .put("height", 900).put("deviceScaleFactor", 1).put("mobile", false));
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
    @DisplayName("Workbench layout UC-1: compact complete Current, honest Filters, inspector and read-only breakpoints")
    void inspectsCompactWideCurrentWorkbenchInRealBrowser() throws Exception {
        ObjectNode document = investigationScaleDocument();
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
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1600)
                    .put("height", 900).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.command("Page.navigate", object("url", page));
            assertTrue(cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20)).contains("Scale School"));
            Set<String> represented = renderedLessonIds(cdp);
            assertEquals(1_000, represented.size(), "UC-1 main 2: every verified accepted lesson is represented");
            for (int index = 0; index < 1_000; index++) {
                assertTrue(represented.contains("lesson-" + index), "missing lesson-" + index);
            }
            JsonNode wide = cdp.evaluateValue("""
                    (() => {
                      const shell = document.querySelector('.workspace-card');
                      const wrap = document.querySelector('.week-wrap');
                      const slot = document.querySelector('.week-slot:has([data-lesson-id=lesson-1])');
                      const inspector = document.querySelector('#workbench-inspector');
                      return {shell:shell.getBoundingClientRect().width, wrap:wrap.clientWidth,
                        content:wrap.scrollWidth, slot:slot.getBoundingClientRect().height,
                        inspector:inspector.getBoundingClientRect().width,
                        days:document.querySelectorAll('.week-matrix thead th').length - 1,
                        empty:document.querySelectorAll('.week-slot .empty-cell').length,
                        filters:document.querySelector('#filters').open,
                        utilities:document.querySelector('#utilities').open,
                        task:document.querySelector('#workbench-task-area').hidden,
                        toolbarOutside:!wrap.contains(document.querySelector('.inspection-toolbar'))};
                    })()
                    """).path("result").path("result").path("value");
            assertTrue(wide.path("shell").doubleValue() >= 1520, "UC-1 G1: shell occupies at least 95% of 1600 px");
            assertTrue(wide.path("content").doubleValue() <= wide.path("wrap").doubleValue() + 1,
                    "UC-1 G1: five weekdays fit with inspector open");
            assertTrue(wide.path("slot").doubleValue() <= 36, "UC-1 G2: ordinary occupied Week slot is compact");
            assertEquals(5, wide.path("days").intValue());
            assertTrue(browserTrue(cdp, "(() => { const slots=[...document.querySelectorAll('.week-matrix tbody tr:first-child td:first-of-type .week-period')]; return slots.length === 12 && slots[0].textContent.startsWith('1 ·') && slots[11].textContent.startsWith('12 ·') && slots[0].title === 'Declared period 0'; })()"),
                    "UC-1 G2/G3: Week keeps authoritative period names and visible declared order");
            assertTrue(wide.path("empty").intValue() > 0, "UC-1 ext 2a: declared empty positions are retained");
            assertTrue(wide.path("inspector").doubleValue() >= 200);
            assertFalse(wide.path("filters").booleanValue());
            assertFalse(wide.path("utilities").booleanValue());
            assertTrue(wide.path("task").booleanValue(), "Current task area starts closed");
            assertTrue(wide.path("toolbarOutside").booleanValue());
            cdp.evaluate("""
                    window.__inspectionMutations = [];
                    const originalFetch = window.fetch.bind(window);
                    window.fetch = (input, options = {}) => {
                      const method = (options.method || input?.method || 'GET').toUpperCase();
                      if (method !== 'GET' && method !== 'HEAD') window.__inspectionMutations.push(method);
                      return originalFetch(input, options);
                    };
                    """);
            captureWorkbenchScreenshot(cdp, "uc1-current-1600.png");
            cdp.evaluate("document.querySelector('[data-range=DAY]').click()");
            captureWorkbenchScreenshot(cdp, "uc1-current-day-1600.png");
            JsonNode ordinaryDay = cdp.evaluateValue("document.querySelector('[data-lesson-id=lesson-60]').closest('td').getBoundingClientRect().height")
                    .path("result").path("result").path("value");
            assertTrue(ordinaryDay.doubleValue() <= 60,
                    "UC-1 G2: ordinary occupied Day cell is compact: " + ordinaryDay);
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click()");

            cdp.evaluate("document.querySelector('#lesson-search').value='Sixteen'; document.querySelector('#lesson-search').dispatchEvent(new Event('input',{bubbles:true}))");
            cdp.evaluate("document.querySelector('#subject-investigation').value='subject-0'; document.querySelector('#subject-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.evaluate("document.querySelector('#teacher-investigation').value='teacher-16'; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "document.querySelector('#filter-title').textContent === 'Complete school population' && document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 1000' && document.querySelector('.teacher-ribbon').textContent.includes('Unavailable')"),
                    "UC-1 main 3-4: search and highlights do not narrow and availability is visible");
            cdp.evaluate("document.querySelector('#filters').open=true; document.querySelector('#cohort-filter').value='cohort-16'; document.querySelector('#cohort-filter').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 40' && document.querySelector('#filter-title').textContent === 'Filtered whole-school matrix'"),
                    "UC-1 main 4: explicit class narrowing intersects with the highlighted subject and teacher only when requested");
            cdp.evaluate("document.querySelector('#room-filter').value='room-99'; document.querySelector('#room-filter').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#filters').open=false");
            assertTrue(browserTrue(cdp, "document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 0' && !document.querySelector('#no-matches').hidden && document.querySelectorAll('.week-matrix tbody tr').length === 60 && !document.querySelector('#clear-filters').hidden && document.querySelector('#active-criteria').textContent.includes('Room: Room 99')"),
                    "UC-1 ext 4a: zero matches retain declared time structure and visible closed-filter summary");
            cdp.evaluate("document.querySelector('#clear-filters').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#represented-lesson-count').textContent === 'Represented lessons: 1000' && document.querySelector('#lesson-search').value === 'Sixteen' && document.querySelector('#subject-investigation').value === 'subject-0' && document.querySelector('#teacher-investigation').value === 'teacher-16'"),
                    "UC-1 main 4: clearing explicit narrowing retains search and highlights");

            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-960]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title').textContent === 'Declared lesson 960' && document.querySelector('#workbench-inspector').textContent.includes('Class Sixteen with a deliberately long authoritative display name') && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-label').includes('Monday · Declared period 0 · lesson-960')"),
                    "UC-1 main 6: inspector and accessible name retain authoritative full details and identity");
            cdp.evaluate("document.querySelector('[data-open-focus=teacherId]').click()");
            cdp.awaitText("Teacher schedule · Teacher Sixteen", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-search').value === 'Sixteen'"),
                    "UC-1 main 5-6: focused return restores selection and investigation");
            cdp.evaluate("document.querySelector('#toggle-inspector').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector').hidden && document.querySelector('#inspector-summary').textContent.includes('Declared lesson 960')"));
            cdp.evaluate("document.querySelector('#reopen-inspector').click(); document.querySelector('#open-repair-setup').click()");
            assertEquals(before, storedDocument(), "UC-1 ext 5a: merely opening repair setup writes nothing");
            cdp.evaluate("document.querySelector('#close-repair-setup').click()");
            assertEquals(before, storedDocument(), "UC-1 ext 5a: closing unstaged setup writes nothing");
            assertTrue(browserTrue(cdp, "window.__inspectionMutations.length === 0"),
                    "UC-1 G4/RULE-14: inspection and unstaged repair setup issue no mutating request");

            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.evaluate("document.querySelector('#open-repair-setup').click()");
            JsonNode medium = cdp.evaluateValue("""
                    (() => { const wrap=document.querySelector('.matrix-wrap');
                      const inspector=document.querySelector('#workbench-inspector');
                      const task=document.querySelector('#workbench-task-area');
                      return {page:document.documentElement.scrollWidth, viewport:innerWidth,
                        taskHeight:task.getBoundingClientRect().height, taskTop:task.getBoundingClientRect().top,
                        matrixBottom:wrap.getBoundingClientRect().bottom,
                        inspectorLeft:inspector.getBoundingClientRect().left,
                        canvasRight:document.querySelector('.canvas-region').getBoundingClientRect().right,
                        row:document.querySelector('.week-matrix tbody tr').getBoundingClientRect().height,
                        visible:wrap.clientHeight}; })()
                    """).path("result").path("result").path("value");
            captureWorkbenchScreenshot(cdp, "uc1-current-1280.png");
            assertTrue(medium.path("page").doubleValue() <= medium.path("viewport").doubleValue() + 1,
                    "UC-1 G1: 1280 page has no horizontal scrolling");
            assertTrue(medium.path("inspectorLeft").doubleValue() >= medium.path("canvasRight").doubleValue(),
                    "UC-1 G1: inspector stays beside the canvas at 1280");
            assertTrue(medium.path("taskHeight").doubleValue() <= 280,
                    "UC-1 G1: task area occupies no more than 35% of 800 px");
            assertTrue(medium.path("taskTop").doubleValue() >= medium.path("matrixBottom").doubleValue(),
                    "UC-1 G1: task area does not overlay the canvas");
            assertTrue(medium.path("visible").doubleValue() >= medium.path("row").doubleValue(),
                    "UC-1 G1: an entire class row remains visible with setup open: " + medium);

            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1279)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.command("Page.reload", JSON.createObjectNode());
            awaitBrowserCondition(cdp, "document.querySelector('.week-wrap') !== null");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector').hidden && document.querySelector('#inspector-summary').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && !document.querySelector('#filters').open && !document.querySelector('#utilities').open && document.querySelector('#workbench-task-area').hidden && !document.querySelector('#lesson-panel-title')"),
                    "UC-1 ext 3a/G1: intermediate reload resets presentation state and stacks collapsed inspector below canvas");
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-960]').click()");
            assertTrue(browserTrue(cdp, "!document.querySelector('#workbench-inspector').hidden && document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom"),
                    "UC-1 main 6: selection opens below-canvas inspector at 1279");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 701)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", false));
            assertTrue(browserTrue(cdp, "!document.querySelector('#workbench-inspector').hidden && document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom"),
                    "UC-1 G1: the 701 px inspector is below the canvas");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 700)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", false));
            assertTrue(cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(5)).contains("Accepted baseline"));
            assertFalse(browserTrue(cdp, "document.querySelector('#open-repair-setup, #start-repair-form, #workbench-modes, .matrix-wrap')"),
                    "UC-1 ext 1b: 700 px is read-only and does not claim the desktop canvas");
            assertEquals(1, cdp.evaluateValue("document.querySelectorAll('.narrow-banner').length")
                    .path("result").path("result").path("value").intValue(),
                    "UC-1 ext 1b: narrow Current has one clear read-only notice");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 390)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", true));
            assertTrue(cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(5)).contains("Accepted baseline"));
            captureWorkbenchScreenshot(cdp, "uc1-current-390.png");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "UC-1 G4/minimal: every presentation action leaves the exact durable document unchanged");
    }

    private static void captureWorkbenchScreenshot(Cdp cdp, String filename) throws Exception {
        Path directory = Path.of("target/workbench-layout");
        Files.createDirectories(directory);
        String data = cdp.command("Page.captureScreenshot", JSON.createObjectNode().put("format", "png"))
                .path("result").path("data").stringValue();
        Files.write(directory.resolve(filename), Base64.getDecoder().decode(data));
    }

    @Test
    @DisplayName("Workbench layout UC-2 main/1a/1b/2a/3a/4a/4b/5a/6a/6b/G1-G7: normative browser prepares and discards a protected wide Draft")
    void preparesWideProtectedDraftAtNormativeScaleInRealBrowser() throws Exception {
        ObjectNode document = investigationScaleDocument();
        storeAccepted(document);
        JsonNode baseline = document.path("acceptedBaseline").deepCopy();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1600)
                    .put("height", 900).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.command("Page.reload", JSON.createObjectNode());
            awaitBrowserCondition(cdp, "document.querySelector('[data-lesson-id=lesson-960]') !== null");
            assertEquals(1_000, renderedLessonIds(cdp).size(), "UC-2 Requires UC-1's complete accepted canvas");
            String acceptedBefore = storedDocument();
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-960]').click(); document.querySelector('#open-repair-setup').click()");
            assertTrue(browserTrue(cdp, "!document.querySelector('#workbench-task-area').hidden && document.querySelector('#repair-resource').value === 'teacher-16' && !document.querySelector('#workbench-inspector #start-repair-form')"));
            assertEquals(acceptedBefore, storedDocument(), "UC-2 main 1: opening setup is presentation-only");
            cdp.evaluate("document.querySelector('#close-repair-setup').click()");
            assertEquals(acceptedBefore, storedDocument(), "UC-2 ext 1a: closing unstaged setup saves nothing");
            cdp.evaluate("document.querySelector('#open-repair-setup').click(); document.querySelector('#start-repair-form').requestSubmit()");
            assertTrue(cdp.awaitText("Select one or more weekly periods", Duration.ofSeconds(10)).contains("Select one or more weekly periods"));
            assertEquals(acceptedBefore, storedDocument(), "UC-2 ext 2a: invalid period selection creates no Draft");
            cdp.evaluate("document.querySelector('[name=period][value=period-0]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Draft is durably saved with no blocking conflict", Duration.ofSeconds(15));
            JsonNode initial = assertDraftUnchangedBaseline(baseline);
            assertEquals("TEACHER", initial.path("intent").path("changes").get(0).path("resourceType").stringValue());
            assertEquals("teacher-16", initial.path("intent").path("changes").get(0).path("resourceId").stringValue());
            assertEquals(JSON.readTree("[\"period-0\"]"), initial.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
            assertEquals(Set.of("lesson-960"), jsonStrings(initial.path("directEffectLessonIds")));
            assertEquals(1_000, renderedLessonIds(cdp).size(), "Draft decorates, rather than replaces, accepted assignments");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-lesson-id=lesson-960]').textContent.includes('Directly affected') && document.querySelector('#workbench-task-area #stage-repair-form') && document.querySelector('#workbench-task-area #apply-pin') && document.querySelector('#workbench-task-area #preview-bulk') && document.querySelector('#workbench-task-area #solve-draft') && !document.querySelector('#workbench-inspector #stage-repair-form, #workbench-inspector #apply-pin, #workbench-inspector #preview-bulk')"),
                    "UC-2 main 3/G2: accepted canvas cues and all Draft work belong to the wide task area");
            cdp.evaluate("window.scrollTo(0, 0)");
            JsonNode wide = draftTaskGeometry(cdp);
            assertDraftTaskGeometry(wide, 1600, 900);
            assertTrue(draftDecisionReachable(cdp), "UC-2 G2: 1600px solve/discard controls are reachable in the task scrollport");
            captureWorkbenchScreenshot(cdp, "uc2-draft-1600.png");

            cdp.evaluate("document.querySelector('#apply-pin').click()");
            cdp.awaitText("Resolve blocking conflicts before solving", Duration.ofSeconds(10));
            JsonNode conflicted = assertDraftUnchangedBaseline(baseline);
            assertEquals("lesson-960", conflicted.path("conflicts").get(0).path("lessonId").stringValue());
            assertEquals("PIN_CONTRADICTS_UNAVAILABILITY", conflicted.path("conflicts").get(0).path("code").stringValue());
            assertFalse(conflicted.path("readyToSolve").booleanValue());
            assertTrue(browserTrue(cdp, "document.querySelector('#solve-draft').disabled && document.querySelector('[data-lesson-id=lesson-960]').textContent.includes('Blocking conflict')"));
            String beforeNavigation = storedDocument();
            cdp.evaluate("document.querySelector('[data-range=DAY]').click(); document.querySelector('#weekday').value='TUESDAY'; document.querySelector('#weekday').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#room-filter').value='room-99'; document.querySelector('#room-filter').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#lesson-search').value='Sixteen'; document.querySelector('#lesson-search').dispatchEvent(new Event('input',{bubbles:true})); document.querySelector('#teacher-investigation').value='teacher-16'; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.evaluate("document.querySelector('[data-draft-conflict=lesson-960]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#weekday').value === 'MONDAY' && document.querySelector('#room-filter').value === '' && document.querySelector('#lesson-search').value === 'Sixteen' && document.querySelector('#teacher-investigation').value === 'teacher-16' && document.querySelector('[data-lesson-id=lesson-960]').getAttribute('aria-pressed') === 'true' && document.querySelector('#inspection-notice').textContent.includes('view was adjusted')"),
                    "UC-2 ext 4b: navigation changes only excluding context and keeps investigation");
            assertEquals(beforeNavigation, storedDocument(), "navigation cannot alter the exact durable Draft");
            cdp.evaluate("document.querySelector('#remove-pin').click()");
            cdp.awaitText("Draft is durably saved with no blocking conflict", Duration.ofSeconds(10));
            assertTrue(assertDraftUnchangedBaseline(baseline).path("conflicts").isEmpty());

            cdp.evaluate("document.querySelector('[data-range=WEEK]').click(); document.querySelector('[data-lesson-id=lesson-500]').click(); document.querySelector('[name=lesson-dimension][value=PERIOD]').checked=false; document.querySelector('[name=lesson-dimension][value=ROOM]').checked=true; document.querySelector('#apply-pin').click()");
            cdp.awaitText("Accepted room pinned", Duration.ofSeconds(10));
            JsonNode individual = assertDraftUnchangedBaseline(baseline);
            assertEquals("lesson-500", individual.path("intent").path("pins").get(0).path("lessonId").stringValue());
            assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), individual.path("intent").path("pins").get(0).path("roomSources"));
            Set<String> classEightLessons = new HashSet<>();
            for (int number = 480; number < 540; number++) classEightLessons.add("lesson-" + number);
            String beforePreview = storedDocument();
            cdp.evaluate("document.querySelector('.repair-controls').open=true; document.querySelector('#bulk-scope').value='CLASS'; document.querySelector('#bulk-scope').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#bulk-scope-id').value='cohort-8'; document.querySelector('#preview-bulk').click()");
            cdp.awaitText("60 lessons in this immutable snapshot", Duration.ofSeconds(10));
            assertEquals(beforePreview, storedDocument(), "UC-2 G5: preview has no durable effect");
            assertTrue(browserTrue(cdp, "Array.from(document.querySelectorAll('#bulk-preview-host li')).length === 60 && Array.from(document.querySelectorAll('#bulk-preview-host li')).every((item, index) => item.textContent.includes('lesson-' + (480 + index))) && document.querySelector('#bulk-preview-host').textContent.includes('Accepted period')"),
                    "UC-2 main 5: the browser preview names every expected class-eight lesson in order");
            cdp.evaluate("document.querySelector('#cancel-bulk').click()");
            assertEquals(beforePreview, storedDocument(), "UC-2 ext 5a: cancel retains exact prior Draft");
            cdp.evaluate("document.querySelector('#bulk-scope').value='CLASS'; document.querySelector('#bulk-scope').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#bulk-scope-id').value='cohort-8'; document.querySelector('#preview-bulk').click()");
            cdp.awaitText("60 lessons in this immutable snapshot", Duration.ofSeconds(10));
            jdbc.sql("""
                    CREATE FUNCTION reject_wide_bulk_save() RETURNS trigger AS $$
                    BEGIN RAISE EXCEPTION 'test: wide bulk storage unavailable'; END;
                    $$ LANGUAGE plpgsql
                    """).update();
            jdbc.sql("""
                    CREATE TRIGGER reject_wide_bulk_save BEFORE UPDATE ON workspace_aggregate
                    FOR EACH ROW WHEN (NEW.lifecycle_state = 'REPAIR_DRAFT')
                    EXECUTE FUNCTION reject_wide_bulk_save()
                    """).update();
            try {
                cdp.evaluate("document.querySelector('#confirm-bulk').click()");
                cdp.awaitText("The latest repair change was not durably saved", Duration.ofSeconds(10));
                assertEquals(beforePreview, storedDocument(), "UC-2 ext 5b: failed bulk confirmation preserves exact Draft/version");
                assertTrue(browserTrue(cdp, "document.querySelector('#solve-draft').disabled && !document.querySelector('[data-undo-bulk]')"));
            } finally {
                jdbc.sql("DROP TRIGGER IF EXISTS reject_wide_bulk_save ON workspace_aggregate").update();
                jdbc.sql("DROP FUNCTION IF EXISTS reject_wide_bulk_save()").update();
            }
            cdp.evaluate("document.querySelector('#bulk-scope').value='CLASS'; document.querySelector('#bulk-scope').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#bulk-scope-id').value='cohort-8'; document.querySelector('#preview-bulk').click()");
            cdp.awaitText("60 lessons in this immutable snapshot", Duration.ofSeconds(10));
            cdp.evaluate("document.querySelector('#confirm-bulk').click()");
            cdp.awaitText("Confirmed bulk snapshot · 60 lessons", Duration.ofSeconds(10));
            JsonNode bulk = assertDraftUnchangedBaseline(baseline);
            JsonNode action = bulk.path("intent").path("bulkActions").get(0);
            assertEquals("CLASS", action.path("scope").stringValue());
            assertEquals("cohort-8", action.path("scopeId").stringValue());
            assertEquals(JSON.readTree("[\"PERIOD\"]"), action.path("dimensions"));
            assertEquals(classEightLessons, jsonStrings(action.path("lessonIds")),
                    "UC-2 main 5: confirmation applies the exact previewed class-eight lesson IDs");
            assertTrue(browserTrue(cdp, "document.querySelector('.bulk-history').textContent.includes('Confirmed bulk snapshot · 60 lessons') && document.querySelector('.bulk-history').textContent.includes('Accepted period')"));
            String beforeFailedUndo = storedDocument();
            jdbc.sql("""
                    CREATE FUNCTION reject_wide_bulk_undo() RETURNS trigger AS $$
                    BEGIN RAISE EXCEPTION 'test: wide undo storage unavailable'; END;
                    $$ LANGUAGE plpgsql
                    """).update();
            jdbc.sql("""
                    CREATE TRIGGER reject_wide_bulk_undo BEFORE UPDATE ON workspace_aggregate
                    FOR EACH ROW WHEN (NEW.lifecycle_state = 'REPAIR_DRAFT')
                    EXECUTE FUNCTION reject_wide_bulk_undo()
                    """).update();
            try {
                cdp.evaluate("document.querySelector('[data-undo-bulk]').click()");
                cdp.awaitText("The latest repair change was not durably saved", Duration.ofSeconds(10));
                assertEquals(beforeFailedUndo, storedDocument(), "UC-2 ext 5b: failed undo retains the named bulk action exactly");
                assertTrue(browserTrue(cdp, "document.querySelector('#solve-draft').disabled && document.querySelector('[data-undo-bulk]')"));
            } finally {
                jdbc.sql("DROP TRIGGER IF EXISTS reject_wide_bulk_undo ON workspace_aggregate").update();
                jdbc.sql("DROP FUNCTION IF EXISTS reject_wide_bulk_undo()").update();
            }
            cdp.evaluate("document.querySelector('[data-undo-bulk]').click()");
            awaitBrowserCondition(cdp, "!document.querySelector('[data-undo-bulk]')");
            assertEquals(individual.path("intent"), assertDraftUnchangedBaseline(baseline).path("intent"),
                    "UC-2 main 5: undo removes only the named bulk action and retains individual room protection");
            assertTrue(assertDraftUnchangedBaseline(baseline).path("readyToSolve").booleanValue());
            assertTrue(browserTrue(cdp, "!document.querySelector('#solve-draft').disabled && document.querySelector('.protection-list summary').textContent.endsWith('1')"),
                    "UC-2 success: the saved conflict-free Draft is ready to solve with one unique protected lesson");

            String beforePresentation = storedDocument();
            cdp.evaluate("""
                    window.__draftPresentationMutations = [];
                    const draftFetch = window.fetch.bind(window);
                    window.fetch = (input, options = {}) => {
                      const method = (options.method || input?.method || 'GET').toUpperCase();
                      if (method !== 'GET' && method !== 'HEAD') window.__draftPresentationMutations.push(method);
                      return draftFetch(input, options);
                    };
                    """);
            cdp.evaluate("document.querySelector('#collapse-draft-task').click(); document.querySelector('[data-mode=CURRENT]').click(); document.querySelector('[data-mode=DRAFT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area').hidden && !document.querySelector('#reopen-draft-task').hidden"));
            cdp.evaluate("document.querySelector('#reopen-draft-task').click(); document.querySelector('[data-open-focus=teacherId]').click()");
            cdp.awaitText("Teacher schedule", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            assertTrue(browserTrue(cdp, "!document.querySelector('#workbench-task-area').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 500'"));
            assertTrue(browserTrue(cdp, "window.__draftPresentationMutations.length === 0"),
                    "UC-2 G3: mode, task collapse, focus and return issue no mutating request");
            assertEquals(beforePresentation, storedDocument(), "UC-2 main 6/G3: mode, collapse, focus and return save nothing");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.evaluate("window.scrollTo(0, 0)");
            assertDraftTaskGeometry(draftTaskGeometry(cdp), 1280, 800);
            assertTrue(draftDecisionReachable(cdp), "UC-2 G2: 1280px solve/discard controls are reachable in the task scrollport");
            captureWorkbenchScreenshot(cdp, "uc2-draft-1280.png");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1279)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && document.querySelector('#workbench-task-area').getBoundingClientRect().top >= document.querySelector('#workbench-inspector').getBoundingClientRect().bottom && document.documentElement.scrollWidth <= innerWidth"),
                    "UC-2 G6: at 1279px the inspector stacks between canvas and wide task area without page-level horizontal scroll");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            jdbc.sql("UPDATE workspace_aggregate SET version=version+1 WHERE workspace_id=1").update();
            String beforeStale = storedDocument();
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('#stage-repair-form').requestSubmit()");
            cdp.awaitText("The latest repair change was not durably saved", Duration.ofSeconds(10));
            assertEquals(beforeStale, storedDocument(), "UC-2 ext 2b/7a: stale revision preserves the last durable Draft/version");
            assertTrue(browserTrue(cdp, "document.querySelector('#solve-draft').disabled"));
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 701)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", false));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && document.querySelector('#workbench-task-area #stage-repair-form') && document.documentElement.scrollWidth <= innerWidth"),
                    "UC-2 G6: intermediate width keeps a wide task area and below-canvas inspector");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 700)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(10));
            assertTrue(browserTrue(cdp, "!document.querySelector('#start-repair-form, #stage-repair-form, #apply-pin, #preview-bulk, #solve-draft, #discard-draft, #workbench-task-area')"),
                    "UC-2 ext 1b: 700 px is already read-only");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 390)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", true));
            cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(10));
            assertTrue(browserTrue(cdp, "!document.querySelector('#start-repair-form, #stage-repair-form, #apply-pin, #preview-bulk, #solve-draft, #discard-draft, #workbench-task-area')"),
                    "UC-2 ext 1b: narrow Draft is truly read-only");
            assertEquals(beforeStale, storedDocument());
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.command("Page.reload", JSON.createObjectNode());
            awaitBrowserCondition(cdp, "document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#workbench-task-area') && !document.querySelector('#workbench-task-area').hidden");
            assertTrue(browserTrue(cdp, "!document.querySelector('#lesson-panel-title') && document.querySelector('#room-filter').value === '' && document.querySelector('#lesson-search').value === ''"));
            assertEquals(beforeStale, storedDocument(), "UC-2 ext 6b: reload retains only durable Draft and range preference");
            cdp.evaluate("document.querySelector('#confirm-discard-draft').click(); document.querySelector('#discard-draft').click()");
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(10));
            assertEquals("ACCEPTED_BASELINE", storedLifecycle());
            assertEquals(baseline, storedWorkspaceDocument().path("acceptedBaseline"));
            assertFalse(storedWorkspaceDocument().has("repairDraft"));

            cdp.evaluate("document.querySelector('#open-repair-setup').click(); document.querySelector('#repair-resource-type').value='ROOM'; document.querySelector('#repair-resource-type').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#repair-resource').value='room-99'; document.querySelector('[name=period][value=period-0]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("This rule currently conflicts with no accepted assignment", Duration.ofSeconds(15));
            JsonNode room = assertDraftUnchangedBaseline(baseline);
            assertEquals("ROOM", room.path("intent").path("changes").get(0).path("resourceType").stringValue());
            assertEquals("room-99", room.path("intent").path("changes").get(0).path("resourceId").stringValue());
            assertTrue(room.path("directEffectLessonIds").isEmpty());
            assertTrue(room.path("readyToSolve").booleanValue());
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    private static JsonNode draftTaskGeometry(Cdp cdp) throws Exception {
        return cdp.evaluateValue("""
                (() => { const task=document.querySelector('#workbench-task-area');
                  const canvas=document.querySelector('.canvas-region'); const wrap=document.querySelector('.matrix-wrap');
                  const inspector=document.querySelector('#workbench-inspector');
                  return {taskHeight:task.getBoundingClientRect().height, taskTop:task.getBoundingClientRect().top,
                    taskBottom:task.getBoundingClientRect().bottom,
                    canvasBottom:canvas.getBoundingClientRect().bottom, inspectorLeft:inspector.getBoundingClientRect().left,
                    canvasRight:canvas.getBoundingClientRect().right, row:document.querySelector('.week-matrix tbody tr').getBoundingClientRect().height,
                    heading:document.querySelector('.week-matrix thead').getBoundingClientRect().height,
                    visible:wrap.clientHeight, page:document.documentElement.scrollWidth, viewport:innerWidth,
                    taskRight:task.getBoundingClientRect().right, shellRight:document.querySelector('.workspace-card').getBoundingClientRect().right,
                    taskScroll:task.scrollHeight > task.clientHeight}; })()
                """).path("result").path("result").path("value");
    }

    private static void assertDraftTaskGeometry(JsonNode geometry, int width, int height) {
        assertTrue(geometry.path("taskHeight").doubleValue() <= height * .35, "UC-2 G2: task height: " + geometry);
        assertTrue(geometry.path("taskBottom").doubleValue() <= height, "UC-2 G2: task and canvas remain concurrently visible: " + geometry);
        assertTrue(geometry.path("taskTop").doubleValue() >= geometry.path("canvasBottom").doubleValue(), "UC-2 G2: task cannot overlay canvas: " + geometry);
        assertTrue(geometry.path("inspectorLeft").doubleValue() >= geometry.path("canvasRight").doubleValue(), "UC-2 G2: inspector remains beside canvas: " + geometry);
        assertTrue(geometry.path("visible").doubleValue() >= geometry.path("heading").doubleValue() + geometry.path("row").doubleValue(),
                "UC-2 G2: time heading and one complete class row remain visible: " + geometry);
        assertTrue(geometry.path("page").doubleValue() <= width + 1, "UC-2 G2: no page-level horizontal scroll: " + geometry);
        assertTrue(geometry.path("taskRight").doubleValue() <= geometry.path("shellRight").doubleValue(), "UC-2 G2: task remains within shell: " + geometry);
        assertTrue(geometry.path("taskScroll").booleanValue(), "UC-2 G2: long task content scrolls independently: " + geometry);
    }

    private static boolean draftDecisionReachable(Cdp cdp) throws Exception {
        return browserTrue(cdp, """
                (() => { const task=document.querySelector('#workbench-task-area'); task.scrollTop=task.scrollHeight;
                  const bounds=task.getBoundingClientRect();
                  return ['#solve-draft', '#discard-draft'].every(selector => {
                    const action=document.querySelector(selector).getBoundingClientRect();
                    return action.top >= bounds.top && action.bottom <= bounds.bottom && action.bottom <= innerHeight;
                  }); })()
                """);
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
            String currentView = cdp.awaitText("Complete school population", Duration.ofSeconds(5));
            assertTrue(currentView.contains("Repair draft · not current") && currentView.contains("Current · accepted"));
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
    @DisplayName("Timetable polish UC-2 main/7b/G1-G5: teacher intent preserves accepted IDs, selection, mode and focused context")
    void retainsAcceptedCanvasThroughTeacherDraftModesAndReload() throws Exception {
        ObjectNode accepted = validAcceptedDocument();
        verifier.verify(new ImportDocuments(accepted.path("acceptedBaseline").path("definition"),
                accepted.path("acceptedBaseline").path("result"), null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Complete recurring Week", Duration.ofSeconds(15));
            assertEquals(Set.of("lesson-math-1", "lesson-science-1"), renderedLessonIds(cdp));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-math-1]').click(); document.querySelector('[data-range=DAY]').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#teacher-investigation').value='teacher-alex'; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('[name=period][value=\"mon-1\"]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            String draftText = cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            assertTrue(draftText.contains("Accepted baseline remains current"));
            assertTrue(draftText.contains("Alex"));
            JsonNode draft = assertDraftUnchangedBaseline(baseline);
            assertEquals(1, draft.path("intent").path("changes").size());
            assertEquals("TEACHER", draft.path("intent").path("changes").get(0).path("resourceType").stringValue());
            assertEquals("teacher-alex", draft.path("intent").path("changes").get(0).path("resourceId").stringValue());
            assertEquals(JSON.readTree("[\"mon-1\"]"), draft.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
            assertEquals(JSON.readTree("[\"lesson-math-1\"]"), draft.path("directEffectLessonIds"));
            assertEquals(0, draft.path("conflicts").size());
            assertTrue(draft.path("readyToSolve").booleanValue());
            assertTrue(draft.path("persisted").booleanValue());
            assertEquals(0, draft.path("intent").path("pins").size());
            assertEquals(0, draft.path("intent").path("bulkActions").size());
            String durable = storedDocument();
            assertEquals(Set.of("lesson-math-1", "lesson-science-1"), renderedLessonIds(cdp));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-modes [data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Mathematics 1' && document.querySelector('[data-lesson-id=lesson-math-1]')?.textContent.includes('Directly affected')"));

            cdp.evaluate("document.querySelector('#workbench-modes [data-mode=CURRENT]').click()");
            cdp.awaitText("Current · accepted", Duration.ofSeconds(5));
            assertEquals(Set.of("lesson-math-1", "lesson-science-1"), renderedLessonIds(cdp));
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title')?.textContent === 'Mathematics 1' && !document.querySelector('#apply-pin') && document.querySelector('#teacher-investigation')?.value === 'teacher-alex' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true'"));
            cdp.evaluate("document.querySelector('#workbench-modes [data-mode=DRAFT]').click()");
            cdp.awaitText("Directly affected", Duration.ofSeconds(5));
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title')?.textContent === 'Mathematics 1' && document.querySelector('#apply-pin') !== null"));
            cdp.evaluate("document.querySelector('#toggle-inspector').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.workbench-layout')?.classList.contains('inspector-collapsed') && document.querySelector('#inspector-summary')?.textContent.includes('Mathematics 1')"));
            cdp.evaluate("document.querySelector('#reopen-inspector').click(); document.querySelector('[data-open-focus=teacherId]').click()");
            cdp.awaitText("Teacher schedule · Alex", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            assertEquals(Set.of("lesson-math-1", "lesson-science-1"), renderedLessonIds(cdp));
            assertEquals(durable, storedDocument(), "mode, range, inspector and focused return are presentation-only");

            cdp.evaluate("window.__uc2ReloadMarker = true");
            cdp.command("Page.reload", JSON.createObjectNode());
            awaitBrowserCondition(cdp, "window.__uc2ReloadMarker !== true && document.querySelector('#workbench-modes [data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true'");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-modes [data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('#lesson-panel-title')"));
            assertEquals(durable, storedDocument(), "reload restores the durable draft, not ephemeral selection or mode");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("Timetable polish UC-2 extensions 4a/4b/5a: conflicting individual pin, canceled preview, confirmed bulk and exact undo")
    void resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources() throws Exception {
        ObjectNode accepted = validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('[name=period][value=\"mon-1\"]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            JsonNode initial = assertDraftUnchangedBaseline(baseline);
            assertEquals(JSON.readTree("[\"lesson-math-1\"]"), initial.path("directEffectLessonIds"));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-math-1]').click(); document.querySelector('#apply-pin').click()");
            String conflictText = cdp.awaitText("Resolve blocking conflicts before solving", Duration.ofSeconds(10));
            assertTrue(conflictText.contains("Blocking conflict"));
            JsonNode conflicted = assertDraftUnchangedBaseline(baseline);
            assertEquals(1, conflicted.path("conflicts").size());
            assertEquals("lesson-math-1", conflicted.path("conflicts").get(0).path("lessonId").stringValue());
            assertEquals("PIN_CONTRADICTS_UNAVAILABILITY", conflicted.path("conflicts").get(0).path("code").stringValue());
            assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), conflicted.path("intent").path("pins").get(0).path("periodSources"));
            assertFalse(conflicted.path("readyToSolve").booleanValue());
            assertTrue(browserTrue(cdp, "document.querySelector('#solve-draft')?.disabled === true && document.querySelector('[data-lesson-id=lesson-math-1]')?.textContent.includes('Blocking conflict')"));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-math-1]').click(); document.querySelector('#remove-pin').click()");
            cdp.awaitText("Draft is durably saved with no blocking conflict", Duration.ofSeconds(10));
            JsonNode resolved = assertDraftUnchangedBaseline(baseline);
            assertEquals(0, resolved.path("conflicts").size());
            assertEquals(0, resolved.path("intent").path("pins").size());
            assertTrue(resolved.path("readyToSolve").booleanValue());

            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-science-1]').click(); document.querySelector('[name=lesson-dimension][value=ROOM]').checked=true; document.querySelector('#apply-pin').click()");
            cdp.awaitText("Accepted room pinned", Duration.ofSeconds(10));
            JsonNode individual = assertDraftUnchangedBaseline(baseline);
            assertEquals(1, individual.path("intent").path("pins").size());
            JsonNode individualPin = individual.path("intent").path("pins").get(0);
            assertEquals("lesson-science-1", individualPin.path("lessonId").stringValue());
            assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), individualPin.path("periodSources"));
            assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), individualPin.path("roomSources"));
            String beforePreview = storedDocument();
            cdp.evaluate("document.querySelector('#preview-bulk').click()");
            assertTrue(cdp.awaitText("Bulk pin preview · no changes applied yet", Duration.ofSeconds(10)).contains("1 lesson in this immutable snapshot"));
            assertEquals(beforePreview, storedDocument(), "preview must not write pins or advance version");
            cdp.evaluate("document.querySelector('#cancel-bulk').click()");
            assertTrue(browserTrue(cdp, "!document.querySelector('#confirm-bulk') && document.querySelector('#lesson-panel-title')?.textContent === 'Science 1'"));
            assertEquals(beforePreview, storedDocument(), "cancellation must leave exact individual protection intact");

            cdp.evaluate("document.querySelector('#preview-bulk').click()");
            cdp.awaitText("Bulk pin preview · no changes applied yet", Duration.ofSeconds(10));
            assertEquals(beforePreview, storedDocument());
            cdp.evaluate("document.querySelector('#confirm-bulk').click()");
            cdp.awaitText("Confirmed bulk snapshot · 1 lesson", Duration.ofSeconds(10));
            JsonNode bulk = assertDraftUnchangedBaseline(baseline);
            assertEquals(1, bulk.path("intent").path("bulkActions").size());
            JsonNode action = bulk.path("intent").path("bulkActions").get(0);
            assertEquals("UNAFFECTED", action.path("scope").stringValue());
            assertEquals(JSON.readTree("[\"lesson-science-1\"]"), action.path("lessonIds"));
            assertEquals(JSON.readTree("[\"PERIOD\"]"), action.path("dimensions"));
            JsonNode bulkPin = bulk.path("intent").path("pins").get(0);
            assertEquals("lesson-science-1", bulkPin.path("lessonId").stringValue());
            assertEquals(Set.of("INDIVIDUAL", action.path("id").stringValue()), jsonStrings(bulkPin.path("periodSources")));
            assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), bulkPin.path("roomSources"));
            cdp.evaluate("document.querySelector('[data-undo-bulk]').click()");
            awaitBrowserCondition(cdp, "!document.querySelector('[data-undo-bulk]') && document.querySelector('#attempt-pin-count')?.textContent === '1'");
            JsonNode undone = assertDraftUnchangedBaseline(baseline);
            assertEquals(individual.path("intent"), undone.path("intent"), "undo restores individual dimensions, removing only this bulk source");
            assertEquals(0, undone.path("conflicts").size());
            assertTrue(undone.path("readyToSolve").booleanValue());
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("Timetable polish UC-2 extensions 3a/7a/7b/G6: no-effect room intent, narrow read-only draft and confirmed discard")
    void keepsZeroEffectRoomDraftOnReloadThenDiscardsExplicitly() throws Exception {
        ObjectNode accepted = validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('#repair-resource-type').value='ROOM'; document.querySelector('#repair-resource-type').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#repair-resource').value='room-101'; document.querySelector('[name=period][value=\"mon-1\"]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            String zero = cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            assertTrue(zero.contains("Accepted baseline remains current"));
            assertTrue(zero.contains("This rule currently conflicts with no accepted assignment"));
            JsonNode noEffect = assertDraftUnchangedBaseline(baseline);
            assertEquals(0, noEffect.path("directEffectLessonIds").size());
            assertEquals(0, noEffect.path("conflicts").size());
            assertEquals("ROOM", noEffect.path("intent").path("changes").get(0).path("resourceType").stringValue());
            assertEquals("room-101", noEffect.path("intent").path("changes").get(0).path("resourceId").stringValue());
            assertEquals(JSON.readTree("[\"mon-1\"]"), noEffect.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
            assertTrue(noEffect.path("readyToSolve").booleanValue());
            assertEquals(Set.of("lesson-math-1", "lesson-science-1"), renderedLessonIds(cdp));
            String beforeNarrow = storedDocument();
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 390)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", true));
            String narrow = cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(10));
            assertTrue(narrow.contains("Repair draft · not current"));
            assertTrue(browserTrue(cdp, "!document.querySelector('#start-repair-form') && !document.querySelector('#apply-pin') && !document.querySelector('#preview-bulk') && !document.querySelector('#discard-draft') && !document.querySelector('#solve-draft') && !document.querySelector('#workbench-modes')"));
            assertEquals(beforeNarrow, storedDocument());
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.awaitText("Discard repair draft", Duration.ofSeconds(10));
            cdp.evaluate("window.__uc2ReloadMarker = true");
            cdp.command("Page.reload", JSON.createObjectNode());
            awaitBrowserCondition(cdp, "window.__uc2ReloadMarker !== true && document.querySelector('#workbench-modes [data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true'");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-modes [data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#discard-draft')?.disabled === true"));
            assertEquals(beforeNarrow, storedDocument());
            cdp.evaluate("document.querySelector('#confirm-discard-draft').click(); document.querySelector('#discard-draft').click()");
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(10));
            JsonNode discarded = storedWorkspaceDocument();
            assertEquals("ACCEPTED_BASELINE", storedLifecycle());
            assertEquals(baseline, discarded.path("acceptedBaseline"));
            assertFalse(discarded.has("repairDraft"));
            assertFalse(discarded.has("run"));
            assertFalse(discarded.has("proposal"));
            assertTrue(browserTrue(cdp, "document.querySelectorAll('#workbench-modes [data-mode]').length === 1 && document.querySelector('#workbench-modes [data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true'"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("Timetable polish UC-2 extension 2a/minimal guarantee: failed durable pin save cannot enable repair")
    void refusesSolveAfterRealDraftPersistenceFailure() throws Exception {
        ObjectNode accepted = validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('[name=period][value=\"mon-1\"]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            String durable = storedDocument();
            jdbc.sql("""
                    CREATE FUNCTION reject_browser_draft_save() RETURNS trigger AS $$
                    BEGIN
                      RAISE EXCEPTION 'test: draft storage unavailable';
                    END;
                    $$ LANGUAGE plpgsql
                    """).update();
            jdbc.sql("""
                    CREATE TRIGGER reject_browser_draft_save BEFORE UPDATE ON workspace_aggregate
                    FOR EACH ROW WHEN (NEW.lifecycle_state = 'REPAIR_DRAFT')
                    EXECUTE FUNCTION reject_browser_draft_save()
                    """).update();
            try {
                cdp.evaluate("document.querySelector('[data-lesson-id=lesson-science-1]').click(); document.querySelector('[name=lesson-dimension][value=PERIOD]').checked=false; document.querySelector('[name=lesson-dimension][value=ROOM]').checked=true; document.querySelector('#apply-pin').click()");
                String error = cdp.awaitText("The latest repair change was not durably saved", Duration.ofSeconds(15));
                assertTrue(error.contains("Accepted baseline remains current"));
                assertTrue(error.contains("Local storage is unavailable. The action did not complete."));
                assertEquals(durable, storedDocument(), "failed autosave must roll back version and complete document");
                assertTrue(browserTrue(cdp, "document.querySelector('#solve-draft')?.disabled === true"),
                        "the unpersisted revision must not be presented as ready to solve");
            } finally {
                jdbc.sql("DROP TRIGGER IF EXISTS reject_browser_draft_save ON workspace_aggregate").update();
                jdbc.sql("DROP FUNCTION IF EXISTS reject_browser_draft_save()").update();
            }
            assertEquals(durable, storedDocument());
            assertDraftUnchangedBaseline(baseline);
            assertFalse(storedWorkspaceDocument().has("run"));
            assertFalse(storedWorkspaceDocument().has("proposal"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("UC-7 main/7a/G1-G4/RULE-24: real browser accepts a protected teacher repair then a directly parented room repair")
    void generatesRepairProposalInRealBrowser() throws Exception {
        ObjectNode document = validAcceptedDocument();
        JsonNode acceptedBefore = document.path("acceptedBaseline").deepCopy();
        storeAccepted(document);
        JsonNode proposedDocument = null;
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
            JsonNode frozenDraft = assertDraftUnchangedBaseline(acceptedBefore).deepCopy();
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            String solving = cdp.awaitText("Repair generation · running", Duration.ofSeconds(10));
            assertTrue(solving.contains("Accepted baseline remains current"));
            assertTrue(solving.contains("Search lessons"));
            assertFalse(solving.contains("Apply selected pins"));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area #cancel-run') !== null && document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Frozen repair intent') && !document.querySelector('#workbench-inspector #cancel-run')"),
                    "Workbench layout UC-3 main 2: active run state and cancellation belong in the wide task area");

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
            assertEquals("REPAIR_PROPOSAL", storedLifecycle());
            proposedDocument = storedWorkspaceDocument();
            assertEquals(acceptedBefore, proposedDocument.path("acceptedBaseline"));
            assertEquals(frozenDraft, proposedDocument.path("repairDraft"));
            assertEquals("REPAIR", proposedDocument.path("proposal").path("kind").stringValue());
            assertEquals("FEASIBLE", proposedDocument.path("lastRun").path("status").stringValue());
            assertEquals(2, proposedDocument.path("proposal").path("result").path("timetable").path("assignments").size());
            assertFalse(proposedDocument.has("run"));
            assertEquals(Set.of("lesson-math-1", "lesson-science-1"), renderedLessonIds(cdp));
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=SOLVING]') && document.querySelector('#lesson-panel-title')?.textContent === 'Science 1' && document.querySelector('#workbench-inspector .state.accepted')?.textContent.includes('Accepted assignment')"));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector .before-after')?.textContent.includes('Old assignment') && document.querySelector('#workbench-inspector .before-after')?.textContent.includes('Proposed assignment') && document.querySelector('#accept-repair')?.disabled === true"),
                    "UC-4 main 1-5: accepted and proposed fields stay inspectable and acceptance remains gated");
            JsonNode beforeDecision = storedWorkspaceDocument();
            cdp.evaluate("document.querySelector('[data-mode=CURRENT]').click(); document.querySelector('[data-mode=DRAFT]').click(); document.querySelector('[data-mode=PROPOSAL]').click()");
            assertEquals(beforeDecision, storedWorkspaceDocument(), "UC-4 G3: mode navigation must not change durable proposal or accepted baseline");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true'"));
            jdbc.sql("UPDATE workspace_aggregate SET document = document #- '{proposal,review}' WHERE workspace_id=1")
                    .update();
            cdp.command("Page.navigate", object("url", page));
            String restoredLegacyProposal = cdp.awaitText("Repair proposal · feasible", Duration.ofSeconds(15));
            assertTrue(restoredLegacyProposal.contains("Unique changed lessons"));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-science-1]').click()");
            assertTrue(cdp.awaitText("Old assignment", Duration.ofSeconds(5)).contains("Proposed assignment"));
            cdp.evaluate("document.querySelector('#confirm-repair-accept').focus()");
            cdp.pressKey(" ", "Space");
            String beforeFailedAccept = storedDocument();
            jdbc.sql("""
                    CREATE FUNCTION fail_browser_repair_accept() RETURNS trigger AS $$
                    BEGIN
                      IF NEW.lifecycle_state = 'ACCEPTED_BASELINE' THEN RAISE EXCEPTION 'test: acceptance unavailable'; END IF;
                      RETURN NEW;
                    END; $$ LANGUAGE plpgsql
                    """).update();
            jdbc.sql("""
                    CREATE TRIGGER fail_browser_repair_accept_trigger BEFORE UPDATE ON workspace_aggregate
                    FOR EACH ROW EXECUTE FUNCTION fail_browser_repair_accept()
                    """).update();
            try {
                cdp.evaluate("document.querySelector('#accept-repair').click()");
                assertTrue(cdp.awaitText("Current did not advance", Duration.ofSeconds(10))
                        .contains("Repair proposal · feasible"), "UC-4 6b: failed durable acceptance cannot present Current");
                assertEquals(beforeFailedAccept, storedDocument(), "UC-4 6b: failed acceptance preserves entire accepted/draft/proposal document and version");
                assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('#accept-repair').disabled"));
            } finally {
                jdbc.sql("DROP TRIGGER IF EXISTS fail_browser_repair_accept_trigger ON workspace_aggregate").update();
                jdbc.sql("DROP FUNCTION IF EXISTS fail_browser_repair_accept()").update();
            }
            cdp.evaluate("document.querySelector('#accept-repair').focus()");
            cdp.pressKey(" ", "Space");
            String accepted = cdp.awaitText("Accepted baseline · current timetable", Duration.ofSeconds(15));
            assertTrue(accepted.contains("Start a protected repair"));
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL], [data-mode=DRAFT]') && document.querySelector('[data-range=WEEK]')?.getAttribute('aria-pressed') === 'true'"),
                    "UC-4 success: only Current remains on the retained range after durable acceptance");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }

        JsonNode firstAccepted = JSON.readTree(jdbc.sql(
                        "SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single()).path("acceptedBaseline").deepCopy();
        assertEquals(proposedDocument.path("proposal").path("definition"), firstAccepted.path("definition"));
        assertEquals(proposedDocument.path("proposal").path("result"), firstAccepted.path("result"));
        assertEquals("ACCEPTED_BASELINE", storedLifecycle());
        assertFalse(storedWorkspaceDocument().has("proposal"));
        assertFalse(storedWorkspaceDocument().has("repairDraft"));
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
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-science-1]').click()");
            assertTrue(browserTrue(cdp, "!document.querySelector('#draft-selected-protection')?.textContent.includes('Policy room lock') && !document.querySelector('[data-lesson-id=lesson-science-1]')?.textContent.includes('Policy room lock')"),
                    "Workbench layout UC-2 G4: an accepted prior attempt-scoped lock is not mislabeled as persistent policy");
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
    @DisplayName("Timetable polish UC-3 main 1-4/2a/2b/G1-G8: frozen run stays inspectable and cancellation or recovery restores the exact draft")
    void inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser() throws Exception {
        ObjectNode accepted = validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('[name=period][value=mon-1]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-science-1]').click(); document.querySelector('[name=lesson-dimension][value=PERIOD]').checked=false; document.querySelector('[name=lesson-dimension][value=ROOM]').checked=true; document.querySelector('#apply-pin').click()");
            cdp.awaitText("Attempt-scoped pins\n1", Duration.ofSeconds(10));
            cdp.evaluate("document.querySelector('[data-range=DAY]').click(); document.querySelector('#cohort-filter').value='cohort-7a'; document.querySelector('#cohort-filter').dispatchEvent(new Event('change',{bubbles:true}))");
            JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
            assertEquals("lesson-science-1", draft.path("intent").path("pins").get(0).path("lessonId").stringValue());

            processes.blockReplan = true;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            processes.awaitBlocked();
            cdp.awaitText("Repair generation · running", Duration.ofSeconds(15));
            assertEquals("SOLVING_REPAIR", storedLifecycle());
            JsonNode running = storedWorkspaceDocument();
            String runId = running.path("run").path("id").stringValue();
            assertEquals("PT30S", running.path("run").path("limit").stringValue());
            assertEquals(baseline, running.path("acceptedBaseline"));
            assertEquals(draft, running.path("repairDraft"));
            assertFalse(running.has("proposal"));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area #cancel-run')?.textContent === 'Cancel run' && document.querySelector('#workbench-task-area')?.textContent.includes('PT30S') && !document.querySelector('#workbench-inspector #cancel-run') && document.querySelector('#cohort-filter')?.value === 'cohort-7a' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Science 1' && !document.querySelector('#apply-pin')"));
            cdp.evaluate("window.__uc3RunCanvas = document.querySelector('[data-lesson-id=lesson-science-1]')");
            Thread.sleep(800);
            assertTrue(browserTrue(cdp, "window.__uc3RunCanvas === document.querySelector('[data-lesson-id=lesson-science-1]')"),
                    "unchanged run polling must not replace the selected lesson or steal focus");

            cdp.evaluate("document.querySelector('[data-mode=DRAFT]').click()");
            cdp.awaitText("Frozen repair intent · not current", Duration.ofSeconds(5));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Alex') && document.querySelector('[data-lesson-id=lesson-science-1]')?.textContent.includes('Accepted room pinned') && !document.querySelector('#apply-pin') && !document.querySelector('#solve-draft') && !document.querySelector('#stage-repair-form')"));
            cdp.evaluate("document.querySelector('[data-mode=CURRENT]').click()");
            cdp.awaitText("Current · accepted", Duration.ofSeconds(5));
            assertTrue(browserTrue(cdp, "!document.querySelector('#apply-pin') && document.querySelector('#cancel-run') !== null && document.querySelector('#cohort-filter')?.value === 'cohort-7a'"));
            cdp.evaluate("document.querySelector('[data-mode=SOLVING]').click(); document.querySelector('[data-open-focus=teacherId]').click()");
            cdp.awaitText("Teacher schedule · Alex", Duration.ofSeconds(5));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area #cancel-run') !== null"));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));

            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 390).put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            awaitBrowserCondition(cdp, "document.querySelector('.focused-schedule') !== null && document.querySelector('#workbench-modes') === null && document.querySelector('#cancel-run') === null");
            assertEquals(running, storedWorkspaceDocument(), "narrow inspection must not mutate the active run");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280).put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            awaitBrowserCondition(cdp, "document.querySelector('#workbench-task-area #cancel-run') !== null");
            assertEquals(runId, storedWorkspaceDocument().path("run").path("id").stringValue());
            cdp.evaluate("document.querySelector('#cancel-run').click()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(10));
            assertEquals("REPAIR_DRAFT", storedLifecycle());
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            assertEquals("CANCELLED", storedWorkspaceDocument().path("lastRun").path("status").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('#solve-draft') !== null && document.querySelector('#cancel-run') === null"));

            processes.reset();
            processes.blockReplan = true;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            processes.awaitBlocked();
            cdp.awaitText("Repair generation · running", Duration.ofSeconds(10));
            recovery.recoverInterruptedRun();
            assertEquals("REPAIR_DRAFT", storedLifecycle());
            cdp.command("Page.reload", JSON.createObjectNode());
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            assertEquals("INTERRUPTED", storedWorkspaceDocument().path("lastRun").path("code").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area .conflict-list')?.textContent.includes('Repair generation was interrupted.') && document.querySelector('#utilities')?.textContent.includes('Repair generation was interrupted.') && !document.querySelector('#utilities').open"));
            assertTrue(browserTrue(cdp, "document.querySelector('#solve-draft') !== null && !document.querySelector('#cancel-run')"));
            processes.reset();
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("Timetable polish UC-3 extensions 5a/5b/6a/G1-G6: run failures expose only safe Utilities evidence and unchanged retry is bounded")
    void showsFailedRepairEvidenceAndGatedRetryInRealBrowser() throws Exception {
        ObjectNode accepted = validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('[name=period][value=mon-1]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
            processes.failure = WorkspaceRepairPlanningIT.RepairFailure.NO_FEASIBLE;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            cdp.awaitText("Retry unchanged draft for two minutes", Duration.ofSeconds(15));
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            assertTrue(browserTrue(cdp, "document.querySelector('#utilities')?.open && !document.querySelector('#workbench-inspector').hidden && document.querySelector('#workbench-task-area .conflict-list')?.textContent.includes('hard.teacher-period') && document.querySelector('#utilities')?.textContent.includes('2 matches')"));
            cdp.evaluate("document.querySelector('#utilities').open=true; document.querySelector('#utilities [data-diagnostic-id=lesson-math-1]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title')?.textContent === 'Mathematics 1'"));
            cdp.evaluate("document.querySelector('[data-mode=CURRENT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#utilities')?.textContent.includes('hard.teacher-period') && document.querySelector('#solve-draft') === null"));
            cdp.evaluate("document.querySelector('[data-mode=DRAFT]').click()");
            processes.failure = null;
            processes.blockReplan = true;
            cdp.evaluate("document.querySelector('#retry-repair').click()");
            processes.awaitBlocked();
            cdp.awaitText("Repair generation · running", Duration.ofSeconds(10));
            assertEquals("PT2M", storedWorkspaceDocument().path("run").path("limit").stringValue());
            cdp.evaluate("document.querySelector('#cancel-run').click()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(10));
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            assertTrue(browserTrue(cdp, "document.querySelector('#retry-repair') === null"));

            for (var failure : new WorkspaceRepairPlanningIT.RepairFailure[] {
                    WorkspaceRepairPlanningIT.RepairFailure.INVALID_INPUT,
                    WorkspaceRepairPlanningIT.RepairFailure.TRANSPORT,
                    WorkspaceRepairPlanningIT.RepairFailure.INTERRUPTED,
                    WorkspaceRepairPlanningIT.RepairFailure.MISMATCHED,
                    WorkspaceRepairPlanningIT.RepairFailure.WATCHDOG }) {
                processes.reset();
                processes.failure = failure;
                cdp.evaluate("document.querySelector('#solve-draft').click()");
                String message = switch (failure) {
                    case INVALID_INPUT -> "School Kernel rejected the repair definition.";
                    case TRANSPORT -> "School Kernel could not be started.";
                    case INTERRUPTED -> "Repair generation was interrupted.";
                    case MISMATCHED -> "School Kernel returned an unverified repair result.";
                    case WATCHDOG -> "Repair generation did not finish within its bounded run.";
                    default -> throw new IllegalStateException();
                };
                cdp.awaitText(message, Duration.ofSeconds(15));
                assertEquals(draft, assertDraftUnchangedBaseline(baseline));
                assertTrue(browserTrue(cdp, "document.querySelector('#utilities')?.textContent.includes('" + message + "') && !document.body.innerText.includes('secret raw') && !document.querySelector('#retry-repair')"));
            }
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("Timetable polish UC-3 ext 2b/G7/RULE-11: stop the application mid-run and restart on the same durable school")
    void restoresInterruptedRepairAfterActualApplicationRestartInRealBrowser() throws Exception {
        ObjectNode accepted = validAcceptedDocument();
        storeAccepted(accepted);
        JsonNode baseline = accepted.path("acceptedBaseline").deepCopy();
        ConfigurableApplicationContext first = startRestartableWorkspace();
        ConfigurableApplicationContext restarted = null;
        WorkspaceRepairPlanningIT.SwitchingRepairProcessLauncher blocked = first.getBean(
                WorkspaceRepairPlanningIT.SwitchingRepairProcessLauncher.class);
        try (Cdp cdp = openWorkspaceBrowser(first.getEnvironment().getProperty("local.server.port", Integer.class))) {
            cdp.awaitText("Start a protected repair", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('[name=period][value=mon-1]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
            JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
            blocked.blockReplan = true;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            blocked.awaitBlocked();
            cdp.awaitText("Repair generation · running", Duration.ofSeconds(15));
            assertEquals("SOLVING_REPAIR", storedLifecycle());
            JsonNode running = storedWorkspaceDocument();
            String originalRunId = running.path("run").path("id").stringValue();
            long runningVersion = jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1")
                    .query(Long.class).single();
            assertEquals(baseline, running.path("acceptedBaseline"));
            assertEquals(draft, running.path("repairDraft"));
            assertFalse(running.has("proposal"));

            cdp.command("Page.navigate", object("url", "about:blank"));
            awaitBrowserCondition(cdp, "document.location.href === 'about:blank'");
            first.close();
            assertFalse(first.isActive(), "the application that started the repair is stopped before recovery");
            restarted = startRestartableWorkspace();
            int restoredPort = restarted.getEnvironment().getProperty("local.server.port", Integer.class);
            cdp.command("Page.navigate", object("url", "http://localhost:" + restoredPort + "/workspace/"));
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(20));
            assertEquals("REPAIR_DRAFT", storedLifecycle());
            assertEquals(runningVersion + 1, jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1")
                    .query(Long.class).single());
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            JsonNode recovered = storedWorkspaceDocument();
            assertEquals(originalRunId, recovered.path("lastRun").path("id").stringValue());
            assertEquals("INTERRUPTED", recovered.path("lastRun").path("code").stringValue());
            assertEquals("FAILED", recovered.path("lastRun").path("status").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL]') && !document.querySelector('#cancel-run') && document.querySelector('#utilities')?.textContent.includes('Repair generation was interrupted.') && document.querySelector('#solve-draft') !== null"));
            blocked.reset();
            Thread.sleep(300);
            assertEquals(recovered, storedWorkspaceDocument(), "the old process must not publish after a fresh instance recovers its run");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        } finally {
            blocked.reset();
            if (first.isActive()) first.close();
            if (restarted != null) restarted.close();
        }
    }

    @Test
    @DisplayName("Workbench layout UC-3 main/2a/6a/G1-G7: normative browser retains Current during wide Solving, cancellation, rejection and verified handoff")
    void followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshotInRealBrowser() throws Exception {
        ObjectNode document = investigationScaleDocument();
        JsonNode baseline = document.path("acceptedBaseline").deepCopy();
        JsonNode definition = baseline.path("definition");
        JsonNode assignments = baseline.path("result").path("timetable").path("assignments");
        KernelVerifier.Verification verified = verifier.verify(new ImportDocuments(definition,
                baseline.path("result"), null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        assertEquals(document.path("definitionRevision").stringValue(), verified.definitionRevision());
        assertEquals(document.path("timetableRevision").stringValue(), verified.timetableRevision());
        assertEquals(60, definition.path("cohorts").size());
        assertEquals(100, definition.path("teachers").size());
        assertEquals(100, definition.path("rooms").size());
        assertEquals(1_000, assignments.size());
        assertEquals(60, definition.path("periods").size());
        assertEquals("subject-0", assignments.get(960).path("subjectId").stringValue());
        assertEquals("teacher-16", assignments.get(960).path("teacherId").stringValue());
        assertEquals("cohort-16", assignments.get(960).path("cohortId").stringValue());
        assertEquals("period-0", assignments.get(960).path("periodId").stringValue());
        assertEquals("room-16", assignments.get(960).path("roomId").stringValue());
        Set<String> expectedIds = new HashSet<>();
        Set<String> expectedMondayIds = new HashSet<>();
        for (JsonNode assignment : assignments) expectedIds.add(assignment.path("lessonId").stringValue());
        for (JsonNode assignment : assignments) {
            int periodOrdinal = Integer.parseInt(assignment.path("periodId").stringValue().substring("period-".length()));
            if (periodOrdinal < 12) expectedMondayIds.add(assignment.path("lessonId").stringValue());
        }
        assertEquals(1_000, expectedIds.size());
        storeAccepted(document);
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1600)
                    .put("height", 900).put("deviceScaleFactor", 1).put("mobile", false));
            cdp.command("Page.reload", JSON.createObjectNode());
            cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            assertEquals(expectedIds, renderedLessonIds(cdp));
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('#repair-resource').value='teacher-16'; document.querySelector('[name=period][value=period-0]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(20));
            JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
            assertEquals("teacher-16", draft.path("intent").path("changes").get(0).path("resourceId").stringValue());
            assertEquals(JSON.readTree("[\"period-0\"]"), draft.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
            assertTrue(jsonStrings(draft.path("directEffectLessonIds")).contains("lesson-960"));
            assertEquals(expectedIds, renderedLessonIds(cdp));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-500]').click(); document.querySelector('[name=lesson-dimension][value=PERIOD]').checked=false; document.querySelector('[name=lesson-dimension][value=ROOM]').checked=true; document.querySelector('#apply-pin').click()");
            cdp.awaitText("Accepted room pinned", Duration.ofSeconds(15));
            draft = assertDraftUnchangedBaseline(baseline).deepCopy();
            assertEquals("lesson-500", draft.path("intent").path("pins").get(0).path("lessonId").stringValue());
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-960]').click(); document.querySelector('[data-range=DAY]').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            assertEquals(expectedMondayIds, renderedLessonIds(cdp));
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('[data-lesson-id=lesson-960]')?.textContent.includes('Directly affected')"));

            processes.blockReplan = true;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            processes.awaitBlocked();
            cdp.awaitText("Repair generation · running", Duration.ofSeconds(20));
            JsonNode running = storedWorkspaceDocument();
            assertEquals("SOLVING_REPAIR", storedLifecycle());
            assertEquals(baseline, running.path("acceptedBaseline"));
            assertEquals(draft, running.path("repairDraft"));
            assertFalse(running.has("proposal"));
            assertEquals("PT30S", running.path("run").path("limit").stringValue());
            assertEquals(expectedMondayIds, renderedLessonIds(cdp));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area #cancel-run') && !document.querySelector('#workbench-inspector #cancel-run, #workbench-inspector #stage-repair-form, #workbench-inspector #proposal-context') && document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Teacher Sixteen with a deliberately long authoritative display name for timetable tiles') && document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Declared period 0') && document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('lesson-500') && !document.querySelector('#apply-pin, #discard-draft, #accept-repair')"),
                    "UC-3 main 2/G1: frozen intent, status and cancellation are in the wide task area only");
            cdp.evaluate("""
                    window.__uc3PresentationMutations = [];
                    const runFetch = window.fetch.bind(window);
                    window.fetch = (input, options = {}) => {
                      const method = (options.method || input?.method || 'GET').toUpperCase();
                      if (method !== 'GET' && method !== 'HEAD') window.__uc3PresentationMutations.push(method);
                      return runFetch(input, options);
                    };
                    """);
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click()");
            assertEquals(expectedIds, renderedLessonIds(cdp));
            assertSolvingTaskGeometry(solvingTaskGeometry(cdp), 1600, 900);
            captureWorkbenchScreenshot(cdp, "uc3-solving-1600.png");
            cdp.evaluate("document.querySelector('#toggle-run-detail').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#run-secondary').hidden && document.querySelector('#cancel-run')?.getBoundingClientRect().bottom <= innerHeight && document.querySelector('#workbench-task-area')?.textContent.includes('PT30S')"),
                    "UC-3 G2: collapsed secondary detail cannot hide status, limit or cancellation");
            cdp.evaluate("document.querySelector('#toggle-run-detail').click()");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            assertSolvingTaskGeometry(solvingTaskGeometry(cdp), 1280, 800);
            captureWorkbenchScreenshot(cdp, "uc3-solving-1280.png");
            cdp.evaluate("document.querySelector('#toggle-inspector').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector').hidden && document.querySelector('#inspector-summary')?.textContent.includes('Declared lesson 960') && document.querySelector('.canvas-region').getBoundingClientRect().width > 1000"),
                    "UC-3 G3: inspector collapse retains selection and widens the running canvas");
            cdp.evaluate("document.querySelector('#reopen-inspector').click()");
            assertTrue(browserTrue(cdp, "!document.querySelector('#workbench-inspector').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'"));
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1279)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && document.querySelector('#workbench-task-area').getBoundingClientRect().top >= document.querySelector('#workbench-inspector').getBoundingClientRect().bottom && document.documentElement.scrollWidth <= innerWidth"),
                    "UC-3 G2/RULE-5: 1279px inspector stacks between canvas and task area");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 701)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", false));
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector').getBoundingClientRect().top >= document.querySelector('.canvas-region').getBoundingClientRect().bottom && document.querySelector('#workbench-task-area #cancel-run') && document.documentElement.scrollWidth <= innerWidth"),
                    "UC-3 G2/RULE-5: the last editing width retains a wide stacked task area");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 700)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", false));
            awaitBrowserCondition(cdp, "document.querySelector('.focused-schedule') !== null && document.querySelector('#workbench-modes') === null && document.querySelector('#cancel-run, #toggle-run-detail, #workbench-task-area') === null");
            assertTrue(browserTrue(cdp, "document.body.innerText.includes('Repair generation · running') && document.body.innerText.includes('Accepted baseline remains current') && document.documentElement.scrollWidth <= innerWidth"),
                    "UC-3 G6/RULE-12: the 700px agenda names the true lifecycle and is read-only");
            assertEquals(running, storedWorkspaceDocument(), "UC-3 G6: responsive inspection cannot mutate the run");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 390)
                    .put("height", 844).put("deviceScaleFactor", 1).put("mobile", false));
            assertTrue(browserTrue(cdp, "document.querySelector('.focused-schedule') !== null && document.querySelector('#cancel-run, #toggle-run-detail, #workbench-task-area') === null && document.documentElement.scrollWidth <= innerWidth"),
                    "UC-3 G6/G7: normative 390px agenda remains read-only without page overflow");
            captureWorkbenchScreenshot(cdp, "uc3-solving-390.png");
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            awaitBrowserCondition(cdp, "document.querySelector('#workbench-task-area #cancel-run') !== null");
            cdp.evaluate("document.querySelector('[data-range=DAY]').click()");
            assertEquals(expectedMondayIds, renderedLessonIds(cdp));
            cdp.evaluate("document.querySelector('[data-mode=CURRENT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#workbench-task-area #cancel-run') && document.querySelector('#run-secondary').hidden"));
            cdp.evaluate("document.querySelector('[data-mode=DRAFT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Frozen repair intent') && !document.querySelector('#solve-draft')"));
            cdp.evaluate("document.querySelector('[data-mode=SOLVING]').click()");
            cdp.evaluate("document.querySelector('#weekday').value='TUESDAY'; document.querySelector('#weekday').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "document.querySelector('#range-summary')?.textContent.includes('Tuesday') && !document.querySelector('#lesson-panel-title') && document.querySelector('#inspection-notice')?.textContent.includes('outside the represented Day') && document.querySelector('#workbench-task-area #cancel-run')"),
                    "UC-3 extension 3a: an unrepresentable selected lesson clears with an explanation while the run stays visible");
            assertEquals(running, storedWorkspaceDocument(), "UC-3 extension 3a: clearing selection cannot change the running workspace");
            cdp.evaluate("document.querySelector('#weekday').value='MONDAY'; document.querySelector('#weekday').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('[data-lesson-id=lesson-960]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'"));
            assertEquals(running, storedWorkspaceDocument());
            assertTrue(browserTrue(cdp, "window.__uc3PresentationMutations.length === 0"),
                    "UC-3 G3/RULE-2: range, inspector, task-detail, mode, and responsive actions issue no mutating request");
            assertEquals(1, processes.commands().stream().filter(command -> command.size() > 1 && "replan".equals(command.get(1))).count(),
                    "UC-3 G3: presentation actions cannot restart the scheduler");
            cdp.evaluate("document.querySelector('#cancel-run').click()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(20));
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            assertEquals("CANCELLED", storedWorkspaceDocument().path("lastRun").path("status").stringValue());

            processes.reset();
            processes.failure = WorkspaceRepairPlanningIT.RepairFailure.MISMATCHED;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            cdp.awaitText("School Kernel returned an unverified repair result.", Duration.ofSeconds(20));
            assertEquals(draft, assertDraftUnchangedBaseline(baseline));
            assertEquals("FAILED", storedWorkspaceDocument().path("lastRun").path("status").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area .conflict-list')?.textContent.includes('unverified repair result') && document.querySelector('#utilities')?.textContent.includes('unverified repair result') && !document.querySelector('[data-mode=PROPOSAL]')"));
            assertEquals(expectedMondayIds, renderedLessonIds(cdp));

            processes.reset();
            processes.verifiedFeasibleResult = this::verifiedNormativeRepairResult;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            cdp.awaitText("Repair proposal · feasible", Duration.ofSeconds(30));
            JsonNode proposed = storedWorkspaceDocument();
            assertEquals("REPAIR_PROPOSAL", storedLifecycle());
            assertEquals(baseline, proposed.path("acceptedBaseline"));
            assertEquals(draft, proposed.path("repairDraft"));
            assertFalse(proposed.has("run"));
            assertEquals("FEASIBLE", proposed.path("lastRun").path("status").stringValue());
            assertEquals("period-40", proposed.path("proposal").path("result").path("timetable").path("assignments").get(960).path("periodId").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=SOLVING]') && !document.querySelector('#workbench-task-area').hidden && document.querySelector('#workbench-task-area #proposal-context') && !document.querySelector('#workbench-inspector #proposal-context, #workbench-inspector #accept-repair') && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#cohort-filter')?.value === ''"),
                    "UC-3 main 6: independently verified Proposal opens its wide review area and retains the representable accepted context");
            captureWorkbenchScreenshot(cdp, "uc3-proposal-handoff-1280.png");
            JsonNode handoffGeometry = cdp.evaluateValue("""
                    (() => { const task=document.querySelector('#workbench-task-area');
                      const canvas=document.querySelector('.canvas-region'); const wrap=document.querySelector('.matrix-wrap');
                      const inspector=document.querySelector('#workbench-inspector');
                      task.scrollTop=task.scrollHeight;
                      const bounds=task.getBoundingClientRect();
                      return {taskHeight:bounds.height, taskBottom:bounds.bottom, taskTop:bounds.top,
                        canvasBottom:canvas.getBoundingClientRect().bottom,
                        inspectorLeft:inspector.getBoundingClientRect().left,
                        canvasRight:canvas.getBoundingClientRect().right,
                        visible:wrap.clientHeight, heading:document.querySelector('.matrix thead').getBoundingClientRect().height,
                        row:document.querySelector('.matrix tbody tr').getBoundingClientRect().height,
                        page:document.documentElement.scrollWidth,
                        decisionVisible:['#accept-repair','#revise-proposal','#discard-proposal'].every(selector => {
                          const action=document.querySelector(selector).getBoundingClientRect();
                          return action.top >= bounds.top && action.bottom <= bounds.bottom;
                        })}; })()
                    """).path("result").path("result").path("value");
            assertTrue(handoffGeometry.path("taskHeight").doubleValue() <= 800 * .35
                            && handoffGeometry.path("taskBottom").doubleValue() <= 800
                            && handoffGeometry.path("taskTop").doubleValue() >= handoffGeometry.path("canvasBottom").doubleValue()
                            && handoffGeometry.path("inspectorLeft").doubleValue() >= handoffGeometry.path("canvasRight").doubleValue()
                            && handoffGeometry.path("visible").doubleValue() >= handoffGeometry.path("heading").doubleValue() + handoffGeometry.path("row").doubleValue()
                            && handoffGeometry.path("page").doubleValue() <= 1281
                            && handoffGeometry.path("decisionVisible").booleanValue(),
                    "UC-3 main 6/RULE-5: verified handoff keeps the review task and its decisions reachable below the canvas: " + handoffGeometry);
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    private static JsonNode solvingTaskGeometry(Cdp cdp) throws Exception {
        return cdp.evaluateValue("""
                (() => { const task=document.querySelector('#workbench-task-area');
                  const canvas=document.querySelector('.canvas-region'); const wrap=document.querySelector('.matrix-wrap');
                  const inspector=document.querySelector('#workbench-inspector');
                  const cancel=document.querySelector('#cancel-run').getBoundingClientRect();
                  return {taskHeight:task.getBoundingClientRect().height, taskTop:task.getBoundingClientRect().top,
                    taskBottom:task.getBoundingClientRect().bottom, canvasBottom:canvas.getBoundingClientRect().bottom,
                    inspectorLeft:inspector.getBoundingClientRect().left, canvasRight:canvas.getBoundingClientRect().right,
                    row:document.querySelector('.week-matrix tbody tr').getBoundingClientRect().height,
                    heading:document.querySelector('.week-matrix thead').getBoundingClientRect().height,
                    visible:wrap.clientHeight, page:document.documentElement.scrollWidth,
                    cancelBottom:cancel.bottom, cancelRight:cancel.right}; })()
                """).path("result").path("result").path("value");
    }

    private static void assertSolvingTaskGeometry(JsonNode geometry, int width, int height) {
        assertTrue(geometry.path("taskHeight").doubleValue() <= height * .35,
                "UC-3 G2: task area is at most 35% of the viewport: " + geometry);
        assertTrue(geometry.path("taskBottom").doubleValue() <= height,
                "UC-3 G2: task remains visible with the canvas: " + geometry);
        assertTrue(geometry.path("taskTop").doubleValue() >= geometry.path("canvasBottom").doubleValue(),
                "UC-3 G2: task never overlays timetable cells: " + geometry);
        assertTrue(geometry.path("inspectorLeft").doubleValue() >= geometry.path("canvasRight").doubleValue(),
                "UC-3 G2: inspector remains beside the canvas: " + geometry);
        assertTrue(geometry.path("visible").doubleValue() >= geometry.path("heading").doubleValue() + geometry.path("row").doubleValue(),
                "UC-3 G2: a complete class row and time heading are visible: " + geometry);
        assertTrue(geometry.path("page").doubleValue() <= width + 1 && geometry.path("cancelRight").doubleValue() <= width + 1
                        && geometry.path("cancelBottom").doubleValue() <= height,
                "UC-3 G2: no page overflow or clipped cancellation: " + geometry);
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
            assertTrue(browserTrue(cdp, "document.querySelector('.workbench-layout .matrix-wrap [data-lesson-id=lesson-0][data-comparison-side=accepted]') !== null && document.querySelector('.workbench-layout .matrix-wrap [data-lesson-id=lesson-0][data-comparison-side=proposed]') !== null"),
                    "UC-4 main/1a: accepted origin and proposed destination must coexist on the whole-school canvas");
            String durable = storedDocument();
            cdp.evaluate("document.querySelector('#teacher-filter').value='teacher-0'; document.querySelector('#teacher-filter').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "!document.querySelector('[data-lesson-id=lesson-0][data-comparison-side=accepted]').hidden && !document.querySelector('[data-lesson-id=lesson-0][data-comparison-side=proposed]').hidden && document.querySelector('#represented-lesson-count').textContent.includes('60')"),
                    "UC-4 extension 3a: filtering must retain both representations and count unique lesson IDs");
            cdp.evaluate("document.querySelector('#reset-view').click(); document.querySelector('[data-lesson-id=lesson-0][data-comparison-side=proposed]').click()");
            assertTrue(browserTrue(cdp, "document.querySelectorAll('[data-lesson-id=lesson-0].selected').length === 2 && document.querySelector('.comparison-details')?.textContent.includes('Declared period 0') && document.querySelector('.comparison-details')?.textContent.includes('Declared period 1')"),
                    "UC-4 G1/G5: either side selects one identity and exposes both exact periods");
            cdp.evaluate("document.querySelector('#toggle-inspector').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-inspector').hidden && document.querySelector('#inspector-summary').textContent.includes('Declared lesson 0')"));
            cdp.evaluate("document.querySelector('#reopen-inspector').click(); document.querySelector('[data-range=DAY]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#workbench-inspector .before-after')?.textContent.includes('Declared period 1')"));
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click(); document.querySelector('[data-open-focus=cohortId]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.focused-schedule [data-lesson-id=lesson-0][data-comparison-side=accepted]') !== null && document.querySelector('.focused-schedule [data-lesson-id=lesson-0][data-comparison-side=proposed]') !== null"));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.workbench-layout .matrix-wrap [data-lesson-id=lesson-0]') !== null"));
            cdp.evaluate("document.querySelector('#show-unchanged').click()");
            assertTrue(cdp.awaitText("Accepted and unchanged · not included in change totals", Duration.ofSeconds(5))
                    .contains("Old assignment"));
            cdp.evaluate("document.querySelector('[data-review-lesson=lesson-0]').click()");
            assertTrue(cdp.awaitText("Proposed change · not current", Duration.ofSeconds(5)).contains("Proposal impact review"));
            assertEquals(durable, storedDocument(), "UC-4 G6: comparison, filter, focus, and inspector actions are presentation-only");
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

    @Test
    @DisplayName("UC-4 extensions 1b/1c/2a/3a/3b/4a/6c: comparison shapes and narrow read-only agenda in real browser")
    void comparesOneSidedAndSameSlotChangesInRealBrowser() throws Exception {
        ObjectNode document = comparisonShapeDocument();
        storeAccepted(document);
        jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=10 WHERE workspace_id=1").update();
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
            String rendered = cdp.awaitText("Unique changed lessons", Duration.ofSeconds(15));
            assertTrue(rendered.contains("103"), "overlapping category membership counts each changed ID once");
            assertTrue(rendered.contains("Teacher changes"));
            assertTrue(browserTrue(cdp, "document.querySelector('[data-category=teacherChanges] span')?.textContent === '0' && document.querySelector('[data-category=forcedMoves] span')?.textContent === '1'"),
                    "empty categories and overlapping explanations retain independent meanings");
            assertTrue(browserTrue(cdp, "[...document.querySelectorAll('.proposal-facts div')].some(row => row.textContent.includes('Protected accepted assignments') && row.textContent.includes('1'))"));
            assertTrue(browserTrue(cdp, "document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-100]').length === 1 && document.querySelector('[data-lesson-id=lesson-100]')?.dataset.comparisonSide === 'combined' && document.querySelector('[data-lesson-id=lesson-101]')?.dataset.comparisonSide === 'accepted' && document.querySelector('[data-lesson-id=lesson-added]')?.dataset.comparisonSide === 'proposed'"),
                    "UC-4 extensions 1b/1c: same-slot change, cancellation and addition have exactly their existing sides");
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-100]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('Room 1') && document.querySelector('.comparison-details')?.textContent.includes('room-2 (Name unavailable)')"),
                    "UC-4 4a: missing proposal display name retains the stable room ID and unavailable-name cue");
            cdp.evaluate("document.querySelector('#room-filter').value='room-2'; document.querySelector('#room-filter').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "!document.querySelector('[data-lesson-id=lesson-100]').hidden && document.querySelector('[data-lesson-id=lesson-100]')?.textContent.includes('Proposed-side match') && document.querySelector('#represented-lesson-count')?.textContent.includes('61')"),
                    "UC-4 3a: proposal-only resource filter retains the changed lesson as one identity");
            cdp.evaluate("document.querySelector('#reset-view').click(); document.querySelector('[data-lesson-id=lesson-101]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('Not present') && document.querySelector('.comparison-details')?.textContent.includes('Cancellations')"));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-added]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('Additions') && document.querySelector('.comparison-details')?.textContent.includes('Not present')"));
            cdp.evaluate("document.querySelector('[data-mode=CURRENT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#inspection-notice')?.textContent.includes('not present') && !document.querySelector('#lesson-panel-title')"),
                    "UC-4 mode navigation clears a proposal-only identity without inventing it in Current");
            cdp.evaluate("document.querySelector('[data-mode=PROPOSAL]').click()");
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-200]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('Accepted and unchanged') && document.querySelector('.comparison-details')?.textContent.includes('Declared lesson 200')"));
            ObjectNode narrow = JSON.createObjectNode().put("width", 390).put("height", 844)
                    .put("deviceScaleFactor", 1).put("mobile", true);
            cdp.command("Emulation.setDeviceMetricsOverride", narrow);
            String agenda = cdp.awaitText("Read-only focused schedule", Duration.ofSeconds(5));
            assertTrue(agenda.contains("Repair proposal") && agenda.contains("Accepted origin") && agenda.contains("Proposed destination"));
            assertFalse(browserTrue(cdp, "document.querySelector('#accept-repair, #revise-proposal, #discard-proposal, .matrix-wrap') !== null"),
                    "UC-4 6c: narrow proposal agenda must not expose mutation or full-desktop controls");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument(), "comparison and narrow viewing cannot mutate durable workspace data");
    }

    @Test
    @DisplayName("UC-4 C-1: focusing either teacher or room retains the joined accepted/proposed lesson")
    void retainsBothSidesInFocusedResourceSchedulesInRealBrowser() throws Exception {
        ObjectNode document = comparisonShapeDocument();
        ObjectNode proposal = (ObjectNode) document.path("proposal");
        ObjectNode result = (ObjectNode) proposal.path("result");
        ((ObjectNode) result.path("timetable").path("assignments").get(0)).put("roomId", "room-90");
        ((ObjectNode) result.path("timetable").path("assignments").get(1)).put("teacherId", "teacher-21")
                .put("subjectId", "subject-1").put("cohortId", "cohort-21");
        ((ArrayNode) result.path("changeReport").path("teacherChanges")).addObject().put("lessonId", "lesson-1");
        proposal.set("review", reviews.create(document.path("acceptedBaseline"), document.path("repairDraft"),
                proposal.path("definition"), result));
        storeAccepted(document);
        jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=10 WHERE workspace_id=1").update();
        String durable = storedDocument();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Unique changed lessons", Duration.ofSeconds(15));
            for (String[] focus : new String[][] {
                    { "teacherId", "teacher-0", "teacher-21", "lesson-1" },
                    { "roomId", "room-0", "room-90", "lesson-0" } }) {
                String filter = focus[0].equals("teacherId") ? "#teacher-filter" : "#room-filter";
                cdp.evaluate("document.querySelector('" + filter + "').value='" + focus[1] + "'; document.querySelector('" + filter + "').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('[data-open-focus=" + focus[0] + "]').click()");
                for (String resource : new String[] { focus[1], focus[2] }) {
                    cdp.evaluate("document.querySelector('#focus-entity').value='" + resource + "'; document.querySelector('#focus-entity').dispatchEvent(new Event('change',{bubbles:true}))");
                    String selector = ".focused-schedule [data-lesson-id=" + focus[3] + "]";
                    assertTrue(browserTrue(cdp, "document.querySelectorAll('" + selector + "').length === 2 && document.querySelector('" + selector + "[data-comparison-side=accepted]') && document.querySelector('" + selector + "[data-comparison-side=proposed]') && [...document.querySelectorAll('" + selector + "')].some(item => item.textContent.includes('Related comparison side'))"),
                            "UC-4 C-1: old/new " + focus[0] + " focus " + resource + " retains both exact sides without calling both current");
                }
                cdp.evaluate("document.querySelector('#return-matrix').click(); document.querySelector('#reset-view').click()");
            }
            cdp.evaluate("document.querySelector('#subject-investigation').value='subject-1'; document.querySelector('#subject-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "[...document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-1]')].every(el=>!el.hidden && el.textContent.includes('Proposed-side match'))"),
                    "UC-4 3a: proposed-only subject investigation retains accepted origin");
            cdp.evaluate("document.querySelector('#subject-investigation').value='subject-0'; document.querySelector('#subject-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "[...document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-1]')].every(el=>!el.hidden && el.textContent.includes('Accepted-side match'))"),
                    "UC-4 3a: accepted-only subject investigation retains proposed destination");
            cdp.evaluate("document.querySelector('#reset-view').click(); document.querySelector('#cohort-filter').value='cohort-21'; document.querySelector('#cohort-filter').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "[...document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-1]')].every(el=>!el.hidden && el.textContent.includes('Proposed-side match'))"),
                    "UC-4 3a: one-sided class filter retains both placements: " + cdp.evaluateValue("[...document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-1]')].map(el=>[el.dataset.comparisonSide,el.hidden,el.textContent,el.closest('tr')?.hidden])"));
            cdp.evaluate("document.querySelector('#reset-view').click(); document.querySelector('#lesson-search').value='Room 90'; document.querySelector('#lesson-search').dispatchEvent(new Event('input',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "[...document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-0]')].every(el=>!el.hidden && el.textContent.includes('Proposed-side match')) && document.querySelector('#search-summary')?.textContent.includes('1')"),
                    "UC-4 3a: a proposed-only search name highlights exactly one joined identity and retains the accepted origin");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(durable, storedDocument());
    }

    @Test
    @DisplayName("UC-4 G-1/G-2 and RULE-9/12: full verified 1,000-lesson repair has exact two-sided overlay, review and durable decision")
    void reviewsIndependentlyVerifiedNormativeRepairInRealBrowser() throws Exception {
        ObjectNode document = investigationScaleDocument();
        JsonNode baseline = document.path("acceptedBaseline").deepCopy();
        assertEquals(60, baseline.path("definition").path("cohorts").size());
        assertEquals(100, baseline.path("definition").path("teachers").size());
        assertEquals(100, baseline.path("definition").path("rooms").size());
        assertEquals(60, baseline.path("definition").path("periods").size());
        assertEquals(1_000, baseline.path("result").path("timetable").path("assignments").size());
        assertEquals("Subject Zero with a deliberately long authoritative display name for timetable tiles",
                baseline.path("definition").path("subjects").get(0).path("displayName").stringValue());
        assertEquals("Room Sixteen with a deliberately long authoritative display name for timetable tiles",
                baseline.path("definition").path("rooms").get(16).path("displayName").stringValue());
        assertEquals(document.path("timetableRevision").stringValue(), verifier.verify(new ImportDocuments(
                baseline.path("definition"), baseline.path("result"), null,
                ImportDocuments.ImportMode.ACCEPTED_BASELINE)).timetableRevision());
        storeAccepted(document);
        AtomicReference<ObjectNode> independentlyVerified = new AtomicReference<>();
        processes.verifiedFeasibleResult = arguments -> {
            ObjectNode result = verifiedNormativeRepairResult(arguments);
            independentlyVerified.set(result.deepCopy());
            return result;
        };
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('#repair-resource').value='teacher-16'; document.querySelector('[name=period][value=period-0]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(20));
            assertEquals(Set.of("lesson-960"), jsonStrings(storedWorkspaceDocument().path("repairDraft").path("directEffectLessonIds")));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-500]').click(); document.querySelector('[name=lesson-dimension][value=ROOM]').checked=true; document.querySelector('#apply-pin').click()");
            cdp.awaitText("Accepted room pinned", Duration.ofSeconds(15));
            JsonNode draft = assertDraftUnchangedBaseline(baseline).deepCopy();
            assertEquals("lesson-500", draft.path("intent").path("pins").get(0).path("lessonId").stringValue());
            assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), draft.path("intent").path("pins").get(0).path("roomSources"));
            assertTrue(draft.path("readyToSolve").booleanValue());
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-0]').click(); document.querySelector('[data-range=DAY]').click()");
            cdp.awaitText("Day · Monday", Duration.ofSeconds(5));
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            try {
                cdp.awaitText("Repair proposal · feasible", Duration.ofSeconds(30));
            } catch (AssertionError failure) {
                throw new AssertionError("The normative candidate must pass the independent production verifier: "
                        + processes.verifiedFeasibleFailure, processes.verifiedFeasibleFailure == null ? failure : processes.verifiedFeasibleFailure);
            }
            cdp.evaluate("""
                    window.__proposalReviewMutations = [];
                    const proposalFetch = window.fetch.bind(window);
                    window.fetch = (input, options = {}) => {
                      const method = (options.method || input?.method || 'GET').toUpperCase();
                      if (method !== 'GET' && method !== 'HEAD') {
                        window.__proposalReviewMutations.push({method, path: new URL(
                          typeof input === 'string' ? input : input.url, location.href).pathname});
                      }
                      return proposalFetch(input, options);
                    };
                    """);
            JsonNode stored = storedWorkspaceDocument();
            assertEquals("REPAIR_PROPOSAL", storedLifecycle());
            assertEquals(baseline, stored.path("acceptedBaseline"));
            assertEquals(draft, stored.path("repairDraft"));
            JsonNode proposal = stored.path("proposal");
            assertEquals(independentlyVerified.get(), proposal.path("result"),
                    "the real planner must persist only the result independently verified by the production kernel");
            JsonNode review = proposal.path("review");
            assertEquals(2, review.path("uniqueChangedLessonCount").intValue());
            assertEquals(1, review.path("directEffectChangedCount").intValue());
            assertEquals(1, review.path("rippleEffectCount").intValue());
            assertEquals(JSON.readTree("[\"lesson-960\"]"), review.path("directEffectLessonIds"));
            assertEquals(JSON.readTree("""
                    [{"id":"additions","count":0,"lessonIds":[]},{"id":"cancellations","count":0,"lessonIds":[]},
                    {"id":"teacherChanges","count":0,"lessonIds":[]},{"id":"forcedMoves","count":0,"lessonIds":[]},
                    {"id":"periodMoves","count":1,"lessonIds":["lesson-960"]},
                    {"id":"roomOnlyMoves","count":1,"lessonIds":["lesson-0"]}]
                    """), review.path("categories"));
            assertEquals(JSON.readTree("""
                    [{"lessonId":"lesson-0","directEffect":false,"rippleEffect":true,"categories":["roomOnlyMoves"],
                    "old":{"subjectId":"subject-0","cohortId":"cohort-0","teacherId":"teacher-0","periodId":"period-0","roomId":"room-0"},
                    "proposed":{"subjectId":"subject-0","cohortId":"cohort-0","teacherId":"teacher-0","periodId":"period-0","roomId":"room-50"},"changedDimensions":["roomId"]},
                    {"lessonId":"lesson-960","directEffect":true,"rippleEffect":false,"categories":["periodMoves"],
                    "old":{"subjectId":"subject-0","cohortId":"cohort-16","teacherId":"teacher-16","periodId":"period-0","roomId":"room-16"},
                    "proposed":{"subjectId":"subject-0","cohortId":"cohort-16","teacherId":"teacher-16","periodId":"period-40","roomId":"room-16"},"changedDimensions":["periodId"]}]
                    """), review.path("changedLessons"));
            assertEquals(JSON.readTree("""
                    {"classes":[{"id":"cohort-0","context":"BOTH","lessonIds":["lesson-0"]},
                    {"id":"cohort-16","context":"BOTH","lessonIds":["lesson-960"]}],
                    "teachers":[{"id":"teacher-0","context":"BOTH","lessonIds":["lesson-0"]},
                    {"id":"teacher-16","context":"BOTH","lessonIds":["lesson-960"]}],
                    "rooms":[{"id":"room-0","context":"OLD","lessonIds":["lesson-0"]},
                    {"id":"room-16","context":"BOTH","lessonIds":["lesson-960"]},
                    {"id":"room-50","context":"PROPOSED","lessonIds":["lesson-0"]}],
                    "days":[{"id":"MONDAY","context":"BOTH","lessonIds":["lesson-0","lesson-960"]},
                    {"id":"THURSDAY","context":"PROPOSED","lessonIds":["lesson-960"]}]}
                    """), review.path("groupings"));
            assertTrue(browserTrue(cdp, "document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent.includes('Declared lesson 0') && document.querySelector('.impact-totals')?.textContent.includes('1')"));
            assertEquals(JSON.readTree("""
                    [["additions",0],["cancellations",0],["teacherChanges",0],["forcedMoves",0],
                    ["periodMoves",1],["roomOnlyMoves",1]]
                    """), cdp.evaluateValue("[...document.querySelectorAll('.review-category')].map(c=>[c.dataset.category,Number(c.querySelector('h4 span').textContent)])")
                    .path("result").path("result").path("value"));
            assertEquals(JSON.readTree("""
                    [["Unique changed lessons","2"],["Protected accepted assignments","1"],
                    ["Termination reason","TIME_LIMIT"],["Execution limit","PT30S"],["Elapsed time","2004 ms"]]
                    """), cdp.evaluateValue("[...document.querySelectorAll('.proposal-facts div')].map(row=>[row.querySelector('dt').textContent,row.querySelector('dd').textContent])")
                    .path("result").path("result").path("value"));
            assertEquals(JSON.readTree("[\"Direct effects of your intent: 1\",\"Solver ripple effects: 1\"]"),
                    cdp.evaluateValue("[...document.querySelectorAll('.impact-totals span')].map(el=>el.textContent)")
                            .path("result").path("result").path("value"));
            assertEquals(JSON.readTree("[\"Protected accepted assignments · 1\",\"By class · 2\",\"By teacher · 2\",\"By room · 3\",\"By day · 2\"]"),
                    cdp.evaluateValue("[...document.querySelectorAll('.review-groups summary')].map(el=>el.textContent)")
                            .path("result").path("result").path("value"));
            cdp.evaluate("document.querySelector('#teacher-investigation').value='teacher-16'; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "document.querySelector('#teacher-ribbon-title')?.textContent.includes('Proposed teacher load by period') && [...document.querySelectorAll('.teacher-ribbon li')].find(el=>el.querySelector('strong')?.textContent==='Declared period 0')?.querySelector('span')?.textContent==='Unavailable'"),
                    "RULE-6: proposal Monday availability must use the successor definition");
            cdp.evaluate("document.querySelector('[data-mode=CURRENT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#teacher-ribbon-title')?.textContent.includes('Teacher load by period') && !document.querySelector('#teacher-ribbon-title')?.textContent.includes('Proposed') && [...document.querySelectorAll('.teacher-ribbon li')].find(el=>el.querySelector('strong')?.textContent==='Declared period 0')?.querySelector('span')?.textContent==='Assigned'"),
                    "RULE-6: accepted Monday load must still use the accepted definition and assignment");
            cdp.evaluate("document.querySelector('[data-mode=PROPOSAL]').click(); document.querySelector('#teacher-investigation').value=''; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click()");
            cdp.awaitText("Complete recurring Week", Duration.ofSeconds(5));
            var expectedTiles = new ArrayList<String>();
            for (int i = 0; i < 1_000; i++) {
                String id = "lesson-" + i;
                if (i == 960) { expectedTiles.add(id + ":accepted"); expectedTiles.add(id + ":proposed"); }
                else if (i == 0) expectedTiles.add(id + ":combined");
                else expectedTiles.add(id + ":unchanged");
            }
            expectedTiles.sort(String::compareTo);
            ArrayNode expectedTileValues = JSON.createArrayNode();
            expectedTiles.forEach(expectedTileValues::add);
            assertEquals(expectedTileValues, cdp.evaluateValue("[...document.querySelectorAll('.matrix-wrap [data-lesson-id]')].map(el=>el.dataset.lessonId+':'+el.dataset.comparisonSide).sort()")
                    .path("result").path("result").path("value"), "all 1,000 stable IDs and exactly one additional period-move side must be present");
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=accepted]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('period-0') && document.querySelector('.comparison-details')?.textContent.includes('period-40') && document.querySelector('.comparison-details')?.textContent.includes('room-16') && document.querySelector('.comparison-details')?.textContent.includes('Directly affected')"));
            assertDisplayedComparisonSides(cdp, JSON.readTree("""
                    [["Old assignment",[["Weekday","Monday",null],
                    ["Subject","Subject Zero with a deliberately long authoritative display name for timetable tiles","subject-0"],
                    ["Class","Class Sixteen with a deliberately long authoritative display name for timetable tiles","cohort-16"],
                    ["Teacher","Teacher Sixteen with a deliberately long authoritative display name for timetable tiles","teacher-16"],
                    ["Period · Changed","Declared period 0","period-0"],
                    ["Room","Room Sixteen with a deliberately long authoritative display name for timetable tiles","room-16"]]],
                    ["Proposed assignment",[["Weekday","Thursday",null],
                    ["Subject","Subject Zero with a deliberately long authoritative display name for timetable tiles","subject-0"],
                    ["Class","Class Sixteen with a deliberately long authoritative display name for timetable tiles","cohort-16"],
                    ["Teacher","Teacher Sixteen with a deliberately long authoritative display name for timetable tiles","teacher-16"],
                    ["Period · Changed","Declared period 40","period-40"],
                    ["Room","Room Sixteen with a deliberately long authoritative display name for timetable tiles","room-16"]]]]
                    """));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-0][data-comparison-side=combined]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('room-0') && document.querySelector('.comparison-details')?.textContent.includes('room-50') && document.querySelector('.comparison-details')?.textContent.includes('Solver ripple')"));
            assertDisplayedComparisonSides(cdp, JSON.readTree("""
                    [["Old assignment",[["Weekday","Monday",null],
                    ["Subject","Subject Zero with a deliberately long authoritative display name for timetable tiles","subject-0"],
                    ["Class","Class 0","cohort-0"],["Teacher","Teacher 0","teacher-0"],
                    ["Period","Declared period 0","period-0"],["Room · Changed","Room 0","room-0"]]],
                    ["Proposed assignment",[["Weekday","Monday",null],
                    ["Subject","Subject Zero with a deliberately long authoritative display name for timetable tiles","subject-0"],
                    ["Class","Class 0","cohort-0"],["Teacher","Teacher 0","teacher-0"],
                    ["Period","Declared period 0","period-0"],["Room · Changed","Room 50","room-50"]]]]
                    """));
            assertTrue(browserTrue(cdp, "document.querySelector('#review-selection .before-after')?.textContent.includes('Room 0') && document.querySelector('#review-selection .before-after')?.textContent.includes('Room 50') && document.querySelector('#review-selection')?.textContent.includes('Solver ripple effects')"),
                    "UC-4 main 4: the wide review area repeats the exact selected before/after and effect explanation");
            cdp.evaluate("document.querySelector('[data-category=periodMoves] [data-review-lesson=lesson-960][data-review-side=accepted]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#review-selection')?.textContent.includes('Review target: Accepted origin · current') && document.querySelector('#review-selection .before-after')?.textContent.includes('Declared period 40')"),
                    "UC-4 main 3-4: category origin navigation selects one stable identity and keeps the destination details in the task area");
            cdp.evaluate("document.querySelector('[data-range=DAY]').click(); document.querySelector('#period-focus').value='period-0'; document.querySelector('#period-focus').dispatchEvent(new Event('change',{bubbles:true}))");
            cdp.evaluate("document.querySelector('[data-category=periodMoves] [data-review-lesson=lesson-960][data-review-side=proposed]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#range-summary')?.textContent.includes('Thursday') && document.querySelector('#period-focus')?.value === '' && !document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=proposed]')?.hidden && document.querySelector('#inspection-notice')?.textContent.includes('Cleared Period focus')"),
                    "UC-4 extension 3a: destination navigation clears an origin-only Day period column");
            cdp.evaluate("document.querySelector('#review-selection [data-review-lesson=lesson-960][data-review-side=accepted]').click()");
            cdp.evaluate("document.querySelector('#room-filter').value='room-50'; document.querySelector('#room-filter').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#lesson-search').value='Room 50'; document.querySelector('#lesson-search').dispatchEvent(new Event('input',{bubbles:true})); document.querySelector('#teacher-investigation').value='teacher-16'; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "document.querySelector('#room-filter')?.value === 'room-50' && document.querySelector('#range-summary')?.textContent.includes('Monday')"));
            cdp.evaluate("document.querySelector('[data-category=periodMoves] [data-review-lesson=lesson-960][data-review-side=proposed]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#range-summary')?.textContent.includes('Thursday') && document.querySelector('#room-filter')?.value === '' && document.querySelector('#lesson-search')?.value === 'Room 50' && document.querySelector('#teacher-investigation')?.value === 'teacher-16' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#review-selection')?.textContent.includes('Review target: Proposed destination · not current') && !document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=proposed]')?.hidden && document.querySelector('#inspection-notice')?.textContent.includes('Selected Thursday') && document.querySelector('#inspection-notice')?.textContent.includes('Cleared Room filter')"),
                    "UC-4 extension 3a: destination navigation changes only Day and excluding room filter, with search and highlight retained");
            cdp.evaluate("document.querySelector('#review-selection [data-review-lesson=lesson-960][data-review-side=accepted]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#range-summary')?.textContent.includes('Monday') && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#review-selection')?.textContent.includes('Review target: Accepted origin · current') && document.querySelector('#lesson-search')?.value === 'Room 50'"),
                    "UC-4 main 3: review detail can navigate back to accepted origin without losing the investigation");
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click(); document.querySelector('#cohort-filter').value='cohort-0'; document.querySelector('#cohort-filter').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('[data-open-focus=cohortId]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.focused-schedule') !== null"),
                    "UC-4 focused entry must open the selected class schedule");
            cdp.evaluate("document.querySelectorAll('.review-groups')[1].open=true; document.querySelectorAll('.review-groups')[1].querySelector('[data-review-lesson=lesson-960][data-review-side=proposed]').click()");
            assertTrue(browserTrue(cdp, "!document.querySelector('.focused-schedule') && document.querySelector('.matrix-wrap [data-lesson-id=lesson-960][data-comparison-side=proposed]') && document.querySelector('#review-selection')?.textContent.includes('Review target: Proposed destination · not current') && document.querySelector('#lesson-search')?.value === 'Room 50' && document.querySelector('#inspection-notice')?.textContent.includes('Returned to the whole-school canvas')"),
                    "UC-4 extension 3a: grouping navigation leaves only an excluding focus and retains the search and stable lesson identity");
            cdp.evaluate("document.querySelector('[data-open-focus=cohortId]').click(); document.querySelector('[data-category=periodMoves] [data-review-lesson=lesson-960][data-review-side=accepted]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.focused-schedule [data-lesson-id=lesson-960][data-comparison-side=accepted].selected[aria-current=true] .selected-label')?.textContent === 'Selected' && document.querySelector('#review-selection')?.textContent.includes('Review target: Accepted origin · current') && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'"),
                    "UC-4 extension 3a: an in-focus accepted origin remains in the focused schedule with an explicit selected cue");
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-search')?.value === 'Room 50'"),
                    "UC-4 G4: focused review navigation and return retain the search");
            cdp.evaluate("document.querySelector('#reset-view').click(); document.querySelector('[data-range=WEEK]').click()");
            cdp.evaluate("document.querySelector('[data-review-lesson=lesson-500]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('Accepted and unchanged') && document.querySelector('.comparison-details')?.textContent.includes('Accepted room pinned')"));
            assertTrue(browserTrue(cdp, "document.querySelector('.matrix-wrap [data-lesson-id=lesson-500] .pin-label')?.textContent === 'Both pinned' && document.querySelector('.matrix-wrap [data-lesson-id=lesson-500]')?.getAttribute('aria-label').includes('Accepted period pinned · Accepted room pinned')"),
                    "UC-4 G5: compact Week protection remains visible and exposes its full accessible meaning: "
                            + cdp.evaluateValue("(() => { const tile=document.querySelector('.matrix-wrap [data-lesson-id=lesson-500]'); return {cue:tile?.querySelector('.pin-label')?.textContent, name:tile?.getAttribute('aria-label'), range:document.querySelector('#range-summary')?.textContent}; })()"));
            assertTrue(browserTrue(cdp, "document.querySelector('.matrix-wrap [data-lesson-id=lesson-500] strong')?.getBoundingClientRect().width >= 38 && document.querySelector('.matrix-wrap [data-lesson-id=lesson-500] .week-room')?.getBoundingClientRect().width > 0 && document.querySelector('.matrix-wrap [data-lesson-id=lesson-500]')?.getAttribute('aria-pressed') === 'true'"),
                    "UC-4 G5: a selected protected Week tile keeps subject and room visible with structural selection");
            assertEquals("room-8", proposal.path("result").path("timetable").path("assignments").get(500).path("roomId").stringValue());
            assertEquals("period-20", proposal.path("result").path("timetable").path("assignments").get(500).path("periodId").stringValue());
            assertEquals(stored, storedWorkspaceDocument(), "inspection and protected-lesson navigation must not change the accepted/draft/proposal bundle");
            for (int[] viewport : new int[][] { { 1600, 900 }, { 1280, 800 }, { 1279, 800 }, { 701, 844 } }) {
                cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", viewport[0])
                        .put("height", viewport[1]).put("deviceScaleFactor", 1).put("mobile", false));
                JsonNode geometry = cdp.evaluateValue("""
                        (() => { const task=document.querySelector('#workbench-task-area');
                          const canvas=document.querySelector('.canvas-region'); const wrap=document.querySelector('.matrix-wrap');
                          const inspector=document.querySelector('#workbench-inspector');
                          task.scrollTop=task.scrollHeight;
                          const bounds=task.getBoundingClientRect();
                          return {taskHeight:bounds.height, taskBottom:bounds.bottom, taskTop:bounds.top,
                            canvasBottom:canvas.getBoundingClientRect().bottom,
                            inspectorTop:inspector.getBoundingClientRect().top,
                            inspectorBottom:inspector.getBoundingClientRect().bottom,
                            inspectorLeft:inspector.getBoundingClientRect().left,
                            canvasRight:canvas.getBoundingClientRect().right,
                            visible:wrap.clientHeight, heading:document.querySelector('.week-matrix thead').getBoundingClientRect().height,
                            row:document.querySelector('.week-matrix tbody tr').getBoundingClientRect().height,
                            page:document.documentElement.scrollWidth,
                            taskClientWidth:task.clientWidth, taskScrollWidth:task.scrollWidth,
                            decisions:['#accept-repair','#revise-proposal','#discard-proposal'].every(selector => {
                              const action=document.querySelector(selector).getBoundingClientRect();
                              return action.top >= bounds.top && action.bottom <= bounds.bottom
                                && action.left >= bounds.left && action.right <= bounds.right;
                            })}; })()
                        """).path("result").path("result").path("value");
                assertTrue(geometry.path("taskHeight").doubleValue() <= viewport[1] * .35
                                && geometry.path("visible").doubleValue() >= geometry.path("heading").doubleValue() + geometry.path("row").doubleValue()
                                && geometry.path("page").doubleValue() <= viewport[0] + 1
                                && geometry.path("taskScrollWidth").doubleValue() <= geometry.path("taskClientWidth").doubleValue() + 1
                                && geometry.path("decisions").booleanValue(),
                        "UC-4 G3: Proposal review controls, headings and a full class row fit without horizontal page/task clipping at " + viewport[0] + ": " + geometry);
                if (viewport[0] >= 1280) {
                    assertTrue(geometry.path("taskBottom").doubleValue() <= viewport[1]
                                    && geometry.path("taskTop").doubleValue() >= geometry.path("canvasBottom").doubleValue()
                                    && geometry.path("inspectorLeft").doubleValue() >= geometry.path("canvasRight").doubleValue(),
                            "UC-4 G3: wide Proposal keeps canvas and inspector beside its below-canvas task at " + viewport[0] + ": " + geometry);
                    captureWorkbenchScreenshot(cdp, "uc4-proposal-" + viewport[0] + ".png");
                } else {
                    assertTrue(geometry.path("inspectorTop").doubleValue() >= geometry.path("canvasBottom").doubleValue()
                                    && geometry.path("taskTop").doubleValue() >= geometry.path("inspectorBottom").doubleValue(),
                            "UC-4 G3/RULE-5: Proposal inspector stacks between the canvas and wide task at " + viewport[0] + ": " + geometry);
                }
            }
            for (int[] viewport : new int[][] { { 700, 844 }, { 390, 844 } }) {
                cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", viewport[0])
                        .put("height", viewport[1]).put("deviceScaleFactor", 1).put("mobile", viewport[0] == 390));
                awaitBrowserCondition(cdp, "document.querySelector('.focused-schedule') !== null && document.querySelector('#workbench-task-area') === null");
                cdp.evaluate("document.querySelector('#focus-entity').value='cohort-16'; document.querySelector('#focus-entity').dispatchEvent(new Event('change',{bubbles:true}))");
                assertTrue(browserTrue(cdp, "document.body.innerText.includes('Repair proposal') && document.body.innerText.includes('Accepted baseline remains current') && document.body.innerText.includes('Read-only focused schedule') && document.querySelectorAll('.focused-schedule [data-lesson-id=lesson-960]').length === 2 && document.querySelector('.focused-schedule [data-lesson-id=lesson-960][data-comparison-side=accepted]')?.textContent.includes('Accepted origin') && document.querySelector('.focused-schedule [data-lesson-id=lesson-960][data-comparison-side=proposed]')?.textContent.includes('Proposed destination') && !document.querySelector('#accept-repair, #revise-proposal, #discard-proposal, #workbench-task-area, .matrix-wrap') && document.documentElement.scrollWidth <= innerWidth"),
                        "UC-4 extension 1b/G7: verified Proposal at " + viewport[0] + "px is a read-only accepted/proposed agenda without page overflow or decisions");
                assertEquals(stored, storedWorkspaceDocument(), "responsive Proposal reading must not change the durable workspace");
            }
            cdp.command("Emulation.setDeviceMetricsOverride", JSON.createObjectNode().put("width", 1280)
                    .put("height", 800).put("deviceScaleFactor", 1).put("mobile", false));
            awaitBrowserCondition(cdp, "document.querySelector('.matrix-wrap') !== null && document.querySelector('#workbench-task-area') !== null");
            JsonNode beforeCollapseScroll = cdp.evaluateValue("(() => { const matrix=document.querySelector('.matrix-wrap'); matrix.scrollTo(140,120); return {left:matrix.scrollLeft, top:matrix.scrollTop}; })()")
                    .path("result").path("result").path("value");
            assertTrue(beforeCollapseScroll.path("left").intValue() > 0 && beforeCollapseScroll.path("top").intValue() > 0,
                    "UC-4 G3: the Proposal matrix must have a real horizontal and vertical scroll position before collapse: " + beforeCollapseScroll);
            cdp.evaluate("document.querySelector('#collapse-proposal-task').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area').hidden && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 500'"));
            cdp.evaluate("document.querySelector('#reopen-proposal-task').click()");
            assertTrue(browserTrue(cdp, "!document.querySelector('#workbench-task-area').hidden && document.querySelector('#review-selection')?.textContent.includes('Declared lesson 500') && document.querySelector('.proposal-facts')?.textContent.includes('Unique changed lessons')"),
                    "UC-4 G3: task collapse and reopen retain selected protection and authoritative counts");
            assertEquals(beforeCollapseScroll, cdp.evaluateValue("(() => { const matrix=document.querySelector('.matrix-wrap'); return {left:matrix.scrollLeft, top:matrix.scrollTop}; })()")
                            .path("result").path("result").path("value"),
                    "UC-4 G3: task collapse and reopen retain the representable matrix scroll position");
            assertEquals(JSON.readTree("[]"), cdp.evaluateValue("window.__proposalReviewMutations")
                            .path("result").path("result").path("value"),
                    "UC-4 G4/RULE-2: review navigation, mode, search, filter, focus, responsive and task actions issue no mutating request");
            assertEquals(stored, storedWorkspaceDocument(), "UC-4 G4: review-only actions keep the exact accepted/Draft/Proposal document and version");
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-0]').click(); document.querySelector('[data-range=DAY]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent.includes('Declared lesson 0')"));
            cdp.evaluate("document.querySelector('#confirm-repair-accept').click()");
            assertEquals(JSON.readTree("[]"), cdp.evaluateValue("window.__proposalReviewMutations")
                            .path("result").path("result").path("value"),
                    "UC-4 G6: confirmation alone cannot send an acceptance request");
            cdp.evaluate("document.querySelector('#accept-repair').click()");
            cdp.awaitText("Accepted baseline · current timetable", Duration.ofSeconds(20));
            assertEquals(JSON.readTree("[{\"method\":\"POST\",\"path\":\"/api/proposal/accept\"}]"),
                    cdp.evaluateValue("window.__proposalReviewMutations").path("result").path("result").path("value"),
                    "UC-4 G4/G6: only the explicit confirmed acceptance issues a mutation request");
            JsonNode accepted = storedWorkspaceDocument();
            assertEquals("ACCEPTED_BASELINE", storedLifecycle());
            assertEquals(proposal.path("definition"), accepted.path("acceptedBaseline").path("definition"));
            assertEquals(proposal.path("result"), accepted.path("acceptedBaseline").path("result"));
            assertFalse(accepted.has("proposal")); assertFalse(accepted.has("repairDraft"));
            assertTrue(browserTrue(cdp, "document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent.includes('Declared lesson 0') && !document.querySelector('[data-mode=PROPOSAL]')"),
                    "accepted Monday and unchanged lesson-0 selection remain representable on the exact new Current baseline");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("UC-4 G-1: revise and discard two independently verified whole-school proposals without advancing Current")
    void revisesAndDiscardsVerifiedNormativeRepairInRealBrowser() throws Exception {
        processes.verifiedFeasibleResult = this::verifiedNormativeRepairResult;
        try (Cdp cdp = openWorkspaceBrowser()) {
            for (String decision : new String[] { "#revise-proposal", "#discard-proposal" }) {
                ObjectNode document = investigationScaleDocument();
                JsonNode baseline = document.path("acceptedBaseline").deepCopy();
                storeAccepted(document);
                cdp.command("Page.navigate", object("url", "http://localhost:" + port + "/workspace/"));
                cdp.awaitText("Complete school population", Duration.ofSeconds(15));
                cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('#repair-resource').value='teacher-16'; document.querySelector('[name=period][value=period-0]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
                cdp.awaitText("Repair draft · not current", Duration.ofSeconds(15));
                cdp.evaluate("document.querySelector('[data-lesson-id=lesson-500]').click(); document.querySelector('[name=lesson-dimension][value=ROOM]').checked=true; document.querySelector('#apply-pin').click()");
                cdp.awaitText("Accepted room pinned", Duration.ofSeconds(15));
                JsonNode draft = storedWorkspaceDocument().path("repairDraft").deepCopy();
                cdp.evaluate("document.querySelector('#solve-draft').click()");
                cdp.awaitText("Repair proposal · feasible", Duration.ofSeconds(30));
                JsonNode before = storedWorkspaceDocument();
                assertEquals(baseline, before.path("acceptedBaseline"));
                assertEquals(draft, before.path("repairDraft"));
                assertEquals(2, before.path("proposal").path("review").path("uniqueChangedLessonCount").intValue());
                assertEquals("sha256:", before.path("proposal").path("proposedTimetableRevision").stringValue().substring(0, 7));
                cdp.evaluate("document.querySelector('" + decision + "').click()");
                cdp.awaitText("Repair draft · not current", Duration.ofSeconds(10));
                JsonNode after = storedWorkspaceDocument();
                assertEquals("REPAIR_DRAFT", storedLifecycle());
                assertEquals(baseline, after.path("acceptedBaseline"));
                assertEquals(draft, after.path("repairDraft"));
                assertFalse(after.has("proposal"));
                assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL], #accept-repair')"));
                assertTrue(browserTrue(cdp, "!document.querySelector('#workbench-task-area')?.hidden && document.querySelector('#workbench-task-area #stage-repair-form') && document.querySelector('#workbench-task-area').textContent.includes('Teacher Sixteen') && document.querySelector('#workbench-task-area').textContent.includes('Attempt-scoped pins') && !document.querySelector('#workbench-inspector #stage-repair-form')"),
                        "Workbench layout UC-2 ext 1c: verified proposal revision reopens the same saved Draft in the wide task area");
            }
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("UC-4 extension 2a: a zero-change review retains empty categories and groupings without publishing a proposal")
    void displaysEmptyComparisonGroupsInRealBrowser() throws Exception {
        ObjectNode document = scaleProposalDocument();
        ObjectNode proposal = (ObjectNode) document.path("proposal");
        ObjectNode result = (ObjectNode) proposal.path("result");
        ((ObjectNode) result.path("timetable")).set("assignments", document.path("acceptedBaseline").path("result").path("timetable").path("assignments").deepCopy());
        ((ArrayNode) result.path("changeReport").path("periodMoves")).removeAll();
        proposal.set("review", reviews.create(document.path("acceptedBaseline"), document.path("repairDraft"),
                proposal.path("definition"), result));
        storeAccepted(document);
        jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=10 WHERE workspace_id=1").update();
        String durable = storedDocument();
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Unique changed lessons", Duration.ofSeconds(15));
            assertEquals(JSON.readTree("[\"By class · 0\",\"By teacher · 0\",\"By room · 0\",\"By day · 0\"]"),
                    cdp.evaluateValue("[...document.querySelectorAll('.review-groups summary')].slice(1).map(el=>el.textContent)")
                            .path("result").path("result").path("value"));
            assertTrue(browserTrue(cdp, "[...document.querySelectorAll('.review-category')].every(el=>el.querySelector('h4 span').textContent==='0' && el.textContent.includes('No lessons in this category')) && [...document.querySelectorAll('.review-groups')].slice(1).every(el=>el.textContent.includes('No lessons in this category')) && document.querySelectorAll('.matrix-wrap [data-comparison-side=unchanged]').length === 1000"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(durable, storedDocument(), "supplemental UI-only zero-group snapshot must not advance Current");
    }

    @Test
    @DisplayName("UC-4 extensions 5a/5b/6a: browser revision, discard and stale acceptance preserve exact accepted bundle")
    void revisesDiscardsAndRejectsStaleProposalInRealBrowser() throws Exception {
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
            for (String action : new String[] { "#revise-proposal", "#discard-proposal" }) {
                storeAccepted(scaleProposalDocument());
                jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=10 WHERE workspace_id=1").update();
                JsonNode before = storedWorkspaceDocument();
                cdp.command("Page.navigate", object("url", page));
                cdp.awaitText("Unique changed lessons", Duration.ofSeconds(15));
                cdp.evaluate("document.querySelector('" + action + "').click()");
                cdp.awaitText("Repair draft · not current", Duration.ofSeconds(5));
                JsonNode after = storedWorkspaceDocument();
                assertEquals("REPAIR_DRAFT", storedLifecycle());
                assertEquals(before.path("acceptedBaseline"), after.path("acceptedBaseline"));
                assertEquals(before.path("repairDraft"), after.path("repairDraft"));
                assertFalse(after.has("proposal"));
                assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL]') && !document.querySelector('#accept-repair')"));
            }
            ObjectNode document = scaleProposalDocument();
            ((ObjectNode) document.path("proposal")).put("sourceWorkspaceVersion", -1);
            storeAccepted(document);
            jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=10 WHERE workspace_id=1").update();
            JsonNode before = storedWorkspaceDocument();
            cdp.command("Page.navigate", object("url", page));
            cdp.awaitText("Unique changed lessons", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('#confirm-repair-accept').click(); document.querySelector('#accept-repair').click()");
            assertTrue(cdp.awaitText("Repair draft · not current", Duration.ofSeconds(10))
                    .contains("no longer matches"), "UC-4 6a: stale identity must be refused and proposal presentation removed");
            JsonNode after = storedWorkspaceDocument();
            assertEquals("REPAIR_DRAFT", storedLifecycle());
            assertEquals(before.path("acceptedBaseline"), after.path("acceptedBaseline"));
            assertEquals(before.path("repairDraft"), after.path("repairDraft"));
            assertFalse(after.has("proposal"));
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('#accept-repair')"));
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    @Test
    @DisplayName("Timetable polish UC-5 main/2a/3a/5a/G1-G6: complete verified whole-school repair retains context and parents the next draft")
    void completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessorInRealBrowser() throws Exception {
        ObjectNode document = investigationScaleDocument();
        JsonNode original = document.path("acceptedBaseline").deepCopy();
        assertEquals(document.path("timetableRevision").stringValue(), verifier.verify(new ImportDocuments(
                original.path("definition"), original.path("result"), null,
                ImportDocuments.ImportMode.ACCEPTED_BASELINE)).timetableRevision());
        assertEquals(60, original.path("definition").path("cohorts").size());
        assertEquals(100, original.path("definition").path("teachers").size());
        assertEquals(100, original.path("definition").path("rooms").size());
        assertEquals(1_000, original.path("result").path("timetable").path("assignments").size());
        assertEquals(JSON.readTree("{\"lessonId\":\"lesson-960\",\"subjectId\":\"subject-0\",\"cohortId\":\"cohort-16\",\"teacherId\":\"teacher-16\",\"periodId\":\"period-0\",\"roomId\":\"room-16\"}"),
                original.path("result").path("timetable").path("assignments").get(960));
        storeAccepted(document);
        try (Cdp cdp = openWorkspaceBrowser()) {
            cdp.awaitText("Showing 60 of 60 classes", Duration.ofSeconds(20));
            assertTrue(browserTrue(cdp, "document.querySelector('.accepted-heading')?.textContent.includes('Scale School') && document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('[data-range=WEEK]')?.getAttribute('aria-pressed') === 'true'"));
            Set<String> expectedIds = new HashSet<>();
            for (int i = 0; i < 1_000; i++) expectedIds.add("lesson-" + i);
            assertEquals(expectedIds, renderedLessonIds(cdp), "UC-5 main 1: every accepted lesson identity is visible");
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-960]').click(); document.querySelector('[data-range=DAY]').click(); document.querySelector('#subject-investigation').value='subject-0'; document.querySelector('#subject-investigation').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#teacher-investigation').value='teacher-16'; document.querySelector('#teacher-investigation').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#workbench-inspector')?.textContent.includes('period-0') && document.querySelector('#workbench-inspector')?.textContent.includes('room-16') && document.querySelector('#range-summary')?.textContent.includes('Monday')"));
            cdp.evaluate("document.querySelector('[data-open-focus=teacherId]').click(); document.querySelector('#focus-entity').value='teacher-16'; document.querySelector('#focus-entity').dispatchEvent(new Event('change',{bubbles:true}))");
            assertTrue(browserTrue(cdp, "document.querySelector('#return-matrix') && document.querySelector('.focused-schedule')?.textContent.includes('Teacher Sixteen')"));
            cdp.evaluate("document.querySelector('#return-matrix').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'"));
            assertEquals(original, storedWorkspaceDocument().path("acceptedBaseline"));

            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('#repair-resource').value='teacher-16'; document.querySelector('[name=period][value=period-0]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(20));
            JsonNode started = assertDraftUnchangedBaseline(original).deepCopy();
            assertEquals(JSON.readTree("[\"lesson-960\"]"), started.path("directEffectLessonIds"));
            assertEquals("teacher-16", started.path("intent").path("changes").get(0).path("resourceId").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('#teacher-investigation')?.value === 'teacher-16'"));
            cdp.evaluate("document.querySelector('[name=lesson-dimension][value=PERIOD]').checked=true; document.querySelector('#apply-pin').click()");
            cdp.awaitText("Resolve blocking conflicts before solving", Duration.ofSeconds(15));
            JsonNode conflicted = assertDraftUnchangedBaseline(original);
            assertFalse(conflicted.path("readyToSolve").booleanValue());
            assertEquals("lesson-960", conflicted.path("conflicts").get(0).path("lessonId").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('[data-draft-conflict=lesson-960]') && document.querySelector('#solve-draft')?.disabled === true"));
            assertEquals(0, processes.commands().stream().filter(command -> command.size() > 1 && "replan".equals(command.get(1))).count(), "UC-5 2a: a conflict starts no scheduling process");
            cdp.evaluate("document.querySelector('#remove-pin').click()");
            cdp.awaitText("Draft is durably saved with no blocking conflict", Duration.ofSeconds(15));
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click(); document.querySelector('[data-lesson-id=lesson-500]').click(); document.querySelector('[name=lesson-dimension][value=ROOM]').checked=true; document.querySelector('#apply-pin').click()");
            cdp.awaitText("Accepted room pinned", Duration.ofSeconds(15));
            JsonNode draft = assertDraftUnchangedBaseline(original).deepCopy();
            assertTrue(draft.path("readyToSolve").booleanValue());
            assertEquals(JSON.readTree("[\"lesson-960\"]"), draft.path("directEffectLessonIds"));
            assertEquals("lesson-500", draft.path("intent").path("pins").get(0).path("lessonId").stringValue());
            assertEquals(JSON.readTree("[\"INDIVIDUAL\"]"), draft.path("intent").path("pins").get(0).path("roomSources"));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-960]').click(); document.querySelector('[data-range=DAY]').click(); document.querySelector('#cohort-filter').value='cohort-16'; document.querySelector('#cohort-filter').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#toggle-inspector').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#inspector-summary')?.textContent.includes('Declared lesson 960') && document.querySelector('#workbench-inspector')?.hidden === true"));
            cdp.evaluate("document.querySelector('#reopen-inspector').click()");
            assertEquals(draft, assertDraftUnchangedBaseline(original), "UC-5 main 2: inspector and filter changes cannot edit the durable Draft");

            processes.blockReplan = true;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            processes.awaitBlocked();
            cdp.awaitText("Repair generation · running", Duration.ofSeconds(20));
            JsonNode running = storedWorkspaceDocument();
            assertEquals("SOLVING_REPAIR", storedLifecycle());
            assertEquals(original, running.path("acceptedBaseline"));
            assertEquals(draft, running.path("repairDraft"));
            assertFalse(running.has("proposal"));
            assertEquals("PT30S", running.path("run").path("limit").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=SOLVING]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#cancel-run') && !document.querySelector('#apply-pin') && document.querySelector('#cohort-filter')?.value === 'cohort-16' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'"));
            cdp.evaluate("document.querySelector('[data-mode=CURRENT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'"));
            cdp.evaluate("document.querySelector('[data-mode=DRAFT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#workbench-task-area #run-secondary')?.textContent.includes('Frozen repair intent') && !document.querySelector('#solve-draft')"));
            cdp.evaluate("document.querySelector('[data-mode=SOLVING]').click()");
            assertEquals(running, storedWorkspaceDocument());
            assertEquals(1, processes.commands().stream().filter(command -> command.size() > 1 && "replan".equals(command.get(1))).count(), "UC-5 main 3: presentation changes cannot launch another run");
            cdp.evaluate("document.querySelector('#cancel-run').click()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(20));
            assertEquals(draft, assertDraftUnchangedBaseline(original));
            assertEquals("CANCELLED", storedWorkspaceDocument().path("lastRun").path("status").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=PROPOSAL]') && document.querySelector('#solve-draft') && document.querySelector('#cohort-filter')?.value === 'cohort-16'"));

            processes.blockReplan = false;
            processes.verifiedFeasibleResult = this::verifiedNormativeRepairResult;
            cdp.evaluate("document.querySelector('#solve-draft').click()");
            cdp.awaitText("Repair proposal · feasible", Duration.ofSeconds(30));
            JsonNode proposed = storedWorkspaceDocument();
            assertEquals("REPAIR_PROPOSAL", storedLifecycle());
            assertEquals(original, proposed.path("acceptedBaseline"));
            assertEquals(draft, proposed.path("repairDraft"));
            assertFalse(proposed.has("run"));
            assertEquals(2, processes.commands().stream().filter(command -> command.size() > 1 && "replan".equals(command.get(1))).count());
            JsonNode proposal = proposed.path("proposal");
            assertEquals(2, proposal.path("review").path("uniqueChangedLessonCount").intValue());
            assertEquals(1, proposal.path("review").path("directEffectChangedCount").intValue());
            assertEquals(1, proposal.path("review").path("rippleEffectCount").intValue());
            assertEquals("period-40", proposal.path("result").path("timetable").path("assignments").get(960).path("periodId").stringValue());
            assertEquals("room-50", proposal.path("result").path("timetable").path("assignments").get(0).path("roomId").stringValue());
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=PROPOSAL]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#cohort-filter')?.value === 'cohort-16' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960' && document.querySelector('.comparison-details')?.textContent.includes('period-40')"));
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click(); document.querySelector('#reset-view').click()");
            assertTrue(browserTrue(cdp, "document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-960]').length === 2 && document.querySelectorAll('.matrix-wrap [data-lesson-id=lesson-0]').length === 1 && document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=accepted]') && document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=proposed]')"));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-0][data-comparison-side=combined]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('room-0') && document.querySelector('.comparison-details')?.textContent.includes('room-50') && document.querySelector('.comparison-details')?.textContent.includes('Solver ripple')"));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-500]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('.comparison-details')?.textContent.includes('Accepted and unchanged') && document.querySelector('.comparison-details')?.textContent.includes('Accepted room pinned')"));
            cdp.evaluate("document.querySelector('[data-lesson-id=lesson-960][data-comparison-side=accepted]').click(); document.querySelector('[data-range=DAY]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('#range-summary')?.textContent.includes('Monday') && document.querySelector('.comparison-details')?.textContent.includes('period-40')"));
            cdp.evaluate("document.querySelector('[data-mode=CURRENT]').click()");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-lesson-id=lesson-960]')?.textContent.includes('Proposed destination')"));
            cdp.evaluate("document.querySelector('[data-mode=DRAFT]').click(); document.querySelector('[data-mode=PROPOSAL]').click()");
            assertEquals(proposed, storedWorkspaceDocument(), "UC-5 main 4: review and navigation cannot advance Current");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#lesson-panel-title')?.textContent === 'Declared lesson 960'"));
            cdp.evaluate("document.querySelector('#confirm-repair-accept').click(); document.querySelector('#accept-repair').click()");
            cdp.awaitText("Accepted baseline · current timetable", Duration.ofSeconds(20));
            JsonNode successorDocument = storedWorkspaceDocument();
            JsonNode successor = successorDocument.path("acceptedBaseline").deepCopy();
            assertEquals("ACCEPTED_BASELINE", storedLifecycle());
            assertEquals(proposal.path("definition"), successor.path("definition"));
            assertEquals(proposal.path("result"), successor.path("result"));
            assertFalse(successorDocument.has("repairDraft"));
            assertFalse(successorDocument.has("proposal"));
            assertEquals(proposal.path("proposedTimetableRevision").stringValue(), successorDocument.path("timetableRevision").stringValue());
            assertFalse(original.path("result").path("timetableRevision").equals(successorDocument.path("timetableRevision")),
                    "UC-5 main 5: the accepted revision must advance to the verified successor");
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=CURRENT]')?.getAttribute('aria-pressed') === 'true' && !document.querySelector('[data-mode=DRAFT], [data-mode=SOLVING], [data-mode=PROPOSAL]') && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#range-summary')?.textContent.includes('Monday') && !document.querySelector('[data-lesson-id=lesson-960]') && !document.querySelector('#lesson-panel-title') && document.querySelector('#inspection-notice')?.textContent.includes('outside the represented Day')"),
                    "UC-5 main 5: accepted period move must clear only an unrepresentable Monday selection, with an explanation");
            assertEquals(1_000, successor.path("result").path("timetable").path("assignments").size());
            cdp.evaluate("document.querySelector('[data-range=WEEK]').click()");
            assertEquals(expectedIds, renderedLessonIds(cdp), "UC-5 main 5: no accepted lesson can disappear or duplicate after acceptance");
            cdp.evaluate("document.querySelector('[data-range=DAY]').click()");

            cdp.evaluate("document.querySelector('.repair-entry').open=true; document.querySelector('#repair-resource-type').value='ROOM'; document.querySelector('#repair-resource-type').dispatchEvent(new Event('change',{bubbles:true})); document.querySelector('#repair-resource').value='room-50'; document.querySelector('[name=period][value=period-0]').checked=true; document.querySelector('#start-repair-form').requestSubmit()");
            cdp.awaitText("Repair draft · not current", Duration.ofSeconds(20));
            JsonNode nextDraft = assertDraftUnchangedBaseline(successor).deepCopy();
            assertEquals("ROOM", nextDraft.path("intent").path("changes").get(0).path("resourceType").stringValue());
            assertEquals("room-50", nextDraft.path("intent").path("changes").get(0).path("resourceId").stringValue());
            assertEquals(JSON.readTree("[\"period-0\"]"), nextDraft.path("intent").path("changes").get(0).path("unavailablePeriodIds"));
            assertEquals(JSON.readTree("[\"lesson-0\"]"), nextDraft.path("directEffectLessonIds"));
            assertTrue(nextDraft.path("intent").path("pins").isEmpty(), "UC-5 5a: the next attempt must not inherit previous pins");
            assertTrue(nextDraft.path("intent").path("bulkActions").isEmpty());
            assertTrue(browserTrue(cdp, "document.querySelector('[data-mode=DRAFT]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('[data-range=DAY]')?.getAttribute('aria-pressed') === 'true' && document.querySelector('#draft-conflict-count')?.textContent === '0' && document.querySelector('#attempt-pin-count')?.textContent === '0'"));
            assertEquals(2, processes.commands().stream().filter(command -> command.size() > 1 && "replan".equals(command.get(1))).count(), "the next Draft must not launch a run without an explicit request");
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
    }

    private Cdp openWorkspaceBrowser() throws Exception {
        return openWorkspaceBrowser(port);
    }

    private Cdp openWorkspaceBrowser(int workspacePort) throws Exception {
        int debuggingPort = startBrowser();
        String page = "http://localhost:" + workspacePort + "/workspace/";
        String target = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort
                                + "/json/new?" + URLEncoder.encode(page, StandardCharsets.UTF_8)))
                        .PUT(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        Cdp cdp = new Cdp(JSON.readTree(target).path("webSocketDebuggerUrl").stringValue());
        cdp.command("Page.enable", JSON.createObjectNode());
        cdp.command("Runtime.enable", JSON.createObjectNode());
        cdp.command("Page.navigate", object("url", page));
        return cdp;
    }

    private ConfigurableApplicationContext startRestartableWorkspace() {
        return new SpringApplicationBuilder(WorkspaceApplication.class, WorkspaceRepairPlanningIT.ProcessConfiguration.class)
                .properties(Map.of("server.port", "0", "spring.datasource.url", POSTGRES.getJdbcUrl(),
                        "spring.datasource.username", POSTGRES.getUsername(),
                        "spring.datasource.password", POSTGRES.getPassword(),
                        "workspace.kernel-executable", ROOT.resolve("school-kernel").toString()))
                .run();
    }

    private JsonNode storedWorkspaceDocument() throws Exception {
        return JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single());
    }

    private String storedLifecycle() {
        return jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single();
    }

    private JsonNode assertDraftUnchangedBaseline(JsonNode baseline) throws Exception {
        assertEquals("REPAIR_DRAFT", storedLifecycle());
        JsonNode document = storedWorkspaceDocument();
        assertEquals(baseline, document.path("acceptedBaseline"), "accepted definition, result and manifest must remain exact");
        assertFalse(document.has("run"));
        assertFalse(document.has("proposal"));
        assertTrue(document.has("repairDraft"));
        return document.path("repairDraft");
    }

    private static boolean browserTrue(Cdp cdp, String expression) throws Exception {
        return cdp.evaluateValue("Boolean(" + expression + ")")
                .path("result").path("result").path("value").booleanValue();
    }

    private static void assertDisplayedComparisonSides(Cdp cdp, JsonNode expected) throws Exception {
        JsonNode actual = cdp.evaluateValue("[...document.querySelectorAll('.comparison-details .before-after section')].map(side=>[side.querySelector('h5').textContent,[...side.querySelectorAll('dl > div')].map(row=>[row.querySelector('dt').textContent.trim(),row.querySelector('dd').firstChild.textContent.trim(),row.querySelector('dd small')?.textContent||null])])")
                .path("result").path("result").path("value");
        assertEquals(expected, actual, "UC-4 main 4: each displayed weekday, subject, class, teacher, period and room must match independently verified definition/result by name, stable ID and changed cue");
    }

    private static void awaitBrowserCondition(Cdp cdp, String expression) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            if (browserTrue(cdp, expression)) return;
            Thread.sleep(50);
        }
        assertTrue(browserTrue(cdp, expression), "the reloaded browser must restore the durable draft");
    }

    private static Set<String> renderedLessonIds(Cdp cdp) throws Exception {
        String ids = cdp.evaluateValue("JSON.stringify([...new Set([...document.querySelectorAll('#accepted-view [data-lesson-id]')].map(button => button.dataset.lessonId))].sort())")
                .path("result").path("result").path("value").stringValue();
        return jsonStrings(JSON.readTree(ids));
    }

    private static Set<String> jsonStrings(JsonNode array) {
        Set<String> values = new HashSet<>();
        array.forEach(item -> values.add(item.stringValue()));
        assertEquals(array.size(), values.size(), "lesson IDs and pin sources must be unique");
        return values;
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
        KernelVerifier.Verification verified = verifier.verify(new ImportDocuments(definition, result, null,
                ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        assertEquals(revision, verified.definitionRevision(), "normative accepted definition must pass the packaged verifier");
        assertEquals(timetableRevision, verified.timetableRevision(), "normative accepted result must pass the packaged verifier");
        return document;
    }

    private ObjectNode verifiedNormativeRepairResult(java.util.List<String> arguments) {
        try {
            JsonNode acceptedDefinition = JSON.readTree(Path.of(arguments.get(arguments.indexOf("--current-definition") + 1)).toFile());
            JsonNode acceptedResult = JSON.readTree(Path.of(arguments.get(arguments.indexOf("--current") + 1)).toFile());
            JsonNode successor = JSON.readTree(Path.of(arguments.get(arguments.indexOf("--definition") + 1)).toFile());
            KernelVerifier.Verification original = verifier.verify(new ImportDocuments(acceptedDefinition, acceptedResult,
                    null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
            assertEquals(acceptedResult.path("timetableRevision").stringValue(), original.timetableRevision());
            assertEquals(original.definitionRevision(), normalizedDefinitionRevision(acceptedDefinition));
            assertFalse(jsonStrings(successor.path("teachers").get(16).path("availablePeriodIds")).contains("period-0"));
            assertEquals("room-8", successor.path("lessons").get(500).path("roomLock").stringValue());
            String revision = normalizedDefinitionRevision(successor);
            ObjectNode result = (ObjectNode) acceptedResult.deepCopy();
            result.put("correlationId", arguments.get(arguments.indexOf("--correlation-id") + 1));
            result.put("inputRevision", revision).put("elapsedTimeMs", 2004).put("terminationReason", "TIME_LIMIT");
            result.putObject("limit").put("type", "TIME").put("duration", "PT30S");
            ObjectNode moved = (ObjectNode) result.path("timetable").path("assignments").get(960);
            ObjectNode roomOnly = (ObjectNode) result.path("timetable").path("assignments").get(0);
            assertEquals("period-0", moved.path("periodId").stringValue());
            assertEquals("room-0", roomOnly.path("roomId").stringValue());
            moved.put("periodId", "period-40");
            roomOnly.put("roomId", "room-50");
            result.withObject("score").put("periodMoves", 1).put("roomOnlyMoves", 1);
            ObjectNode report = result.putObject("changeReport");
            report.putArray("additions"); report.putArray("cancellations"); report.putArray("teacherChanges");
            report.putArray("forcedMoves");
            report.putArray("periodMoves").addObject().put("lessonId", "lesson-960")
                    .put("oldPeriodId", "period-0").put("newPeriodId", "period-40")
                    .put("oldRoomId", "room-16").put("newRoomId", "room-16");
            report.putArray("roomOnlyMoves").addObject().put("lessonId", "lesson-0")
                    .put("oldRoomId", "room-0").put("newRoomId", "room-50");
            ObjectNode scope = JSON.createObjectNode().put("schemaVersion", 1).put("schoolId", "opaque-scale-school")
                    .put("inputRevision", revision);
            var orderedAssignments = new ArrayList<JsonNode>();
            result.path("timetable").path("assignments").forEach(orderedAssignments::add);
            orderedAssignments.sort(Comparator.comparing(item -> item.path("lessonId").stringValue()));
            var ordered = scope.putArray("assignments");
            orderedAssignments.forEach(ordered::add);
            result.put("timetableRevision", "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(CanonicalJson.bytes(scope))));
            KernelVerifier.Verification verified = verifier.verify(new ImportDocuments(successor, result,
                    null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
            assertEquals(revision, verified.definitionRevision());
            assertEquals(result.path("timetableRevision").stringValue(), verified.timetableRevision());
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Normative candidate must pass the actual kernel verifier before it reaches the planner", exception);
        }
    }

    private String normalizedDefinitionRevision(JsonNode definition) throws Exception {
        ObjectNode normalized = (ObjectNode) definition.deepCopy();
        for (String collection : new String[] { "subjects", "teachers", "cohorts", "rooms", "periods", "lessons", "softConstraintOverrides" }) {
            if (!(normalized.path(collection) instanceof ArrayNode array)) continue;
            var ordered = new ArrayList<JsonNode>();
            array.forEach(ordered::add);
            ordered.sort(Comparator.comparing(item -> item.path(collection.equals("softConstraintOverrides") ? "constraintId" : "id").stringValue()));
            array.removeAll();
            ordered.forEach(array::add);
            for (JsonNode value : array) {
                for (String field : new String[] { "qualifiedSubjectIds", "availablePeriodIds", "undesirablePeriodIds", "capabilityIds",
                        "requiredRoomCapabilityIds", "preferredRoomIds" }) {
                    if (!(value.path(field) instanceof ArrayNode set)) continue;
                    var entries = new ArrayList<String>();
                    set.forEach(item -> entries.add(item.stringValue()));
                    entries.sort(String::compareTo);
                    set.removeAll();
                    entries.forEach(set::add);
                }
            }
        }
        return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(CanonicalJson.bytes(normalized)));
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

    private ObjectNode comparisonShapeDocument() {
        ObjectNode document = scaleProposalDocument();
        JsonNode baseline = document.path("acceptedBaseline");
        ObjectNode draft = (ObjectNode) document.path("repairDraft");
        ObjectNode proposal = (ObjectNode) document.path("proposal");
        ObjectNode definition = (ObjectNode) proposal.path("definition");
        ObjectNode result = (ObjectNode) proposal.path("result");
        var report = result.path("changeReport");
        ((ArrayNode) report.path("forcedMoves")).addObject().put("lessonId", "lesson-0");
        ((ObjectNode) result.path("timetable").path("assignments").get(100)).put("roomId", "room-2");
        ((ArrayNode) draft.path("intent").path("pins")).addObject().put("lessonId", "lesson-100")
                .putArray("roomSources").add("INDIVIDUAL");
        ((ArrayNode) report.path("roomOnlyMoves")).addObject().put("lessonId", "lesson-100");
        ((ObjectNode) definition.path("rooms").get(2)).remove("displayName");
        ((ArrayNode) result.path("timetable").path("assignments")).remove(101);
        ((ArrayNode) definition.path("lessons")).remove(101);
        ((ArrayNode) report.path("cancellations")).addObject().put("lessonId", "lesson-101");
        ((ArrayNode) definition.path("lessons")).addObject().put("id", "lesson-added")
                .put("displayName", "Additional school lesson").put("subjectId", "subject-0")
                .put("cohortId", "cohort-0").put("teacherId", "teacher-0");
        ((ArrayNode) result.path("timetable").path("assignments")).add(assignment(
                "lesson-added", "subject-0", "cohort-0", "teacher-0", "period-59", "room-0"));
        ((ArrayNode) report.path("additions")).addObject().put("lessonId", "lesson-added");
        ObjectNode counts = (ObjectNode) proposal.path("changeCounts");
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
                "--window-size=1600,900",
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
