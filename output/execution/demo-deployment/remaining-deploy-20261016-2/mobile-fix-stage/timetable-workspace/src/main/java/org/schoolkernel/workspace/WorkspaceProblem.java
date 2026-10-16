package org.schoolkernel.workspace;

import org.springframework.http.HttpStatus;

public final class WorkspaceProblem extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public WorkspaceProblem(HttpStatus status, String code, String safeMessage) {
        super(safeMessage);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
