package org.schoolkernel.workspace;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Semaphore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/** Runs after authorization/CSRF and before MVC decoding, including multipart resolution. */
final class DemoInputAdmissionFilter extends OncePerRequestFilter {
    private static final String SESSION_GATE = DemoInputAdmissionFilter.class.getName();
    private static final Set<String> INPUT_ROUTES = Set.of("/api/demo/definition/check", "/api/demo/definition",
            "/api/demo/timetable/review", "/api/import", "/api/workspace/upload-definition");
    private final Semaphore global = new Semaphore(4);
    private final ProblemResponder problems;

    DemoInputAdmissionFilter(ProblemResponder problems) { this.problems = problems; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !INPUT_ROUTES.contains(request.getRequestURI());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var session = request.getSession(false);
        if (session == null) throw DemoWorkspaces.ended();
        Semaphore owner;
        synchronized (session) {
            owner = (Semaphore) session.getAttribute(SESSION_GATE);
            if (owner == null) { owner = new Semaphore(2); session.setAttribute(SESSION_GATE, owner); }
        }
        if (!owner.tryAcquire()) { refuse(response); return; }
        try {
            if (!global.tryAcquire()) { refuse(response); return; }
            try { chain.doFilter(request, response); }
            finally { global.release(); }
        } finally { owner.release(); }
    }

    private void refuse(HttpServletResponse response) throws IOException {
        response.setHeader("Retry-After", "1");
        problems.writeWithoutSnapshot(response, HttpStatus.TOO_MANY_REQUESTS, "INPUT_BUSY",
                "School data checking is busy. Existing work is unchanged. Retry shortly.");
    }
}
