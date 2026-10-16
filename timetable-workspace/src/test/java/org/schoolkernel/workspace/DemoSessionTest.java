package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"demo.public-hosts=demo.example", "workspace.kernel-executable=../school-kernel"})
@ActiveProfiles("demo")
class DemoSessionTest {
    MockMvc mvc;
    @Autowired org.springframework.web.context.WebApplicationContext web;
    @Autowired DemoWorkspaces workspaces;
    @Autowired WorkspaceStore store;
    @Autowired ObjectMapper json;
    @Autowired org.springframework.context.ApplicationContext context;

    @org.junit.jupiter.api.BeforeEach void configureMvc() {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test void profileHasNoDatabaseAndHealthCreatesNoSession() throws Exception {
        assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
        assertInstanceOf(DemoWorkspaceStore.class, store);
        var response = mvc.perform(get("/healthz").header("Host", "healthcheck.railway.app"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP")).andReturn();
        assertNull(response.getRequest().getSession(false));
        mvc.perform(get("/api/workspace").header("Host", "healthcheck.railway.app"))
                .andExpect(status().isForbidden());
    }

    @Test void everyReadAndForeignRunUsesOnlyTheBoundOwner() throws Exception {
        var first = new MockHttpSession();
        var second = new MockHttpSession();
        mvc.perform(get("/api/workspace").secure(true).header("Host", "demo.example").session(first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("EMPTY"));
        mvc.perform(get("/api/workspace").secure(true).header("Host", "demo.example").session(second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("EMPTY"));
        var owner = workspaces.open(first, false);
        UUID run = UUID.randomUUID();
        workspaces.bound(owner, () -> {
            var document = json.createObjectNode();
            document.put("privateValue", "owner-one-only");
            // Terminal records need no live coordinator; active ownership is covered by DemoPlanningTest and the real browser journey.
            document.putObject("lastRun").put("id", run.toString()).put("kind", "INITIAL").put("status", "CANCELLED");
            assertTrue(store.replace(0, WorkspaceState.EMPTY, WorkspaceState.INITIAL_DRAFT, document).isPresent());
        });
        mvc.perform(get("/api/workspace").secure(true).header("Host", "demo.example").session(first))
                .andExpect(jsonPath("$.workspace.privateValue").value("owner-one-only"));
        mvc.perform(get("/api/workspace").secure(true).header("Host", "demo.example").session(second))
                .andExpect(jsonPath("$.workspace.privateValue").doesNotExist());
        mvc.perform(get("/api/runs/" + run).secure(true).header("Host", "demo.example").session(second))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RUN_NOT_FOUND"));
        mvc.perform(get("/api/runs/" + run).secure(true).header("Host", "demo.example").session(first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThrows(WorkspaceProblem.class, store::load, "request authority must be cleared after the filter");
    }

    @Test void authoringIsAbsentAndOriginCsrfSchemeAndHostAreEnforced() throws Exception {
        mvc.perform(post("/api/definition-draft").secure(true).header("Host", "demo.example")
                .header("Origin", "https://demo.example")).andExpect(status().isNotFound());
        mvc.perform(get("/api/workspace").header("Host", "demo.example")
                .header("X-Forwarded-Proto", "https")).andExpect(status().isForbidden());
        mvc.perform(get("/api/workspace").secure(true).header("Host", "evil.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/workspace/clear").secure(true).header("Host", "demo.example")
                .header("Origin", "https://evil.example")).andExpect(status().isForbidden());
        var session = new MockHttpSession();
        mvc.perform(get("/api/workspace").secure(true).header("Host", "demo.example").session(session))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists("Content-Security-Policy"));
        mvc.perform(post("/api/workspace/clear").secure(true).header("Host", "demo.example")
                .header("Origin", "https://demo.example").header("If-Match", "\"ws-0\"").session(session))
                .andExpect(status().isForbidden());
    }

    @Test void endedSessionCanObtainCsrfForExplicitRestartWithoutOpeningAWorkspace() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("demo.ended", true);
        int before = workspaces.count();
        var tokenResponse = mvc.perform(get("/api/csrf").secure(true).header("Host", "demo.example").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString()).andReturn();
        assertEquals(before, workspaces.count());
        mvc.perform(get("/api/workspace").secure(true).header("Host", "demo.example").session(session))
                .andExpect(status().isGone()).andExpect(jsonPath("$.code").value("SESSION_ENDED"));
        var token = json.readTree(tokenResponse.getResponse().getContentAsString());
        mvc.perform(post("/api/demo/session").secure(true).header("Host", "demo.example")
                .header("Origin", "https://demo.example").session(session)
                .header(token.path("headerName").stringValue(), token.path("token").stringValue()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/workspace").secure(true).header("Host", "demo.example").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("EMPTY"));
    }
}
