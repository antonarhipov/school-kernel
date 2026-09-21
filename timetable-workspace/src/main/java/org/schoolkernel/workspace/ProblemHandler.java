package org.schoolkernel.workspace;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ProblemHandler {
    private final ProblemResponder responder;

    public ProblemHandler(ProblemResponder responder) {
        this.responder = responder;
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

    @ExceptionHandler(TransactionException.class)
    ResponseEntity<ProblemResponse> transactionUnavailable() {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "STORAGE_UNAVAILABLE",
                "Local storage is unavailable. Import did not complete.");
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ProblemResponse> malformedMultipart() {
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "The import request could not be read.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemResponse> resourceNotFound() {
        return response(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested local resource does not exist.");
    }

    private ResponseEntity<ProblemResponse> response(HttpStatus status, String code, String message) {
        return responder.response(status, code, message);
    }
}
