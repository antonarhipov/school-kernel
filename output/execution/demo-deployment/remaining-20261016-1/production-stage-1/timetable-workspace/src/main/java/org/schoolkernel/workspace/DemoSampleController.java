package org.schoolkernel.workspace;

import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@Profile("demo")
class DemoSampleController {
    private final DemoSamples samples;
    private final WorkspaceController workspace;
    DemoSampleController(DemoSamples samples, WorkspaceController workspace) {
        this.samples = samples;
        this.workspace = workspace;
    }
    @GetMapping("/api/samples") ResponseEntity<JsonNode> catalog() throws Exception { return publicContent(samples.catalog()); }
    @GetMapping("/api/samples/{id}") ResponseEntity<JsonNode> preview(@PathVariable String id) throws Exception {
        return publicContent(samples.preview(id));
    }
    @PostMapping("/api/samples/{id}/use") ResponseEntity<JsonNode> use(@PathVariable String id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody(required = false) JsonNode body) {
        return workspace.response(samples.use(id, ifMatch, body != null && body.path("confirmed").isBoolean() && body.path("confirmed").booleanValue()));
    }
    private static ResponseEntity<JsonNode> publicContent(JsonNode content) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(CanonicalJson.bytes(content)));
        return ResponseEntity.ok().eTag(hash).cacheControl(CacheControl.noCache().cachePublic()).body(content);
    }
}
