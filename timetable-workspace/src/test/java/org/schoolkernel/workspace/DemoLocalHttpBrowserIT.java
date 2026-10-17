package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoLocalHttpBrowserIT {
    @TempDir Path temporary;
    @Test void plainDemoProfileServesLocalHttpWithoutStartupKernelProcesses() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.plainHttp=true;
        h.initialize("local-http",Path.of("../output/playwright/school-setup-editor/UI-local-http/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=browser.newContext(new Browser.NewContextOptions().setViewportSize(1440,1000))) {
            Page p=owner.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");DemoTimetableBrowserIT.readyWait(p);
                assertTrue(p.url().startsWith("http://localhost:"));
                assertThat(p.locator("#school-selector")).hasValue("full");
                assertThat(p.locator("#timetable-selector")).hasValue("a");
                assertEquals(520,p.locator(".demo-lesson").count());
                assertEquals("",Files.readString(temporary.resolve("launches")));
                assertFalse(owner.cookies().isEmpty());assertTrue(owner.cookies().stream().noneMatch(c->c.secure));
                h.record("Plain demo profile, default localhost configuration: actual HTTP Chrome arrival has a usable local session, clean precomputed matrix and zero startup kernel launches; no certificate or HTTPS-ignore setting",p,"01-http-matrix");
                p.locator("#school-selector").selectOption("two-class");
                p.locator(".demo-lesson").first().click();
                String original=p.locator("#inspector-period").inputValue();
                String other=(String)p.locator("#inspector-period option").evaluateAll("(rows,original)=>rows.find(r=>r.value!==original).value",original);
                p.locator("#inspector-period").selectOption(other);
                p.waitForFunction("()=>document.querySelector('#timetable-save-bar').textContent.includes('Ready to save')||document.querySelector('#timetable-save-bar').textContent.includes('Must fix')");
                assertFalse(Files.readString(temporary.resolve("launches")).isBlank());
                h.record("Explicit local HTTP timetable review POST passes same-origin/CSRF gates using real cookies. Kernel review starts only after the visitor edits; feedback is rendered",p,"02-http-explicit-review");
                p.keyboard().press("Escape");p.locator("#school-setup-entry").click();p.locator("#setup-new").click();
                p.locator("#setup-name").fill("Local HTTP school");SchoolSetupBrowserIT.saved(p);SchoolSetupBrowserIT.close(p);
                p.reload();DemoTimetableBrowserIT.readyWait(p);assertThat(p.locator("#school-selector")).hasValue("full");
                assertThat(p.locator("#school-setup")).not().isVisible();
                p.locator("#school-selector").selectOption("two-class");p.locator("#school-setup-entry").click();
                assertThat(p.locator("#setup-name")).hasValue("Local HTTP school");
                p.locator("#school-setup").evaluate("async d=>{await Promise.all(d.getAnimations().map(a=>a.finished.catch(()=>{})));}");
                h.record("Local HTTP setup overlay saves an independent browser draft; reload opens clean bundled timetable and explicit school/setup navigation resumes the exact draft",p,"03-http-recovery");
                Page ip=owner.newPage();ip.onPageError(h.errors::add);ip.navigate(h.base.replace("localhost","127.0.0.1")+"/workspace/");DemoTimetableBrowserIT.readyWait(ip);
                assertThat(ip.locator("#school-selector")).hasValue("full");
                assertTrue((Boolean)ip.evaluate("async()=>{const {demoRequest}=await import('/workspace/demo-transport.js');const b=await(await fetch('/api/demo/timetables')).json();const d=b.schools.find(s=>s.identifier==='two-class').definition;const {value}=await demoRequest('/api/demo/definition/check',{method:'POST',headers:{'Content-Type':'text/plain'},body:JSON.stringify(d)});return typeof value.definitionRevision==='string';}"));
                h.record("127.0.0.1 HTTP alias also opens its independent matrix and same-origin session without custom host configuration",ip,"04-ipv4-http");
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){h.record("FAIL: "+failure,p,"failure");Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# Local demo HTTP and direct bundled loading\n\nPackaged local app with demo profile; ephemeral test port and kernel wrapper are the only test boundaries. No TLS/public-host override and no ignored certificate errors. Installed Chrome, isolated disposable context; no operator browser data or database.\n\n"+String.join("\n\n",h.steps)+"\n\n"+Files.readString(h.output.resolve("verdict.txt"))+"\nBrowser contexts and local process closed by the harness.\n");}
        }finally{h.stop();}
    }
}
