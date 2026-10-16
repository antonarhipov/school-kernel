package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.net.*;
import javax.net.ssl.*;
import java.nio.charset.StandardCharsets;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.node.ObjectNode;

/** Actual packaged HTTP, kernel and Chrome journey; no operator data or database. */
class DefinitionInputHardeningBrowserIT {
    @TempDir Path temporary;

    @Test void safelyChecksDefinitionsAndRetainsDebouncedEditorWork() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("input-hardening", Path.of(System.getProperty("hardening.walkthrough.dir",
                "../output/playwright/definition-input-hardening/UC-1/local-" + System.currentTimeMillis())));
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext owner = h.context(browser)) {
            Page p = owner.newPage(); p.onPageError(h.errors::add);
            try {
                ready(p, h); p.locator("#school-selector").selectOption("two-class");
                p.locator("#demo-utilities summary").click();
                String original = p.locator("#school-definition-json").inputValue();
                int schoolCount = p.locator("#school-selector option").count();
                instrumentStorage(p);
                for (int i = 0; i < 8; i++) p.locator("#school-definition-json").fill("{\n draft " + i);
                p.waitForFunction("()=>JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1')).editor?.text==='{\\n draft 7'");
                assertEquals(1, ((Number) p.evaluate("window.auditWrites")).intValue());
                p.locator("#school-definition-json").fill("{\n latest before reload");
                p.reload(); waitReady(p); p.locator("#demo-utilities summary").click();
                assertEquals("{\n latest before reload", p.locator("#school-definition-json").inputValue());
                h.record("UC-1 main 1-2: eight rapid edits produce one durable write; an immediate reload flushes the exact latest incomplete JSON", p, "01-debounced-reload");

                instrumentStorage(p); String prior = raw(p);
                p.locator("#school-definition-json").evaluate("e=>{e.value='€'.repeat(699051);e.dispatchEvent(new Event('input',{bubbles:true}));}");
                assertThat(p.locator("#editor-status")).containsText("only in the open editor");
                settle(p); assertEquals(prior, raw(p)); assertEquals(0, ((Number)p.evaluate("window.auditWrites")).intValue());
                assertEquals(699051, p.locator("#school-definition-json").inputValue().length());
                assertEquals(schoolCount, p.locator("#school-selector option").count());
                p.locator("#school-copy-name").fill("Oversized audit copy");
                p.locator("#save-school-copy").click();
                assertThat(p.locator("#editor-status")).containsText("at most 2 MiB");
                assertEquals(prior, raw(p));
                h.record("UC-1 extension 2a/G2/G5: a multibyte paste above 2 MiB remains editable, visibly explains memory-only retention, performs zero storage writes and preserves the exact prior raw browser document", p, "02-oversized-editor");

                p.evaluate("window.auditQuota=true");
                p.locator("#school-definition-json").fill(original);
                p.waitForFunction("()=>document.querySelector('#demo-notice').textContent.includes('full or unavailable')");
                assertEquals(1, ((Number)p.evaluate("window.auditWrites")).intValue());
                for (int i = 0; i < 4; i++) p.locator("#school-copy-name").fill("Recovered copy " + i);
                settle(p); assertEquals(1, ((Number)p.evaluate("window.auditWrites")).intValue());
                assertEquals(prior, raw(p));
                p.evaluate("window.auditQuota=false"); p.locator("#close-editor").click();
                assertThat(p.locator("#demo-notice")).isHidden();
                assertEquals(original, DemoPlanningBrowserIT.JSON.readTree(raw(p)).path("editor").path("text").asString());
                p.reload(); waitReady(p); p.locator("#demo-utilities summary").click();
                assertEquals(original, p.locator("#school-definition-json").inputValue());
                assertThat(p.locator("#school-copy-name")).hasValue("Recovered copy 3");
                h.record("UC-1 extension 2b/G5: injected browser quota refusal preserves prior bytes, pauses automatic retry across later edits, and explicit close retries successfully after recovery; exact text/name survive reload", p, "03-quota-recovery");

                var before = h.snapshot(p);
                long launches = Files.readAllLines(temporary.resolve("launches")).size();
                var token = DemoPlanningBrowserIT.JSON.readTree(p.request().get(h.base + "/api/csrf").body());
                String malformed = original.replaceFirst("\"catalogVersion\"\\s*:\\s*9", "\"catalogVersion\":" + "9".repeat(400));
                var response = p.request().post(h.base + "/api/demo/definition/check", RequestOptions.create()
                        .setHeader("Origin", h.base).setHeader(token.path("headerName").asString(), token.path("token").asString())
                        .setHeader("Content-Type", "text/plain").setData(malformed));
                assertEquals(422, response.status());
                assertEquals(before, h.snapshot(p));
                assertEquals(launches, Files.readAllLines(temporary.resolve("launches")).size());
                Files.writeString(h.output.resolve("malformed-response.json"), response.text());
                h.record("UC-1 extension 4a/G2: the actual packaged HTTP endpoint rejects the 400-digit integer with 422, starts no kernel process and retains exact server state", p, "04-malformed-http");

                p.locator("#school-copy-name").fill("Checked audit copy");
                p.locator("#save-school-copy").click();
                p.waitForFunction("()=>document.querySelector('#editor-status').textContent.includes('saved in this browser')");
                assertEquals(schoolCount + 1, p.locator("#school-selector option").count());
                var saved = DemoPlanningBrowserIT.JSON.readTree(raw(p));
                var copy = saved.path("schools").path(p.locator("#school-selector").inputValue());
                assertEquals(DemoPlanningBrowserIT.JSON.readTree(original), copy.path("definition"));
                assertEquals(0, Files.readAllLines(temporary.resolve("launches")).stream().filter("plan"::equals).count());
                assertFalse(saved.hasNonNull("editor"));
                settle(p); assertFalse(DemoPlanningBrowserIT.JSON.readTree(raw(p)).hasNonNull("editor"));
                p.setViewportSize(390, 844);
                assertTrue((Boolean)p.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                h.record("UC-1 main 3-5/G4/G6: real kernel verification saves a separate exact 56-lesson school without solving or resurrecting the pending editor; feedback fits 390px", p, "05-checked-school-mobile");
                assertTrue(h.errors.isEmpty(), h.errors.toString());
                Files.writeString(h.output.resolve("verdict.txt"), "PASS\n");
            } catch (Throwable failure) {
                Files.writeString(h.output.resolve("verdict.txt"), "FAIL: " + failure + "\n");
                h.record("FAIL: " + failure, p, "failure"); throw failure;
            } finally { ledger(h); }
        } finally { h.stop(); }
    }

    @Test void packagedAdmissionRejectsSlowBodiesBeforeDecodingAndRecovers() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        Path output = Path.of(System.getProperty("hardening.http.dir", "../output/execution/definition-input-hardening/http-" + System.currentTimeMillis()));
        h.initialize("admission", output);
        var sockets = new ArrayList<SSLSocket>();
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext one = h.context(browser); BrowserContext two = h.context(browser); BrowserContext three = h.context(browser)) {
            Page a = one.newPage(), b = two.newPage(), c = three.newPage();
            ready(a,h); ready(b,h); ready(c,h);
            var before = h.snapshot(a);
            var token = DemoPlanningBrowserIT.JSON.readTree(a.request().get(h.base + "/api/csrf").body());
            sockets.add(slowBody(h, one, token)); sockets.add(slowBody(h, one, token));
            awaitBusy(h, a, token);
            assertEquals(200, a.request().get(h.base + "/api/workspace").status());
            var tokenTwo = DemoPlanningBrowserIT.JSON.readTree(b.request().get(h.base + "/api/csrf").body());
            sockets.add(slowBody(h, two, tokenTwo)); sockets.add(slowBody(h, two, tokenTwo));
            var tokenThree = DemoPlanningBrowserIT.JSON.readTree(c.request().get(h.base + "/api/csrf").body());
            var busy = awaitBusy(h, c, tokenThree);
            Files.writeString(output.resolve("global-busy.json"), busy.text());
            assertEquals(before, h.snapshot(a));
            sockets.forEach(s -> { try { s.close(); } catch(Exception ignored) {} }); sockets.clear();
            // EOF must release every input lease. A small invalid object then reaches validation again.
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            int status;
            do { status = check(h, a, token).status(); } while (status == 429 && System.nanoTime() < deadline);
            assertEquals(422, status);
            assertEquals(before, h.snapshot(a));
            Files.writeString(output.resolve("ledger.md"), "# UC-1 production HTTP admission evidence\n\nDate: " + Instant.now()
                    + "\nApplication: " + h.base + "\nIsolated packaged service, real TLS sockets and disposable browser sessions.\n\n"
                    + "- PASS: two incomplete bodies occupy one owner's slots; a third is rejected with 429/Retry-After before decoding.\n"
                    + "- PASS: two other incomplete bodies fill the global ceiling; a third owner is rejected with INPUT_BUSY.\n"
                    + "- PASS: safe workspace reads remain usable; exact initial workspace is unchanged.\n"
                    + "- PASS: closing incomplete clients releases leases; subsequent input reaches ordinary validation (422).\n\n"
                    + "No kernel launches beyond startup were requested. All sockets, contexts and the disposable server close.\nOverall: PASS\n");
        } finally {
            for (var s : sockets) s.close(); h.stop();
        }
    }
    static APIResponse check(DemoPlanningBrowserIT h, Page p, tools.jackson.databind.JsonNode token) {
        return p.request().post(h.base + "/api/demo/definition/check", RequestOptions.create().setHeader("Origin", h.base)
                .setHeader(token.path("headerName").asString(), token.path("token").asString()).setHeader("Content-Type", "text/plain").setData("{}"));
    }
    static APIResponse awaitBusy(DemoPlanningBrowserIT h, Page p, tools.jackson.databind.JsonNode token) {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        APIResponse response;
        do { response = check(h,p,token); } while(response.status() != 429 && System.nanoTime() < deadline);
        assertEquals(429, response.status()); assertEquals("1", response.headers().get("retry-after"));
        assertEquals("INPUT_BUSY", DemoPlanningBrowserIT.JSON.readTree(response.body()).path("code").asString()); return response;
    }
    static SSLSocket slowBody(DemoPlanningBrowserIT h, BrowserContext owner, tools.jackson.databind.JsonNode token) throws Exception {
        var context = SSLContext.getInstance("TLS");
        context.init(null, new javax.net.ssl.TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs,String auth) {}
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs,String auth) {}
        }}, new java.security.SecureRandom());
        var socket = (SSLSocket)context.getSocketFactory().createSocket("localhost",h.port);
        socket.setSoTimeout(5000); socket.startHandshake();
        String cookie = owner.cookies().stream().filter(x -> x.name.equals("JSESSIONID")).findFirst().orElseThrow().value;
        String headers = "POST /api/demo/definition/check HTTP/1.1\r\nHost: localhost:" + h.port + "\r\nOrigin: " + h.base
                + "\r\nCookie: JSESSIONID=" + cookie + "\r\n" + token.path("headerName").asString() + ": " + token.path("token").asString()
                + "\r\nContent-Type: text/plain\r\nContent-Length: 1000\r\n\r\n{";
        socket.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8)); socket.getOutputStream().flush(); return socket;
    }
    static void ready(Page p, DemoPlanningBrowserIT h) { p.navigate(h.base + "/workspace/"); waitReady(p); }
    static void waitReady(Page p) { p.locator("#school-selector option[value=full]").waitFor(new Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)); }
    static String raw(Page p) { return (String)p.evaluate("()=>localStorage.getItem('school-kernel.timetable-demo.v1')"); }
    static void instrumentStorage(Page p) { p.evaluate("""
            ()=>{window.auditWrites=0;window.auditQuota=false;
              const original=Storage.prototype.setItem;
              Storage.prototype.setItem=function(k,v){if(k==='school-kernel.timetable-demo.v1'){
                window.auditWrites++;if(window.auditQuota)throw new DOMException('Injected quota','QuotaExceededError');}
                return original.call(this,k,v);};}
            """); }
    static void settle(Page p) { p.evaluate("()=>new Promise(resolve=>setTimeout(resolve,650))"); }
    static void ledger(DemoPlanningBrowserIT h) throws Exception {
        Files.write(h.output.resolve("browser-errors.txt"),h.errors);
        Files.writeString(h.output.resolve("ledger.md"), "# UC-1 definition input hardening walkthrough\n\nDate: " + Instant.now()
                + "\nApplication: " + h.base + "\nPackaged workspace/kernel, real Chrome via Java Playwright, real TLS; isolated disposable browser data; no database.\n\n"
                + String.join("\n\n",h.steps) + "\n\nOverall: " + Files.readString(h.output.resolve("verdict.txt"))
                + "\nQuota failures are explicitly injected at the browser storage boundary; successful checks use the real kernel. Contexts close and the private server stops.\n");
    }
}
