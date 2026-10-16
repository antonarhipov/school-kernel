package org.schoolkernel.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class ImportService {
    @org.springframework.beans.factory.annotation.Autowired(required = false) private DemoEvents events;
    private final WorkspaceStore repository;
    private final SafeImportReader reader;
    private final KernelVerifier verifier;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DemoVerificationService demoVerification;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DemoDefinitionLimits demoLimits;
    private final ManifestService manifests;
    private final WorkspaceMutation mutation;
    private final ObjectMapper json;

    public ImportService(
            WorkspaceStore repository,
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
        DefinitionDraftService.requireNotAuthoring(current);
        if (current.state() != WorkspaceState.EMPTY) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "INVALID_WORKSPACE_TRANSITION",
                    "School data can be imported only into an empty workspace.");
        }
        if (repository.ephemeral() && current.activeRunId() != null) throw new WorkspaceProblem(HttpStatus.CONFLICT,
                "INVALID_WORKSPACE_TRANSITION", "Wait for or cancel the active run before importing. Existing work is unchanged.");
        ImportDocuments documents = reader.read(definition, result, archive);
        requireDemoBundle(documents);
        KernelVerifier.Verification verification = verify(documents);
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
        var installed = mutation.importVerified(expectedVersion, nextState, workspace);
        if (events != null) events.product(DemoEvents.Product.IMPORT);
        return installed;
    }

    public WorkspaceAggregate clearWorkspace(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = requireMatchingVersion(ifMatch, current);
        DefinitionDraftService.requireNotAuthoring(current);
        return mutation.clear(expectedVersion);
    }

    public WorkspaceAggregate resetAndUploadDefinition(
            String ifMatch,
            MultipartFile definition,
            MultipartFile result,
            MultipartFile archive) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = requireMatchingVersion(ifMatch, current);
        DefinitionDraftService.requireNotAuthoring(current);
        if (repository.ephemeral() && current.activeRunId() != null) throw new WorkspaceProblem(HttpStatus.CONFLICT,
                "INVALID_WORKSPACE_TRANSITION", "Wait for or cancel the active run before importing. Existing work is unchanged.");
        ImportDocuments documents = reader.read(definition, result, archive);
        requireDemoBundle(documents);
        KernelVerifier.Verification verification = verify(documents);
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
        var installed = mutation.resetAndImport(expectedVersion, nextState, workspace);
        if (events != null) events.product(DemoEvents.Product.IMPORT);
        return installed;
    }

    public WorkspaceAggregate replaceInitial(String ifMatch, MultipartFile definition) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = requireMatchingVersion(ifMatch, current);
        DefinitionDraftService.requireNotAuthoring(current);
        if (current.state() != WorkspaceState.INITIAL_DRAFT) {
            throw new WorkspaceProblem(
                    HttpStatus.CONFLICT,
                    "INVALID_WORKSPACE_TRANSITION",
                    "The initial definition can be replaced only before planning.");
        }
        ImportDocuments documents = reader.read(definition, null, null);
        requireDemoBundle(documents);
        KernelVerifier.Verification verification = verify(documents);
        ObjectNode workspace = json.createObjectNode();
        ObjectNode school = workspace.putObject("school");
        school.put("id", verification.schoolId());
        school.put("displayName", documents.definition().path("displayName").stringValue());
        workspace.put("importMode", ImportDocuments.ImportMode.INITIAL_DEFINITION.name());
        workspace.put("definitionRevision", verification.definitionRevision());
        workspace.set("initialDefinition", documents.definition());
        return mutation.replaceInitial(
                expectedVersion, WorkspaceState.INITIAL_DRAFT, workspace);
    }

    private KernelVerifier.Verification verify(ImportDocuments documents) {
        if (demoVerification == null) return verifier.verify(documents);
        demoLimits.check(documents.definition(), false);
        if (documents.result() == null || !"FEASIBLE".equals(documents.result().path("status").asString()))
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "ACCEPTED_BUNDLE_REQUIRED", "A complete matching feasible result is required. Nothing was imported.");
        manifests.validatedOrGenerated(documents); // Metadata is independently checked even on an exact verification hit.
        return demoVerification.verify(documents);
    }

    private void requireDemoBundle(ImportDocuments documents) {
        if (repository.ephemeral() && documents.mode() == ImportDocuments.ImportMode.INITIAL_DEFINITION) {
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "SAVED_DEFINITION_REQUIRED",
                    "Save a browser version in the School definition panel, then choose Use this definition. Definition-only import is unavailable in the demo.");
        }
    }

    static long requireMatchingVersion(String ifMatch, WorkspaceAggregate current) {
        return requireMatchingVersion(ifMatch, current.version());
    }

    static long requireMatchingVersion(String ifMatch, long version) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new WorkspaceProblem(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED",
                    "Reload the workspace and retry with its current version.");
        }
        if (!("\"ws-" + version + "\"").equals(ifMatch)) {
            throw new WorkspaceProblem(
                    HttpStatus.PRECONDITION_FAILED,
                    "STALE_WORKSPACE_VERSION",
                    "The workspace changed. Reload it before importing.");
        }
        return version;
    }
}
