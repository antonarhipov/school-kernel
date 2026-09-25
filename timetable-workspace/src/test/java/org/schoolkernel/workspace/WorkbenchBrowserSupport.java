package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Shared harness for the browser journeys. Every browser test class uses one PostgreSQL container, one cached Spring
 * context and one Chrome process; each test gets a fresh browser context (isolated storage) and a reset workspace.
 *
 * <p>System properties: {@code -Dplaywright.headed=true} shows the browser, {@code -Dplaywright.trace=true} records a
 * Playwright trace for failed tests under {@code target/playwright-failures}. {@code CHROME_BINARY} overrides the
 * installed Google Chrome.
 */
@SpringBootTest(classes = {WorkspaceApplication.class, WorkspaceRepairPlanningIT.ProcessConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class WorkbenchBrowserSupport {
    static final ObjectMapper JSON = WorkbenchFixtures.JSON;
    static final Path ROOT = WorkbenchFixtures.ROOT;
    private static final Path FAILURES = Path.of("target/playwright-failures");
    private static final boolean TRACE = Boolean.getBoolean("playwright.trace");

    // Started once per JVM and shared by the cached context; Ryuk removes it when the JVM exits.
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("browser_workspace")
            .withUsername("workspace")
            .withPassword("workspace");

    static {
        POSTGRES.start();
    }

    private static Playwright playwright;
    private static Browser browser;

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

    WorkbenchFixtures fixtures;
    Workbench workbench;
    private BrowserContext context;
    private final List<String> pageErrors = new CopyOnWriteArrayList<>();

    @RegisterExtension
    final AfterTestExecutionCallback failureArtifacts = extension -> {
        if (context == null) return;
        String name = extension.getRequiredTestClass().getSimpleName() + "." + extension.getRequiredTestMethod().getName();
        boolean failed = extension.getExecutionException().isPresent();
        if (failed) {
            Files.createDirectories(FAILURES);
            for (int index = 0; index < context.pages().size(); index++) {
                Page page = context.pages().get(index);
                if (page.isClosed()) continue;
                String suffix = index == 0 ? "" : "-" + index;
                page.screenshot(new Page.ScreenshotOptions().setPath(FAILURES.resolve(name + suffix + ".png")));
                Files.writeString(FAILURES.resolve(name + suffix + ".txt"), (String) page.evaluate("document.body?.innerText || ''"));
            }
        }
        if (TRACE) {
            if (failed) context.tracing().stop(new Tracing.StopOptions().setPath(FAILURES.resolve(name + ".zip")));
            else context.tracing().stop();
        }
    };

    @BeforeAll
    static void launchBrowser() {
        if (browser != null) return;
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(!Boolean.getBoolean("playwright.headed"));
        String binary = System.getenv("CHROME_BINARY");
        if (binary != null) options.setExecutablePath(Path.of(binary));
        else options.setChannel("chrome");
        browser = playwright.chromium().launch(options);
        Runtime.getRuntime().addShutdownHook(new Thread(playwright::close));
    }

    @BeforeEach
    void resetWorkspaceAndBrowser() {
        jdbc.sql("""
                        UPDATE workspace_aggregate
                        SET lifecycle_state='EMPTY', version=0, active_run_id=NULL, document='{}'::jsonb
                        WHERE workspace_id=1
                        """)
                .update();
        processes.reset();
        fixtures = new WorkbenchFixtures(verifier, reviews);
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1600, 900).setDeviceScaleFactor(1));
        context.setDefaultTimeout(Workbench.UI.toMillis());
        context.onPage(page -> page.onPageError(error -> pageErrors.add(page.url() + ": " + error)));
        if (TRACE) context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        workbench = new Workbench(context.newPage(), port);
    }

    @AfterEach
    void closeBrowserContext() {
        processes.reset();
        try {
            assertTrue(pageErrors.isEmpty(), "uncaught browser errors: " + pageErrors);
        } finally {
            context.close();
            context = null;
        }
    }

    /** A second tab in the same browser context, sharing storage with {@link #workbench}. */
    Workbench newTab() {
        return new Workbench(context.newPage(), port);
    }

    // Durable workspace state

    void storeAccepted(ObjectNode document) {
        jdbc.sql("""
                        UPDATE workspace_aggregate SET lifecycle_state='ACCEPTED_BASELINE', version=7,
                        active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                        """).param("document", JSON.writeValueAsString(document)).update();
    }

    /** Stores a document that already carries a draft and proposal, in the REPAIR_PROPOSAL lifecycle. */
    void storeProposal(ObjectNode document) {
        jdbc.sql("""
                        UPDATE workspace_aggregate SET lifecycle_state='REPAIR_PROPOSAL', version=10,
                        active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1
                        """).param("document", JSON.writeValueAsString(document)).update();
    }

    /** Version and complete document; equal values mean nothing durable changed. */
    String storedDocument() {
        return jdbc.sql("SELECT version || ':' || document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single();
    }

    JsonNode storedWorkspaceDocument() {
        return JSON.readTree(jdbc.sql("SELECT document::text FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single());
    }

    String storedLifecycle() {
        return jdbc.sql("SELECT lifecycle_state FROM workspace_aggregate WHERE workspace_id=1")
                .query(String.class).single();
    }

    long storedVersion() {
        return jdbc.sql("SELECT version FROM workspace_aggregate WHERE workspace_id=1").query(Long.class).single();
    }

    /** Asserts a durable Draft over the exact accepted baseline, with no run or proposal, and returns the Draft. */
    JsonNode assertDraftUnchangedBaseline(JsonNode baseline) {
        assertEquals("REPAIR_DRAFT", storedLifecycle());
        JsonNode document = storedWorkspaceDocument();
        assertEquals(baseline, document.path("acceptedBaseline"), "accepted definition, result and manifest must remain exact");
        assertFalse(document.has("run"));
        assertFalse(document.has("proposal"));
        assertTrue(document.has("repairDraft"));
        return document.path("repairDraft");
    }

    long replanCommands() {
        return processes.commands().stream().filter(command -> command.size() > 1 && "replan".equals(command.get(1))).count();
    }

    /** Runs {@code step} while every workspace update matching {@code condition} fails, like unavailable storage. */
    void withRejectedWorkspaceUpdates(String condition, BrowserStep step) throws Exception {
        jdbc.sql("""
                CREATE FUNCTION reject_browser_workspace_update() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'test: workspace storage unavailable'; END;
                $$ LANGUAGE plpgsql
                """).update();
        jdbc.sql("""
                CREATE TRIGGER reject_browser_workspace_update BEFORE UPDATE ON workspace_aggregate
                FOR EACH ROW WHEN (%s) EXECUTE FUNCTION reject_browser_workspace_update()
                """.formatted(condition)).update();
        try {
            step.run();
        } finally {
            jdbc.sql("DROP TRIGGER IF EXISTS reject_browser_workspace_update ON workspace_aggregate").update();
            jdbc.sql("DROP FUNCTION IF EXISTS reject_browser_workspace_update()").update();
        }
    }

    ConfigurableApplicationContext startRestartableWorkspace() {
        return new SpringApplicationBuilder(WorkspaceApplication.class, WorkspaceRepairPlanningIT.ProcessConfiguration.class)
                .properties(Map.of("server.port", "0", "spring.datasource.url", POSTGRES.getJdbcUrl(),
                        "spring.datasource.username", POSTGRES.getUsername(),
                        "spring.datasource.password", POSTGRES.getPassword(),
                        "workspace.kernel-executable", ROOT.resolve("school-kernel").toString()))
                .run();
    }

    @FunctionalInterface
    interface BrowserStep {
        void run() throws Exception;
    }
}
