package org.schoolkernel.workspace;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@Profile("demo")
class DemoDefinitionController {
    @org.springframework.beans.factory.annotation.Autowired(required = false) private DemoEvents events;
    private final WorkspaceStore store;
    private final WorkspaceMutation mutation;
    private final DemoDefinitionLimits limits;
    private final DemoVerificationService verification;
    private final WorkspaceController workspace;
    private final ObjectMapper json;
    private final int threshold, quick, thorough;

    DemoDefinitionController(WorkspaceStore store, WorkspaceMutation mutation, DemoDefinitionLimits limits,
            DemoVerificationService verification, WorkspaceController workspace, ObjectMapper json,
            @Value("${demo.search.lesson-threshold:200}") int threshold,
            @Value("${demo.search.quick-seconds:5}") int quick,
            @Value("${demo.search.thorough-seconds:60}") int thorough) {
        this.store = store; this.mutation = mutation; this.limits = limits; this.verification = verification;
        this.workspace = workspace; this.json = json; this.threshold = threshold; this.quick = quick; this.thorough = thorough;
    }

    @PostMapping(value = "/api/demo/definition", consumes = "text/plain")
    ResponseEntity<JsonNode> use(HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = "X-Demo-Confirmed", defaultValue = "false") boolean confirmed,
            @RequestHeader(value = "X-Demo-Source", defaultValue = "custom") String source) throws IOException {
        var current = store.load();
        long version = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.activeRunId() != null || current.state() == WorkspaceState.SOLVING_INITIAL || current.state() == WorkspaceState.SOLVING_REPAIR)
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", "Wait for or cancel the active run first. The definition was not used.");
        if (current.state() != WorkspaceState.EMPTY && !confirmed) throw new WorkspaceProblem(HttpStatus.PRECONDITION_REQUIRED,
                "REPLACEMENT_CONFIRMATION_REQUIRED", "Confirm replacing all existing workspace work first. Export Current to keep it.");
        if (request.getContentLengthLong() > DemoDefinitionLimits.DOCUMENT_BYTES)
            throw new WorkspaceProblem(HttpStatus.PAYLOAD_TOO_LARGE, "DEFINITION_TOO_LARGE", "The definition must be at most 2 MiB UTF-8. It was not used.");
        var definition = limits.read(request.getInputStream());
        var trusted = verification.verify(new ImportDocuments(definition, null, null, ImportDocuments.ImportMode.INITIAL_DEFINITION));
        var document = json.createObjectNode();
        document.putObject("school").put("id", trusted.schoolId()).put("displayName", definition.path("displayName").asString());
        document.put("importMode", "INITIAL_DEFINITION");
        // Only fixed public sample identifiers or file/custom origins are retained; labels never cross this API.
        document.put("origin", java.util.Set.of("two-class", "full", "star-wars", "english", "file").contains(source) ? source : "custom");
        document.put("definitionRevision", trusted.definitionRevision());
        document.set("initialDefinition", definition);
        boolean large = DemoDefinitionLimits.sessionCount(definition) > threshold;
        document.put("defaultEffort", large ? "THOROUGH" : "QUICK");
        document.put("defaultEffortSeconds", large ? thorough : quick);
        var installed = mutation.resetAndImport(version, WorkspaceState.INITIAL_DRAFT, document);
        if (events != null) events.product(java.util.Set.of("two-class", "full", "star-wars", "english").contains(source) ? DemoEvents.Product.DEFINITION_SAMPLE
                : "file".equals(source) ? DemoEvents.Product.DEFINITION_FILE : DemoEvents.Product.DEFINITION_CUSTOM, source, definition);
        return workspace.response(installed);
    }
}
