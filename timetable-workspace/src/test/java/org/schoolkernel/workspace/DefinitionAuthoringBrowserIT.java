package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** UC-1: isolated PostgreSQL, actual HTTP security, real Chrome, and process restart. */
class DefinitionAuthoringBrowserIT extends WorkbenchBrowserSupport {
    @Test
    void uc1EmptyEntryNavigationResumeAndResponsiveInspection() {
        workbench.open().awaitText("Create school");
        assertTrue(workbench.page().locator("#import-card").isVisible());
        openInBrowser();
        workbench.awaitText("Saved school-definition draft");
        assertEquals("School", workbench.page().locator("#authoring-section h3").textContent());
        JsonNode draft = storedWorkspaceDocument().path("definitionDraft");
        ObjectNode expected = JSON.createObjectNode().put("schemaVersion", 1).put("catalogVersion", 9);
        for (String key : List.of("subjects", "teachers", "cohorts", "rooms", "periods", "lessons")) expected.putArray(key);
        assertEquals(expected, draft.path("definition"));
        assertEquals(JSON.createObjectNode(), draft.path("sourceDocument"));
        assertEquals("EMPTY", draft.path("sourceState").stringValue());
        assertEquals(2, draft.path("issues").size());
        String saved = storedDocument();
        for (String name : List.of("School", "Grid", "Subjects", "Teachers", "Classes", "Rooms", "Room policies", "Weekly requirements", "Review")) {
            workbench.page().locator("button[data-section='" + name + "']").click();
            assertEquals(name, workbench.page().locator("#authoring-section h3").textContent());
        }
        assertEquals(0, workbench.page().locator("button[data-section='Resources']").count());
        for (int[] viewport : List.of(new int[]{1600,900}, new int[]{1280,800}, new int[]{390,844}, new int[]{700,844})) {
            workbench.viewport(viewport[0], viewport[1]);
            if (viewport[0] <= 700) workbench.awaitText("Editing requires a wider view");
            assertEquals(true, workbench.page().evaluate("document.documentElement.scrollWidth <= innerWidth"));
            assertTrue(workbench.page().locator(".authoring-navigation").isVisible());
            if (viewport[0] <= 700) assertEquals(0, workbench.page().locator("form input, form select, form textarea, button[type=submit]").filter(
                    new com.microsoft.playwright.Locator.FilterOptions().setVisible(true)).count());
        }
        workbench.viewport(1280,800);
        workbench.reload(); workbench.awaitText("Saved school-definition draft");
        assertEquals("Review", workbench.page().locator("#authoring-section h3").textContent());
        assertEquals(saved, storedDocument());
        assertEquals(200, command("POST", "/api/definition-draft", "{}", null).status());
        assertEquals(saved, storedDocument(), "resume cannot create a new revision or reset content");
    }

