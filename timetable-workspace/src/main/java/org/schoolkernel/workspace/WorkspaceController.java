package org.schoolkernel.workspace;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Controller
public class WorkspaceController {
    private final WorkspaceRepository repository;
    private final ImportService imports;
    private final InitialPlanningService planning;
    private final ObjectMapper json;

    public WorkspaceController(
            WorkspaceRepository repository,
            ImportService imports,
            InitialPlanningService planning,
            ObjectMapper json) {
        this.repository = repository;
        this.imports = imports;
        this.planning = planning;
        this.json = json;
    }

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(302).location(URI.create("/workspace/")).build();
    }

    @GetMapping("/workspace/")
    public String workspaceIndex() {
        return "forward:/workspace/index.html";
    }

    @GetMapping("/api/csrf")
    @ResponseBody
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/api/workspace")
    @ResponseBody
    public ResponseEntity<JsonNode> workspace() {
        return response(repository.load());
    }

    @PostMapping(path = "/api/import", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<JsonNode> importSchool(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestParam(value = "definition", required = false) MultipartFile definition,
            @RequestParam(value = "result", required = false) MultipartFile result,
            @RequestParam(value = "archive", required = false) MultipartFile archive) {
        return response(imports.importSchool(ifMatch, definition, result, archive));
    }

    @PostMapping(path = "/api/initial-draft/replace", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<JsonNode> replaceInitialDraft(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestParam("definition") MultipartFile definition) {
        return response(imports.replaceInitial(ifMatch, definition));
    }

    @PostMapping("/api/runs")
    @ResponseBody
    public ResponseEntity<JsonNode> startRun(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        WorkspaceAggregate started = planning.start(ifMatch);
        return ResponseEntity.accepted()
                .eTag(started.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(snapshot(started));
    }

    @GetMapping("/api/runs/{runId}")
    @ResponseBody
    public ResponseEntity<JsonNode> run(@PathVariable UUID runId) {
        WorkspaceAggregate current = repository.load();
        return ResponseEntity.ok()
                .eTag(current.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(planning.run(runId));
    }

    @DeleteMapping("/api/runs/{runId}")
    @ResponseBody
    public ResponseEntity<JsonNode> cancelRun(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @PathVariable UUID runId) {
        return response(planning.cancel(ifMatch, runId));
    }

    @GetMapping("/api/proposal")
    @ResponseBody
    public ResponseEntity<JsonNode> proposal() {
        WorkspaceAggregate current = repository.load();
        if (current.state() != WorkspaceState.INITIAL_PROPOSAL) {
            throw new WorkspaceProblem(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "PROPOSAL_NOT_FOUND",
                    "There is no current proposal.");
        }
        return ResponseEntity.ok()
                .eTag(current.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(current.document().path("proposal"));
    }

    @PostMapping("/api/proposal/accept")
    @ResponseBody
    public ResponseEntity<JsonNode> acceptProposal(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(planning.accept(ifMatch));
    }

    @DeleteMapping("/api/proposal")
    @ResponseBody
    public ResponseEntity<JsonNode> discardProposal(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(planning.discard(ifMatch));
    }

    private ResponseEntity<JsonNode> response(WorkspaceAggregate aggregate) {
        return ResponseEntity.ok()
                .eTag(aggregate.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(snapshot(aggregate));
    }

    private ObjectNode snapshot(WorkspaceAggregate aggregate) {
        ObjectNode body = json.createObjectNode();
        body.put("state", aggregate.state().name());
        body.put("version", aggregate.version());
        body.put("acceptedTimetable", aggregate.state() == WorkspaceState.ACCEPTED_BASELINE);
        body.set("workspace", aggregate.document());
        return body;
    }
}
