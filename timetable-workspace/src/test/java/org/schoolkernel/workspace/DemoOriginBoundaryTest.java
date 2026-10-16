package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class DemoOriginBoundaryTest {
    private final HostOriginFilter filter;

    DemoOriginBoundaryTest() {
        var beans = new StaticListableBeanFactory();
        beans.addBean("settings", new DemoSettings(List.of("demo.example"), List.of("100.64.0.10"), false,
                Duration.ofMinutes(60), Duration.ofHours(8), 2));
        WorkspaceStore unbound = (WorkspaceStore) java.lang.reflect.Proxy.newProxyInstance(
                WorkspaceStore.class.getClassLoader(), new Class<?>[]{WorkspaceStore.class},
                (proxy, method, args) -> { throw new IllegalStateException("No owner is bound before the host gate"); });
        filter = new HostOriginFilter(new ProblemResponder(unbound, JsonMapper.builder().build()),
                beans.getBeanProvider(DemoSettings.class));
    }

    @Test void exactTrustedIpv4AndMappedIpv6ProxyCanSupplyOnlyHttpsScheme() throws Exception {
        for (String peer : List.of("100.64.0.10", "0:0:0:0:0:ffff:6440:a")) {
            var request = request(); request.setRemoteAddr(peer);
            assertEquals(200, status(request), peer);
        }
    }

    @Test void clientSchemeOrAnUnobservedProxyCannotClaimHttps() throws Exception {
        for (String peer : List.of("203.0.113.10", "100.64.0.11")) {
            var request = request(); request.setRemoteAddr(peer);
            assertEquals(403, status(request));
            assertNull(request.getSession(false));
        }
    }

    @Test void railwayForwardedHostMayConfirmButNeverReplaceTheAllowedHost() throws Exception {
        var matching = request(); matching.addHeader("X-Forwarded-Host", "demo.example");
        assertEquals(200, status(matching));
        var duplicate = request(); duplicate.addHeader("X-Forwarded-Host", "demo.example");
        duplicate.addHeader("X-Forwarded-Host", "demo.example");
        assertEquals(403, status(duplicate));
        var host = request(); host.removeHeader("Host"); host.addHeader("Host", "evil.example");
        host.addHeader("X-Forwarded-Host", "demo.example");
        assertEquals(403, status(host));
        var untrusted = request(); untrusted.setRemoteAddr("203.0.113.1"); untrusted.setSecure(true);
        untrusted.removeHeader("X-Forwarded-Proto"); untrusted.addHeader("X-Forwarded-Host", "demo.example");
        assertEquals(403, status(untrusted));
    }

    @Test void ambiguousSchemeAndForeignAuthorityAreRefused() throws Exception {
        for (String header : List.of("Forwarded", "X-Forwarded-Host")) {
            var request = request(); request.addHeader(header, "evil.example");
            assertEquals(403, status(request));
        }
        var duplicate = request(); duplicate.addHeader("X-Forwarded-Proto", "https");
        assertEquals(403, status(duplicate));
        var combined = request(); combined.removeHeader("X-Forwarded-Proto"); combined.addHeader("X-Forwarded-Proto", "https,http");
        assertEquals(403, status(combined));
        var host = request(); host.removeHeader("Host"); host.addHeader("Host", "evil.example");
        assertEquals(403, status(host));
        var origin = request(); origin.addHeader("Origin", "https://evil.example");
        assertEquals(403, status(origin));
    }

    private static MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/healthz");
        request.setRemoteAddr("100.64.0.10");
        request.addHeader("Host", "demo.example");
        request.addHeader("X-Forwarded-Proto", "https");
        return request;
    }

    private int status(MockHttpServletRequest request) throws Exception {
        var response = new MockHttpServletResponse();
        var allowed = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> allowed.set(true));
        assertEquals(response.getStatus() == 200, allowed.get());
        return response.getStatus();
    }
}