    @Test
    void uc1InitialSourceOptionalFidelityAndRealRestart() {
        ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("kernel-cli/src/test/resources/fixtures/policy-school.json").toFile());
        definition.put("displayName", "Õpetajate <img src=x onerror=alert(1)> kool");
        ((ObjectNode) definition.path("teachers").get(0)).putArray("availablePeriodIds");
        ObjectNode source = JSON.createObjectNode(); source.set("initialDefinition", definition);
        source.putObject("school").put("id", definition.path("schoolId").stringValue()).put("displayName", definition.path("displayName").stringValue());
        store("INITIAL_DRAFT", source);
        workbench.open().awaitText("Author definition"); openInBrowser();
        workbench.awaitText("Saved school-definition draft");
        assertEquals(definition, storedWorkspaceDocument().path("definitionDraft").path("definition"));
        assertEquals(source, storedWorkspaceDocument().path("definitionDraft").path("sourceDocument"));
        assertEquals(0, workbench.page().locator("#state-card img").count());
        String saved = storedDocument();
        int restartPort;
        try (var context = startRestartableWorkspace()) {
            restartPort = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context).getWebServer().getPort();
            workbench.open(restartPort).awaitText("Saved school-definition draft");
            assertEquals("Review", workbench.page().locator("#authoring-section h3").textContent());
        }
        try (var context = startRestartableWorkspace()) {
            restartPort = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context).getWebServer().getPort();
            workbench.open(restartPort).awaitText("Saved school-definition draft");
            assertEquals(saved, storedDocument());
        }
    }

    @Test
    void uc1AcceptedUpgradeCancellationAdjustmentsPinsAndCurrent() throws Exception {
        ObjectNode source = fixtures.validAcceptedDocument();
        ObjectNode definition = (ObjectNode) source.path("acceptedBaseline").path("definition");
        ObjectNode lesson = (ObjectNode) definition.path("lessons").get(0);
        lesson.put("periodLock", "mon-1").put("roomLock", "room-102");
        String inputRevision = verifier.verify(new ImportDocuments(definition, null, null,
                ImportDocuments.ImportMode.INITIAL_DEFINITION)).definitionRevision();
        ObjectNode result = (ObjectNode) source.path("acceptedBaseline").path("result");
        result.put("inputRevision", inputRevision);
        String timetableRevision = timetableRevision(result, inputRevision);
        result.put("timetableRevision", timetableRevision);
        source.put("definitionRevision", inputRevision).put("timetableRevision", timetableRevision);
        ((ObjectNode) source.path("acceptedBaseline").path("manifest"))
                .put("inputRevision", inputRevision).put("timetableRevision", timetableRevision);
        ((ObjectNode) source.path("acceptedBaseline").path("manifest")).withArray("locks").addObject()
                .put("lessonId", lesson.path("id").stringValue()).put("periodLockOrigin", "ATTEMPT_SCOPED").put("roomLockOrigin", "PERSISTENT_POLICY");
        storeAccepted(source);
        workbench.open().awaitText("Author definition");
        String before = storedDocument();
        workbench.page().locator("#open-authoring").click(); workbench.awaitText("Required upgrade: catalog 1");
        workbench.awaitText("No limit"); workbench.awaitText("Expire prior attempt-scoped pin");
        workbench.page().locator("#cancel-authoring").click(); assertEquals(before, storedDocument());
        assertEquals(409, command("POST", "/api/definition-draft", "{}", null).status()); assertEquals(before, storedDocument());
        openInBrowser(); workbench.awaitText("Saved school-definition draft");
        JsonNode document = storedWorkspaceDocument(); JsonNode draft = document.path("definitionDraft");
        assertEquals(source.path("acceptedBaseline"), document.path("acceptedBaseline"));
        assertEquals(source, draft.path("sourceDocument"));
        ObjectNode expected = definition.deepCopy(); expected.put("catalogVersion", 8);
        expected.put("basedOnRevision", source.path("acceptedBaseline").path("result").path("inputRevision").stringValue());
        ((ObjectNode) expected.path("lessons").get(0)).remove("periodLock");
        for (JsonNode cohort : expected.path("cohorts")) ((ObjectNode) cohort).put("maxDailyGaps", Integer.MAX_VALUE);
        for (String id : List.of("soft.cohort-gap", "soft.cohort-week-balance", "soft.cohort-late-start")) expected.withArray("softConstraintOverrides").addObject().put("constraintId", id).put("weight", 0);
        assertEquals(expected, draft.path("definition"));
        assertEquals(1, draft.path("expiredPins").size()); assertEquals(4, draft.path("openingAdjustments").size());
        String saved = storedDocument();
        workbench.page().locator("#inspect-authoring-current").click(); workbench.awaitText("Current");
        workbench.expectNot("document.querySelector('#clear-workspace, #utilities-upload-definition, #start-manual-draft, #open-repair-setup')");
        workbench.page().locator("#resume-authoring").click(); workbench.awaitText("Expired attempt pins — exact source provenance");
        assertEquals(saved, storedDocument());
        APIResponse exported = workbench.page().request().get(base() + "/api/accepted/export");
        assertEquals(200, exported.status());
        var bundle = new SafeImportReader().readArchive(exported.body());
        assertEquals(definition, bundle.definition()); assertEquals(source.path("acceptedBaseline").path("result"), bundle.result());
        assertEquals(saved, storedDocument());
    }

    @Test
    void uc1AcceptedCurrentCatalogsAndSavedContextAreRetainedWithoutDefaultMaterialization() {
        workbench.open();
        for (int catalog : List.of(8, 9)) {
            ObjectNode source = fixtures.validAcceptedDocument();
            ObjectNode definition = (ObjectNode) source.path("acceptedBaseline").path("definition");
            definition.put("catalogVersion", catalog);
            ((ObjectNode) source.path("acceptedBaseline").path("manifest")).put("catalogVersion", catalog);
            definition.putArray("reservedPeriodIds");
            ((ObjectNode) definition.path("teachers").get(0)).putArray("undesirablePeriodIds");
            source.putObject("authoringContext").putObject("requirements").put("explicitException", true);
            storeAccepted(source);
            ObjectNode expected = definition.deepCopy();
            expected.put("basedOnRevision", source.path("acceptedBaseline").path("result").path("inputRevision").stringValue());
            APIResponse opened = command("POST", "/api/definition-draft", "{}", null);
            assertEquals(200, opened.status());
            JsonNode draft = storedWorkspaceDocument().path("definitionDraft");
            assertEquals(expected, draft.path("definition"));
            assertEquals(source, draft.path("sourceDocument"));
            assertEquals(source.path("authoringContext"), draft.path("editingContext"));
            assertTrue(draft.path("openingAdjustments").isEmpty());
            String before = storedDocument();
            assertEquals(200, command("POST", "/api/definition-draft", "{}", null).status());
            assertEquals(before, storedDocument());
        }
        // A previously saved draft may contain incomplete work, removed IDs, and persistent locks.
        ObjectNode saved = (ObjectNode) storedWorkspaceDocument();
        ObjectNode draft = (ObjectNode) saved.path("definitionDraft");
        ((ObjectNode) draft.path("definition").path("lessons").get(0)).put("periodLock", "mon-1");
        draft.withArray("issues").addObject().put("severity", "BLOCKING").put("ownerType", "Teacher")
                .put("ownerId", "teacher-alex").put("field", "qualifiedSubjectIds").put("message", "Saved issue");
        draft.withArray("removedIdentities").add("removed-lesson.01");
        store("DEFINITION_DRAFT", saved);
        String before = storedDocument();
        workbench.reload(); workbench.awaitText("Saved school-definition draft");
        assertEquals(200, command("POST", "/api/definition-draft", "{}", null).status());
        assertEquals(before, storedDocument(), "resume retains issues, context, removed identities, and newly authored policy locks");
    }

    @Test
    void uc1AllOtherLifecycleStatesRefuseWithoutMutationAndNameTheState() {
        workbench.open();
        for (WorkspaceState state : WorkspaceState.values()) {
            if (List.of(WorkspaceState.EMPTY, WorkspaceState.INITIAL_DRAFT, WorkspaceState.ACCEPTED_BASELINE, WorkspaceState.DEFINITION_DRAFT).contains(state)) continue;
            store(state.name(), JSON.createObjectNode()); String before = storedDocument();
            APIResponse response = command("POST", "/api/definition-draft", "{}", null);
            assertEquals(409, response.status(), state.name());
            assertTrue(response.text().contains(state.name())); assertEquals(before, storedDocument());
        }
        ObjectNode accepted = fixtures.validAcceptedDocument(); storeAccepted(accepted);
        assertEquals(200, command("POST", "/api/definition-draft", "{\"confirmUpgrade\":true}", null).status());
        String before = storedDocument();
        for (String path : List.of("/api/runs", "/api/repair-draft", "/api/manual-draft", "/api/import", "/api/workspace/clear", "/api/workspace/upload-definition", "/api/initial-draft/replace", "/api/proposal/accept")) {
            APIResponse response = command("POST", path, "{}", null);
            assertEquals(409, response.status(), path); assertTrue(response.text().contains("DEFINITION_DRAFT_ACTIVE")); assertEquals(before, storedDocument());
        }
        assertEquals(409, command("DELETE", "/api/workspace", "{}", null).status()); assertEquals(before, storedDocument());
    }

    @Test
    void uc1UnsupportedSourcesStaleOpeningAndSecurityDenialPreserveData() {
        workbench.open();
        for (String bad : List.of("version", "unknownRoot", "unknownNested", "unmappable")) {
            ObjectNode definition = (ObjectNode) JSON.readTree(ROOT.resolve("kernel-cli/src/test/resources/fixtures/initial-school.json").toFile());
            switch (bad) {
                case "version" -> definition.put("catalogVersion", 10);
                case "unknownRoot" -> definition.put("externalMetadata", "must not vanish");
                case "unknownNested" -> ((ObjectNode) definition.path("teachers").get(0)).put("teamTeaching", true);
                default -> ((ObjectNode) definition.path("cohorts").get(0)).put("size", 1.5);
            }
            ObjectNode source = JSON.createObjectNode(); source.set("initialDefinition", definition); store("INITIAL_DRAFT", source);
            String before = storedDocument(); APIResponse denied = command("POST", "/api/definition-draft", "{}", null);
            assertEquals(422, denied.status(), bad); assertTrue(denied.text().contains("Source unchanged")); assertEquals(before, storedDocument());
        }
        store("EMPTY", JSON.createObjectNode()); String before = storedDocument();
        assertEquals(412, command("POST", "/api/definition-draft", "{}", "\"ws-0\"").status());
        assertEquals(before, storedDocument());
        APIResponse csrfDenied = workbench.page().request().post(base() + "/api/definition-draft",
                RequestOptions.create().setHeader("Origin", base()).setHeader("If-Match", "\"ws-7\"").setData("{}"));
        assertEquals(403, csrfDenied.status()); assertEquals(before, storedDocument());
        APIResponse originDenied = workbench.page().request().post(base() + "/api/definition-draft",
                RequestOptions.create().setHeader("Origin", "https://outside.example").setData("{}"));
        assertEquals(403, originDenied.status()); assertEquals(before, storedDocument());
        APIResponse hostDenied = workbench.page().request().get(base() + "/api/workspace", RequestOptions.create().setHeader("Host", "outside.example"));
        assertEquals(403, hostDenied.status()); assertFalse(hostDenied.text().contains("definitionDraft"));
        assertEquals(before, storedDocument());
    }

    @Test
    void uc1StorageFailureAndStaleConfirmationAreObservedInTheBrowser() throws Exception {
        workbench.open().awaitText("Create school"); String before = storedDocument();
        withRejectedWorkspaceUpdates("NEW.lifecycle_state = 'DEFINITION_DRAFT'", () -> {
            openInBrowser(); workbench.awaitText("No draft was opened"); assertEquals(before, storedDocument());
            assertEquals("EMPTY", storedLifecycle());
        });
        workbench.page().locator("#open-authoring").click(); workbench.awaitText("Open school-definition authoring");
        jdbc.sql("UPDATE workspace_aggregate SET version=version+1 WHERE workspace_id=1").update();
        String changed = storedDocument(); workbench.page().locator("#confirm-authoring").click();
        workbench.awaitText("No draft was opened"); assertEquals(changed, storedDocument());
        workbench.reload(); workbench.awaitText("Create school"); openInBrowser(); workbench.awaitText("Saved school-definition draft");
    }

    private String timetableRevision(JsonNode result, String revision) throws Exception {
        ObjectNode scope = JSON.createObjectNode().put("schemaVersion", 1)
                .put("schoolId", result.path("schoolId").stringValue()).put("inputRevision", revision);
        var ordered = new java.util.ArrayList<JsonNode>();
        result.path("timetable").path("assignments").forEach(ordered::add);
        ordered.sort(java.util.Comparator.comparing(item -> item.path("lessonId").stringValue()));
        ordered.forEach(scope.putArray("assignments")::add);
        return "sha256:" + java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(CanonicalJson.bytes(scope)));
    }

    private void openInBrowser() {
        workbench.page().locator("#open-authoring").click(); workbench.awaitText("Open school-definition authoring");
        workbench.page().locator("#confirm-authoring").click();
    }
    private String base() { return "http://localhost:" + port; }
    private APIResponse command(String method, String path, String body, String version) {
        JsonNode csrf = JSON.readTree(workbench.page().request().get(base() + "/api/csrf").text());
        RequestOptions options = RequestOptions.create().setMethod(method).setData(body).setHeader("Content-Type", "application/json")
                .setHeader("Origin", base()).setHeader("If-Match", version == null ? "\"ws-" + storedVersion() + "\"" : version)
                .setHeader(csrf.path("headerName").stringValue(), csrf.path("token").stringValue());
        return workbench.page().request().fetch(base() + path, options);
    }
    private void store(String state, ObjectNode document) {
        jdbc.sql("UPDATE workspace_aggregate SET lifecycle_state=:state, version=7, active_run_id=NULL, document=CAST(:document AS jsonb) WHERE workspace_id=1")
                .param("state", state).param("document", JSON.writeValueAsString(document)).update();
    }
}
