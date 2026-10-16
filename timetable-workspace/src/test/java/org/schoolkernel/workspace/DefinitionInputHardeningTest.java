package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class DefinitionInputHardeningTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final DemoDefinitionLimits limits = new DemoDefinitionLimits(11_000_000, 2500);

    ObjectNode school() throws Exception {
        try (var input = getClass().getResourceAsStream("/demo/timetables/two-class-definition.json")) {
            return (ObjectNode) json.readTree(input);
        }
    }
    WorkspaceProblem rejected(String input) {
        return assertThrows(WorkspaceProblem.class, () -> limits.read(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
    }

    @Test void allBundledSchoolsPassExactlyAndNamesRemainData() throws Exception {
        for (String name : List.of("two-class", "full", "star-wars", "english")) {
            try (var input = getClass().getResourceAsStream("/demo/timetables/" + name + "-definition.json")) {
                byte[] bytes = input.readAllBytes();
                assertEquals(json.readTree(bytes), limits.read(new ByteArrayInputStream(bytes)));
            }
        }
        var value = school().put("displayName", "O'Brien <img src=x onerror=alert(1)>; SELECT * FROM data; --");
        assertEquals(value, limits.read(new ByteArrayInputStream(json.writeValueAsBytes(value))));
    }

    @Test void malformedNumbersNestingDuplicatesAndUtf8AreClientErrors() throws Exception {
        String good = json.writeValueAsString(school());
        for (String invalid : List.of(good.replace("\"catalogVersion\":9", "\"catalogVersion\":" + "9".repeat(400)),
                good.replace("\"catalogVersion\":9", "\"catalogVersion\":1e9999"),
                good.replace("\"catalogVersion\":9", "\"catalogVersion\":9,\"catalogVersion\":9"),
                good + " {}", "[".repeat(70) + "0" + "]".repeat(70))) {
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, rejected(invalid).status());
        }
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, assertThrows(WorkspaceProblem.class,
                () -> limits.read(new ByteArrayInputStream(new byte[]{(byte) 0xc3, 0x28}))).status());
    }

    @Test void everyCollectionStringAndObjectHasABoundEvenWithoutLessons() throws Exception {
        for (var bound : java.util.Map.of("subjects", 512, "teachers", 1024, "cohorts", 1024,
                "rooms", 512, "periods", 512, "roomAssignments", 4096, "softConstraintOverrides", 7).entrySet()) {
            var value = school(); value.putArray("lessons");
            var array = value.putArray(bound.getKey());
            for (int i = 0; i <= bound.getValue(); i++) array.addObject();
            var failure = assertThrows(WorkspaceProblem.class, () -> limits.check(value));
            assertTrue(failure.getMessage().contains("/" + bound.getKey()), failure.getMessage());
            assertTrue(failure.getMessage().contains("at most " + bound.getValue()), failure.getMessage());
        }
        var refs = school(); var ids = ((ObjectNode) refs.path("teachers").get(0)).putArray("qualifiedSubjectIds");
        for (int i = 0; i < 2049; i++) ids.add("subject-" + i);
        assertTrue(assertThrows(WorkspaceProblem.class, () -> limits.check(refs)).getMessage().contains("/teachers/0/qualifiedSubjectIds"));
        var longName = school().put("displayName", "x".repeat(513));
        assertTrue(assertThrows(WorkspaceProblem.class, () -> limits.check(longName)).getMessage().contains("/displayName"));
        var flood = school(); for (int i = 0; i < 33; i++) flood.put("unknown" + i, "x");
        assertTrue(assertThrows(WorkspaceProblem.class, () -> limits.check(flood)).getMessage().contains("32 fields"));
    }

    @Test void boundedFirstSchemaErrorAndInvalidHttpInputStartNoVerifierAndChangeNoWork() throws Exception {
        var verifier = mock(DemoVerificationService.class);
        var store = mock(WorkspaceStore.class);
        var original = new WorkspaceAggregate(WorkspaceState.EMPTY, 0, null, json.createObjectNode().put("sentinel", "unchanged"));
        when(store.load()).thenReturn(original);
        var mvc = MockMvcBuilders.standaloneSetup(new DemoTimetableController(limits, verifier))
                .setControllerAdvice(new ProblemHandler(new ProblemResponder(store, json))).build();
        var value = school(); var subjects = value.putArray("subjects");
        for (int i = 0; i < 512; i++) subjects.addObject().put("id", "s" + i);
        var failure = assertThrows(WorkspaceProblem.class, () -> limits.check(value));
        assertTrue(failure.getMessage().length() < 512);
        assertTrue(failure.getMessage().contains("/subjects/0"), failure.getMessage());
        String malformed = json.writeValueAsString(school()).replace("\"catalogVersion\":9", "\"catalogVersion\":" + "9".repeat(400));
        mvc.perform(post("/api/demo/definition/check").contentType("text/plain").content(malformed))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(verifier);
        verify(store, never()).replace(anyLong(), any(), any(), any());
        assertEquals("unchanged", original.document().path("sentinel").asString());
    }

    @Test void unexpectedHttpErrorsNeverExposeExceptionMessages() throws Exception {
        var store = mock(WorkspaceStore.class);
        var verifier = mock(DemoVerificationService.class);
        when(verifier.verify(any())).thenThrow(new IllegalStateException("SECRET password=hunter2 /private/customer/school.json"));
        var mvc = MockMvcBuilders.standaloneSetup(new DemoTimetableController(limits, verifier))
                .setControllerAdvice(new ProblemHandler(new ProblemResponder(store, json))).build();
        var response = mvc.perform(post("/api/demo/definition/check").contentType("text/plain").content(json.writeValueAsBytes(school())))
                .andExpect(status().isInternalServerError()).andReturn().getResponse();
        var body = json.readTree(response.getContentAsString());
        assertEquals("INTERNAL_ERROR", body.path("code").asString());
        assertTrue(body.path("message").asString().startsWith("An unexpected server error occurred."));
        assertDoesNotThrow(() -> java.util.UUID.fromString(body.path("correlationId").asString()));
        assertFalse(response.getContentAsString().contains("SECRET"));
        assertFalse(response.getContentAsString().contains("hunter2"));
    }

    @Test void admissionIsImmediateBeforeBodyReadingAndReleasesOnEveryExit() throws Exception {
        var store = mock(WorkspaceStore.class);
        var responder = new ProblemResponder(store, json);
        var filter = new DemoInputAdmissionFilter(responder);
        var arrived = new CountDownLatch(4); var release = new CountDownLatch(1);
        var session = new MockHttpSession();
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            var first = executor.submit(() -> hold(filter, session, arrived, release));
            var second = executor.submit(() -> hold(filter, session, arrived, release));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (arrived.getCount() != 2 && System.nanoTime() < deadline) Thread.onSpinWait();
            assertEquals(2, arrived.getCount());
            refused(filter, session);
            var third = executor.submit(() -> hold(filter, new MockHttpSession(), arrived, release));
            var fourth = executor.submit(() -> hold(filter, new MockHttpSession(), arrived, release));
            assertTrue(arrived.await(5, TimeUnit.SECONDS));
            refused(filter, new MockHttpSession());
            var reads = new AtomicInteger();
            var read = request(session); read.setMethod("GET");
            filter.doFilter(read, new MockHttpServletResponse(), (a, b) -> reads.incrementAndGet());
            var cancel = request(session); cancel.setMethod("DELETE"); cancel.setRequestURI("/api/runs/123");
            filter.doFilter(cancel, new MockHttpServletResponse(), (a, b) -> reads.incrementAndGet());
            assertEquals(2, reads.get());
            release.countDown(); first.get(); second.get(); third.get(); fourth.get();
            for (String outcome : List.of("success", "refused", "failed", "interrupted")) {
                var request = request(session);
                if (outcome.equals("success")) filter.doFilter(request, new MockHttpServletResponse(), (a,b) -> {});
                else assertThrows(Exception.class, () -> filter.doFilter(request, new MockHttpServletResponse(), (a,b) -> {
                    if (outcome.equals("refused")) throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY,"INVALID_INPUT","no");
                    throw new ServletException(outcome);
                }));
            }
            var allowed = new AtomicInteger();
            filter.doFilter(request(session), new MockHttpServletResponse(), (a,b) -> allowed.incrementAndGet());
            assertEquals(1, allowed.get());
            verifyNoInteractions(store);
        } finally { release.countDown(); executor.close(); }
    }
    void refused(DemoInputAdmissionFilter filter, MockHttpSession session) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(request(session), response, (a,b) -> fail("Rejected input reached decoding"));
        assertEquals(429, response.getStatus()); assertEquals("1", response.getHeader("Retry-After"));
        assertEquals("INPUT_BUSY", json.readTree(response.getContentAsString()).path("code").asString());
    }
    void hold(DemoInputAdmissionFilter filter, MockHttpSession session, CountDownLatch arrived, CountDownLatch release) {
        try { filter.doFilter(request(session), new MockHttpServletResponse(), (a,b) -> {
            arrived.countDown();
            try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ServletException(e); }
        }); } catch (Exception e) { throw new RuntimeException(e); }
    }
    MockHttpServletRequest request(MockHttpSession session) {
        var request = new MockHttpServletRequest("POST", "/api/demo/definition/check") {
            @Override public jakarta.servlet.ServletInputStream getInputStream() { throw new AssertionError("Body was read before admission"); }
        };
        request.setSession(session); return request;
    }
}
