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
import java.time.Duration;
import java.util.Map;
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
    @DisplayName("UC-3 main, extensions, guarantees: accepted whole-school inspection is local, keyboard-native, narrow-safe, and immutable")
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
            String complete = cdp.awaitText("Complete whole-school matrix", Duration.ofSeconds(15));
            assertTrue(complete.contains("Accepted baseline · current timetable"));
            assertTrue(complete.contains("Mathematics"));
            assertTrue(complete.contains("Alex"));
            assertTrue(complete.contains("Room 102"));
            assertTrue(complete.contains("Empty"));
            assertTrue(complete.contains("Showing 1 of 1 classes"));

            cdp.evaluate("document.querySelector('#lesson-search').focus()");
            cdp.command("Input.insertText", object("text", "Science"));
            String filtered = cdp.awaitText("Filtered whole-school matrix", Duration.ofSeconds(5));
            assertTrue(filtered.contains("Search: Science"));
            assertTrue(cdp.evaluateValue("Boolean(document.querySelector('.lesson-cell.match'))")
                    .path("result").path("result").path("value").booleanValue());

            cdp.evaluate("document.querySelector('#lesson-search').value='not-present'; document.querySelector('#lesson-search').dispatchEvent(new Event('input',{bubbles:true}))");
            String noMatch = cdp.awaitText("No lessons match the active criteria", Duration.ofSeconds(5));
            assertTrue(noMatch.contains("Search: not-present"));
            assertTrue(noMatch.contains("Reset view"));
            cdp.evaluate("document.querySelector('#reset-empty').click()");
            cdp.awaitText("Complete whole-school matrix", Duration.ofSeconds(5));

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
            cdp.evaluate("document.querySelector('#return-matrix').click(); document.querySelector('[data-density=\"compact\"]').click()");
            assertTrue(cdp.evaluateValue("document.querySelector('[data-density=\"compact\"]').getAttribute('aria-pressed') === 'true'")
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
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument());
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
    @DisplayName("UC-3 G5 and RULE-25: target-scale post-load inspection interactions remain below 250 ms p95")
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
            JsonNode days = measured(cdp, """
                    const element=document.querySelector('#weekday');
                    element.selectedIndex=i%element.options.length;
                    element.dispatchEvent(new Event('change',{bubbles:true}));
                    """);
            JsonNode selections = measured(cdp, """
                    const element=document.querySelector('[data-lesson-id]');
                    element.click();
                    """);

            assertBelowTarget("search", search);
            assertBelowTarget("filter", filters);
            assertBelowTarget("day", days);
            assertBelowTarget("selection", selections);
            System.out.printf("UC-3 scale samples search=%s filter=%s day=%s selection=%s; solver time excluded%n",
                    search, filters, days, selections);
            assertTrue(cdp.errors().isEmpty(), cdp.errors().toString());
        }
        assertEquals(before, storedDocument());
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

    private static void assertBelowTarget(String interaction, JsonNode samples) {
        double[] ordered = new double[samples.size()];
        for (int index = 0; index < samples.size(); index++) ordered[index] = samples.get(index).doubleValue();
        java.util.Arrays.sort(ordered);
        double p95 = ordered[(int) Math.ceil(ordered.length * 0.95) - 1];
        assertTrue(p95 < 250.0, () -> interaction + " p95 was " + p95 + " ms: " + samples);
    }

    private ObjectNode acceptedDocument(boolean empty) throws Exception {
        ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("examples/initial-school.json").toFile());
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
            throw new AssertionError("Browser did not render: " + expected);
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
