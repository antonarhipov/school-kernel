package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Isolated administrator journeys against pinned packaged demo and installed Chrome. */
class SchoolSetupBrowserIT {
    @TempDir Path temporary;
    static final String KEY = "school-kernel.timetable-demo.v1";
    static void section(Page p, String label) { p.locator("#school-setup [data-section='"+label+"']").click(); }
    static void saved(Page p) { assertThat(p.locator("#setup-durability")).hasText("Saved in this browser"); }
    static Object durable(Page p) { return p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"'))"); }
    static void open(Page p) { p.locator("#school-setup-entry").click(); assertThat(p.locator("#school-setup")).isVisible(); if(p.locator(".setup-start").isVisible()) p.locator("#setup-copy-source").click(); }
    static void close(Page p) { p.locator("#school-setup [data-close]").click(); assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000)); }
    static Locator tool(Page p, String selector) {
        if (!p.locator(selector).isVisible()) p.locator("#setup-tools > summary").click();
        return p.locator(selector);
    }

    @Test void startsResumesRetainsSourcesAndRecoversFailures() throws Exception {
        var h = new DemoPlanningBrowserIT(); h.temporary = temporary;
        h.initialize("setup", Path.of("../output/playwright/school-setup-editor/UC-1/"+System.currentTimeMillis()));
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
             BrowserContext owner = h.context(browser)) {
            Page p = owner.newPage(); p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/"); p.locator("#school-selector option[value=english]").waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
                p.locator("#school-selector").selectOption("english");
                p.locator("#cohort-filter").selectOption("meadowbrook-cohort-1A");
                String matrix = p.locator("#timetable-matrix").innerHTML();
                open(p); assertTrue((Boolean)p.evaluate("document.querySelector('#school-setup').contains(document.activeElement)"));
                assertEquals(6, p.locator("nav[aria-label='Setup sections'] > button").count());
                assertThat(p.locator("#setup-source")).containsText("saved school definition");
                section(p,"Teachers"); assertThat(p.locator("#setup-record-count")).hasText("41 of 41 records");
                p.locator("#setup-search").fill("meadowbrook-teacher");
                assertThat(p.locator("#setup-record-count")).hasText("41 of 41 records");
                p.locator("#school-setup [data-record]").last().click();
                assertThat(p.locator("#setup-record-detail pre")).isVisible();
                section(p,"Classes"); assertThat(p.locator("#setup-record-count")).hasText("22 of 22 records");
                p.locator("#school-setup [data-record]").last().click();
                p.locator("#setup-convert").click(); assertThat(p.locator(".setup-confirm")).containsText("Existing source and working draft remain independent",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));p.locator(".setup-confirm [data-cancel]").click();
                h.record("UC-1 1-3/2a/G3/G4/G8: modal focus; exactly six sections; retained source permits search by IDs/names across 41 teachers and 22 classes, focused inspection; explicit UC-15 handoff leaves source untouched",p,"01-source-browser");
                tool(p,"#setup-new").click(); assertThat(p.locator("#setup-name")).hasValue("");
                assertThat(p.locator("#setup-section-title")).hasText("Timetable grid");
                p.locator("#setup-name").fill("<script>unfinished & school</script>");
                section(p,"Teachers"); assertThat(p.locator("#setup-record-count")).hasText("0 of 0 records"); saved(p);
                String firstId=(String)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).id");
                close(p); assertThat(p.locator("#cohort-filter")).hasValue("meadowbrook-cohort-1A");
                assertEquals(matrix,p.locator("#timetable-matrix").innerHTML());
                assertTrue((Boolean)p.evaluate("document.activeElement.id==='school-setup-entry'"));
                p.reload(); p.locator("#school-setup-entry").waitFor(); p.locator("#school-selector").selectOption("english"); open(p);
                assertThat(p.locator("#setup-name")).hasValue("<script>unfinished & school</script>");
                assertThat(p.locator("#setup-section-title")).hasText("Teachers");
                assertEquals(0,p.locator("#school-setup script").count());
                h.record("UC-1 4-6/G1-G3/G6-G7: empty school has zero records; incomplete literal name and editing location restore after reload; close restores exact filtered matrix and entry focus; no solve or school save",p,"02-restored-empty");
                tool(p,"#setup-new").click(); p.locator("#setup-name").fill("Independent new draft"); saved(p);
                tool(p,"#setup-draft-selector").selectOption(firstId); assertThat(p.locator("#setup-name")).hasValue("<script>unfinished & school</script>");
                Object before=durable(p); tool(p,"#setup-discard").click();
                assertTrue((Boolean)p.evaluate("document.activeElement.hasAttribute('data-cancel')"));
                p.locator(".setup-confirm [data-cancel]").click(); assertEquals(before,durable(p));
                // Browser fault injection: real storage setter refuses writes while the full rendered draft stays usable.
                p.evaluate("()=>{window.realSet=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='"+KEY+"')throw new DOMException('quota','QuotaExceededError');return window.realSet.call(this,k,v)}}");
                p.locator("#setup-name").fill("Quota retained edit");
                assertThat(p.locator("#setup-durability")).hasText("Only on this page"); assertEquals(before,durable(p));
                section(p,"Rooms"); close(p); open(p); assertThat(p.locator("#setup-name")).hasValue("Quota retained edit");
                h.record("UC-1 4a/5a/6a/G1-G2: separate same-browser drafts retain identity; cancelled discard changes nothing; injected quota refusal preserves exact durable bytes and in-memory edit across sections/close/reopen",p,"03-quota-retention");
                p.evaluate("()=>{Storage.prototype.setItem=window.realSet}"); p.locator("#setup-retry").click(); saved(p);
                Page other=owner.newPage(); other.onPageError(h.errors::add); other.navigate(h.base+"/workspace/"); other.locator("#school-setup-entry").waitFor(); other.locator("#school-selector").selectOption("english"); open(other);
                other.locator("#setup-name").fill("Other tab current"); saved(other);
                assertThat(p.locator("#setup-durability")).hasText("Storage conflict");
                p.locator("#setup-name").fill("Conflicting page edit");
                assertThat(p.locator("#setup-durability")).hasText("Storage conflict");
                assertTrue((Boolean)other.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).some(d=>d.name==='Other tab current')"));
                p.locator("#setup-recover").click();
                Download download=p.waitForDownload(()->p.locator(".setup-confirm [data-confirm]").click());
                download.saveAs(h.output.resolve("conflict-recovery.json"));
                assertTrue(Files.readString(h.output.resolve("conflict-recovery.json")).contains("Conflicting page edit"));
                assertThat(p.locator("#setup-name")).hasValue("Other tab current");
                assertThat(p.locator("#setup-message")).containsText("Latest browser data loaded");
                p.locator("#setup-keep-recovered").click(); saved(p);
                assertThat(p.locator("#setup-name")).hasValue("Conflicting page edit");
                assertTrue((Boolean)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).some(d=>d.recoveredFrom&&d.name==='Conflicting page edit')"));
                h.record("UC-1 5b: real second tab changes catalog; stale overwrite refused; explicit recovery downloads full exact conflicting snapshot, reads latest source then creates independent recovered draft without merging",p,"04-recovered-conflict");
                other.close();
                for(int width:List.of(768,1024,1440)) {
                    p.setViewportSize(width,900); assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth<=innerWidth+1"));
                    assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                    h.record("UC-1 G5/G6: all setup actions/sections remain within "+width+" CSS pixels without overlay or page overflow",p,"05-layout-"+width);
                }
                tool(p,"#setup-discard").click(); p.locator(".setup-confirm [data-confirm]").click();
                assertThat(p.locator("#school-setup")).not().isVisible();
                assertTrue((Boolean)p.evaluate("!Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).some(d=>d.recoveredFrom)"));
                // Retain unknown setup data and allow independent empty work without downgrading it.
                p.evaluate("const s=JSON.parse(localStorage.getItem('"+KEY+"'));s.setupDrafts.future={formatVersion:99,raw:'keep exactly'};localStorage.setItem('"+KEY+"',JSON.stringify(s))");
                p.reload(); p.locator("#school-setup-entry").waitFor(); p.locator("#school-selector").selectOption("english"); open(p);
                assertThat(p.locator("#setup-message")).containsText("unsupported authoring data");
                assertTrue((Boolean)p.evaluate("JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts.future.raw==='keep exactly'"));
                // Multiple source drafts require explicit choice, even if their display names match.
                p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"'));const d=Object.values(s.setupDrafts).find(d=>d.source);s.setupDrafts.second={...structuredClone(d),id:'second',recoveredFrom:d.id};delete s.setupSelection.english;localStorage.setItem('"+KEY+"',JSON.stringify(s));}");
                p.reload(); p.locator("#school-selector").selectOption("english"); open(p); assertEquals(2,p.locator(".setup-draft-choice").count());
                p.locator(".setup-draft-choice").last().click(); section(p,"Teachers");
                p.setViewportSize(768,900); assertThat(p.locator("#setup-record-count")).hasText("41 of 41 records");
                p.locator("#setup-search").fill("no match"); assertThat(p.locator("#setup-record-count")).hasText("0 of 41 records");
                p.locator("#setup-search").fill(""); p.locator("#school-setup [data-record]").last().click();
                assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                h.record("UC-1 2b/2c/G4-G8: unsupported raw authoring remains exact; same-name source/recovered drafts require explicit choice; 41-teacher search and focused details fit the 768px desktop layout",p,"06-narrow-source-choice");
                p.keyboard().press("Escape"); assertThat(p.locator("#school-setup")).not().isVisible();
                Page unavailable=owner.newPage(); unavailable.onPageError(h.errors::add);
                unavailable.addInitScript("const get=Storage.prototype.getItem;Storage.prototype.getItem=function(k){if(k==='"+KEY+"')throw new DOMException('unavailable','SecurityError');return get.call(this,k)}");
                unavailable.navigate(h.base+"/workspace/"); open(unavailable); tool(unavailable,"#setup-new").click();
                unavailable.locator("#setup-name").fill("Unreadable storage working edit");
                assertThat(unavailable.locator("#setup-durability")).hasText("Only on this page");
                section(unavailable,"Classes"); close(unavailable); open(unavailable);
                assertThat(unavailable.locator("#setup-name")).hasValue("Unreadable storage working edit");
                h.record("UC-1 6a: initial storage SecurityError leaves bundled examples and empty setup usable, shows Only on this page and retains input across navigation/reopen",unavailable,"07-storage-unavailable");
                unavailable.close();
                assertTrue(h.errors.isEmpty(),h.errors.toString());
                Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            } catch(Throwable failure) { Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure); h.record("FAIL: "+failure,p,"failure"); throw failure; }
            finally {
                Files.write(h.output.resolve("browser-errors.txt"),h.errors);
                Files.writeString(h.output.resolve("ledger.md"),"# UC-1 administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nBuild: pinned packaged workspace/kernel JARs; base be5b76d plus working diff. Browser: installed Chrome via Java Playwright. Isolated disposable storage; no operator data.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nConversion preview/adoption is an outstanding UC-15 handoff. Grid/catalog editing, readiness and saving are later UCs, not approved here. Quota failure uses explicit Storage prototype injection. Context closes and packaged service stops.\n");
            }
        } finally { h.stop(); }
    }
    @Test void authorsWeekCopiesIndependentlyAndResolvesReferences() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("setup-grid",Path.of("../output/playwright/school-setup-editor/UC-2/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=h.context(browser)) {
            Page p=owner.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");open(p);tool(p,"#setup-new").click();
                p.locator("#setup-name").fill("Grid school");
                p.locator("[data-active-day=MONDAY]").check();
                addInterval(p,"Morning","08:00","08:45","Lesson");
                addInterval(p,"Short break","08:45","09:00","Break");
                addInterval(p,"Support","09:00","09:45","Reserved lesson");
                addInterval(p,"School lunch","09:45","10:15","Lunch");
                addInterval(p,"Late lesson","10:15","11:00","Lesson");
                saved(p);assertEquals(5,p.locator(".setup-interval").count());
                assertThat(p.locator("#setup-grid-issues")).hasText("");
                Object before=durable(p);
                p.locator("[data-copy-day=TUESDAY]").check();p.locator("[data-copy-day=WEDNESDAY]").check();
                p.locator("#setup-copy-day").click();assertThat(p.locator(".setup-confirm")).containsText("replace all their existing intervals");
                p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                p.locator("#setup-copy-day").click();p.locator(".setup-confirm [data-apply]").click();saved(p);
                assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.grid.intervals.length===15&&new Set(d.grid.intervals.map(i=>i.id)).size===15&&new Set(d.grid.intervals.filter(i=>i.periodId).map(i=>i.periodId)).size===9}"));
                p.locator("[data-grid-day=TUESDAY]").click();p.locator("[data-interval-field=label]").first().fill("Tuesday only");saved(p);
                p.locator("[data-grid-day=MONDAY]").click();assertThat(p.locator("[data-interval-field=label]").first()).hasValue("Morning");
                h.record("UC-2 main 1-5/G1-G3: five named intervals use all four kinds; copy preview cancellation preserves exact catalog; confirmed two-day copy creates 15 independent intervals and nine teaching periods; destination rename leaves source exact",p,"01-week-copy");
                p.locator("[data-interval-field=start]").first().fill("unfinished");saved(p);
                assertThat(p.locator("#setup-grid-issues")).containsText("enter both times");
                close(p);p.reload();open(p);assertThat(p.locator("[data-interval-field=start]").first()).hasValue("unfinished");
                p.locator("[data-interval-field=start]").first().fill("08:00");
                p.locator("[data-interval-field=end]").first().fill("09:15");
                assertThat(p.locator("#setup-grid-issues")).containsText("overlap");
                p.locator("[data-interval-field=end]").first().fill("08:45");saved(p);
                h.record("UC-2 1a/G1/G4: malformed time survives autosave/close/reload with located issue; overlapping intervals remain editable and correction clears issue without changing identities",p,"02-invalid-retained");
                // Equivalent durable prerequisite: independent availability/lunch/lock references owned by later UCs.
                close(p);
                p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source),id=d.grid.intervals.find(i=>i.day==='TUESDAY'&&i.periodId).periodId;d.teachers=[{id:'t',name:'Teacher',availability:{intent:'whitelist',availablePeriodIds:[id]},undesirablePeriodIds:[id]}];d.classes=[{id:'c',name:'Class',lunchPeriodIds:[id]}];d.rooms=[{id:'r',name:'Room',availability:{intent:'all',unavailablePeriodIds:[id]}}];d.plans=[{id:'plan',occurrences:[{id:'lesson',periodLock:id}]}];localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
                p.reload();open(p);p.locator("[data-copy-day=TUESDAY]").check();saved(p);before=durable(p);
                p.locator("#setup-copy-day").click();assertEquals(5,p.locator("[data-reference]").count());
                p.locator(".setup-confirm [data-apply]").click();assertThat(p.locator(".setup-remap-error")).containsText("Every affected reference");assertEquals(before,durable(p));
                p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                p.locator("#setup-copy-day").click();
                for(Locator select:p.locator("[data-reference]").all())select.selectOption("__clear__");
                p.locator(".setup-confirm [data-apply]").click();saved(p);
                assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.teachers[0].availability.intent==='whitelist'&&d.teachers[0].availability.availablePeriodIds.length===0&&d.teachers[0].undesirablePeriodIds.length===0&&d.classes[0].lunchPeriodIds.length===0&&d.rooms[0].availability.intent==='all'&&d.rooms[0].availability.unavailablePeriodIds.length===0&&!('periodLock' in d.plans[0].occurrences[0])}"));
                h.record("UC-2 3a/G1-G2: isolated durable reference fixture exposes all five affected availability/preference/lunch/lock references; unresolved and cancelled copy preserve exact stored values; explicit clears retain empty whitelist versus all-period intent",p,"03-reference-resolution");
                if(!p.locator("[data-active-day=THURSDAY]").isVisible())p.locator("#setup-school-days > summary").click();p.locator("[data-active-day=THURSDAY]").check();p.locator("[data-grid-day=THURSDAY]").click();p.locator("#setup-add-interval").click();saved(p);
                assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.teachers[0].availability.availablePeriodIds.length===0&&d.rooms[0].availability.intent==='all'}"));
                for(int width:List.of(768,1024,1440)){
                    p.setViewportSize(width,900);assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                    h.record("UC-2 G4: weekday switching and all interval/copy actions fit "+width+"px; adding a period does not expand explicit whitelist",p,"04-grid-"+width);
                }
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-2 week editor walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nBuild: pinned packaged current working diff; source hashes in submission.json. Real installed Chrome, disposable isolated browser storage, local HTTPS.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nAvailability/lunch/lock reference fixture is explicitly restored durable data; their authoring UCs are not approved here. Kernel adjacency consumption remains UC-12/13. Context and packaged server cleaned up.\n");}
        }finally{h.stop();}
    }
    static void addInterval(Page p,String name,String start,String end,String kind){
        p.locator("#setup-add-interval").click();Locator row=p.locator(".setup-interval").last();
        if(!p.locator("[data-interval-field=label]").last().isVisible())p.locator("#setup-interval-options > summary").click();
        p.locator("[data-interval-field=label]").last().fill(name);row.locator("[data-interval-field=start]").fill(start);row.locator("[data-interval-field=end]").fill(end);
        if(!kind.equals("Lesson")){
            row.locator("[data-interval-kind]").selectOption(kind);
            if(kind.equals("Break")||kind.equals("Lunch"))p.locator(".setup-confirm [data-apply]").click();
        }
    }

    @Test void maintainsSubjectRulesWithoutChangingReferencesOrOptionalPresence() throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("setup-subjects",Path.of("../output/playwright/school-setup-editor/UC-3/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=h.context(browser)){
            Page p=owner.newPage();p.onPageError(h.errors::add);
            try{
                p.navigate(h.base+"/workspace/");open(p);tool(p,"#setup-new").click();section(p,"Subjects");
                p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Matemaatika & <literal>");
                p.locator("#setup-record-detail details:has(.setup-context-fields) > summary").click();
                assertEquals(5,p.locator(".setup-context-fields [data-catalog-field]").count());
                p.locator("[data-catalog-field=maxWeeklyReservedLessonsPerCohort]").fill("2");
                assertThat(p.locator("#setup-record-issues")).containsText("requires explicit reserved-slot permission");
                p.locator("[data-catalog-field=reservedPeriodsAllowed]").selectOption("true");
                p.locator("[data-catalog-field=maxDailyLessonsPerCohort]").fill("unfinished");
                assertThat(p.locator("#setup-record-issues")).containsText("positive integer");saved(p);
                close(p);p.reload();open(p);assertThat(p.locator("#setup-record-name")).hasValue("Matemaatika & <literal>");
                p.locator("#setup-record-detail details:has(.setup-context-fields) > summary").click();
                assertThat(p.locator("[data-catalog-field=maxDailyLessonsPerCohort]")).hasValue("unfinished");
                p.locator("[data-catalog-field=maxDailyLessonsPerCohort]").fill("3");
                p.locator("[data-catalog-field=dayEdgeOnly]").selectOption("true");
                p.locator("[data-catalog-field=curatorLesson]").selectOption("false");saved(p);
                assertTrue((Boolean)p.evaluate("()=>{const s=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).subjects[0];return s.reservedPeriodsAllowed===true&&s.maxWeeklyReservedLessonsPerCohort===2&&s.maxDailyLessonsPerCohort===3&&s.dayEdgeOnly===true&&s.curatorLesson===false}"));
                assertThat(p.locator("#setup-record-issues")).hasText("");
                h.record("UC-3 main 1-4/2a/G1-G4: literal subject name; exactly five contextual hard controls; reserved maximum never grants permission; unfinished daily maximum survives reload; exact true/false and numeric values saved",p,"01-subject-rules");
                p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Unreferenced");saved(p);
                Object before=durable(p);p.locator("#setup-remove-record").click();p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
                p.locator("#setup-remove-record").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
                assertEquals(1,p.locator("[data-record]").count());p.locator("[data-record]").first().click();
                close(p);
                // Restore equivalent prerequisite plan/eligibility/room-policy data to exercise safe removal and linked rules.
                p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source),id=d.subjects[0].id;d.teachers=[{id:'t',displayName:'Teacher',qualifiedSubjectIds:[id],eligibleClassIdsBySubject:{[id]:['c']}}];d.plans=[{id:'plan',subjectId:id,classId:'c',blockRules:[{length:4,count:1,operator:'EXACTLY'}]}];d.subjectRoomPolicies=[{id:'room-policy',subjectId:id,allowedRoomIds:[]}];localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
                p.reload();open(p);before=durable(p);
                p.locator("#setup-remove-record").click();assertThat(p.locator("#setup-message")).containsText("Cannot remove");
                assertThat(p.locator("#setup-message")).containsText("qualifiedSubjectIds");assertThat(p.locator("#setup-message")).containsText("eligibleClassIdsBySubject");
                assertThat(p.locator("#setup-message")).containsText("subjectRoomPolicies");assertEquals(before,durable(p));
                assertThat(p.locator("#setup-record-issues")).containsText("below required block length 4");
                h.record("UC-3 2b/3a: restored plan block length conflicts with daily cap; subject removal names every qualification/eligibility/plan/room-policy reference and leaves exact durable data unchanged",p,"02-refused-removal");
                for(int width:List.of(768,1024,1440)){
                    p.setViewportSize(width,900);assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                    h.record("UC-3/UC-1 G5-G8: subject form, contextual rules and refusal fit "+width+"px",p,"03-subject-"+width);
                }
                p.locator("#setup-record-detail details:has(.setup-context-fields) > summary").click();
                p.locator("[data-catalog-field=maxDailyLessonsPerCohort]").fill("");
                p.locator("[data-catalog-field=dayEdgeOnly]").selectOption("default");saved(p);
                assertTrue((Boolean)p.evaluate("()=>{const s=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).subjects[0];return !('maxDailyLessonsPerCohort' in s)&&!('dayEdgeOnly' in s)&&s.curatorLesson===false&&s.reservedPeriodsAllowed===true&&s.maxWeeklyReservedLessonsPerCohort===2}"));
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# UC-3 subject walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nPinned packaged working diff; real Chrome with isolated disposable storage; local HTTPS.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nPlan/eligibility/room-policy fixture is explicit durable precondition data; destination authoring and kernel enforcement remain their UCs. Context and server cleaned up.\n");}
        }finally{h.stop();}
    }

}
