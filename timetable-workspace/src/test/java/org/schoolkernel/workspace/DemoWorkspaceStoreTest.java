package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.json.JsonMapper;

class DemoWorkspaceStoreTest {
    @Test void expiryCapacityPollingAndRestartNeverRecoverLostWork() {
        var clock = new MutableClock();
        var settings = new DemoSettings(List.of("demo.example"), List.of(), false, Duration.ofMinutes(60), Duration.ofHours(8), 2);
        var json = JsonMapper.builder().build();
        var factory = new StaticListableBeanFactory();
        var registry = new DemoWorkspaces(settings, json, factory.getBeanProvider(KernelPlanner.class), clock);
        try {
            var session = new MockHttpSession();
            var owner = registry.open(session, true);
            registry.open(new MockHttpSession(), true);
            assertEquals("DEMO_FULL", assertThrows(WorkspaceProblem.class,
                    () -> registry.open(new MockHttpSession(), true)).code());
            var store = new DemoWorkspaceStore(registry);
            registry.bound(owner, () -> {
                var document = json.createObjectNode().put("secret", "one");
                assertEquals(1, store.replace(0, WorkspaceState.EMPTY, WorkspaceState.INITIAL_DRAFT, document).orElseThrow());
                document.put("secret", "changed caller document");
                assertEquals("one", store.load().document().path("secret").stringValue());
                ((tools.jackson.databind.node.ObjectNode) store.load().document()).put("secret", "changed returned copy");
                assertEquals("one", store.load().document().path("secret").stringValue());
            });
            for (int hour = 1; hour <= 7; hour++) {
                clock.advance(Duration.ofMinutes(59));
                registry.open(session, false); // Polling refreshes idle activity.
            }
            assertEquals(1, registry.count());
            clock.advance(Duration.ofMinutes(59));
            registry.open(session, false);
            clock.advance(Duration.ofMinutes(8));
            assertEquals("SESSION_ENDED", assertThrows(WorkspaceProblem.class, () -> registry.open(session, false)).code());
            assertEquals(0, registry.count());
            registry.bound(owner, () -> assertThrows(WorkspaceProblem.class, store::load));
            registry.restart(session);
            registry.bound(registry.open(session, false), () -> {
                assertEquals(WorkspaceState.EMPTY, store.load().state());
                assertTrue(store.load().document().isEmpty());
            });
            clock.advance(Duration.ofMinutes(60));
            assertThrows(WorkspaceProblem.class, () -> registry.open(session, false));
        } finally { registry.close(); }
    }

    @Test void backgroundCompletionNeedsItsCapturedOwnerAndExactRunAndVersion() throws Exception {
        var clock = new MutableClock();
        var factory = new StaticListableBeanFactory();
        var json = JsonMapper.builder().build();
        var registry = new DemoWorkspaces(new DemoSettings(List.of("demo.example"), List.of(), false,
                Duration.ofHours(1), Duration.ofHours(8), 50), json, factory.getBeanProvider(KernelPlanner.class), clock);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var store = new DemoWorkspaceStore(registry);
            var first = registry.open(new MockHttpSession(), true);
            var second = registry.open(new MockHttpSession(), true);
            var id = UUID.randomUUID();
            Runnable[] captured = new Runnable[1];
            registry.bound(first, () -> {
                var document = json.createObjectNode().put("private", "first");
                store.replace(0, WorkspaceState.EMPTY, WorkspaceState.INITIAL_DRAFT, document);
                store.startRun(1, id, document);
                assertThrows(WorkspaceProblem.class, () -> store.clear(2));
                assertTrue(store.finishRun(2, UUID.randomUUID(), WorkspaceState.INITIAL_PROPOSAL, document).isEmpty());
                captured[0] = store.contextual(() -> {
                    assertEquals("first", store.load().document().path("private").stringValue());
                    assertTrue(store.finishRun(2, id, WorkspaceState.INITIAL_PROPOSAL, document).isPresent());
                    assertTrue(store.finishRun(2, id, WorkspaceState.INITIAL_PROPOSAL, document).isEmpty());
                });
            });
            executor.submit(captured[0]).get();
            registry.bound(second, () -> assertEquals(WorkspaceState.EMPTY, store.load().state()));
            assertThrows(WorkspaceProblem.class, store::load);
            registry.bound(first, () -> {
                assertTrue(store.clear(2).isEmpty());
                assertEquals(WorkspaceState.INITIAL_PROPOSAL, store.load().state());
                assertTrue(store.clear(3).isPresent());
                assertEquals(WorkspaceState.EMPTY, store.load().state());
                assertThrows(WorkspaceProblem.class,
                        () -> store.replace(4, WorkspaceState.EMPTY, WorkspaceState.DEFINITION_DRAFT, json.createObjectNode()));
            });
        } finally { registry.close(); }
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-16T00:00:00Z");
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
