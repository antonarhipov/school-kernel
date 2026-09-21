package org.schoolkernel.workspace;

import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ProblemHandler {
    private final WorkspaceRepository repository;

    public ProblemHandler(WorkspaceRepository repository) {
        this.repository = repository;
    }

    @ExceptionHandler(WorkspaceProblem.class)
    ResponseEntity<ProblemResponse> workspaceProblem(WorkspaceProblem problem) {
        return response(problem.status(), problem.code(), problem.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemResponse> uploadTooLarge() {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "IMPORT_TOO_LARGE", "The selected import is too large.");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemResponse> databaseUnavailable() {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "STORAGE_UNAVAILABLE",
                "Local storage is unavailable. Import did not complete.");
    }

    private ResponseEntity<ProblemResponse> response(HttpStatus status, String code, String message) {
        String state = "UNKNOWN";
        String etag = null;
        try {
            WorkspaceAggregate current = repository.load();
            state = current.state().name();
            etag = current.etag();
        } catch (RuntimeException ignored) {
            // A database outage cannot safely disclose a current state.
        }
        ProblemResponse body = new ProblemResponse(code, message, UUID.randomUUID().toString(), state, etag);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff");
        if (etag != null) {
            response.header(HttpHeaders.ETAG, etag);
        }
        return response.body(body);
    }
}
