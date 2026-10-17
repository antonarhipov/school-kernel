package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class DemoLocalHttpTest {
    final DemoSettings settings = new DemoSettings(List.of("localhost:8080", "127.0.0.1:8080"), List.of(), false,
            Duration.ofHours(1), Duration.ofHours(8), 2);
    @Test void directSameOriginLoopbackHttpWorksWithoutForwardingOrTls() throws Exception {
        assertTrue(settings.localOnly());
        for(String host:List.of("localhost:8080","127.0.0.1:8080")) {
            var request=request();request.addHeader("Host",host);request.addHeader("Origin","http://"+host);
            var response=invoke(request);assertEquals(200,response.getStatus());
            assertNull(response.getHeader("Strict-Transport-Security"));
        }
    }
    @Test void foreignOriginsRemotePeersForeignHostsAndForwardingAreRefused() throws Exception {
        for(String kind:List.of("origin","host","remote","local","Forwarded","X-Forwarded-Proto","X-Forwarded-Host","missing-origin")) {
            var request=request();request.addHeader("Host",kind.equals("host")?"evil.example":"localhost:8080");
            if(!kind.equals("missing-origin"))request.addHeader("Origin",kind.equals("origin")?"http://evil.example":"http://localhost:8080");
            if(kind.equals("remote"))request.setRemoteAddr("203.0.113.1");
            if(kind.equals("local"))request.setLocalAddr("192.168.1.10");
            if(kind.startsWith("X-")||kind.equals("Forwarded"))request.addHeader(kind,"https");
            assertEquals(403,invoke(request).getStatus(),kind);assertNull(request.getSession(false));
        }
    }
    @Test void publicOrProxyConfigurationNeverSelectsLocalMode() {
        assertFalse(new DemoSettings(List.of("localhost:8080","demo.example"),List.of(),false,Duration.ofHours(1),Duration.ofHours(8),2).localOnly());
        assertFalse(new DemoSettings(List.of("localhost:8080"),List.of("127.0.0.1"),false,Duration.ofHours(1),Duration.ofHours(8),2).localOnly());
    }
    @Test void onlyLocalConfigurationChangesTheBindingAndCookie() {
        var local = new org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory();
        local.getSettings().getSession().getCookie().setSecure(true);
        new DemoConfiguration().localDemoServer(settings).customize(local);
        assertTrue(local.getAddress().isLoopbackAddress());
        assertFalse(local.getSettings().getSession().getCookie().getSecure());
        var publicSettings=new DemoSettings(List.of("demo.example"),List.of(),false,Duration.ofHours(1),Duration.ofHours(8),2);
        var external=new org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory();
        external.getSettings().getSession().getCookie().setSecure(true);
        new DemoConfiguration().localDemoServer(publicSettings).customize(external);
        assertNull(external.getAddress());
        assertTrue(external.getSettings().getSession().getCookie().getSecure());
    }
    private MockHttpServletResponse invoke(MockHttpServletRequest request)throws Exception {
        var beans=new StaticListableBeanFactory();beans.addBean("settings",settings);
        var filter=new HostOriginFilter(new ProblemResponder(null,JsonMapper.builder().build()),beans.getBeanProvider(DemoSettings.class));
        var response=new MockHttpServletResponse();filter.doFilter(request,response,(req,res)->{});return response;
    }
    private static MockHttpServletRequest request() {
        var request=new MockHttpServletRequest("POST","/api/demo/definition/check");
        request.setLocalPort(8080);request.setLocalAddr("127.0.0.1");request.setRemoteAddr("127.0.0.1");return request;
    }
}
