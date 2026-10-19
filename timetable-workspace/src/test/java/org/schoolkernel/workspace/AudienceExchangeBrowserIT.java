package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.schoolkernel.workspace.SchoolSetupBrowserIT.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AudienceExchangeBrowserIT {
 @TempDir Path temporary;
 @Test void transfersExactAudienceDefinitionsWithExplicitPreviewAndRawRecovery() throws Exception {
  var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.initialize("audience-exchange",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-15/"+System.currentTimeMillis()));
  try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)) {
   Page p=context.newPage();p.onPageError(h.errors::add);
   try {
    p.navigate(h.base+"/workspace/");p.locator("#school-setup-entry").waitFor();
    var source=DemoPlanningBrowserIT.JSON.readTree(Files.readString(Path.of("src/test/resources/student-groups/inspection-pairs.json"))).path("nested").path("definition").deepCopy();
    p.evaluate("""
     source=>{const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'))??{formatVersion:1,schools:{},timetables:{},drafts:{},lastTimetable:{},selected:null,editor:null};s.schools.exchange={id:'exchange',name:'Exact grouped Õ source',definition:source};localStorage.setItem('school-kernel.timetable-demo.v1',JSON.stringify(s));}
    """,DemoPlanningBrowserIT.JSON.readValue(source.toString(),Map.class));
    p.reload();p.locator("#school-selector").selectOption("exchange");open(p);
    Object before=durable(p);p.locator("#setup-convert").click();assertThat(p.locator(".setup-confirm")).containsText("No automatic teacher selection",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
    h.record("UC-15 main1–3/3a/G4: exact grouped source conversion names contract2/catalog11 and no policy adoption; preview cancellation preserves both working model and immutable source by value",p,"01-preview-cancel");
    p.locator("#setup-convert").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
    Object compiled=p.evaluate("""
     async()=>{const{compileDraft}=await import('/workspace/setup-compile.js');const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'));return compileDraft(Object.values(s.setupDrafts).find(d=>!d.retained));}
    """);
    assertEquals(DemoPlanningBrowserIT.JSON.readValue(source.toString(),Map.class),compiled);
    Download download=p.waitForDownload(()->tool(p,"#setup-export").click());Path exported=h.output.resolve("grouped-school.json");download.saveAs(exported);
    var exportedDocument=DemoPlanningBrowserIT.JSON.readTree(Files.readString(exported));assertEquals(source.path("audiencePlans"),exportedDocument.path("audiencePlans"));assertEquals(source.path("cohorts"),exportedDocument.path("classes"));
    close(p);p.reload();p.locator("#school-selector").selectOption("exchange");open(p);
    assertEquals(compiled,p.evaluate("""
     async()=>{const{compileDraft}=await import('/workspace/setup-compile.js');const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'));return compileDraft(Object.values(s.setupDrafts).find(d=>!d.retained));}
    """));
    h.record("UC-15 main4–5/G1/G5: accepted separate structured draft compiles by value to exact source, full school JSON export preserves audience policies/anonymous relationships; reload preserves identities, original labels and source meaning",p,"02-exact-reload");
    String raw=source.toString().replaceFirst("\\{","{\"futureSetting\":{\"verbatim\":\"Õ raw original\"},");
    tool(p,"#school-definition-json").fill(raw);saved(p);Object pending=durable(p);
    p.locator("#setup-pasted-import").click();assertThat(p.locator("#setup-message")).containsText("Import refused",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));assertThat(p.locator("#setup-message")).containsText("futureSetting");assertEquals(pending,durable(p));
    Download recovery=p.waitForDownload(()->p.locator("#setup-download").click());recovery.saveAs(h.output.resolve("unmappable-recovery.json"));
    close(p);p.reload();p.locator("#school-selector").selectOption("exchange");open(p);assertThat(tool(p,"#school-definition-json")).hasValue(raw);
    p.locator("#setup-export").click();assertThat(p.locator("#setup-message")).containsText("Export refused");
    h.record("UC-15 2b/2c/G4/G5: real kernel check identifies unknown futureSetting; conversion and export refuse without dropping raw Unicode text, reload/recovery retain it, structured model/source remain unchanged",p,"03-unmappable-raw");
    p.locator("#setup-discard-json").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
    // A failed durable transfer must not report success or replace the source.
    tool(p,"#setup-import-file").setInputFiles(exported);
    p.locator(".setup-confirm").waitFor();p.locator(".setup-confirm [data-cancel]").click();
    assertEquals(compiled,p.evaluate("""
     async()=>{const{compileDraft}=await import('/workspace/setup-compile.js');const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'));return compileDraft(Object.values(s.setupDrafts).find(d=>!d.retained));}
    """));
    Object stable=durable(p);
    tool(p,"#setup-import-file").setInputFiles(exported);p.locator(".setup-confirm").waitFor();
    p.evaluate("()=>{window.__exchangeSet=Storage.prototype.setItem;const count=Object.keys(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).length;Storage.prototype.setItem=function(k,v){if(k==='"+KEY+"'&&Object.keys(JSON.parse(v).setupDrafts??{}).length>count)throw new DOMException('isolated conversion quota','QuotaExceededError');return window.__exchangeSet.call(this,k,v)}}");
    p.locator(".setup-confirm [data-confirm]").click();assertThat(p.locator("#setup-durability")).hasText("Only on this page");assertEquals(stable,durable(p));
    Download unsaved=p.waitForDownload(()->tool(p,"#setup-download").click());unsaved.saveAs(h.output.resolve("unsaved-conversion-recovery.json"));
    p.evaluate("()=>{Storage.prototype.setItem=window.__exchangeSet}");p.locator("#setup-retry").click();saved(p);
    h.record("UC-15 4b: injected atomic new-draft quota refuses durable transfer without replacing source or older working draft; complete imported work stays recoverable on page/download; explicit retry saves once storage is restored",p,"04-retention-retry");
    assertEquals(source.toString(),p.evaluate("JSON.stringify(JSON.parse(localStorage.getItem('"+KEY+"')).schools.exchange.definition)"));
    assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
   }catch(Throwable f){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+f);h.record("FAIL "+f,p,"failure");throw f;}
   finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-15 administrator advanced/structured exchange\n\n"+Instant.now()+"\n"+h.base+"\nPinned packaged working diff based on3cb66f3. Real Chrome; disposable browser data; published CLI check.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nBrowser/server/temp cleanup by harness.\n");}
  }finally{h.stop();}
 }
}
