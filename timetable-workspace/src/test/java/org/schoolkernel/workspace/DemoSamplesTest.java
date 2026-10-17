package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Lifecycle/refusal and immutable-copy checks; real process and scoring checks live at the packaged boundary. */
class DemoSamplesTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private DemoWorkspaces owners;
    private DemoWorkspaceStore store;
    private DemoSamples samples;
    private final String artifact = "../kernel-cli/target/school-kernel.jar";

    @BeforeEach void setup() throws Exception {
        var beans = new StaticListableBeanFactory();
        owners = new DemoWorkspaces(new DemoSettings(List.of("demo.example"), List.of(), false,
                Duration.ofHours(1), Duration.ofHours(8), 50), json, beans.getBeanProvider(KernelPlanner.class));
        store = new DemoWorkspaceStore(owners);
        samples = sampleService(new WorkspaceMutation(store));
        samples.loadPackagedSamples();
    }
    @AfterEach void close() { owners.close(); }
    private DemoSamples sampleService(WorkspaceMutation mutation) {
        return new DemoSamples(json, new ManifestService(), store, mutation, artifact);
    }
    private JsonNode read(String name) { try { return samples.read(name); } catch (IOException e) { throw new RuntimeException(e); } }
    @Test void exactCatalogCopiesPreviewAndFourOpensNeverInvokeVisitorVerification() {
        var catalog = samples.catalog();
        assertEquals(List.of("two-class", "full", "star-wars", "english"), java.util.stream.StreamSupport.stream(catalog.spliterator(), false)
                .map(row -> row.path("identifier").asString()).toList());
        var owner = owners.open(new MockHttpSession(), true);
        owners.bound(owner, () -> {
            for (String id : List.of("two-class", "full", "star-wars", "english")) {
                JsonNode original = samples.preview(id).path("definition").deepCopy();
                ObjectNode preview = samples.preview(id); ((ObjectNode) preview.path("definition")).put("displayName", "Edited privately");
                assertEquals(original, samples.preview(id).path("definition"));
                var current = store.load();
                var opened = samples.use(id, current.etag(), current.state() != WorkspaceState.EMPTY);
                assertEquals(WorkspaceState.ACCEPTED_BASELINE, opened.state());
                assertEquals(original, opened.document().path("acceptedBaseline").path("definition"));
                assertEquals(read(id + "-a-result.json"), opened.document().path("acceptedBaseline").path("result"));
                ((ObjectNode) opened.document().path("acceptedBaseline").path("definition")).put("displayName", "Caller mutation");
                assertEquals(original, store.load().document().path("acceptedBaseline").path("definition"));
            }
        });

    }
    @Test void declineUnknownStaleAndActiveRunPreserveCompleteWorkInEveryLifecycle() {
        var owner = owners.open(new MockHttpSession(), true);
        owners.bound(owner, () -> {
            samples.use("two-class", store.load().etag(), false);
            for (var state : WorkspaceState.values()) {
                if (state == WorkspaceState.EMPTY || state == WorkspaceState.DEFINITION_DRAFT) continue;
                var before = store.load();
                var document = before.document().deepCopy();
                ((ObjectNode) document).putObject("privateDraft").put("exact", state.name());
                store.replace(before.version(), before.state(), state, document);
                var prior = store.load();
                assertEquals("SAMPLE_NOT_FOUND", assertThrows(WorkspaceProblem.class,
                        () -> samples.use("unknown", prior.etag(), true)).code());
                assertEquals(prior, store.load());
                assertEquals("STALE_WORKSPACE_VERSION", assertThrows(WorkspaceProblem.class,
                        () -> samples.use("full", "\"ws-0\"", true)).code());
                assertEquals(prior, store.load());
                boolean active = state == WorkspaceState.SOLVING_INITIAL || state == WorkspaceState.SOLVING_REPAIR;
                assertEquals(active ? "INVALID_WORKSPACE_TRANSITION" : "REPLACEMENT_CONFIRMATION_REQUIRED",
                        assertThrows(WorkspaceProblem.class, () -> samples.use("full", prior.etag(), false)).code());
                assertEquals(prior, store.load());
                if (active) {
                    assertEquals("INVALID_WORKSPACE_TRANSITION", assertThrows(WorkspaceProblem.class,
                            () -> samples.use("full", prior.etag(), true)).code());
                    assertEquals(prior, store.load());
                    store.replace(prior.version(), prior.state(), WorkspaceState.ACCEPTED_BASELINE, prior.document());
                } else assertEquals(WorkspaceState.ACCEPTED_BASELINE, samples.use("full", prior.etag(), true).state());
            }
        });

    }
    @Test void englishSchoolHasItsOwnScaleCurriculumAndTwoCompleteDifferentTimetables() {
        var english = samples.preview("english");
        var definition = english.path("definition");
        var full = samples.preview("full").path("definition");
        assertEquals("Meadowbrook Community School", definition.path("displayName").asString());
        var dimensions = Map.of("cohorts", 22, "teachers", 41, "rooms", 35, "lessons", 614, "periods", 30);
        dimensions.forEach((field, count) -> {
            assertEquals(count.intValue(), definition.path(field).size(), field);
            assertNotEquals(full.path(field).size(), count.intValue(), "Distinct from Kasevälja: " + field);
        });
        var subjectNames = new HashSet<String>();
        definition.path("subjects").forEach(s -> subjectNames.add(s.path("displayName").asString()));
        assertTrue(subjectNames.containsAll(List.of("English", "Mathematics", "Science", "French", "Spanish", "Design and Technology")));
        var teaching = new HashMap<String, String>();
        definition.path("lessons").forEach(l -> {
            String previous = teaching.putIfAbsent(l.path("seriesId").asString(), l.path("teacherId").asString());
            if (previous != null) assertEquals(previous, l.path("teacherId").asString(), "Consistent course teacher");
        });
        var a = english.path("timetables").get(0).path("result").path("timetable").path("assignments");
        var b = english.path("timetables").get(1).path("result").path("timetable").path("assignments");
        assertEquals(614, a.size()); assertEquals(614, b.size());
        var first = new HashMap<String, JsonNode>(); a.forEach(l -> first.put(l.path("lessonId").asString(), l));
        int moved = 0;
        for (JsonNode lesson : b) {
            var original = first.get(lesson.path("lessonId").asString());
            assertNotNull(original);
            assertEquals(original.path("teacherId"), lesson.path("teacherId"));
            if (!original.path("periodId").equals(lesson.path("periodId"))) moved++;
        }
        assertTrue(moved >= 4, "Variants must visibly differ");
    }
    @Test void imperialCurriculumProgressesFromBasicsToOperationalTrainingWithBalancedDays() {
        var academy = samples.preview("star-wars"); var d = academy.path("definition");
        assertEquals("Coruscant Imperial Academy", academy.path("title").asString());
        assertEquals(18,d.path("cohorts").size()); assertEquals(540,d.path("lessons").size());
        assertEquals(29,d.path("teachers").size()); assertEquals(37,d.path("rooms").size());
        var names = new HashMap<String,String>(); d.path("subjects").forEach(s -> names.put(s.path("id").asString(),s.path("displayName").asString()));
        var subjects = Map.of(
            "junior", Set.of("Galactic Basic","Applied Numeracy","Galactic History","Natural Sciences","Droid Literacy","Physical Conditioning","Academy Citizenship","Teamwork and First Aid"),
            "intermediate", Set.of("Technical Communication","Applied Physics","Astrogation Fundamentals","Starship Systems","Flight Simulation","Tactical Fundamentals","Fieldcraft and Survival","Physical Conditioning","Emergency Medicine","Command Ethics and Law"),
            "senior", Set.of("Advanced Astrogation","Starfighter Operations","Fleet Tactics","Combined Operations","Mission Planning","Intelligence Analysis","Starship Engineering","Field Leadership","Combat Medicine","Physical Conditioning","Officer Briefing"));
        var perDay = Map.of("junior",5,"intermediate",6,"senior",7);
        var stageOf = new HashMap<String,String>(); var teaching = new HashMap<String,String>();
        for (JsonNode cohort : d.path("cohorts")) {
            String id=cohort.path("id").asString(); String stage=List.of("junior","intermediate","senior").stream().filter(s->id.startsWith("imperial-cohort-"+s+"-")).findFirst().orElseThrow();
            stageOf.put(id,stage); var counts=new HashMap<String,Integer>();
            for(JsonNode lesson:d.path("lessons")) if(lesson.path("cohortId").asString().equals(id)) {
                counts.merge(names.get(lesson.path("subjectId").asString()),1,Integer::sum);
                String previous=teaching.putIfAbsent(lesson.path("seriesId").asString(),lesson.path("teacherId").asString());
                if(previous!=null) assertEquals(previous,lesson.path("teacherId").asString());
            }
            assertEquals(subjects.get(stage),counts.keySet(),id);
            assertEquals(perDay.get(stage)*5,counts.values().stream().mapToInt(Integer::intValue).sum(),id);
            assertEquals(3,counts.get("Physical Conditioning"));
            if(stage.equals("junior")){assertEquals(5,counts.get("Galactic Basic"));assertEquals(5,counts.get("Applied Numeracy"));}
            if(stage.equals("senior")){assertEquals(4,counts.get("Fleet Tactics"));assertEquals(4,counts.get("Combined Operations"));assertEquals(4,counts.get("Mission Planning"));}
        }
        var weekdays=new HashMap<String,String>(); d.path("periods").forEach(p->weekdays.put(p.path("id").asString(),p.path("weekday").asString()));
        for(JsonNode timetable:academy.path("timetables")) {
            var days=new HashMap<String,Integer>();var dailySubjects=new HashSet<String>();
            for(JsonNode a:timetable.path("result").path("timetable").path("assignments")) {
                String cohort=a.path("cohortId").asString(),day=weekdays.get(a.path("periodId").asString());
                days.merge(cohort+"/"+day,1,Integer::sum);
                assertTrue(dailySubjects.add(cohort+"/"+day+"/"+a.path("subjectId").asString()),"One subject occurrence per day");
            }
            stageOf.forEach((cohort,stage)->weekdays.values().stream().distinct().forEach(day->assertEquals(perDay.get(stage),days.get(cohort+"/"+day))));
        }
    }
    @Test void installationFailureRetainsCurrentAndOtherOwnerIsIndependent() {
        var first = owners.open(new MockHttpSession(), true); var second = owners.open(new MockHttpSession(), true);
        owners.bound(first, () -> samples.use("two-class", store.load().etag(), false));
        owners.bound(second, () -> samples.use("full", store.load().etag(), false));
        var failingMutation = mock(WorkspaceMutation.class);
        when(failingMutation.resetAndImport(anyLong(), any(), any())).thenThrow(new IllegalStateException("save unavailable"));
        DemoSamples failing = sampleService(failingMutation);
        assertDoesNotThrow(failing::loadPackagedSamples);
        owners.bound(first, () -> {
            var prior = store.load();
            assertThrows(IllegalStateException.class, () -> failing.use("two-class", prior.etag(), true));
            assertEquals(prior, store.load());
            samples.use("two-class", prior.etag(), true);
        });
        owners.bound(second, () -> assertEquals("full", store.load().document().path("origin").asString()));
    }
    @Test void startupLoadsArtifactsWithoutReadingScoringEvidence() throws Exception {
        DemoSamples direct = new DemoSamples(json, new ManifestService(), store, new WorkspaceMutation(store), artifact) {
            @Override JsonNode read(String name) throws IOException {
                assertNotEquals("scoring-evidence.json", name, "Startup must not inspect maintenance evidence");
                return super.read(name);
            }
        };
        direct.loadPackagedSamples();
        assertEquals(samples.bundle(), direct.bundle());
    }
    @Test void missingOrNonBooleanConfirmationIsASafeRefusalAtControllerBoundary() {
        var owner = owners.open(new MockHttpSession(), true);
        owners.bound(owner, () -> {
            samples.use("two-class", store.load().etag(), false);
            var prior = store.load(); var controller = new DemoSampleController(samples, mock(WorkspaceController.class));
            for (String text : List.of("{}", "{\"confirmed\":\"true\"}", "{\"confirmed\":1}", "[]", "null")) {
                assertEquals("REPLACEMENT_CONFIRMATION_REQUIRED", assertThrows(WorkspaceProblem.class,
                        () -> controller.use("full", prior.etag(), json.readTree(text))).code());
                assertEquals(prior, store.load());
            }
            assertEquals("REPLACEMENT_CONFIRMATION_REQUIRED", assertThrows(WorkspaceProblem.class,
                    () -> controller.use("full", prior.etag(), null)).code());
            assertEquals(prior, store.load());
        });
    }
    @Test void unreadableResourceDoesNotPublishAPartialCatalog() {
        DemoSamples missing = new DemoSamples(json, new ManifestService(), store, new WorkspaceMutation(store), artifact) {
            @Override JsonNode read(String name) throws IOException {
                if (name.equals("english-b-result.json")) throw new IOException("Missing bundled resource");
                return super.read(name);
            }
        };
        assertThrows(IOException.class, missing::loadPackagedSamples);
        assertTrue(missing.catalog().isEmpty());
    }
}
