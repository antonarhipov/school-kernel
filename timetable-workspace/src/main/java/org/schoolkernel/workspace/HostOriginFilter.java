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
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HostOriginFilter.class);
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final ProblemResponder problems;
    private final DemoSettings demo;

    public HostOriginFilter(ProblemResponder problems) {
        this(problems, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public HostOriginFilter(ProblemResponder problems,
            org.springframework.beans.factory.ObjectProvider<DemoSettings> settings) {
        this.problems = problems;
        this.demo = settings == null ? null : settings.getIfAvailable();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String host = request.getHeader("Host");
        String origin = request.getHeader("Origin");
        if (demo != null) {
            publicRequest(request, response, chain, host, origin);
            return;
        }
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

    private void publicRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            String host, String origin) throws IOException, ServletException {
        boolean health = "/healthz".equals(request.getRequestURI()) && "GET".equals(request.getMethod());
        if (health && "healthcheck.railway.app".equals(host)) {
            chain.doFilter(request, response);
            return;
        }
        String forwardedScheme = request.getHeader("X-Forwarded-Proto");
        // Railway has no stable proxy address range. This opt-in requires an audited single-service
        // environment without TCP ingress; its HTTP edge overwrites these two forwarding headers.
        boolean trusted = demo.railwayIsolatedIngress() || demo.trustedProxyAddresses().stream().filter(address -> !address.isBlank())
                .anyMatch(address -> new org.springframework.security.web.util.matcher.IpAddressMatcher(address).matches(request));
        boolean secure = forwardedScheme == null ? request.isSecure() : trusted && "https".equals(forwardedScheme)
                && java.util.Collections.list(request.getHeaders("X-Forwarded-Proto")).size() == 1;
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        // Railway supplies this even for ordinary requests. It never replaces Host or selects an origin.
        boolean matchingForwardedHost = forwardedHost == null || trusted && host != null
                && host.equalsIgnoreCase(forwardedHost)
                && java.util.Collections.list(request.getHeaders("X-Forwarded-Host")).size() == 1;
        if (host == null || !demo.publicHosts().contains(host.toLowerCase(Locale.ROOT)) || !secure
                || request.getHeader("Forwarded") != null || !matchingForwardedHost
                || (MUTATING_METHODS.contains(request.getMethod()) && origin == null)
                || (origin != null && !origin.equals("https://" + host.toLowerCase(Locale.ROOT)))) {
            LOG.debug("Demo origin refused: allowedHost={} trustedProxy={} secure={} forwardedPresent={} matchingForwardedHost={} originPresent={}",
                    host != null && demo.publicHosts().contains(host.toLowerCase(Locale.ROOT)), trusted, secure,
                    request.getHeader("Forwarded") != null, matchingForwardedHost, origin != null);
            problems.write(response, HttpStatus.FORBIDDEN, "PUBLIC_REQUEST_DENIED",
                    "This demo request does not match the configured HTTPS origin.");
            return;
        }
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        chain.doFilter(request, response);
    }
}
