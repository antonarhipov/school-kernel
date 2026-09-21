package org.schoolkernel.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class ImportService {
    private final WorkspaceRepository repository;
    private final SafeImportReader reader;
    private final KernelVerifier verifier;
    private final ManifestService manifests;
    private final WorkspaceMutation mutation;
    private final ObjectMapper json;

    public ImportService(
            WorkspaceRepository repository,
            SafeImportReader reader,
            KernelVerifier verifier,
            ManifestService manifests,
            WorkspaceMutation mutation,
            ObjectMapper json) {
        this.repository = repository;
        this.reader = reader;
        this.verifier = verifier;
        this.manifests = manifests;
        this.mutation = mutation;
        this.json = json;
    }

    public WorkspaceAggregate importSchool(
            String ifMatch,
            MultipartFile definition,
            MultipartFile result,
            MultipartFile archive) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.EMPTY) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "INVALID_WORKSPACE_TRANSITION",
                    "School data can be imported only into an empty workspace.");
        }
        ImportDocuments documents = reader.read(definition, result, archive);
        KernelVerifier.Verification verification = verifier.verify(documents);
        var manifest = manifests.validatedOrGenerated(documents);
        ObjectNode workspace = json.createObjectNode();
        ObjectNode school = workspace.putObject("school");
        school.put("id", verification.schoolId());
        school.put("displayName", documents.definition().path("displayName").stringValue());
        workspace.put("importMode", documents.mode().name());
        workspace.put("definitionRevision", verification.definitionRevision());
        WorkspaceState nextState;
        if (documents.mode() == ImportDocuments.ImportMode.INITIAL_DEFINITION) {
            nextState = WorkspaceState.INITIAL_DRAFT;
            workspace.set("initialDefinition", documents.definition());
        } else {
            nextState = WorkspaceState.ACCEPTED_BASELINE;
            workspace.put("timetableRevision", verification.timetableRevision());
            ObjectNode accepted = workspace.putObject("acceptedBaseline");
            accepted.set("definition", documents.definition());
            accepted.set("result", documents.result());
            accepted.set("manifest", manifest);
        }
        return mutation.importVerified(expectedVersion, nextState, workspace);
    }

    static long requireMatchingVersion(String ifMatch, WorkspaceAggregate current) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new WorkspaceProblem(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED",
                    "Reload the workspace and retry with its current version.");
        }
        if (!current.etag().equals(ifMatch)) {
            throw new WorkspaceProblem(
                    HttpStatus.PRECONDITION_FAILED,
                    "STALE_WORKSPACE_VERSION",
                    "The workspace changed. Reload it before importing.");
        }
        return current.version();
    }
}
