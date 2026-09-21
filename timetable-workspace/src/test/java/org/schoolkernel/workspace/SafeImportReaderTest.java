package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class SafeImportReaderTest {
    private final SafeImportReader reader = new SafeImportReader();

    @Test
    @DisplayName("UC-1 main and G1: definition-only import is explicitly an initial draft")
    void definitionOnlyMode() {
        var documents = reader.read(json("definition", definition()), null, null);

        assertEquals(ImportDocuments.ImportMode.INITIAL_DEFINITION, documents.mode());
        assertEquals("school", documents.definition().path("schoolId").stringValue());
        assertNull(documents.result());
    }

    @Test
    @DisplayName("UC-1 extension 1a: a result by itself requires its matching definition")
    void resultAloneIsRejected() {
        WorkspaceProblem problem = assertThrows(
                WorkspaceProblem.class,
                () -> reader.read(null, json("result", "{}"), null));

        assertEquals("MATCHING_DEFINITION_REQUIRED", problem.code());
    }

    @Test
    @DisplayName("UC-1 extension 1b and RULE-14: exact archive entries are read without extraction")
    void exactArchive() throws Exception {
        byte[] archive = zip(Map.of(
                "school-definition.json", definition(),
                "timetable-result.json", "{\"status\":\"FEASIBLE\"}",
                "workspace-manifest.json", "{\"manifestVersion\":1}"));

        var documents = reader.read(null, null, new MockMultipartFile(
                "archive", "accepted.zip", "application/zip", archive));

        assertEquals(ImportDocuments.ImportMode.ACCEPTED_BASELINE, documents.mode());
        assertEquals(1, documents.manifest().path("manifestVersion").intValue());
    }

    @Test
    @DisplayName("UC-1 extension 1b: extra and traversal archive entries are rejected")
    void unsafeArchive() throws Exception {
        byte[] archive = zip(Map.of(
                "school-definition.json", definition(),
                "timetable-result.json", "{}",
                "workspace-manifest.json", "{}",
                "../secret.json", "{}"));

        WorkspaceProblem problem = assertThrows(WorkspaceProblem.class, () -> reader.read(null, null,
                new MockMultipartFile("archive", "unsafe.zip", "application/zip", archive)));

        assertEquals("UNSAFE_ARCHIVE", problem.code());
    }

    @Test
    @DisplayName("UC-1 extension 1b and RULE-14: missing, duplicate, directory, unreadable and truncated archives are rejected")
    void completeMalformedArchiveMatrix() throws Exception {
        assertArchiveCode(zip(Map.of(
                "school-definition.json", definition(),
                "timetable-result.json", "{}")), "INCOMPLETE_ARCHIVE");
        assertArchiveCode(rawZip(List.of(
                entry("school-definition.json", definition()),
                entry("school-definition.json", definition()),
                entry("timetable-result.json", "{}"),
                entry("workspace-manifest.json", "{}"))), "UNSAFE_ARCHIVE");
        assertArchiveCode(zip(List.of(
                entry("school-definition.json", definition()),
                entry("timetable-result.json", "{}"),
                entry("workspace-manifest.json", "{}"),
                entry("folder/", ""))), "UNSAFE_ARCHIVE");
        assertArchiveCode("not a zip".getBytes(StandardCharsets.UTF_8), "UNREADABLE_ARCHIVE");

        byte[] valid = zip(Map.of(
                "school-definition.json", definition(),
                "timetable-result.json", "{}",
                "workspace-manifest.json", "{}"));
        assertArchiveCode(Arrays.copyOf(valid, valid.length / 2), "UNREADABLE_ARCHIVE", "INCOMPLETE_ARCHIVE");
    }

    @Test
    @DisplayName("RULE-14: JSON import bounds are enforced below, at, and above the exact byte limit")
    void exactJsonByteBoundaries() {
        assertDoesNotThrow(() -> reader.read(validStreamingFile(SafeImportReader.JSON_LIMIT - 1), null, null));
        assertDoesNotThrow(() -> reader.read(validStreamingFile(SafeImportReader.JSON_LIMIT), null, null));
        assertEquals("IMPORT_TOO_LARGE", readDefinitionProblem(SafeImportReader.JSON_LIMIT + 1).code());
    }

    @Test
    @DisplayName("RULE-14: stream growth beyond the declared length is still bounded")
    void dishonestLengthCannotBypassStreamBound() {
        MockMultipartFile dishonest = streamingFile(SafeImportReader.JSON_LIMIT + 1, 1);

        WorkspaceProblem problem = assertThrows(
                WorkspaceProblem.class,
                () -> reader.read(dishonest, null, null));

        assertEquals("IMPORT_TOO_LARGE", problem.code());
    }

    @Test
    @DisplayName("RULE-14: duplicate JSON keys and trailing tokens are rejected deterministically")
    void strictJsonParsing() {
        for (String invalid : List.of(
                "{\"schoolId\":\"one\",\"schoolId\":\"two\"}",
                "{} {}")) {
            WorkspaceProblem problem = assertThrows(
                    WorkspaceProblem.class,
                    () -> reader.read(json("definition", invalid), null, null));
            assertEquals("MALFORMED_IMPORT", problem.code());
        }
    }

    @Test
    @DisplayName("RULE-14: parsing rejects an over-limit document before reading it")
    void oversizedDocument() {
        MockMultipartFile oversized = new MockMultipartFile("definition", "definition.json", "application/json", new byte[0]) {
            @Override
            public long getSize() {
                return SafeImportReader.JSON_LIMIT + 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };

        WorkspaceProblem problem = assertThrows(
                WorkspaceProblem.class,
                () -> reader.read(oversized, null, null));

        assertEquals("IMPORT_TOO_LARGE", problem.code());
    }

    private static MockMultipartFile json(String field, String content) {
        return new MockMultipartFile(field, field + ".json", "application/json", content.getBytes());
    }

    private static String definition() {
        return "{\"schemaVersion\":1,\"catalogVersion\":1,\"schoolId\":\"school\",\"displayName\":\"School\"}";
    }

    private static byte[] zip(Map<String, String> entries) throws Exception {
        List<ArchiveEntry> ordered = entries.entrySet().stream()
                .map(entry -> entry(entry.getKey(), entry.getValue()))
                .toList();
        return zip(ordered);
    }

    private static byte[] zip(List<ArchiveEntry> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (ArchiveEntry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name()));
                zip.write(entry.value().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private void assertArchiveCode(byte[] archive, String... expectedCodes) {
        WorkspaceProblem problem = assertThrows(WorkspaceProblem.class, () -> reader.read(null, null,
                new MockMultipartFile("archive", "accepted.zip", "application/zip", archive)));
        assertTrue(List.of(expectedCodes).contains(problem.code()), problem.code());
    }

    private WorkspaceProblem readDefinitionProblem(long bytes) {
        return assertThrows(WorkspaceProblem.class, () -> reader.read(streamingFile(bytes, bytes), null, null));
    }

    private static MockMultipartFile validStreamingFile(long totalBytes) {
        MockMultipartFile spaces = streamingFile(totalBytes - 2, totalBytes);
        return new MockMultipartFile("definition", "definition.json", "application/json", new byte[] {1}) {
            @Override public long getSize() { return totalBytes; }
            @Override public boolean isEmpty() { return false; }
            @Override public InputStream getInputStream() throws IOException {
                return new SequenceInputStream(
                        new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)),
                        spaces.getInputStream());
            }
        };
    }

    private static MockMultipartFile streamingFile(long actualBytes, long declaredBytes) {
        return new MockMultipartFile("definition", "definition.json", "application/json", new byte[] {1}) {
            @Override public long getSize() { return declaredBytes; }
            @Override public boolean isEmpty() { return false; }
            @Override public InputStream getInputStream() {
                return new InputStream() {
                    long remaining = actualBytes;
                    @Override public int read() { return remaining-- > 0 ? ' ' : -1; }
                    @Override public int read(byte[] target, int offset, int length) {
                        if (remaining <= 0) return -1;
                        int count = (int) Math.min(remaining, length);
                        Arrays.fill(target, offset, offset + count, (byte) ' ');
                        remaining -= count;
                        return count;
                    }
                };
            }
        };
    }

    private static ArchiveEntry entry(String name, String value) {
        return new ArchiveEntry(name, value);
    }

    private static byte[] rawZip(List<ArchiveEntry> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (ArchiveEntry entry : entries) {
            byte[] name = entry.name().getBytes(StandardCharsets.UTF_8);
            byte[] value = entry.value().getBytes(StandardCharsets.UTF_8);
            CRC32 crc = new CRC32();
            crc.update(value);
            little(bytes, 0x04034b50, 4);
            little(bytes, 20, 2);
            little(bytes, 0, 2);
            little(bytes, 0, 2);
            little(bytes, 0, 2);
            little(bytes, 0, 2);
            little(bytes, crc.getValue(), 4);
            little(bytes, value.length, 4);
            little(bytes, value.length, 4);
            little(bytes, name.length, 2);
            little(bytes, 0, 2);
            bytes.write(name);
            bytes.write(value);
        }
        return bytes.toByteArray();
    }

    private static void little(ByteArrayOutputStream output, long value, int bytes) {
        for (int index = 0; index < bytes; index++) {
            output.write((int) (value >>> (8 * index)) & 0xff);
        }
    }

    private record ArchiveEntry(String name, String value) {}
}
