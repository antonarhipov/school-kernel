package org.schoolkernel.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;

class FileBoundaryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("RULE-28: bounded reads accept below and at the byte limit and refuse above")
    void byteBoundary() throws Exception {
        FileBoundary boundary = new FileBoundary();
        Path below = write("below", 9);
        Path at = write("at", 10);
        Path above = write("above", 11);
        assertEquals(9, boundary.read(below, 10).length);
        assertEquals(10, boundary.read(at, 10).length);
        assertThrows(TransportException.class, () -> boundary.read(above, 10));
    }

    @Test
    @DisplayName("RULE-28: concurrent unforced publishers have exactly one atomic winner")
    void concurrentCreateHasOneWinner() throws Exception {
        FileBoundary boundary = new FileBoundary();
        Path destination = temporaryDirectory.resolve("winner.json");
        var first = JsonSupport.mapper().createObjectNode().put("publisher", "first");
        var second = JsonSupport.mapper().createObjectNode().put("publisher", "second");
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var attempts = List.of(first, second).stream()
                    .map(document -> executor.submit(() -> {
                        barrier.await();
                        try {
                            boundary.publish(document, destination, false);
                            return true;
                        } catch (TransportException exception) {
                            return false;
                        }
                    }))
                    .toList();
            int winners = 0;
            for (var attempt : attempts) {
                if (attempt.get()) {
                    winners++;
                }
            }
            assertEquals(1, winners);
        }
        String publisher = JsonSupport.mapper().readTree(destination).path("publisher").stringValue();
        assertTrue(publisher.equals("first") || publisher.equals("second"));
    }

    @Test
    @DisplayName("RULE-28: forced publication atomically replaces with the last completed document")
    void forcedReplacementUsesLastCompletedDocument() throws Exception {
        FileBoundary boundary = new FileBoundary();
        Path destination = temporaryDirectory.resolve("forced.json");
        boundary.publish(JsonSupport.mapper().createObjectNode().put("publisher", "first"), destination, true);
        boundary.publish(JsonSupport.mapper().createObjectNode().put("publisher", "last"), destination, true);
        assertArrayEquals(
                JsonSupport.canonicalBytes(JsonSupport.mapper().createObjectNode().put("publisher", "last")),
                Files.readAllBytes(destination));
    }

    @Test
    @DisplayName("RULE-28: unsupported hard-link capability fails without publishing or leaking a temporary result")
    void unsupportedLinkCapabilityFailsCleanly() throws Exception {
        Path archive = temporaryDirectory.resolve("filesystem.zip");
        try (var filesystem = FileSystems.newFileSystem(
                URI.create("jar:" + archive.toUri()), Map.of("create", "true"))) {
            Path destination = filesystem.getPath("/result.json");
            FileBoundary boundary = new FileBoundary();

            assertThrows(TransportException.class, () -> boundary.publish(
                    JsonSupport.mapper().createObjectNode().put("status", "complete"), destination, false));

            assertTrue(Files.notExists(destination));
            try (var entries = Files.list(destination.getParent())) {
                assertEquals(0, entries.count());
            }
        }
    }

    @Test
    @DisplayName("RULE-28: directory, same-file and missing-parent collisions preserve existing bytes")
    void publicationCollisionMatrix() throws Exception {
        FileBoundary boundary = new FileBoundary();
        Path input = temporaryDirectory.resolve("input.json");
        Files.writeString(input, "input");
        assertThrows(TransportException.class, () -> boundary.requireDistinct(input, input));

        Path directoryDestination = temporaryDirectory.resolve("directory");
        Files.createDirectory(directoryDestination);
        assertThrows(TransportException.class, () -> boundary.prepareDestination(directoryDestination, true));

        Path missingParent = temporaryDirectory.resolve("missing/result.json");
        assertThrows(TransportException.class, () -> boundary.prepareDestination(missingParent, false));
        assertArrayEquals("input".getBytes(), Files.readAllBytes(input));
    }

    private Path write(String name, int size) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        Files.write(path, new byte[size]);
        return path;
    }
}
