package org.schoolkernel.workspace;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/** Runs after access/CSRF checks and before request decoding, including multipart commands. */
final class DefinitionDraftAccess extends OncePerRequestFilter {
    private final WorkspaceRepository repository;
    private final ProblemResponder problems;

    DefinitionDraftAccess(WorkspaceRepository repository, ProblemResponder problems) {
        this.repository = repository;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/")
                && !java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
                && !java.util.Set.of("/api/definition-draft", "/api/definition-draft/preview", "/api/definition-draft/school", "/api/definition-draft/grid", "/api/definition-draft/grid/preview", "/api/definition-draft/subjects", "/api/definition-draft/teachers", "/api/definition-draft/cohorts", "/api/definition-draft/rooms", "/api/definition-draft/requirements", "/api/definition-draft/requirements/preview").contains(request.getRequestURI())
                && repository.load().state() == WorkspaceState.DEFINITION_DRAFT) {
            problems.write(response, HttpStatus.CONFLICT, "DEFINITION_DRAFT_ACTIVE",
                    "A school-definition draft is open. Resume authoring and finish or discard it first.");
            return;
        }
        chain.doFilter(request, response);
    }
}
