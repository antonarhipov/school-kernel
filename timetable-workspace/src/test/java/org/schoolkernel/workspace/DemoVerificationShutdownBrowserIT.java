package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.node.ObjectNode;

class DemoVerificationShutdownBrowserIT {
    @TempDir Path temporary;
    @Test void actualShutdownStopsBlockedVisitorVerificationAndCleansPrivateFiles() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("shutdown", Path.of(System.getProperty("demo.shutdown.walkthrough.dir", "../output/playwright/demo-deployment/UC-12/shutdown-" + System.currentTimeMillis())));
        h.stop();
        Path wrapper = temporary.resolve("kernel"), pid = temporary.resolve("verify-pid"), input = temporary.resolve("verify-input"), hold = temporary.resolve("hold-verify");
        Files.writeString(wrapper, Files.readString(wrapper).replace("#!/bin/sh\n", "#!/bin/sh\nif [ \"$1\" = verify ] && [ -f '" + hold + "' ]; then\n printf '%s\\n' \"$$\" > '" + pid + "'\n printf '%s\\n' \"$3\" > '" + input + "'\n exec /bin/sleep 60\nfi\n"));
        h.start(List.of("--spring.lifecycle.timeout-per-shutdown-phase=1s", "--demo.observability.summary-interval=1s"), 2);
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
                Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true)); BrowserContext owner = h.context(browser)) {
            Page page = owner.newPage();
            try {
                h.open(page); h.sample(page, "small", false); var before = h.snapshot(page);
                ObjectNode definition = (ObjectNode) DemoPlanningBrowserIT.JSON.readTree(page.locator("#demo-json").inputValue());
                definition.put("displayName", "PRIVATE_SHUTDOWN_SENTINEL"); page.locator("#demo-json").fill(definition.toPrettyString()); h.save(page, "shutdown-copy");
                Files.writeString(hold, "1"); page.onceDialog(Dialog::accept); page.locator("#demo-use").click(); page.waitForCondition(() -> Files.exists(pid));
                assertEquals(before, h.snapshot(page));
                h.record("UC-12 6a/G6: real visitor definition verification is deliberately held in an actual child process; exact prior Current remains unchanged before SIGTERM", page, "01-in-flight");
                long processId = Long.parseLong(Files.readString(pid).trim()); Path privateDirectory = Path.of(Files.readString(input).trim()).getParent(); assertTrue(Files.isDirectory(privateDirectory));
                Process server = h.server; server.destroy(); assertTrue(server.waitFor(8, TimeUnit.SECONDS), "Graceful shutdown must finish without test-forced kill"); h.server = null;
                assertFalse(ProcessHandle.of(processId).map(ProcessHandle::isAlive).orElse(false)); assertFalse(Files.exists(privateDirectory));
                var events = Files.readAllLines(h.output.resolve("server-2.log")).stream().filter(line -> line.startsWith("{\"family\":" )).map(DemoPlanningBrowserIT.JSON::readTree).toList();
                assertEquals(1, DemoObservabilityBrowserIT.count(events, "process_started", "kind", "VERIFICATION"));
                assertEquals(1, events.stream().filter(e -> e.path("event").asString().equals("process_finished") && e.path("kind").asString().equals("VERIFICATION") && e.path("outcome").asString().equals("CANCELLED")).count());
                assertFalse(Files.readString(h.output.resolve("server-2.log")).contains("PRIVATE_SHUTDOWN_SENTINEL"));
                h.steps.add("- PASS **Actual SIGTERM / one-second web grace:** the application exits without forced kill, verifier child is reaped, private directory is removed, and one CANCELLED verification completion is logged. Allowance/queued-caller settlement is independently asserted with two actual processes in DemoVerificationLifecycleTest.");
                Files.writeString(h.output.resolve("verdict.txt"), "PASS\n");
            } catch (Throwable failure) { Files.writeString(h.output.resolve("verdict.txt"), "FAIL: " + failure); throw failure; }
            finally { Files.writeString(h.output.resolve("ledger.md"), "# UC-12 local shutdown actor evidence\n\n" + Instant.now() + "; packaged local TLS, actual Chrome owner and startup sample checks. The visitor verifier is explicitly replaced by a real blocking sleep process; no fabricated success or capacity evidence. SIGTERM is a real infrastructure action, not claimed as a browser click.\n\n" + String.join("\n\n", h.steps) + "\n\nOverall: " + Files.readString(h.output.resolve("verdict.txt")) + "\nNo deployment/provider change; service, browser and test processes cleaned up. The ended in-memory workspace is not restored or accepted.\n"); }
        } finally { h.stop(); if (Files.exists(pid)) ProcessHandle.of(Long.parseLong(Files.readString(pid).trim())).ifPresent(ProcessHandle::destroyForcibly); }
    }
}
