package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.util.Map;
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
        return "{\"schemaVersion\":1,\"catalogVersion\":1,\"schoolId\":\"school\"}";
    }

    private static byte[] zip(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
