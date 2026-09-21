package org.schoolkernel.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HostOriginFilter extends OncePerRequestFilter {
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

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
            denied(response);
            return;
        }
        chain.doFilter(request, response);
    }

    static void denied(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String correlationId = UUID.randomUUID().toString();
        response.getWriter().write("{\"code\":\"LOCAL_REQUEST_DENIED\",\"message\":\"Only same-origin local requests are allowed.\",\"correlationId\":\""
                + correlationId + "\",\"state\":\"UNKNOWN\",\"etag\":null}");
    }
}
