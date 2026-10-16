package org.schoolkernel.workspace;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** Registered inside Spring Security, after the public host gate and before request decoding. */
final class DemoSessionFilter extends OncePerRequestFilter {
    private final DemoWorkspaces workspaces;
    private final ProblemResponder problems;
    DemoSessionFilter(DemoWorkspaces workspaces, ProblemResponder problems) {
        this.workspaces = workspaces;
        this.problems = problems;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/definition-draft") || path.equals("/api/repair-draft/return-to-authoring") || path.equals("/api/initial-draft/replace")) {
            problems.write(response, org.springframework.http.HttpStatus.NOT_FOUND,
                    "ROUTE_NOT_FOUND", "This action is unavailable in the demo.");
            return;
        }
        if (!path.startsWith("/api/") || Set.of("/api/demo/config", "/api/demo/session", "/api/demo/timetables").contains(path)
                || "GET".equals(request.getMethod()) && (path.equals("/api/samples") || path.matches("/api/samples/[^/]+"))) {
            chain.doFilter(request, response);
            return;
        }
        try {
            boolean firstRead = "GET".equals(request.getMethod()) && Set.of("/api/csrf", "/api/workspace").contains(path);
            // A cookie from before restart is evidence of ended work, never authority to silently create a new school.
            if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
                if (path.equals("/api/csrf")) {
                    request.getSession(true).setAttribute("demo.ended", true);
                    chain.doFilter(request, response);
                    return;
                }
                throw DemoWorkspaces.ended();
            }
            var session = request.getSession(firstRead);
            if (path.equals("/api/csrf")) {
                // Refreshing CSRF remains possible for an ended session's explicit Start again action.
                chain.doFilter(request, response);
                return;
            }
            if (session == null || Boolean.TRUE.equals(session.getAttribute("demo.ended"))) throw DemoWorkspaces.ended();
            workspaces.bind(workspaces.open(session, firstRead));
            response.setHeader("Cache-Control", "no-store");
            chain.doFilter(request, response);
        } catch (WorkspaceProblem problem) {
            problems.write(response, problem.status(), problem.code(), problem.getMessage());
        } finally { workspaces.unbind(); }
    }
}
