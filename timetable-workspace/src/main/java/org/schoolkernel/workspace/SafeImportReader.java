package org.schoolkernel.workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SafeImportReader {
    static final long JSON_LIMIT = 10L * 1024L * 1024L;
    static final long MANIFEST_LIMIT = 1L * 1024L * 1024L;
    static final long ARCHIVE_LIMIT = 25L * 1024L * 1024L;
    static final long UNCOMPRESSED_LIMIT = 21L * 1024L * 1024L;
    private static final Set<String> ARCHIVE_ENTRIES = Set.of(
            "school-definition.json", "timetable-result.json", "workspace-manifest.json");
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public ImportDocuments read(MultipartFile definition, MultipartFile result, MultipartFile archive) {
        boolean hasDefinition = present(definition);
        boolean hasResult = present(result);
        boolean hasArchive = present(archive);
        if (hasArchive && (hasDefinition || hasResult)) {
            throw invalid("IMPORT_MODE_AMBIGUOUS", "Choose an archive or JSON documents, not both.");
        }
        if (hasArchive) {
            return readArchive(archive);
        }
        if (!hasDefinition && hasResult) {
            throw invalid("MATCHING_DEFINITION_REQUIRED", "The matching complete school definition is required.");
        }
        if (!hasDefinition) {
            throw invalid("IMPORT_CONTENT_REQUIRED", "Choose a school definition or accepted-bundle archive.");
        }
        JsonNode definitionNode = parse(readBounded(definition, JSON_LIMIT, "definition"), "definition");
        if (!hasResult) {
            return new ImportDocuments(
                    definitionNode, null, null, ImportDocuments.ImportMode.INITIAL_DEFINITION);
        }
        JsonNode resultNode = parse(readBounded(result, JSON_LIMIT, "result"), "result");
        return new ImportDocuments(
                definitionNode, resultNode, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE);
    }

    private ImportDocuments readArchive(MultipartFile archive) {
        if (archive.getSize() > ARCHIVE_LIMIT) {
            throw tooLarge("Accepted-bundle archive exceeds 25 MiB.");
        }
        byte[] bytes = readBounded(archive, ARCHIVE_LIMIT, "archive");
        Map<String, byte[]> entries = new HashMap<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !ARCHIVE_ENTRIES.contains(name)) {
                    throw invalid("UNSAFE_ARCHIVE", "The archive must contain only the three accepted-bundle files.");
                }
                if (entries.containsKey(name)) {
                    throw invalid("UNSAFE_ARCHIVE", "The archive contains a duplicate entry.");
                }
                long entryLimit = name.equals("workspace-manifest.json") ? MANIFEST_LIMIT : JSON_LIMIT;
                byte[] content = readBounded(zip, entryLimit, name);
                total += content.length;
                if (total > UNCOMPRESSED_LIMIT) {
                    throw tooLarge("Accepted-bundle content exceeds 21 MiB.");
                }
                entries.put(name, content);
                zip.closeEntry();
                if (entries.size() > ARCHIVE_ENTRIES.size()) {
                    throw invalid("UNSAFE_ARCHIVE", "The archive contains extra entries.");
                }
            }
        } catch (WorkspaceProblem problem) {
            throw problem;
        } catch (ZipException exception) {
            throw invalid("UNREADABLE_ARCHIVE", "The accepted-bundle archive is unreadable or encrypted.");
        } catch (IOException exception) {
            throw invalid("UNREADABLE_ARCHIVE", "The accepted-bundle archive could not be read.");
        }
        if (!entries.keySet().equals(ARCHIVE_ENTRIES)) {
            throw invalid("INCOMPLETE_ARCHIVE", "The archive must contain all three accepted-bundle files.");
        }
        return new ImportDocuments(
                parse(entries.get("school-definition.json"), "school definition"),
                parse(entries.get("timetable-result.json"), "timetable result"),
                parse(entries.get("workspace-manifest.json"), "workspace manifest"),
                ImportDocuments.ImportMode.ACCEPTED_BASELINE);
    }

    private static boolean present(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static byte[] readBounded(MultipartFile file, long limit, String document) {
        if (file.getSize() > limit) {
            throw tooLarge(document + " exceeds its import size limit.");
        }
        try (InputStream input = file.getInputStream()) {
            return readBounded(input, limit, document);
        } catch (IOException exception) {
            throw invalid("UNREADABLE_IMPORT", "The " + document + " could not be read.");
        }
    }

    private static byte[] readBounded(InputStream input, long limit, String document) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > limit) {
                throw tooLarge(document + " exceeds its import size limit.");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static JsonNode parse(byte[] bytes, String document) {
        try {
            return JSON.readTree(bytes);
        } catch (JacksonException exception) {
            throw invalid("MALFORMED_IMPORT", "The " + document + " is not valid JSON.");
        }
    }

    private static WorkspaceProblem invalid(String code, String message) {
        return new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    private static WorkspaceProblem tooLarge(String message) {
        return new WorkspaceProblem(HttpStatus.PAYLOAD_TOO_LARGE, "IMPORT_TOO_LARGE", message);
    }
}
