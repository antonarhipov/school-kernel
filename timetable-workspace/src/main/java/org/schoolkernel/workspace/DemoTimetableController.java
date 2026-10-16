package org.schoolkernel.workspace;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/** Stateless bounded review; no authority to mutate a school or stored timetable. */
@RestController @Profile("demo")
class DemoTimetableController {
    private static final JsonMapper JSON = DemoInputJson.MAPPER;
    private final DemoDefinitionLimits limits;
    private final DemoVerificationService verification;
    DemoTimetableController(DemoDefinitionLimits limits, DemoVerificationService verification) { this.limits = limits; this.verification = verification; }

    @PostMapping(value = "/api/demo/definition/check", consumes = "text/plain")
    ResponseEntity<?> definition(HttpServletRequest request) throws IOException {
        var definition = limits.read(request.getInputStream());
        var verified = verification.verify(new ImportDocuments(definition, null, null, ImportDocuments.ImportMode.INITIAL_DEFINITION));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of("definitionRevision", verified.definitionRevision(), "build", verification.build()));
    }

    @PostMapping(value = "/api/demo/timetable/review", consumes = "application/json")
    ResponseEntity<JsonNode> review(HttpServletRequest request) throws IOException {
        var document = read(request);
        var definition = document.path("definition"); var result = document.path("result");
        limits.check(definition, false);
        if (!result.path("timetable").path("assignments").isArray() || result.path("timetable").path("assignments").size() != definition.path("lessons").size())
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_CANDIDATE", "/result/timetable/assignments: A complete assignment for every lesson is required.");
        var evidence = verification.review(new ImportDocuments(definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        ((tools.jackson.databind.node.ObjectNode)evidence).put("build", verification.build());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(evidence);
    }
    static JsonNode read(HttpServletRequest request) throws IOException {
        int max = 2 * DemoDefinitionLimits.DOCUMENT_BYTES;
        byte[] bytes = request.getInputStream().readNBytes(max + 1);
        if (bytes.length > max) throw new WorkspaceProblem(HttpStatus.PAYLOAD_TOO_LARGE, "CANDIDATE_TOO_LARGE", "The complete candidate exceeds 4 MiB.");
        try { return JSON.readTree(bytes); }
        catch (tools.jackson.core.JacksonException malformed) { throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_CANDIDATE", "Expected one valid JSON document without duplicate fields or trailing content."); }
    }
}
