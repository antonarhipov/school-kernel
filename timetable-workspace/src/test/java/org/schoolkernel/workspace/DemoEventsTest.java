package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DemoEventsTest {
    static final JsonMapper JSON = JsonMapper.builder().build();
    @SuppressWarnings("unchecked") static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> result = mock(ObjectProvider.class); when(result.getObject()).thenReturn(value); return result;
    }
    DemoEvents events(List<String> lines, DemoKernelAllowance allowance) {
        var result = new DemoEvents(JSON, new MockEnvironment(), lines::add);
        var owners = mock(DemoWorkspaces.class); when(owners.count()).thenReturn(2);
        var planning = mock(DemoPlanningService.class); when(planning.waitingCount()).thenReturn(1);
        var cache = mock(DemoArtifactCache.class); when(cache.occupancy()).thenReturn(new DemoArtifactCache.Occupancy(3, 4096));
        ReflectionTestUtils.setField(result, "owners", provider(owners));
        ReflectionTestUtils.setField(result, "planning", provider(planning));
        ReflectionTestUtils.setField(result, "artifacts", provider(cache));
        ReflectionTestUtils.setField(result, "allowance", provider(allowance)); return result;
    }
    @Test void snapshotsSeparateLogicalReuseFromPhysicalConsumptionAndResetIntervals() {
        var lines = new ArrayList<String>(); var budget = new DemoKernelAllowance(Duration.ofSeconds(100), Clock.systemUTC());
        var events = events(lines, budget);
        try {
            String computation = UUID.randomUUID().toString();
            var span = events.process(DemoEvents.ProcessKind.SOLVE, UUID.randomUUID().toString(), computation);
            events.summary(); JsonNode live = JSON.readTree(lines.getLast());
            assertEquals(1, live.path("processes").path("runningSolve").asInt());
            span.stopped(); span.end("EXIT_0"); span.end("FAILED");
            var artifact = new DemoKernelRunner.Artifact(null, null, 6000, 1000, computation);
            for (int wait : List.of(0, 21, 92)) {
                events.reuse(true, false, computation, true);
                events.logical(UUID.randomUUID().toString(), computation, "INITIAL", "QUICK", "FEASIBLE", wait, wait + 1, artifact, true);
            }
            events.summary(); JsonNode summary = JSON.readTree(lines.getLast());
            assertEquals(3, summary.path("cache").path("HIT").asInt());
            assertEquals(1, summary.path("processes").path("completed.SOLVE.EXIT_0").asInt());
            assertFalse(summary.path("processes").has("completed.SOLVE.FAILED"));
            assertEquals(0, summary.path("processes").path("runningSolve").asInt());
            assertEquals(30, summary.path("queue").path("waitP50UpperMs").asInt());
            assertEquals(100, summary.path("queue").path("waitP90UpperMs").asInt());
            assertEquals(0, summary.path("allowance").path("consumedMs").asLong());
            assertEquals(Set.of("family", "event", "timestamp", "context", "workspaces", "queue", "processes", "cache", "allowance", "productUse"), summary.properties().stream().map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()));
            events.summary(); JsonNode empty = JSON.readTree(lines.getLast()); assertFalse(empty.path("cache").has("HIT"));
            assertEquals(0, empty.path("queue").path("waitP90UpperMs").asInt());
        } finally { events.close(); }
    }
    @Test void rolloverCarriesReservationsAndSettlementsAreIdempotent() {
        var clock = new DemoCustomDefinitionTest.MutableClock(Instant.parse("2026-10-15T23:59:58Z"));
        var budget = new DemoKernelAllowance(Duration.ofSeconds(100), clock); var lines = new ArrayList<String>(); var events = events(lines, budget);
        try {
            var reservation = budget.reserve(Duration.ofSeconds(40)); reservation.started(clock.instant()); clock.advance(5);
            events.summary(); JsonNode summary = JSON.readTree(lines.getLast());
            assertEquals("2026-10-16", summary.path("context").path("utcAllowanceDay").asString());
            assertEquals(1, summary.path("allowance").path("utcResets").asInt());
            assertEquals(3000, summary.path("allowance").path("consumedMs").asInt());
            assertEquals(35000, summary.path("allowance").path("reservedMs").asInt());
            reservation.stopped(); reservation.close(); reservation.close(); events.summary(); summary = JSON.readTree(lines.getLast());
            assertEquals(0, summary.path("allowance").path("utcResets").asInt());
            assertEquals(1, summary.path("allowance").path("settlements").asInt());
            assertEquals(35000, summary.path("allowance").path("releasedReservationMs").asInt());
            assertEquals(0, summary.path("allowance").path("reservedMs").asInt());
        } finally { events.close(); }
    }
    @Test void shutdownAccountsForRunningCoalescedAndQueuedLogicalRequestsExactlyOnce() throws Exception {
        var lines = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var events = new DemoEvents(JSON, new MockEnvironment(), lines::add);
        try (var f = new DemoPlanningTest().new Fixture()) {
            ReflectionTestUtils.setField(f.service, "events", events);
            var first = f.owner(""); var second = f.owner(""); var third = f.owner("PRIVATE_SHUTDOWN_NAME");
            f.start(first, "QUICK"); assertTrue(f.started.await(3, java.util.concurrent.TimeUnit.SECONDS));
            f.start(second, "QUICK"); f.start(third, "QUICK"); f.service.close(); f.service.close();
            var outcomes = lines.stream().map(JSON::readTree).filter(e -> e.path("event").asString().equals("logical_finished")).toList();
            assertEquals(3, outcomes.size()); assertTrue(outcomes.stream().allMatch(e -> e.path("outcome").asString().equals("CANCELLED")));
            assertEquals(0, f.allowance.snapshot().reservedNanos());
            assertFalse(String.join("\n", lines).contains("PRIVATE_SHUTDOWN_NAME"));
            for (var owner : List.of(first, second, third)) assertFalse(String.join("\n", lines).contains(owner.key.toString()));
        } finally { events.close(); }
    }
    @Test void payloadProjectionAndClosedVocabularyCannotEmitPrivateStrings() {
        var lines = new ArrayList<String>(); var events = new DemoEvents(JSON, new MockEnvironment(), lines::add);
        try {
            String secret = "PRIVATE_NAME_cookie_session_IP_address_label";
            var definition = JSON.createObjectNode().put("schoolId", secret).put("displayName", secret);
            definition.putArray("lessons").addObject().put("teacherId", secret);
            definition.putArray("rooms").addObject().put("displayName", secret); definition.putArray("periods").addObject();
            events.product(DemoEvents.Product.DEFINITION_CUSTOM, secret, definition);
            events.refusal(secret); events.logical(secret, secret, secret, secret, secret, 1, 2, null, false);
            assertFalse(String.join("\n", lines).contains(secret));
            for (String line : lines) assertFalse(line.contains("\n"));
            assertEquals("OTHER_REFUSAL", JSON.readTree(lines.get(1)).path("reason").asString());
            assertFalse(JSON.readTree(lines.get(2)).has("correlationId"));
        } finally { events.close(); }
    }
}
