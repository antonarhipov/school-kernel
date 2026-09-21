package org.schoolkernel.workspace;

import java.net.URI;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final ObjectMapper json;

    public WorkspaceController(WorkspaceRepository repository, ImportService imports, ObjectMapper json) {
        this.repository = repository;
        this.imports = imports;
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

    private ResponseEntity<JsonNode> response(WorkspaceAggregate aggregate) {
        ObjectNode body = json.createObjectNode();
        body.put("state", aggregate.state().name());
        body.put("version", aggregate.version());
        body.put("acceptedTimetable", aggregate.state() == WorkspaceState.ACCEPTED_BASELINE);
        body.set("workspace", aggregate.document());
        return ResponseEntity.ok()
                .eTag(aggregate.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }
}
