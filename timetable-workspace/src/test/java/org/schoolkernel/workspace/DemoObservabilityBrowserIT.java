package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/** Operator evidence is the actual packaged process log, reconciled with real isolated visitor actions. */
class DemoObservabilityBrowserIT {
    @TempDir Path temporary;
    @Test void safeSummariesReconcileQueueCoalescingCancellationReuseDecisionsAndActualProcesses() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("observability", Path.of(System.getProperty("demo.observability.walkthrough.dir", "../output/playwright/demo-deployment/UC-13/local-" + System.currentTimeMillis())));
        h.stop(); h.start(List.of("--demo.observability.summary-interval=1s"), 2);
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
                Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
                BrowserContext owner = h.context(browser); BrowserContext other = h.context(browser); BrowserContext third = h.context(browser)) {
            Page one = owner.newPage(), two = other.newPage(), three = third.newPage(); one.onPageError(h.errors::add); two.onPageError(h.errors::add);
            String privateName = "PRIVATE_TELEMETRY_SENTINEL_PERSON_582";
            try {
                h.open(one); h.open(two); h.open(three); h.sample(one, "small", false);
                h.initial(one, "private-browser-label-582", privateName); h.initial(two, "different-browser-label-582", privateName);
                h.initial(three, "waiting-browser-label-582", privateName + " queued");
                one.locator("#start-plan").click(); one.locator("#cancel-run").waitFor();
                two.locator("#start-plan").click(); two.locator("#cancel-run").waitFor();
                three.locator("#start-plan").click(); three.locator("#cancel-run").waitFor(); three.locator("#demo-run-status").filter(new Locator.FilterOptions().setHasText("Queued")).waitFor();
                three.waitForCondition(() -> eventsUnchecked(h).stream().anyMatch(e -> e.path("family").asString().equals("aggregate") && e.path("queue").path("distinctWaiting").asInt() == 1));
                three.locator("#cancel-run").click(); three.locator("#start-plan").waitFor();
                one.locator("#cancel-run").click(); one.locator("#start-plan").waitFor(); h.proposal(two, "INITIAL_PROPOSAL");
                two.locator("#confirm-accept").check(); two.locator("#accept-proposal").click(); two.locator("#open-repair-setup").waitFor();
                one.locator("#start-plan").click(); h.proposal(one, "INITIAL_PROPOSAL");
                assertTrue(h.snapshot(one).path("workspace").path("proposal").path("reused").asBoolean());
                one.locator("#confirm-accept").check(); one.locator("#accept-proposal").click(); one.locator("#open-repair-setup").waitFor();
                Path archive = temporary.resolve("accepted.zip"); DemoExportBrowserIT.download(h, one, archive);
                three.locator("#demo-bundle-panel summary").evaluate("e=>e.parentElement.open=true"); three.locator("#demo-bundle-archive").setInputFiles(archive);
                three.onceDialog(Dialog::accept); three.locator("#demo-bundle-submit").click();
                three.waitForFunction("()=>document.querySelector('#demo-bundle-status').textContent.includes('Accepted bundle opened')");
                String etag = DemoAcceptanceBrowserIT.etag(h, one);
                assertEquals(409, DemoAcceptanceBrowserIT.command(one, "/api/runs", "POST", Map.of("effort", "QUICK"), etag));
                int before = events(h).size(); for (int i = 0; i < 12; i++) { h.snapshot(one); assertEquals(404, one.request().get(h.base + "/api/runs/" + UUID.randomUUID()).status()); }
                List<JsonNode> afterPolls = events(h); assertTrue(afterPolls.subList(Math.min(before, afterPolls.size()), afterPolls.size()).stream().allMatch(e -> e.path("family").asString().equals("aggregate")), "Polling emits no application activity events");
                h.record("UC-13 main 2-5/G1-G3/G7: three owners exercise actual computation, coalescing, queued/owner cancellation, reuse, two explicit generated acceptances, sample open, export and import; repeated snapshots add no activity event", one, "01-safe-use");
                Thread.sleep(1400); h.stop(); List<JsonNode> events = events(h);
                assertEquals(1, count(events, "process_started", "kind", "SOLVE"));
                assertEquals(1, count(events, "process_finished", "kind", "SOLVE"));
                assertTrue(events.stream().filter(e -> e.path("event").asString().equals("process_finished") && e.path("kind").asString().equals("VERIFICATION")).allMatch(e -> e.path("outcome").asString().equals("VERIFIED")));
                assertEquals(2, count(events, "ACCEPT_INITIAL", null, null)); assertEquals(1, count(events, "SAMPLE_OPEN", null, null));
                assertEquals(1, count(events, "EXPORT", null, null)); assertEquals(1, count(events, "IMPORT", null, null));
                assertEquals(4, count(events, "logical_finished", null, null)); assertEquals(1, count(events, "COALESCED", "operation", "SOLVE"));
                assertEquals(1, count(events, "HIT", "operation", "SOLVE"));
                assertEquals(1, events.stream().filter(e -> e.path("event").asString().equals("refused") && e.path("reason").asString().equals("INVALID_WORKSPACE_TRANSITION")).count());
                long commands = Files.readAllLines(temporary.resolve("launches")).size();
                long started = events.stream().filter(e -> e.path("event").asString().equals("process_started")).count();
                // Two startups each independently verify three samples; they are explicitly outside visitor allowance.
                assertEquals(commands - 6, started); assertEquals(0, events.stream().filter(e -> e.path("event").asString().equals("process_finished")).count() - started);
                List<JsonNode> summaries = events.stream().filter(e -> e.path("family").asString().equals("aggregate")).toList(); assertFalse(summaries.isEmpty());
                assertEquals(2, sum(summaries, "productUse", "ACCEPT_INITIAL")); assertEquals(1, sum(summaries, "cache", "COALESCED"));
                assertTrue(summaries.stream().anyMatch(e -> e.path("queue").path("distinctWaiting").asInt() == 1));
                assertTrue(summaries.stream().anyMatch(e -> e.path("processes").path("runningSolve").asInt() == 1));
                JsonNode last = summaries.getLast(); assertEquals(0, last.path("processes").path("runningSolve").asInt()); assertEquals(0, last.path("allowance").path("reservedMs").asLong()); assertTrue(last.path("allowance").path("consumedMs").asLong() > 0);
                String log = Files.readString(h.output.resolve("server-2.log"));
                for (String forbidden : List.of(privateName, "private-browser-label-582", "different-browser-label-582", "demo-school-001", "John Doe", "JSESSIONID", "127.0.0.1")) assertFalse(log.contains(forbidden), forbidden);
                assertEquals(Set.of("startup", "session", "definition", "work", "reuse", "decision", "aggregate"), events.stream().map(e -> e.path("family").asString()).collect(java.util.stream.Collectors.toSet()));
                assertTrue(events.stream().filter(e -> e.path("family").asString().equals("startup")).allMatch(e -> e.path("serviceArtifactSha256").asString().matches("[0-9a-f]{64}")));
                assertTrue(h.errors.isEmpty()); Files.writeString(h.output.resolve("events.json"), DemoPlanningBrowserIT.JSON.writeValueAsString(events)); Files.writeString(h.output.resolve("verdict.txt"), "PASS\n");
            } catch (Throwable failure) { Files.writeString(h.output.resolve("verdict.txt"), "FAIL: " + failure); h.record("FAIL: " + failure, one, "failure"); throw failure; }
            finally { Files.writeString(h.output.resolve("ledger.md"), "# UC-13 local operator walkthrough\n\n" + Instant.now() + "; local packaged TLS/real kernel/Chrome, three isolated visitors. Operator reads raw single-line JSON from server-2.log; one-second configured summaries accelerate the same production code.\n\n" + String.join("\n\n", h.steps) + "\n\nOverall: " + Files.readString(h.output.resolve("verdict.txt")) + "\nPASS requires process counts to reconcile wrapper commands, startup exclusions, interval counters and exact actual decisions, plus private-sentinel and poll-spam checks. UTC rollover/settlement tested with injected clock in DemoEventsTest. Provider resource/spend/log-retention verification deferred until requested deployment. Contexts and child processes cleaned up.\n"); }
        } finally { h.stop(); }
    }
    static List<JsonNode> eventsUnchecked(DemoPlanningBrowserIT h) { try { return events(h); } catch (Exception failed) { throw new RuntimeException(failed); } }
    static long count(List<JsonNode> events, String event, String field, String value) { return events.stream().filter(e -> event.equals(e.path("event").asString()) && (field == null || value.equals(e.path(field).asString()))).count(); }
    static long sum(List<JsonNode> summaries, String group, String field) { return summaries.stream().mapToLong(e -> e.path(group).path(field).asLong()).sum(); }
    static List<JsonNode> events(DemoPlanningBrowserIT h) throws Exception { return Files.readAllLines(h.output.resolve("server-2.log")).stream().filter(line -> line.startsWith("{\"family\":" )).map(DemoPlanningBrowserIT.JSON::readTree).toList(); }
}
