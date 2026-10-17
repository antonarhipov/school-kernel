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

class SchoolSetupCatalogBrowserIT {
    @TempDir Path temporary;
    @FunctionalInterface interface Journey { void run(DemoPlanningBrowserIT h,Page p) throws Exception; }
    void walk(String uc,Journey journey) throws Exception {
        var h=new DemoPlanningBrowserIT();h.temporary=temporary;
        h.initialize("setup-catalog",Path.of("../output/playwright/school-setup-editor/"+uc+"/"+System.currentTimeMillis()));
        try(Playwright pw=Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD","1")));
            Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true));
            BrowserContext owner=h.context(browser)){
            Page p=owner.newPage();p.onPageError(h.errors::add);
            try {
                p.navigate(h.base+"/workspace/");open(p);tool(p,"#setup-new").click();journey.run(h,p);
                assertTrue(h.errors.isEmpty(),h.errors.toString());Files.writeString(h.output.resolve("verdict.txt"),"PASS\n");
            }catch(Throwable failure){Files.writeString(h.output.resolve("verdict.txt"),"FAIL: "+failure);h.record("FAIL: "+failure,p,"failure");throw failure;}
            finally{Files.write(h.output.resolve("browser-errors.txt"),h.errors);Files.writeString(h.output.resolve("ledger.md"),"# "+uc+" administrator walkthrough\n\nDate: "+Instant.now()+"\nApplication: "+h.base+"\nBuild: pinned packaged working diff; exact source hashes in submission.json. Real installed Chrome; isolated disposable origin storage; local HTTPS.\n\n"+String.join("\n\n",h.steps)+"\n\nOverall: "+Files.readString(h.output.resolve("verdict.txt"))+"\nRestored prerequisite fixtures are explicitly identified in individual steps. They do not approve destination authoring UCs or kernel enforcement. Browser context closes and packaged server stops.\n");}
        }finally{h.stop();}
    }
    static void responsive(DemoPlanningBrowserIT h,Page p,String uc) throws Exception {
        for(int width:List.of(320,390,1440)){
            p.setViewportSize(width,900);assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
            h.record(uc+"/UC-1 G5-G8: current controls, issues and actions fit "+width+"px",p,"responsive-"+width);
        }
    }
    @Test void followsPlanningPrototypeAndKeepsSecondaryToolsOutOfTheTask() throws Exception {
        walk("UI-prototype",(h,p)->{
            Page prototype=p.context().newPage();
            prototype.navigate(Path.of("../output/design/school-setup-revival/grid-editor-preview.html").toAbsolutePath().toUri().toString());
            prototype.frameLocator("#codex-visualization").locator(".school-pattern-layout").first().waitFor();
            prototype.screenshot(new Page.ScreenshotOptions().setPath(h.output.resolve("00-planning-prototype.png")).setFullPage(true));
            prototype.close();
            assertFalse((Boolean)p.locator("#setup-tools").evaluate("d=>d.open"));
            assertTrue((Boolean)p.locator(".setup-header").evaluate("d=>d.getBoundingClientRect().height<110"));
            assertEquals(6,p.locator("nav[aria-label='Setup sections'] > button").count());
            assertThat(p.locator("#setup-section-title")).hasText("Timetable grid");
            p.locator("#setup-name").fill("Kasevälja School");
            p.locator("[data-active-day=MONDAY]").check();
            addInterval(p,"Early support","08:00","08:40","Reserved lesson");
            addInterval(p,"Lesson 1","08:45","09:30","Lesson");
            addInterval(p,"Morning break","09:30","09:40","Break");
            addInterval(p,"Lesson 2","09:40","10:25","Lesson");
            addInterval(p,"Long break","10:25","10:45","Break");
            addInterval(p,"Lesson 3","10:45","11:30","Lesson");
            addInterval(p,"School lunch","11:30","12:00","Lunch");
            addInterval(p,"Lesson 4","12:00","12:45","Lesson");
            p.locator("[data-copy-day=TUESDAY]").check();p.locator("[data-copy-day=WEDNESDAY]").check();p.locator("[data-copy-day=THURSDAY]").check();p.locator("[data-copy-day=FRIDAY]").check();
            p.locator("#setup-copy-day").click();p.locator(".setup-confirm [data-apply]").click();saved(p);
            assertEquals(5,p.locator(".setup-week-overview > button").count());
            assertEquals(8,p.locator(".setup-interval").count());
            p.locator("#setup-interval-options").evaluate("d=>d.open=false");
            p.locator("#setup-school-days").evaluate("d=>d.open=false");
            p.locator("#setup-content").evaluate("d=>d.scrollTop=0");
            h.record("Prototype day-pattern hierarchy: compact school header, six calm numbered sections, eight interval rows, independent day copying, live week overview, close footer; technical draft/JSON/recovery tools remain collapsed",p,"01-day-pattern-desktop");
            Object before=durable(p);close(p);
            assertTrue((Boolean)p.locator("#school-setup-entry").evaluate("d=>getComputedStyle(d).writingMode==='vertical-rl'"));
            p.locator("#school-setup-entry").click();
            assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>{const a=d.getAnimations()[0];if(!a)return false;a.pause();a.currentTime=120;return d.getBoundingClientRect().x<0}"));
            h.record("Prototype left-to-right rollout: real modal paused mid-opening with the timetable visible behind it",p,"02-opening-rollout");
            p.locator("#school-setup").evaluate("async d=>{const a=d.getAnimations()[0];a.play();await a.finished;}");
            assertEquals(before,durable(p));
            section(p,"Subjects");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Mathematics");
            assertThat(p.locator("[data-record]")).hasText("Mathematics");
            h.record("Everyday subject screen: search and focused name editing, optional scheduling/placement controls collapsed; section navigation does not force completion",p,"03-subjects-desktop");
            section(p,"Classes");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("5A");p.locator("[data-catalog-field=size]").fill("24");
            h.record("Class screen: focused details and explicit teaching-plan/lunch actions with advanced scheduling settings collapsed",p,"04-classes-desktop");
            section(p,"Teachers");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Mari Tamm");p.locator("[data-teacher-qualification]").check();
            h.record("Teacher screen: subject qualification and expandable class eligibility, derived workload and separate availability",p,"05-teachers-desktop");
            section(p,"Timetable grid");
            for(int width:List.of(320,390,768,1440)){
                p.setViewportSize(width,900);
                p.locator("#setup-content").evaluate("d=>d.scrollTop=0");
                p.locator("nav[aria-label='Setup sections'] > button[aria-current]").hover();
                assertTrue((Boolean)p.locator("nav[aria-label='Setup sections']").evaluate("n=>getComputedStyle(n.querySelector('[aria-current]')).backgroundColor!==getComputedStyle(n.querySelector('button:not([aria-current])')).backgroundColor"));
                assertTrue((Boolean)p.locator("#school-setup").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                assertTrue((Boolean)p.locator("#setup-content").evaluate("d=>d.scrollWidth<=d.clientWidth+1"));
                assertThat(p.locator("[data-back-timetable]")).isInViewport();
                h.record("Prototype responsive layout at "+width+"px: task content fits its scroll container, footer stays reachable, data tools stay collapsed",p,"responsive-"+width);
            }
            p.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE));
            close(p);open(p);
            assertEquals(0,((Number)p.locator("#school-setup").evaluate("d=>d.getAnimations().filter(a=>a.playState==='running').length")).intValue());
            assertThat(p.locator("#setup-section-title")).isFocused();
            p.keyboard().press("Escape");assertThat(p.locator("#school-setup")).not().isVisible();assertThat(p.locator("#school-setup-entry")).isFocused();
            h.record("Reduced motion opens/closes immediately; Escape returns keyboard focus to the vertical left-edge entry",p,"06-return-to-timetable");
        });
    }
    @Test void classDetailsCuratorsDefaultsAndScopedRemoval() throws Exception {
        walk("UC-4",(h,p)->{
            section(p,"Classes");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Year Five A");
            p.locator("[data-catalog-field=size]").fill("unfinished");saved(p);
            assertThat(p.locator("#setup-record-issues")).containsText("student count");
            close(p);p.reload();open(p);assertThat(p.locator("[data-catalog-field=size]")).hasValue("unfinished");
            p.locator("[data-catalog-field=size]").fill("25");p.locator("#setup-record-detail details summary").first().click();
            assertEquals(5,p.locator("[data-class-mode]").count());
            assertThat(p.locator("[data-class-mode=maxDailyGaps]")).containsText("Inherited default: 0");
            p.locator("[data-class-mode=maxDailyGaps]").selectOption("unrestricted");
            p.locator("[data-class-mode=preferredLatestStartSlot]").selectOption("value");
            p.locator("[data-catalog-field=preferredLatestStartSlot]").fill("2");
            p.locator("[data-class-mode=dailyLessonSpreadLimit]").selectOption("value");
            p.locator("[data-catalog-field=dailyLessonSpreadLimit]").fill("0");saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source),c=d.classes[0];return c.size===25&&c.maxDailyGaps===2147483647&&c.preferredLatestStartSlot===2&&c.dailyLessonSpreadLimit===0&&!('latestStartSlot' in c)&&!('maxDailyLessonSpread' in c)&&d.plans.length===0}"));
            p.locator("[data-open-class-plan]").click();assertEquals(0,p.locator("[data-plan]").count());p.locator("#setup-back-class").click();
            h.record("UC-4 main 1/3-4/1a/G1-G4: incomplete size survives reload; positive class details; exactly five controls distinguish inherited 0/3/1 from explicit unlimited/zero/2; no demand or implicit qualification; class-plan handoff",p,"01-class-settings");
            close(p);
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.teachers=[{id:'mari',displayName:'Mari',qualifiedSubjectIds:[],eligibleClassIdsBySubject:{}}];d.rooms=[{id:'home',displayName:'Home',capacity:30,capabilityIds:[]}];localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);
            p.locator("[data-catalog-field=curatorTeacherId]").selectOption("mari");p.locator("[data-catalog-field=homeRoomId]").selectOption("home");
            for(int n=2;n<=20;n++){p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Class "+n);p.locator("[data-catalog-field=size]").fill("20");}
            p.locator("[data-catalog-field=curatorTeacherId]").selectOption("mari");saved(p);
            assertThat(p.locator("#setup-record-count")).hasText("20 of 20 records");
            p.locator("#setup-search").fill("Year Five A");assertThat(p.locator("#setup-record-count")).hasText("1 of 20 records");p.locator("[data-record]").first().click();
            assertThat(p.locator("[data-catalog-field=homeRoomId]")).hasValue("home");
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.classes.filter(c=>c.curatorTeacherId==='mari').length===2&&d.teachers[0].qualifiedSubjectIds.length===0&&d.plans.length===0}"));
            h.record("UC-4 main 2/2a/G1-G4: restored existing teacher/room can be nominated; one teacher curates two independent classes; 20-class browsing/search; nomination preserves zero qualifications, eligibility and demand",p,"02-class-scale");
            responsive(h,p,"UC-4");
            close(p);
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source),id=d.classes[0].id;d.classes[0].availablePeriodIds=[];d.classes[0].undesirablePeriodIds=[];d.classes[0].lunchPeriodIds=['lunch'];d.teachers[0].eligibleClassIdsBySubject={math:[id]};d.plans=[{id:'plan',classId:id,subjectId:'math',requiredCount:2,allocations:[{teacherId:'mari',count:2}],occurrences:[{id:'locked',periodLock:'period'}]}];localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);Object before=durable(p);p.locator("#setup-remove-record").click();assertThat(p.locator("#setup-message")).containsText("Protected imported occurrence");assertEquals(before,durable(p));
            close(p);p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);delete d.plans[0].occurrences[0].periodLock;localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);before=durable(p);p.locator("#setup-remove-record").click();assertThat(p.locator(".setup-confirm")).containsText("weekly demand 2");
            p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
            p.locator("#setup-remove-record").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.classes.length===19&&d.plans.length===0&&d.teachers[0].eligibleClassIdsBySubject.math.length===0&&d.rooms[0].id==='home'}"));
            h.record("UC-4 3a: protected real period lock refuses class removal with exact stored equality; supported deletion previews demand/allocations, cancellation preserves values; confirmed removal clears only that class, plan, lunch and eligibility",p,"03-class-removal");
        });
    }
    @Test void teacherEligibilityStaysSubjectSpecificAndAllocationOwned() throws Exception {
        walk("UC-5",(h,p)->{
            section(p,"Subjects");for(String name:List.of("Mathematics","Science")){p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill(name);}
            section(p,"Classes");for(String name:List.of("Class Alpha","Class Beta")){p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill(name);p.locator("[data-catalog-field=size]").fill("20");}
            section(p,"Teachers");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Mari õpetaja");
            assertThat(p.locator("#setup-teacher-workload")).hasText("0 allocated lessons per week");
            p.locator("[data-teacher-qualification]").first().check();p.locator("[data-teacher-qualification]").last().check();
            p.locator("#setup-record-detail details summary").first().click();p.locator("#setup-record-detail details summary").last().click();
            p.locator("[data-teacher-class]").nth(0).check();p.locator("[data-teacher-class]").nth(3).check();saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source),t=d.teachers[0];return t.qualifiedSubjectIds.length===2&&t.eligibleClassIdsBySubject[d.subjects[0].id].length===1&&t.eligibleClassIdsBySubject[d.subjects[0].id][0]===d.classes[0].id&&t.eligibleClassIdsBySubject[d.subjects[1].id][0]===d.classes[1].id&&t.availability.intent==='all'&&d.plans.length===0}"));
            close(p);p.reload();open(p);assertThat(p.locator("#setup-record-name")).hasValue("Mari õpetaja");
            h.record("UC-5 main 1-4/G1-G3: two qualifications and two distinct subject/class permissions survive reload; no cross-product eligibility, allocation or demand; new teacher is available in all teaching periods",p,"01-eligibility");
            for(int n=2;n<=40;n++){p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Teacher person "+n);}
            saved(p);assertThat(p.locator("#setup-record-count")).hasText("40 of 40 records");
            p.locator("#setup-search").fill("Mari õpetaja");assertThat(p.locator("#setup-record-count")).hasText("1 of 40 records");p.locator("[data-record]").first().click();
            responsive(h,p,"UC-5");close(p);
            // Restored class-plan prerequisite proves derived workload and non-destructive qualification removal.
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source),t=d.teachers[0],c=d.classes[0],subject=d.subjects[0];c.curatorTeacherId=t.id;d.plans=[{id:'allocated',classId:c.id,subjectId:subject.id,requiredCount:5,allocations:[{teacherId:t.id,count:5}],blockRules:[{length:2,count:1,operator:'EXACTLY'}]}];localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);assertThat(p.locator("#setup-teacher-workload")).hasText("5 allocated lessons per week");
            Object before=durable(p);p.locator("#setup-remove-record").click();assertThat(p.locator("#setup-message")).containsText("curatorTeacherId");assertThat(p.locator("#setup-message")).containsText("allocations");assertEquals(before,durable(p));
            p.locator("[data-teacher-qualification]").first().uncheck();
            assertThat(p.locator("#setup-record-issues")).containsText("allocated row allocated");saved(p);
            assertThat(p.locator("#setup-teacher-workload")).hasText("5 allocated lessons per week");
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.plans[0].allocations[0].count===5&&d.teachers[0].eligibleClassIdsBySubject[d.subjects[0].id].length===1}"));
            h.record("UC-5 2a/3a/G2: restored five-occurrence plan including double contributes workload five once; referenced curator/allocated teacher removal refused by value; qualification removal keeps allocation and eligibility with blocking issue",p,"02-allocation-retained");
            p.locator("[data-teacher-plan=allocated]").click();assertThat(p.locator("#setup-section-title")).hasText("Classes");assertThat(p.locator("#setup-class-plan")).containsText("Class Alpha");
            assertThat(p.locator("#setup-message")).containsText("Weekly allocation belongs");
            section(p,"Teachers");p.locator("#setup-search").fill("Teacher person 40");p.locator("[data-record]").first().click();saved(p);before=durable(p);
            p.locator("#setup-remove-record").click();p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
            p.locator("#setup-remove-record").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
            assertTrue((Boolean)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).teachers.length===39"));
            h.record("UC-5 main 3/G2-G3: derived workload links to owning class; 40-teacher search and cancellation/confirmed unreferenced deletion preserve other allocations; availability remains an explicit UC-8 handoff",p,"03-plan-handoff");
        });
    }

    @Test void allocatesClassDemandSplitsTeachersAndPreservesOccurrences() throws Exception {
        walk("UC-6",(h,p)->{
            section(p,"Subjects");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Mathematics");
            section(p,"Classes");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Class Alpha");p.locator("[data-catalog-field=size]").fill("20");
            section(p,"Teachers");for(String name:List.of("Mari","Jüri")){p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill(name);}
            section(p,"Classes");p.locator("[data-record]").first().click();p.locator("[data-open-class-plan]").click();
            String subject=(String)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).subjects[0].id");
            p.locator("#setup-plan-subject").selectOption(subject);p.locator("#setup-add-plan").click();p.locator("[data-plan-demand]").fill("5");saved(p);
            assertThat(p.locator(".setup-plan-totals")).containsText("Unassigned: 5");
            assertEquals(0,((Number)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).plans[0].occurrences.length")).intValue());
            Object before=durable(p);p.locator("[data-plan] details summary").first().click();
            String mari=(String)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).teachers[0].id");
            p.locator("[data-plan-intended]").selectOption(mari);saved(p);before=durable(p);
            p.locator("[data-grant-teacher]").click();assertThat(p.locator(".setup-confirm")).containsText("also adds the subject qualification");
            p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
            p.locator("[data-grant-teacher]").click();p.locator(".setup-confirm [data-confirm]").click();
            p.locator("[data-plan-eligible]").selectOption(mari);p.locator("[data-add-allocation]").click();p.locator("[data-allocation-count]").first().fill("3");saved(p);
            Object first=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).plans[0].occurrences");
            assertThat(p.locator(".setup-plan-totals")).containsText("Allocated: 3 · Unassigned: 2");
            h.record("UC-6 main 1-2/2a/3a/G1-G4: class owns demand five; unassigned demand creates no lessons; merely selecting teacher grants nothing; cancelled grant preserves exact data; explicit named grant adds qualification/eligibility and allocation three",p,"01-demand-grant");
            p.locator("[data-plan] details summary").first().click();
            String juri=(String)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).teachers[1].id");
            p.locator("[data-plan-intended]").selectOption(juri);p.locator("[data-grant-teacher]").click();p.locator(".setup-confirm [data-confirm]").click();
            p.locator("[data-plan-eligible]").selectOption(juri);p.locator("[data-add-allocation]").click();p.locator("[data-allocation-count]").last().fill("2");saved(p);
            assertThat(p.locator(".setup-plan-totals")).containsText("Allocated: 5 · Unassigned: 0");
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source),row=d.plans[0];return row.occurrences.length===5&&new Set(row.occurrences.map(o=>o.seriesId)).size===1&&row.occurrences.filter(o=>o.teacherId===d.teachers[0].id).length===3&&row.occurrences.filter(o=>o.teacherId===d.teachers[1].id).length===2&&row.blockRules.length===0}"));
            assertEquals(first,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).plans[0].occurrences.slice(0,3)"));
            section(p,"Teachers");p.locator("[data-record]").first().click();assertThat(p.locator("#setup-teacher-workload")).hasText("3 allocated lessons per week");
            p.locator("[data-teacher-plan]").click();assertThat(p.locator(".setup-plan-totals")).containsText("Unassigned: 0");
            close(p);p.reload();open(p);assertThat(p.locator(".setup-plan-totals")).containsText("Allocated: 5");
            h.record("UC-6 main 3-4/G2-G5: explicit split three/two creates exactly five assigned occurrences sharing one spreading series; first three exact identities/names/settings survive; derived teacher load and owning-plan link/reload agree",p,"02-split-stable");
            p.locator("[data-allocation-count]").last().fill("3");assertThat(p.locator(".setup-plan-totals")).containsText("Over-allocated: 1");
            p.locator("[data-allocation-count]").last().fill("unfinished");assertThat(p.locator(".setup-plan-issues")).containsText("positive integer");saved(p);
            close(p);p.reload();open(p);assertThat(p.locator("[data-allocation-count]").last()).hasValue("unfinished");p.locator("[data-allocation-count]").last().fill("2");saved(p);
            responsive(h,p,"UC-6");
            // Retained occurrence protection is a restored imported prerequisite, not a UI-authored lock.
            close(p);p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.plans[0].occurrences[0].periodLock='protected-period';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);before=durable(p);p.locator("[data-plan-demand]").fill("7");assertThat(p.locator("#setup-message")).containsText("Change refused");assertThat(p.locator("[data-plan-demand]")).hasValue("5");assertEquals(before,durable(p));
            p.locator("[data-remove-plan]").click();assertThat(p.locator("#setup-message")).containsText("Cannot remove protected row");assertEquals(before,durable(p));
            close(p);p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);delete d.plans[0].occurrences[0].periodLock;localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);before=durable(p);p.locator("[data-remove-plan]").click();assertThat(p.locator(".setup-confirm")).containsText("demand 5");
            p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));p.locator("[data-remove-plan]").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.plans.length===0&&d.teachers.length===2&&d.teachers.every(t=>t.qualifiedSubjectIds.length===1)}"));
            h.record("UC-6 3b-3d/G2: over-allocation and malformed count retained with explicit issues; protected period-lock row refuses count/removal by value; ordinary row deletion previews full scope, cancellation preserves, confirmation retains teachers and explicit eligibility",p,"03-protected-and-delete");
        });
    }

    @Test void copiesOnlyCurriculumIntoAnEmptyIndependentClass() throws Exception {
        walk("UC-7",(h,p)->{
            close(p);
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.subjects=[{id:'math',displayName:'Mathematics'},{id:'science',displayName:'Science'}];d.classes=[{id:'a',displayName:'Alpha',size:20,curatorTeacherId:'t'},{id:'b',displayName:'Beta',size:22,homeRoomId:'home',lunchPeriodIds:['lunch']},{id:'c',displayName:'Gamma',size:25}];d.rooms=[{id:'home',displayName:'Home',capacity:30,capabilityIds:[]}];d.teachers=[{id:'t',displayName:'Teacher',qualifiedSubjectIds:['math','science'],eligibleClassIdsBySubject:{math:['a'],science:['a']}}];d.plans=[{id:'math-plan',classId:'a',subjectId:'math',requiredCount:3,allocations:[{teacherId:'t',count:3}],seriesId:'math-series',blockRules:[{length:2,count:1,operator:'EXACTLY'}],fixedRoomId:'home',occurrences:[{id:'locked',teacherId:'t',periodLock:'period'}]},{id:'science-plan',classId:'a',subjectId:'science',requiredCount:2,allocations:[{teacherId:'t',count:2}],seriesId:'science-series',occurrences:[],blockRules:[]}];d.section='Classes';d.focused.classes='a';d.pendingInput.planClassId='a';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);Object source=p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).plans");
            p.locator("#setup-copy-curriculum-destination").locator("..").locator("..").locator("summary").click();
            p.locator("#setup-copy-curriculum-destination").selectOption("b");saved(p);Object before=durable(p);
            p.locator("#setup-preview-curriculum").click();assertThat(p.locator(".setup-confirm")).containsText("Mathematics: 3");assertThat(p.locator(".setup-confirm")).containsText("Science: 2");
            p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
            p.locator("#setup-preview-curriculum").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
            assertEquals(source,p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).plans.filter(p=>p.classId==='a')"));
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source),target=d.plans.filter(p=>p.classId==='b');return target.length===2&&target[0].requiredCount===3&&target[1].requiredCount===2&&target.every(p=>p.allocations.length===0&&p.occurrences.length===0&&p.blockRules.length===0&&!('fixedRoomId' in p)&&!['math-series','science-series'].includes(p.seriesId))&&d.classes[1].homeRoomId==='home'&&d.classes[1].lunchPeriodIds[0]==='lunch'&&d.teachers[0].eligibleClassIdsBySubject.math.join(',')==='a'}"));
            h.record("UC-7 main 1-4/3a/G1-G2: restored source includes allocations, block/fixed-room/lock settings; preview names exact three/two weekly counts; cancel preserves exact catalog; copy creates independent unassigned rows with none of those contextual settings and source/class/eligibility values unchanged",p,"01-curriculum-copy");
            p.locator("[data-plan=science-plan] [data-plan-demand]").fill("4");saved(p);
            assertTrue((Boolean)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).plans.find(p=>p.classId==='b'&&p.subjectId==='science').requiredCount===2"));
            p.locator("#setup-copy-curriculum-destination").locator("..").locator("..").locator("summary").click();
            before=durable(p);p.locator("#setup-preview-curriculum").click();assertThat(p.locator("#setup-message")).containsText("empty destination");assertEquals(before,durable(p));
            responsive(h,p,"UC-7");
            p.locator("[data-plan=science-plan] [data-plan-demand]").fill("unfinished");
            p.locator("#setup-copy-curriculum-destination").selectOption("c");saved(p);before=durable(p);
            p.locator("#setup-preview-curriculum").click();assertThat(p.locator("#setup-message")).containsText("positive weekly demand");assertEquals(before,durable(p));
            close(p);p.reload();open(p);assertTrue((Boolean)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).plans.filter(p=>p.classId==='c').length===0"));
            h.record("UC-7 2a/2b/G2: source edits do not propagate; occupied destination refusal and invalid-source-demand refusal leave exact stored state with no partial Gamma rows; independent target survives reload",p,"02-independent-refusals");
        });
    }

    @Test void teacherMarkingsSeparateHardSoftBulkRangesAndRetainedFacts() throws Exception {
        walk("UC-8",(h,p)->{
            p.locator("[data-active-day=MONDAY]").check();
            addInterval(p,"Morning","08:00","08:45","Lesson");addInterval(p,"Break","08:45","09:00","Break");
            addInterval(p,"School lunch","12:00","13:00","Lunch");addInterval(p,"Afternoon","13:00","13:45","Lesson");addInterval(p,"Reserved","14:00","14:45","Reserved lesson");
            p.locator("[data-copy-day=TUESDAY]").check();p.locator("#setup-copy-day").click();p.locator(".setup-confirm [data-apply]").click();
            section(p,"Teachers");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Availability teacher");saved(p);close(p);
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source),t=d.teachers[0];d.subjects=[{id:'s',displayName:'Subject',reservedPeriodsAllowed:true}];d.classes=[{id:'c',displayName:'Class',size:20}];t.qualifiedSubjectIds=['s'];t.eligibleClassIdsBySubject={s:['c']};d.plans=[{id:'load',classId:'c',subjectId:'s',requiredCount:4,allocations:[{teacherId:t.id,count:4}],occurrences:[]}];localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);p.locator("[data-teacher-availability]").click();
            assertEquals(3,p.locator("[data-mark-period]").count());assertEquals(3,p.locator("#setup-availability-tool option").count());
            p.locator("[data-mark-period]").first().click();assertThat(p.locator("[data-mark-period]").first()).containsText("Unavailable");
            p.locator("#setup-availability-tool").selectOption("Prefer to avoid");p.locator("[data-mark-period]").first().click();
            assertThat(p.locator("[data-mark-period]").first()).containsText("Prefer to avoid");
            p.locator("#setup-availability-tool").selectOption("Unavailable");p.locator("#setup-mark-day").click();
            assertThat(p.locator("#setup-availability")).containsText("4 allocated lessons exceed 3");saved(p);
            h.record("UC-8 main 1-4/2b/G1-G2: actual authored grid exposes only three teaching opportunities, reserved/time labels and exactly three tools; individual replace distinguishes hard block/soft avoidance; whole-day hard marking retains load four and reports capacity three",p,"01-hard-soft-day");
            p.locator("[data-availability-day=TUESDAY]").click();p.locator("#setup-availability-tool").selectOption("Prefer to avoid");
            p.locator("#setup-afternoon-start").fill("13:00");p.locator("#setup-afternoon-end").fill("15:00");p.locator("#setup-mark-afternoon").click();saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.teachers[0].undesirablePeriodIds.length===2&&d.plans[0].allocations[0].count===4}"));
            p.locator("[data-select-mark]").first().check();p.locator("[data-availability-day=MONDAY]").click();p.locator("[data-select-mark]").first().check();
            p.locator("#setup-availability-tool").selectOption("Available");p.locator("#setup-mark-selected").click();saved(p);
            assertThat(p.locator("[data-mark-period]").first()).containsText("Available");
            p.locator("#setup-afternoon-start").fill("22:00");p.locator("#setup-afternoon-end").fill("23:00");saved(p);Object before=durable(p);
            p.locator("#setup-mark-afternoon").click();assertThat(p.locator("#setup-message")).containsText("No teaching periods");assertEquals(before,durable(p));
            close(p);p.reload();open(p);assertThat(p.locator("#setup-availability")).isVisible();
            h.record("UC-8 main 2-4/2a/G3: defined afternoon marks exact two timed opportunities; multi-selection survives day switches and clears only chosen periods; empty range leaves exact durable data; markings/location restore after reload",p,"02-range-selection");
            responsive(h,p,"UC-8");close(p);
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source),t=d.teachers[0],id=d.grid.intervals.find(i=>i.day==='MONDAY'&&i.periodId).periodId;t.availablePeriodIds=[];t.availability={intent:'whitelist',availablePeriodIds:[]};t.undesirablePeriodIds=[id];d.pendingInput.availability.day='MONDAY';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);assertThat(p.locator("[data-mark-period]").first()).containsText("Unavailable · also Prefer to avoid (retained)");
            p.locator("#setup-availability-tool").selectOption("Available");p.locator("[data-mark-period]").first().click();saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source),t=d.teachers[0];return t.availability.intent==='whitelist'&&t.availablePeriodIds.length===1&&t.availability.availablePeriodIds.length===1&&t.undesirablePeriodIds.length===0&&d.plans[0].allocations[0].count===4}"));
            h.record("UC-8 3a/G1/G3: explicit-empty imported whitelist and simultaneous avoidance are shown as two retained facts; explicit Available changes both for exactly one period while preserving whitelist intent and allocations",p,"03-retained-dual-facts");
        });
    }

    @Test void classLunchRetainsIndependentReasonsAndBellTimes() throws Exception {
        walk("UC-9",(h,p)->{
            p.locator("[data-active-day=MONDAY]").check();addInterval(p,"First","08:00","08:45","Lesson");addInterval(p,"School lunch","08:45","09:15","Lunch");addInterval(p,"Second","09:15","10:00","Lesson");
            section(p,"Classes");for(String name:List.of("Lunch Alpha","Lunch Beta")){p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill(name);p.locator("[data-catalog-field=size]").fill("20");}
            p.locator("[data-record]").first().click();p.locator("[data-open-class-lunch]").click();
            assertEquals(2,p.locator("[data-class-lunch]").count());assertThat(p.locator("#setup-class-lunch")).containsText("School lunch");
            p.locator("[data-class-lunch]").first().click();saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.classes[0].lunchPeriodIds.length===1&&!('lunchPeriodIds' in d.classes[1])&&d.grid.intervals[0].start==='08:00'&&d.grid.intervals[1].kind==='Lunch'&&d.grid.intervals[2].start==='09:15'}"));
            close(p);p.reload();open(p);assertThat(p.locator("[data-class-lunch]").first()).containsText("Class lunch");
            h.record("UC-9 main 1-4/G1/G3: real two-class grid distinguishes named school-wide lunch from teaching-period class lunch; only Alpha changes, common bell times/Beta remain exact; named/time marking survives reload",p,"01-class-lunch");
            close(p);
            // Restore a separate hard class whitelist and demand, then exercise lunch reason removal.
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source),c=d.classes[0],periods=d.grid.intervals.filter(i=>i.periodId);c.availablePeriodIds=[periods[1].periodId];c.undesirablePeriodIds=[periods[0].periodId];d.subjects=[{id:'s',displayName:'Subject'}];d.plans=[{id:'demand',classId:c.id,subjectId:'s',requiredCount:2,allocations:[],occurrences:[],blockRules:[]}];localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);assertThat(p.locator("#setup-class-lunch")).containsText("required 2 lessons exceed 1");
            assertThat(p.locator("[data-class-lunch]").first()).containsText("another retained class restriction");
            p.locator("[data-class-lunch]").first().click();assertThat(p.locator("#setup-message")).containsText("remains unavailable");saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source),c=d.classes[0];return c.lunchPeriodIds.length===0&&c.availablePeriodIds.length===1&&c.undesirablePeriodIds.length===1&&d.plans[0].requiredCount===2}"));
            p.locator("[data-class-lunch]").last().click();saved(p);assertThat(p.locator("#setup-class-lunch")).containsText("required 2 lessons exceed 0");
            h.record("UC-9 1a/3a/G1-G2: restored separate whitelist/avoidance remains after clearing only lunch; capacity issues retain intended lunch and exact demand, do not relocate either; lunch is explicitly excluded from gap accounting and interrupts adjacency",p,"02-lunch-reasons");
            responsive(h,p,"UC-9");
        });
    }

    @Test void roomRequirementsIntersectWithoutRelaxingRetainedPolicies() throws Exception {
        walk("UC-10",(h,p)->{
            p.locator("[data-active-day=MONDAY]").check();addInterval(p,"Morning","08:00","08:45","Lesson");addInterval(p,"Second","09:00","09:45","Lesson");
            section(p,"Rooms");
            for(String name:List.of("Laboratory","Small room","Unused zero")){
                p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill(name);p.locator("[data-catalog-field=capacity]").fill(name.equals("Laboratory")?"30":name.equals("Small room")?"20":"0");
                if(name.equals("Laboratory"))p.locator("[data-room-capabilities]").fill("lab, projector");
            }
            saved(p);assertThat(p.locator("#setup-record-issues")).hasText("");Object before=durable(p);
            p.locator("#setup-remove-record").click();p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));p.locator("#setup-remove-record").click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
            p.locator("[data-record]").first().click();p.locator("[data-room-capabilities]").fill("lab, invalid space");
            assertThat(p.locator("#setup-record-issues")).containsText("unique valid identifiers");saved(p);close(p);p.reload();open(p);
            assertThat(p.locator("[data-room-capabilities]")).hasValue("lab, invalid space");p.locator("[data-room-capabilities]").fill("lab, projector");saved(p);
            h.record("UC-10 main 1/1a/G3: rooms have exact nonnegative capacity/capability values; zero capacity accepted; unreferenced deletion cancel/confirm scoped; invalid capability input survives reload and correction",p,"01-room-values");
            close(p);
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source),r=d.rooms[0];d.subjects=[{id:'s',displayName:'Science'}];d.classes=[{id:'c',displayName:'Class',size:25,homeRoomId:r.id}];d.teachers=[{id:'t',displayName:'Teacher',qualifiedSubjectIds:['s'],eligibleClassIdsBySubject:{s:['c']}}];d.roomAssignments=[{id:'teacher-policy',subjectId:'s',teacherId:'t',allowedRoomIds:[r.id]}];d.plans=[{id:'plan',classId:'c',subjectId:'s',requiredCount:1,allocations:[{teacherId:'t',count:1}],occurrences:[{id:'occurrence',teacherId:'t',requiredRoomCapabilityIds:['lab'],preferredRoomIds:[r.id]}],blockRules:[]}];d.section='Subjects';d.focused.subjects='s';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);p.locator("details:has([data-subject-room-mode]) > summary").click();
            p.locator("[data-subject-room-mode]").selectOption("restricted");assertThat(p.locator("#setup-record-issues")).containsText("must not be empty");
            p.locator("details:has([data-subject-room-mode]) > summary").click();p.locator("[data-subject-room]").last().check();saved(p);
            section(p,"Classes");p.locator("[data-record]").first().click();p.locator("[data-open-class-plan]").click();
            assertThat(p.locator(".setup-plan-issues")).containsText("no suitable room");assertThat(p.locator(".setup-plan-issues")).containsText("teacher-policy");
            p.locator(".setup-row-settings > summary").click();
            String lab=(String)p.evaluate("Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source).rooms[0].id");
            p.locator("[data-plan-fixed-room]").selectOption(lab);saved(p);assertThat(p.locator(".setup-plan-issues")).containsText("no suitable room");
            section(p,"Subjects");p.locator("details:has([data-subject-room-mode]) > summary").click();p.locator("[data-subject-room]").last().uncheck();p.locator("[data-subject-room]").first().check();saved(p);
            section(p,"Classes");assertThat(p.locator(".setup-plan-issues")).hasText("");
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.roomAssignments.find(p=>p.id==='teacher-policy').allowedRoomIds[0]===d.rooms[0].id&&d.plans[0].fixedRoomId===d.rooms[0].id&&d.plans[0].occurrences[0].requiredRoomCapabilityIds[0]==='lab'}"));
            h.record("UC-10 main 2-4/2a/G1-G2: empty allowed pool is blocked; incompatible pool/capability/capacity/retained teacher-policy intersection remains hard-invalid even with fixed room; explicit pool correction preserves teacher policy and occurrence capabilities and resolves placement",p,"02-policy-intersection");
            section(p,"Rooms");p.locator("[data-record]").first().click();saved(p);before=durable(p);p.locator("#setup-remove-record").click();assertThat(p.locator("#setup-message")).containsText("homeRoomId");assertThat(p.locator("#setup-message")).containsText("roomAssignments");assertThat(p.locator("#setup-message")).containsText("fixedRoomId");assertEquals(before,durable(p));
            p.locator("[data-room-availability]").click();assertEquals(2,p.locator("#setup-availability-tool option").count());p.locator("#setup-mark-day").click();saved(p);
            section(p,"Classes");assertThat(p.locator(".setup-plan-issues")).containsText("no suitable room");
            section(p,"Rooms");p.locator("#setup-availability-tool").selectOption("Available");p.locator("#setup-mark-day").click();
            responsive(h,p,"UC-10");close(p);
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.plans[0].occurrences[0].roomLock=d.rooms[0].id;d.section='Classes';d.focused.classes='c';d.pendingInput.planClassId='c';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);p.locator(".setup-row-settings > summary").click();before=durable(p);p.locator("[data-plan-fixed-room]").selectOption("");assertThat(p.locator("#setup-message")).containsText("Fixed-room change refused");assertEquals(before,durable(p));
            h.record("UC-10 2b/3a/G1-G3: referenced room removal lists home/policy/preference/fixed-room references with exact non-mutation; room availability is hard-only and participates in intersection; protected occurrence lock refuses row-wide overwrite by value",p,"03-room-refusals");
        });
    }

    @Test void declaresExactBlockLengthsAndFrequenciesWithNecessaryWindows() throws Exception {
        walk("UC-11",(h,p)->{
            p.locator("[data-active-day=MONDAY]").check();
            addInterval(p,"First","08:00","08:45","Lesson");addInterval(p,"Break","08:45","09:00","Break");addInterval(p,"Second","09:00","09:45","Lesson");addInterval(p,"Third","10:00","10:45","Lesson");addInterval(p,"Lunch","10:45","11:15","Lunch");addInterval(p,"Fourth","11:15","12:00","Lesson");addInterval(p,"Fifth","12:15","13:00","Lesson");
            p.locator("[data-copy-day=TUESDAY]").check();p.locator("#setup-copy-day").click();p.locator(".setup-confirm [data-apply]").click();saved(p);close(p);
            p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.subjects=[{id:'s',displayName:'Subject'}];d.classes=[{id:'c',displayName:'Class',size:20}];d.teachers=[{id:'t',displayName:'Teacher',qualifiedSubjectIds:['s'],eligibleClassIdsBySubject:{s:['c']}}];d.rooms=[{id:'r',displayName:'Room',capacity:25,capabilityIds:[]}];d.plans=[{id:'plan',classId:'c',subjectId:'s',requiredCount:7,allocations:[{teacherId:'t',count:7}],seriesId:'series',occurrences:[],blockRules:[]}];d.section='Classes';d.focused.classes='c';d.pendingInput.planClassId='c';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);p.locator(".setup-row-settings > summary").click();assertThat(p.locator(".setup-block-rules")).containsText("Undeclared consecutive runs are forbidden");
            p.locator("[data-add-block]").click();p.locator("[data-block-field=length]").last().fill("2");
            p.locator("[data-add-block]").click();p.locator("[data-block-field=length]").last().fill("3");p.locator("[data-block-field=operator]").last().selectOption("AT_LEAST");saved(p);
            assertThat(p.locator(".setup-block-consumption")).containsText("Minimum consumed: 5 lessons · Remaining demand: 2");
            assertThat(p.locator(".setup-plan-issues")).hasText("");
            p.locator("[data-block-field=count]").last().fill("2");assertThat(p.locator(".setup-plan-issues")).containsText("exceeds weekly demand 7");
            p.locator("[data-block-field=count]").last().fill("1");p.locator("[data-block-field=length]").last().fill("2");assertThat(p.locator(".setup-plan-issues")).containsText("duplicate length 2");
            p.locator("[data-block-field=length]").last().fill("unfinished");saved(p);close(p);p.reload();open(p);p.locator(".setup-row-settings > summary").click();assertThat(p.locator("[data-block-field=length]").last()).hasValue("unfinished");
            p.locator("[data-block-field=length]").last().fill("4");assertThat(p.locator(".setup-plan-issues")).containsText("consecutive window of 4");saved(p);
            h.record("UC-11 main 1-4/2a/3a/G1-G4: explicit double plus At least triple consumes five of seven occurrences; independent length/frequency, over-consumption and duplicate length issues; unfinished length survives reload; length four cannot cross declared school lunch",p,"01-block-fields");
            section(p,"Timetable grid");String lunchId=p.locator("[data-interval]").filter(new Locator.FilterOptions().setHas(p.locator("input[value=Lunch]"))).getAttribute("data-interval");p.locator(".setup-interval[data-interval='"+lunchId+"'] [data-interval-kind]").selectOption("Break");
            section(p,"Classes");p.locator(".setup-row-settings > summary").click();assertThat(p.locator(".setup-plan-issues")).hasText("");
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.plans[0].requiredCount===7&&d.plans[0].allocations[0].count===7&&d.plans[0].blockRules[0].length===2&&d.plans[0].blockRules[1].length===4&&d.plans[0].blockRules[1].operator==='AT_LEAST'}"));
            responsive(h,p,"UC-11");saved(p);Object before=durable(p);
            p.locator("[data-remove-block]").last().click();assertThat(p.locator(".setup-confirm")).containsText("hard-forbidden");p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
            p.locator("[data-remove-block]").last().click();p.locator(".setup-confirm [data-confirm]").click();saved(p);
            assertEquals(1,p.locator("[data-block-index]").count());
            h.record("UC-11 3a/4a/G2-G4: changing school lunch to ordinary break recomputes a valid four-period common-teacher/common-room window without changing demand/allocations/rules; removal cancellation preserves exact data and explicit removal restores prohibition for that length",p,"02-window-and-removal");
            close(p);p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.plans[0].protectedReasons=['Imported series shared with another row'];localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);p.locator(".setup-row-settings > summary").click();before=durable(p);p.locator("[data-block-field=length]").fill("3");assertThat(p.locator("#setup-message")).containsText("Block change refused");assertThat(p.locator("[data-block-field=length]")).hasValue("2");assertEquals(before,durable(p));
            h.record("UC-11 3b: restored protected imported series refuses rule edits and retains exact authoring snapshot; UI declaration makes no claim of published kernel enforcement",p,"03-protected-block");
        });
    }

    @Test void reviewsExactSetupAndRejectsStaleOrFailedChecks() throws Exception {
        walk("UC-14",(h,p)->{
            section(p,"Review");p.locator("#setup-check").click();assertThat(p.locator("#setup-readiness")).containsText("Blocked");
            assertThat(p.locator("#setup-review-issues")).containsText("Allocate at least one lesson");
            h.record("UC-14 3a/G3: empty setup reports located blockers without fabricated lessons or authoritative request",p,"01-empty-blocked");
            close(p);p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.name='Ready school';d.grid={activeDays:['MONDAY'],intervals:[{id:'i',periodId:'p',day:'MONDAY',kind:'Lesson',label:'First',start:'08:00',end:'08:45'},{id:'r-i',periodId:'rp',day:'MONDAY',kind:'Reserved lesson',label:'Reserved',start:'09:00',end:'09:45'}]};d.subjects=[{id:'s',displayName:'Subject'}];d.classes=[{id:'c',displayName:'Class',size:20}];d.teachers=[{id:'t',displayName:'Teacher',qualifiedSubjectIds:['s'],eligibleClassIdsBySubject:{s:['c']}}];d.rooms=[{id:'r',displayName:'Room',capacity:25,capabilityIds:[]}];d.plans=[{id:'plan',classId:'c',subjectId:'s',requiredCount:1,allocations:[{teacherId:'t',count:1}],seriesId:'series',occurrences:[{id:'l',displayName:'Lesson',teacherId:'t',seriesId:'series'}],blockRules:[]}];d.section='Review';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);p.locator("#setup-check").click();assertThat(p.locator("#setup-readiness")).containsText("Ready",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));saved(p);
            assertThat(p.locator("#setup-totals")).containsText("Required: 1 · Allocated: 1 · Unassigned: 0 · Over-allocated: 0");
            assertThat(p.locator("#setup-content")).containsText("Regular capacity 1 · Reserved capacity 1");
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.check.catalogVersion===10&&d.check.authoringRevision===d.revision&&d.check.definition.lessons[0].id==='l'&&d.check.definition.periods[0].adjacentToNext===true&&d.check.definition.periods[1].adjacentToNext===false&&d.check.build.length>0}"));
            h.record("UC-14 main/G1-G5: restored complete prerequisite snapshot checked by real published kernel verify; exact lesson/grid fields, rule version, authoring revision and build retained; regular/reserved capacities distinct",p,"02-ready");
            // Delay a real response; changing the draft supersedes it.
            p.route("**/api/demo/definition/check",route->{var response=route.fetch();p.locator("#setup-name").fill("Newer name");route.fulfill(new Route.FulfillOptions().setResponse(response));});
            p.locator("#setup-check").click();assertThat(p.locator("#setup-validation")).containsText("Incomplete");saved(p);
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>!d.source);return d.name==='Newer name'&&!d.check}"));p.unroute("**/api/demo/definition/check");
            p.route("**/api/demo/definition/check",r->r.fulfill(new Route.FulfillOptions().setStatus(503).setContentType("application/json").setBody("{\"message\":\"Checking temporarily unavailable\"}")));
            p.locator("#setup-check").click();assertThat(p.locator("#setup-readiness")).containsText("Check failed");assertThat(p.locator("#setup-review-issues")).containsText("temporarily unavailable");p.unroute("**/api/demo/definition/check");
            h.record("UC-14 3c/4a: newer name invalidates an in-flight real response by exact authoring revision; service failure retains work and cannot earn Ready",p,"03-stale-and-failed");
            p.route("**/api/demo/definition/check",r->r.fulfill(new Route.FulfillOptions().setStatus(422).setContentType("application/json").setBody("{\"message\":\"/subjects/0: located invalid definition value\"}")));p.locator("#setup-check").click();assertThat(p.locator("#setup-readiness")).containsText("Blocked");p.locator("[data-review-issue]").click();assertThat(p.locator("#setup-section-title")).hasText("Subjects");assertThat(p.locator("#setup-record-name")).hasValue("Subject");p.unroute("**/api/demo/definition/check");
            h.record("UC-14 3a/4: located authoritative 422 validation refusal is Blocked, distinct from service failure; issue opens the exact affected subject",p,"04-located-blocker");responsive(h,p,"UC-14");
        });
    }

    @Test void convertsExplicitlyAndRecoversIndependentAuthoring() throws Exception {
        walk("UC-15",(h,p)->{
            String legacy="""
              {"schemaVersion":1,"catalogVersion":1,"schoolId":"legacy","displayName":"Imported school","subjects":[{"id":"s","displayName":"Subject"}],"teachers":[{"id":"t","displayName":"Teacher","qualifiedSubjectIds":["s"],"availablePeriodIds":["p"]}],"cohorts":[{"id":"c","displayName":"Class","size":20}],"rooms":[{"id":"r","displayName":"Room","capacity":25,"capabilityIds":[]}],"periods":[{"id":"p","displayName":"First","weekday":"MONDAY","order":5}],"lessons":[{"id":"l","displayName":"Unique lesson","subjectId":"s","cohortId":"c","teacherId":"t","preferredRoomIds":[],"undesirablePeriodIds":[]}],"softConstraintOverrides":[]}
              """;
            p.locator("#setup-name").fill("Keep incomplete work");saved(p);Object before=durable(p);
            Path input=temporary.resolve("legacy.json");Files.writeString(input,legacy);tool(p,"#setup-import-file").setInputFiles(input);
            assertThat(p.locator(".setup-confirm")).containsText("weight zero",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));assertThat(p.locator(".setup-confirm")).containsText("forbidden");
            p.locator(".setup-confirm [data-cancel]").click();assertEquals(before,durable(p));
            tool(p,"#setup-import-file").setInputFiles(new Path[0]);tool(p,"#setup-import-file").setInputFiles(input);p.locator(".setup-confirm [data-confirm]").click();saved(p);
            assertThat(p.locator("#setup-name")).hasValue("Imported school");assertEquals(3,p.locator("#setup-draft-selector option").count());
            assertTrue((Boolean)p.evaluate("()=>{const d=Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts).find(d=>d.name==='Imported school');return d.plans[0].occurrences[0].id==='l'&&!Object.hasOwn(d.plans[0],'seriesId')&&d.grid.intervals[0].order===5&&d.grid.intervals[0].unspecifiedTimes&&d.classes[0].maxDailyGaps===2147483647&&d.kernelExtras.softConstraintOverrides.length===3&&d.teachers[0].eligibleClassIdsBySubject.s[0]==='c'}"));
            h.record("UC-15 1-4/3a/G1-G5: validated legacy JSON explicit preview lists prohibition/default/weight changes; cancellation by-value nonmutation; adoption makes separate draft preserving old work, ungrouped lesson identity, empties, unspecified times/order and observed eligibility",p,"01-import-preview-and-adoption");
            String advanced=p.locator("#school-definition-json").inputValue();tool(p,"#setup-transfer").click();assertThat(p.locator(".setup-confirm")).containsText("Excludes unfinished demand");p.locator(".setup-confirm [data-cancel]").click();assertEquals(advanced,p.locator("#school-definition-json").inputValue());
            tool(p,"#setup-transfer").click();p.locator(".setup-confirm [data-confirm]").click();assertThat(p.locator("#setup-message")).containsText("Explicitly transferred");assertTrue(p.locator("#school-definition-json").inputValue().contains("\"catalogVersion\": 10"));
            p.waitForFunction("text=>JSON.parse(localStorage.getItem(\"school-kernel.timetable-demo.v1\")).editor?.text===text",p.locator("#school-definition-json").inputValue());
            Path recovery=temporary.resolve("recovery.json");Download download=p.waitForDownload(()->tool(p,"#setup-download").click());download.saveAs(recovery);
            tool(p,"#setup-import-file").setInputFiles(recovery);p.locator(".setup-confirm [data-confirm]").click();assertThat(p.locator("#setup-source")).containsText("Recovered");saved(p);
            Path duplicate=temporary.resolve("duplicate.json");Files.writeString(duplicate,"{\"catalogVersion\":1,\"catalogVersion\":10}");before=durable(p);tool(p,"#setup-import-file").setInputFiles(duplicate);assertThat(p.locator("#setup-message")).containsText("Duplicate field catalogVersion");assertEquals(before,durable(p));
            h.record("UC-15 1a/2a/5/G6-G7: explicit scoped transfer only after acceptance; full recovery download imports into another identity; duplicate JSON refuses with exact durable nonmutation and existing input retained",p,"02-exchange-recovery-and-refusal");
            section(p,"Classes");p.locator("[data-record=c]").click();p.locator("[data-open-class-plan=c]").click();p.locator(".setup-row-settings > summary").click();before=durable(p);p.locator("[data-add-block]").click();assertThat(p.locator("#setup-message")).containsText("grouping cannot support");assertEquals(before,durable(p));responsive(h,p,"UC-15");
        });
    }

    @Test void savesOnlyExactCheckedSchoolAtomicallyWithoutSolving() throws Exception {
        walk("UC-16",(h,p)->{
            // Restore a complete checked-authoring prerequisite; UC-17 exercises empty-to-save by real controls.
            close(p);p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.name='Saved setup';d.grid={activeDays:['MONDAY'],intervals:[{id:'i',periodId:'p',day:'MONDAY',kind:'Lesson',label:'First',start:'08:00',end:'08:45'}]};d.subjects=[{id:'s',displayName:'Subject'}];d.classes=[{id:'c',displayName:'Class',size:20}];d.teachers=[{id:'t',displayName:'Teacher',qualifiedSubjectIds:['s'],eligibleClassIdsBySubject:{s:['c']}}];d.rooms=[{id:'r',displayName:'Room',capacity:25,capabilityIds:[]}];d.plans=[{id:'plan',classId:'c',subjectId:'s',requiredCount:1,allocations:[{teacherId:'t',count:1}],seriesId:'series',occurrences:[{id:'l',displayName:'Lesson',teacherId:'t',seriesId:'series'}],blockRules:[]}];d.section='Review';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);Object before=p.evaluate("()=>JSON.parse(localStorage.getItem('"+KEY+"')).setupDrafts");
            p.locator("#setup-name").fill("Kasevälja Põhikool");p.locator("#setup-save-school").click();assertThat(p.locator("#setup-message")).containsText("already exists");
            p.locator("#setup-name").fill("x".repeat(121));p.locator("#setup-save-school").click();assertThat(p.locator("#setup-message")).containsText("at most 120");
            p.locator("#setup-name").fill("Saved setup");saved(p);
            // Fail only the atomic school write; the checked draft remains recoverable.
            p.evaluate("()=>{window.originalSet=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='"+KEY+"'&&Object.keys(JSON.parse(v).schools).length)throw new DOMException('quota','QuotaExceededError');return window.originalSet.call(this,k,v)}}");
            p.locator("#setup-save-school").click();assertThat(p.locator("#setup-message")).containsText("Nothing was saved",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
            assertTrue((Boolean)p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"'));return Object.keys(s.schools).length===0&&Object.values(s.setupDrafts).some(d=>d.name==='Saved setup')}"));
            h.record("UC-16 1a/3b: duplicate/long names refused; injected atomic-write quota failure preserves schools and source draft, claims no success and starts no solve",p,"01-save-refusals");p.evaluate("()=>Storage.prototype.setItem=window.originalSet");
            p.locator("#setup-save-school").click();assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));
            assertThat(p.locator("#timetable-selector")).containsText("No timetable yet");assertThat(p.locator("#school-selector option:checked")).hasText("Saved setup");
            assertTrue((Boolean)p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),c=Object.values(s.schools)[0];return c.name===c.definition.displayName&&c.check.definitionRevision===c.definitionRevision&&c.authoring.revision===c.check.authoringRevision&&c.definition.lessons[0].id==='l'&&!s.setupDrafts[c.authoring.id]&&!c.generation&&Object.keys(s.timetables).length===0}"));
            p.reload();assertThat(p.locator("#timetable-selector")).containsText("No timetable yet");open(p);assertThat(p.locator("#setup-source")).containsText("Saved setup");
            assertTrue((Boolean)p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),c=Object.values(s.schools)[0],d=Object.values(s.setupDrafts).find(d=>d.source?.id===c.id);return d&&d.id!==c.authoring.id&&d.plans[0].occurrences[0].id==='l'}"));
            h.record("UC-16 main/G1-G4: fresh authoritative check and atomic definition+authoring+provenance save creates independent selected identity, consumes exact draft only, no solve; reload reopens successor retaining lesson identity",p,"02-school-without-timetable");responsive(h,p,"UC-16");
        });
    }

    @Test void emptySchoolToNamedTimetableAndInterruptedRun() throws Exception {
        walk("UC-17",(h,p)->{
            p.locator("#setup-name").fill("Authored school");p.locator("[data-active-day=MONDAY]").check();
            addInterval(p,"First","08:00","08:45","Lesson");addInterval(p,"Morning break","08:45","09:00","Break");addInterval(p,"Second","09:00","09:45","Lesson");addInterval(p,"School lunch","09:45","10:15","Lunch");addInterval(p,"Third","10:15","11:00","Lesson");
            section(p,"Subjects");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Mathematics");
            section(p,"Classes");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Class One");p.locator("[data-catalog-field=size]").fill("20");
            p.locator("[data-open-class-lunch]").click();p.locator("[data-class-lunch]").last().click();
            section(p,"Teachers");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Teacher One");p.locator("[data-teacher-qualification]").check();p.locator("#setup-record-detail details > summary").click();p.locator("[data-teacher-class]").check();
            section(p,"Rooms");p.locator("#setup-add-record").click();p.locator("#setup-record-name").fill("Room One");p.locator("[data-catalog-field=capacity]").fill("25");
            section(p,"Classes");p.locator("#setup-back-lunch").click();p.locator("[data-open-class-plan]").click();p.locator("#setup-plan-subject").selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("Mathematics"));p.locator("#setup-add-plan").click();p.locator("[data-plan-demand]").fill("2");p.locator("[data-plan-eligible]").selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("Teacher One"));p.locator("[data-add-allocation]").click();p.locator("[data-allocation-count]").fill("2");p.locator(".setup-row-settings > summary").click();p.locator("[data-add-block]").click();p.locator("[data-block-field=length]").fill("2");
            section(p,"Review");p.locator("#setup-check").click();assertThat(p.locator("#setup-readiness")).containsText("Ready",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
            h.record("UC-17 primary prerequisites UC-1–16: real empty-school actor authors timed grid, ordinary break and school lunch, subject/class/teacher eligibility, room, two allocated occurrences, class lunch and explicit exactly-one double; authoritative readiness passes",p,"01-empty-to-ready");
            p.locator("#setup-save-school").click();assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));Object school=p.evaluate("()=>Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).schools)[0]");
            open(p);section(p,"Review");p.locator("#setup-name").fill("Unsaved newer setup");p.locator("#setup-effort").selectOption("QUICK");p.locator("#setup-start-generation").click();assertThat(p.locator("#setup-generation-status")).containsText("Preparing");
            assertThat(p.locator("#setup-generation-status")).containsText("name and save",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(60000));assertThat(p.locator("#school-setup")).isVisible();assertThat(p.locator("#setup-name")).hasValue("Unsaved newer setup");
            assertTrue((Boolean)p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),c=Object.values(s.schools)[0],d=Object.values(s.drafts).find(d=>d.generated);return d.source.definition.displayName==='Authored school'&&d.source.definition.catalogVersion===10&&d.source.result.inputRevision===c.definitionRevision&&d.source.provenance.pin.authoring.name==='Authored school'&&Object.values(s.setupDrafts).some(d=>d.name==='Unsaved newer setup')}"));
            h.record("UC-17 main 1-5/G1-G4: explicit Quick uses pinned immutable school/definition/build/authoring despite newer setup edits; real kernel generation and verification yield origin candidate without closing editor or changing draft",p,"02-pinned-candidate");close(p);
            assertThat(p.locator("#timetable-matrix")).containsText("Morning break");assertThat(p.locator("#timetable-matrix")).containsText("School lunch");assertThat(p.locator("#timetable-matrix")).containsText("Class lunch");assertEquals(2,p.locator(".demo-lesson").count());
            p.locator(".demo-lesson").first().click();assertThat(p.locator("#lesson-inspector")).containsText("Mathematics");String originalPeriod=p.locator("#inspector-period").inputValue();p.locator("#inspector-period").selectOption(new com.microsoft.playwright.options.SelectOption().setIndex(2));
            assertThat(p.locator("#draft-feedback")).containsText("Consecutive lesson rule",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));assertThat(p.locator("#save-new-timetable")).isDisabled();
            p.locator("#inspector-period").selectOption(originalPeriod);p.locator("[data-close-inspector]").click();
            assertThat(p.locator("#save-new-timetable")).isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions().setTimeout(30000));p.locator("#new-timetable-name").fill("First timetable");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-selector option:checked")).hasText("First timetable");
            p.reload();assertThat(p.locator("#timetable-selector option:checked")).hasText("First timetable");assertThat(p.locator("#timetable-matrix")).containsText("Class lunch");
            Object savedTimetables=p.evaluate("()=>JSON.parse(localStorage.getItem('"+KEY+"')).timetables");
            h.record("UC-17 main 6-7/G5-G7: separate lesson cells retain real times and named break/school/class lunch; inspect then explicitly name/save complete reviewed timetable; exact origin grid and assignments reopen after reload",p,"03-named-timetable");
            open(p);section(p,"Review");p.locator("#setup-effort").selectOption("THOROUGH");p.locator("#setup-start-generation").click();assertThat(p.locator("#setup-generation-status")).containsText("Running",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
            h.stop();h.start(List.of(),2);p.reload();p.locator("#demo-utilities > summary").click();assertThat(p.locator("#generation-status")).containsText("could not be recovered",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
            assertEquals(savedTimetables,p.evaluate("()=>JSON.parse(localStorage.getItem('"+KEY+"')).timetables"));assertEquals(((Map<?,?>)school).get("definition"),p.evaluate("()=>Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).schools)[0].definition"));
            h.record("UC-17 3a/4a/G8: actual packaged-service stop interrupts an explicit second solve; restart/reload reports lost run provenance, retains exact saved school/timetable/setup and starts no retry",p,"04-real-restart");open(p);responsive(h,p,"UC-17");
        });
    }

    @Test void cleansAllSetupArtifactsWithoutStaleTabResurrection() throws Exception {
        walk("UC-18",(h,p)->{
            p.locator("#setup-name").fill("Deleted unfinished setup");saved(p);close(p);
            p.evaluate("()=>{localStorage.setItem('unrelated-test','keep exact');const s=JSON.parse(localStorage.getItem('"+KEY+"'));s.unreadableRecovery={unknown:['raw']};s.editor={text:'unfinished JSON',source:'source'};s.drafts['unknown/candidate']={generated:true,source:{raw:'candidate'}};localStorage.setItem('"+KEY+"',JSON.stringify(s));localStorage.setItem('school-kernel.definition.v1.unreadable','{exact raw')}");
            p.reload();Page stale=p.context().newPage();stale.onPageError(h.errors::add);stale.navigate(h.base+"/workspace/");open(stale);assertThat(stale.locator("#setup-name")).hasValue("Deleted unfinished setup");
            p.locator("#demo-utilities > summary").click();p.locator("#delete-local-data").click();assertThat(p.locator("#delete-local-data-dialog")).containsText("authoring snapshots");assertTrue((Boolean)p.evaluate("()=>document.activeElement.id==='cancel-delete-local-data'"));
            Object before=p.evaluate("()=>Object.fromEntries(Object.keys(localStorage).map(k=>[k,localStorage.getItem(k)]))");p.locator("#cancel-delete-local-data").click();assertEquals(before,p.evaluate("()=>Object.fromEntries(Object.keys(localStorage).map(k=>[k,localStorage.getItem(k)]))"));
            p.evaluate("()=>{window.removeOriginal=Storage.prototype.removeItem;Storage.prototype.removeItem=function(k){if(k==='"+KEY+"')throw new DOMException('refused','SecurityError');return window.removeOriginal.call(this,k)}}");
            p.locator("#delete-local-data").click();p.locator("#confirm-delete-local-data").click();assertThat(p.locator("#delete-local-data-error")).containsText("retained");assertEquals(before,p.evaluate("()=>Object.fromEntries(Object.keys(localStorage).map(k=>[k,localStorage.getItem(k)]))"));p.evaluate("()=>Storage.prototype.removeItem=window.removeOriginal");
            h.record("UC-18 1-2/2a/3a/G1-G2: complete scope named; Cancel has initial focus and exact nonmutation; injected removal failure restores every exact raw value including malformed legacy/recovery metadata and unrelated key",p,"01-cancel-and-rollback");
            List<String> mutations=new ArrayList<>();p.onRequest(r->{if(!r.method().equals("GET"))mutations.add(r.method()+" "+r.url());});p.locator("#confirm-delete-local-data").click();assertThat(p.locator("#school-selector option")).hasCount(4);assertThat(p.locator("#school-selector option:checked")).hasText("Kasevälja Põhikool");
            assertTrue((Boolean)p.evaluate("()=>localStorage.getItem('"+KEY+"')===null&&localStorage.getItem('school-kernel.definition.v1.unreadable')===null&&localStorage.getItem('unrelated-test')==='keep exact'"));
            stale.locator("#setup-name").fill("Attempted resurrection");stale.locator("#setup-retry").click();assertThat(stale.locator("#setup-durability")).hasText("Storage conflict");assertTrue((Boolean)p.evaluate("()=>localStorage.getItem('"+KEY+"')===null"));
            p.reload();open(p);tool(p,"#setup-new").click();p.locator("#setup-name").fill("Fresh work after cleanup");saved(p);assertTrue((Boolean)p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"'));return Object.values(s.setupDrafts).some(d=>d.name==='Fresh work after cleanup')&&!Object.values(s.setupDrafts).some(d=>d.name==='Attempted resurrection')}"));
            assertTrue(mutations.isEmpty(),mutations.toString());h.record("UC-18 main 3-4/3b/G1/G3: confirmed local-only deletion removes all raw artifacts, defaults to bundled timetable after reload; stale tab autosave refuses by cleanup epoch and cannot resurrect; fresh setup saves; no server mutation request",p,"02-cleanup-stale-refusal-and-new-work");stale.close();responsive(h,p,"UC-18");
        });
    }

    @Test void generationCancellationOtherSchoolAndCandidateQuotaRecovery() throws Exception {
        walk("UC-17-faults",(h,p)->{
            close(p);p.evaluate("()=>{const s=JSON.parse(localStorage.getItem('"+KEY+"')),d=Object.values(s.setupDrafts).find(d=>!d.source);d.name='Fault school';d.grid={activeDays:['MONDAY'],intervals:[{id:'i',periodId:'p',day:'MONDAY',kind:'Lesson',label:'First',start:'08:00',end:'08:45'}]};d.subjects=[{id:'s',displayName:'Subject'}];d.classes=[{id:'c',displayName:'Class',size:20}];d.teachers=[{id:'t',displayName:'Teacher',qualifiedSubjectIds:['s'],eligibleClassIdsBySubject:{s:['c']}}];d.rooms=[{id:'r',displayName:'Room',capacity:25,capabilityIds:[]}];d.plans=[{id:'plan',classId:'c',subjectId:'s',requiredCount:1,allocations:[{teacherId:'t',count:1}],seriesId:'series',occurrences:[{id:'l',displayName:'Lesson',teacherId:'t',seriesId:'series'}],blockRules:[]}];d.section='Review';localStorage.setItem('"+KEY+"',JSON.stringify(s))}");
            p.reload();open(p);p.locator("#setup-save-school").click();assertThat(p.locator("#school-setup")).not().isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));String origin=p.locator("#school-selector").inputValue();Object definition=p.evaluate("()=>Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).schools)[0].definition");
            open(p);section(p,"Review");p.locator("#setup-effort").selectOption("THOROUGH");p.locator("#setup-start-generation").click();assertThat(p.locator("#setup-generation-status")).containsText("Running",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));p.locator("#setup-cancel-generation").click();assertThat(p.locator("#setup-generation-status")).containsText("cancelled",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(30000));
            assertEquals(definition,p.evaluate("()=>Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).schools)[0].definition"));h.record("UC-17 3b: real active Thorough cancellation acknowledged, no candidate, immutable saved definition retained",p,"01-cancelled");
            p.locator("#setup-effort").selectOption("QUICK");p.locator("#setup-start-generation").click();close(p);p.locator("#school-selector").selectOption("full");
            p.evaluate("()=>{window.setBeforeQuota=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='"+KEY+"'&&Object.values(JSON.parse(v).drafts??{}).some(d=>d.generated))throw new DOMException('quota','QuotaExceededError');return window.setBeforeQuota.call(this,k,v)}}");
            assertThat(p.locator("#generation-indicator")).containsText("awaits naming",new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(60000));assertEquals("full",p.locator("#school-selector").inputValue());
            assertTrue((Boolean)p.evaluate("()=>!Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).drafts).some(d=>d.generated)"));
            p.locator("[data-view-generated]").click();assertEquals(origin,p.locator("#school-selector").inputValue());p.locator("#demo-utilities > summary").click();assertThat(p.locator("#generation-status")).containsText("Only on this page");p.locator("#close-editor").click();
            assertThat(p.locator("#save-new-timetable")).isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions().setTimeout(30000));p.evaluate("()=>Storage.prototype.setItem=window.setBeforeQuota");p.locator("#new-timetable-name").fill("Recovered candidate");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-selector option:checked")).hasText("Recovered candidate");
            h.record("UC-17 5a/5b/7a: completion attaches to origin while another school remains selected; candidate quota failure reports Only on this page and preserves complete memory candidate; explicit naming/save after storage recovery persists it atomically",p,"02-origin-and-quota-recovery");
            p.locator("#demo-utilities > summary").click();p.locator("#generate-timetable").click();assertThat(p.locator("#timetable-save-bar")).isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(30000));assertThat(p.locator("#save-new-timetable")).isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions().setTimeout(30000));
            p.locator("#new-timetable-name").fill(" recovered CANDIDATE ");p.locator("#save-new-timetable").click();assertThat(p.locator("#timetable-name-error")).containsText("already has");p.locator("#discard-timetable-changes").click();assertThat(p.locator("#timetable-selector option:checked")).hasText("Recovered candidate");
            assertEquals(definition,p.evaluate("()=>Object.values(JSON.parse(localStorage.getItem('"+KEY+"')).schools)[0].definition"));h.record("UC-17 1a/6a/7a: reused complete candidate awaits explicit decision; trimmed case-insensitive duplicate name refused; discard removes only candidate and retains prior timetable, saved definition and setup",p,"03-name-refusal-and-discard");
        });
    }

}
