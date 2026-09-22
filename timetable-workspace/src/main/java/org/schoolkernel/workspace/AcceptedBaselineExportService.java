package org.schoolkernel.workspace;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

@Service
public class AcceptedBaselineExportService {
    private final WorkspaceRepository repository;
    private final AcceptedBundleArchiver archiver;
    private final SafeImportReader reader;
    private final KernelVerifier verifier;
    private final ManifestService manifests;

    public AcceptedBaselineExportService(
            WorkspaceRepository repository,
            AcceptedBundleArchiver archiver,
            SafeImportReader reader,
            KernelVerifier verifier,
            ManifestService manifests) {
        this.repository = repository;
        this.archiver = archiver;
        this.reader = reader;
        this.verifier = verifier;
        this.manifests = manifests;
    }

    public ExportedBaseline export() {
        WorkspaceAggregate current = repository.load();
        if (current.state() != WorkspaceState.ACCEPTED_BASELINE) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "ACCEPTED_BASELINE_REQUIRED",
                    "Accept an initial proposal before exporting the current timetable.");
        }
        JsonNode baseline = current.document().path("acceptedBaseline");
        JsonNode definition = baseline.path("definition");
        JsonNode result = baseline.path("result");
        JsonNode manifest = baseline.path("manifest");
        byte[] archive;
        try {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            entries.put("school-definition.json", CanonicalJson.bytes(definition));
            entries.put("timetable-result.json", CanonicalJson.bytes(result));
            entries.put("workspace-manifest.json", CanonicalJson.bytes(manifest));
            archive = archiver.create(entries);
        } catch (IOException | IllegalStateException exception) {
            throw new WorkspaceProblem(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EXPORT_FAILED",
                    "Accepted-bundle export did not complete.");
        }

        ImportDocuments reproduced;
        try {
            reproduced = reader.readArchive(archive);
            KernelVerifier.Verification verification = verifier.verify(reproduced);
            JsonNode reproducedManifest = manifests.validatedOrGenerated(reproduced);
            if (!same(definition, reproduced.definition())
                    || !same(result, reproduced.result())
                    || !same(manifest, reproducedManifest)
                    || !verification.schoolId().equals(definition.path("schoolId").stringValue())
                    || !verification.definitionRevision().equals(result.path("inputRevision").stringValue())
                    || !verification.timetableRevision().equals(result.path("timetableRevision").stringValue())) {
                throw integrityFailure();
            }
        } catch (WorkspaceProblem problem) {
            if (problem.status().is5xxServerError()) {
                throw problem;
            }
            throw integrityFailure();
        } catch (RuntimeException exception) {
            throw integrityFailure();
        }
        return new ExportedBaseline(
                archive,
                current.etag(),
                definition.path("schoolId").stringValue(),
                result.path("inputRevision").stringValue(),
                result.path("timetableRevision").stringValue());
    }

    private static boolean same(JsonNode expected, JsonNode actual) {
        return Arrays.equals(CanonicalJson.bytes(expected), CanonicalJson.bytes(actual));
    }

    private static WorkspaceProblem integrityFailure() {
        return new WorkspaceProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "EXPORT_VERIFICATION_FAILED",
                "The accepted-bundle archive failed local integrity verification.");
    }

    public record ExportedBaseline(
            byte[] archive,
            String etag,
            String schoolId,
            String inputRevision,
            String timetableRevision) {}
}
