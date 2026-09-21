package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
