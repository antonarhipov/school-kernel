package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class DemoRailwayIngressTest {
    private static final String DOMAIN = "demo-smoke-production.up.railway.app";

    @Test void isolatedBoundaryAcceptsChangingPeersWithoutTrustingForwardedAuthority() throws Exception {
        for (String peer : List.of("100.64.0.2", "100.64.0.234", "10.20.30.40")) {
            var request = request(); request.setRemoteAddr(peer);
            assertEquals(200, status(request));
        }
        var host = request(); host.removeHeader("Host"); host.addHeader("Host", "evil.example");
        assertEquals(403, status(host));
        var forwardedHost = request(); forwardedHost.removeHeader("X-Forwarded-Host");
        forwardedHost.addHeader("X-Forwarded-Host", "evil.example");
        assertEquals(403, status(forwardedHost));
    }

    @Test void forwardingAmbiguityAndForeignOriginRemainDeniedBeforeOwnerBinding() throws Exception {
        for (String name : List.of("X-Forwarded-Proto", "X-Forwarded-Host", "Forwarded", "Origin")) {
            var request = request(); request.addHeader(name, "https://evil.example");
            assertEquals(403, status(request));
        }
        for (String value : List.of("http", "https,http", "https,https")) {
            var request = request(); request.removeHeader("X-Forwarded-Proto");
            request.addHeader("X-Forwarded-Proto", value); assertEquals(403, status(request));
        }
        var missing = request(); missing.removeHeader("X-Forwarded-Proto");
        assertEquals(403, status(missing));
        var mutation = request(); mutation.setMethod("POST"); assertEquals(403, status(mutation));
        mutation.addHeader("Origin", "https://" + DOMAIN); assertEquals(200, status(mutation));
    }

    @Test void optInRequiresDeploymentIdentityAndExactGeneratedDomain() throws Exception {
        var configuration = new DemoConfiguration();
        var environment = new MockEnvironment();
        assertThrows(IllegalStateException.class, () -> configuration.isolatedIngressBoundary(settings(), environment).afterPropertiesSet());
        for (String key : List.of("RAILWAY_PROJECT_ID", "RAILWAY_ENVIRONMENT_ID", "RAILWAY_SERVICE_ID")) {
            environment.setProperty(key, "11111111-2222-3333-4444-555555555555");
        }
        assertThrows(IllegalStateException.class, () -> configuration.isolatedIngressBoundary(settings(), environment).afterPropertiesSet());
        environment.setProperty("RAILWAY_PUBLIC_DOMAIN", DOMAIN);
        configuration.isolatedIngressBoundary(settings(), environment).afterPropertiesSet();
        environment.setProperty("RAILWAY_PUBLIC_DOMAIN", "other.up.railway.app");
        assertThrows(IllegalStateException.class, () -> configuration.isolatedIngressBoundary(settings(), environment).afterPropertiesSet());
        assertThrows(IllegalArgumentException.class, () -> new DemoSettings(List.of(DOMAIN), List.of("100.64.0.2"),
                true, Duration.ofHours(1), Duration.ofHours(8), 2));
    }

    private static DemoSettings settings() {
        return new DemoSettings(List.of(DOMAIN), List.of(), true, Duration.ofHours(1), Duration.ofHours(8), 2);
    }

    private static MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/api/workspace");
        request.addHeader("Host", DOMAIN); request.addHeader("X-Forwarded-Host", DOMAIN);
        request.addHeader("X-Forwarded-Proto", "https"); return request;
    }

    private static int status(MockHttpServletRequest request) throws Exception {
        var beans = new StaticListableBeanFactory(); beans.addBean("settings", settings());
        WorkspaceStore unbound = (WorkspaceStore) java.lang.reflect.Proxy.newProxyInstance(
                WorkspaceStore.class.getClassLoader(), new Class<?>[]{WorkspaceStore.class},
                (proxy, method, args) -> { throw new IllegalStateException("Owner must not be read at the host boundary"); });
        var filter = new HostOriginFilter(new ProblemResponder(unbound, JsonMapper.builder().build()),
                beans.getBeanProvider(DemoSettings.class));
        var response = new MockHttpServletResponse();
        var allowed = new java.util.concurrent.atomic.AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> allowed.set(true));
        assertEquals(response.getStatus() == 200, allowed.get());
        assertNull(request.getSession(false));
        if (!allowed.get()) {
            assertTrue(response.getContentAsString().contains("PUBLIC_REQUEST_DENIED"));
            assertFalse(response.getContentAsString().contains("acceptedBaseline"));
        }
        return response.getStatus();
    }
}
