package org.schoolkernel.workspace;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HostOriginFilter extends OncePerRequestFilter {
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final ProblemResponder problems;

    public HostOriginFilter(ProblemResponder problems) {
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String host = request.getHeader("Host");
        String origin = request.getHeader("Origin");
        int port = request.getLocalPort();
        Set<String> acceptedHosts = Set.of("localhost:" + port, "127.0.0.1:" + port, "[::1]:" + port);
        boolean mutationWithoutOrigin = MUTATING_METHODS.contains(request.getMethod()) && origin == null;
        if (host == null || !acceptedHosts.contains(host.toLowerCase(Locale.ROOT))
                || mutationWithoutOrigin
                || (origin != null && !origin.equalsIgnoreCase(request.getScheme() + "://" + host))) {
            problems.write(response, HttpStatus.FORBIDDEN, "LOCAL_REQUEST_DENIED",
                    "Only same-origin local requests are allowed.");
            return;
        }
        chain.doFilter(request, response);
    }
}
