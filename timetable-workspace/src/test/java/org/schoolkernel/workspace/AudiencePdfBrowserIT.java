package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AudiencePdfBrowserIT {
 @TempDir Path temporary;
 static final String KEY="school-kernel.timetable-demo.v1";
 @Test void exportsTheSelectedReviewedGroupedWeekAndRecoversResourceFailure() throws Exception {
  var h=new DemoPlanningBrowserIT();h.temporary=temporary;h.initialize("audience-pdf",Path.of("../output/playwright/student-groups-and-coordinated-lessons/UC-16/"+System.currentTimeMillis()));
  try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));BrowserContext context=h.context(browser)) {
   Page p=context.newPage();p.onPageError(h.errors::add);
   try {
    p.navigate(h.base+"/workspace/");p.locator("#school-setup-entry").waitFor();
    var fixtures=DemoPlanningBrowserIT.JSON.readTree(Files.readString(Path.of("src/test/resources/student-groups/inspection-pairs.json")));
    for(String id:List.of("nested","groups2","groups3","groups4","groups8","partial")) {
     p.evaluate("""
      async input=>{
       const {demoRequest}=await import('/workspace/demo-transport.js');const pair=input.pair;
       const {value}=await demoRequest('/api/demo/timetable/review',{method:'POST',body:{definition:pair.definition,result:{timetable:{assignments:pair.assignments}}}});
       if(!value.valid)throw Error(JSON.stringify(value));
       const s=JSON.parse(localStorage.getItem('school-kernel.timetable-demo.v1'))??{formatVersion:1,schools:{},timetables:{},drafts:{},lastTimetable:{},selected:null,editor:null};
       s.schools[input.id]={id:input.id,name:'Print '+input.id,definition:pair.definition};s.timetables[input.id]={[input.id]:{id:input.id,name:'Exact '+input.id,definition:pair.definition,result:value.result}};
       localStorage.setItem('school-kernel.timetable-demo.v1',JSON.stringify(s));
      }
     """,Map.of("id",id,"pair",DemoPlanningBrowserIT.JSON.readValue(fixtures.path(id).toString(),Map.class)));
    }
    p.reload();p.locator("#school-selector").waitFor();
    for(String id:List.of("nested","groups2","groups3","groups4","groups8","partial")) {
     p.locator("#school-selector").selectOption(id);String before=(String)p.evaluate("localStorage.getItem('"+KEY+"')");
     Download d=p.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(90000),()->p.locator("#export-timetable-pdf").click());d.saveAs(h.output.resolve(id+".pdf"));
     assertEquals(before,p.evaluate("localStorage.getItem('"+KEY+"')"));assertThat(p.locator("#school-selector")).hasValue(id);
     h.record("UC-16 main1–4/G1–G4: actual selected "+id+" reviewed pair downloads via PDF control; source, exact assignments, selection and prior outputs unchanged",p,"export-"+id);
    }
    p.locator("#school-selector").selectOption("nested");p.locator("#teacher-filter").selectOption("TE");
    Download filtered=p.waitForDownload(()->p.locator("#export-timetable-pdf").click());filtered.saveAs(h.output.resolve("teacher.pdf"));assertThat(p.locator("#teacher-filter")).hasValue("TE");
    h.record("UC-16 2b: teacher-scoped full week downloads; other teaching omitted explicitly rather than marked untaught; filter selection unchanged",p,"teacher-scope");
    // A new document has no cached PDF fonts. Refuse resource failure without a partial download.
    Page failed=context.newPage();failed.onPageError(h.errors::add);failed.route("**/LiberationSans-*.ttf",route->route.abort());failed.navigate(h.base+"/workspace/");failed.locator("#school-selector").selectOption("nested");
    String stable=(String)failed.evaluate("localStorage.getItem('"+KEY+"')");var downloads=new ArrayList<String>();failed.onDownload(d->downloads.add(d.suggestedFilename()));
    failed.locator("#export-timetable-pdf").click();assertThat(failed.locator("#pdf-export-status")).containsText("PDF fonts could not load",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(90000));
    assertTrue(downloads.isEmpty());assertEquals(stable,failed.evaluate("localStorage.getItem('"+KEY+"')"));assertThat(failed.locator("#export-timetable-pdf")).isEnabled();
    failed.unroute("**/LiberationSans-*.ttf");Download retry=failed.waitForDownload(()->failed.locator("#export-timetable-pdf").click());retry.saveAs(h.output.resolve("retry-nested.pdf"));assertEquals(stable,failed.evaluate("localStorage.getItem('"+KEY+"')"));
    h.record("UC-16 4a: actual failed font request reports error with zero download; exact pair/selection survive; retry produces complete PDF; save-dialog cancellation is outside observable browser boundary",failed,"failure-retry");failed.close();
    assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
   }catch(Throwable f){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+f);h.record("FAIL "+f,p,"failure");throw f;}
   finally{Files.writeString(h.output.resolve("ledger.md"),"# UC-16 grouped PDF administrator journey\n\n"+Instant.now()+"\n"+h.base+"\nPinned packaged working diff based on3cb66f3; real Chrome/CSP and published CLI review; disposable data. PDFs retained beside screenshots for independent raster/value checks.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nBrowser/server/temp cleanup by harness.\n");}
  }finally{h.stop();}
 }
}
