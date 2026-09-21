package org.schoolkernel.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class ProblemResponder {
    private final WorkspaceRepository repository;
    private final ObjectMapper json;

    public ProblemResponder(WorkspaceRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    ResponseEntity<ProblemResponse> response(HttpStatus status, String code, String message) {
        Snapshot snapshot = snapshot();
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff");
        if (snapshot.etag() != null) {
            response.header(HttpHeaders.ETAG, snapshot.etag());
        }
        return response.body(body(code, message, snapshot));
    }

    void write(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        Snapshot snapshot = snapshot();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (snapshot.etag() != null) {
            response.setHeader(HttpHeaders.ETAG, snapshot.etag());
        }
        json.writeValue(response.getOutputStream(), body(code, message, snapshot));
    }

    private static ProblemResponse body(String code, String message, Snapshot snapshot) {
        return new ProblemResponse(
                code, message, UUID.randomUUID().toString(), snapshot.state(), snapshot.etag());
    }

    private Snapshot snapshot() {
        try {
            WorkspaceAggregate current = repository.load();
            return new Snapshot(current.state().name(), current.etag());
        } catch (RuntimeException ignored) {
            return new Snapshot("UNKNOWN", null);
        }
    }

    private record Snapshot(String state, String etag) {}
}
