package org.schoolkernel.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ManifestServiceTest {
    private final ObjectMapper json = JsonMapper.builder().build();
    private final ManifestService manifests = new ManifestService();

    @Test
    @DisplayName("UC-1 G3: raw accepted pair treats every imported lock as persistent policy")
    void rawPairCreatesPersistentLockManifest() throws Exception {
        var definition = json.readTree("""
                {"schemaVersion":1,"catalogVersion":1,"schoolId":"school","lessons":[
                  {"id":"a","periodLock":"p"},{"id":"b","roomLock":"r"}
                ]}
                """);
        var result = json.readTree("""
                {"schemaVersion":1,"inputRevision":"sha256:%s","timetableRevision":"sha256:%s"}
                """.formatted("1".repeat(64), "2".repeat(64)));

        var manifest = manifests.validatedOrGenerated(new ImportDocuments(
                definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));

        assertEquals("PERSISTENT_POLICY", manifest.path("locks").get(0).path("periodLockOrigin").stringValue());
        assertEquals("PERSISTENT_POLICY", manifest.path("locks").get(1).path("roomLockOrigin").stringValue());
    }

    @Test
    @DisplayName("UC-1 extension 2a: manifest lineage mismatch rejects the whole archive")
    void mismatchedManifestIsRejected() throws Exception {
        var definition = json.readTree("""
                {"schemaVersion":1,"catalogVersion":1,"schoolId":"school","lessons":[]}
                """);
        var result = json.readTree("""
                {"schemaVersion":1,"inputRevision":"sha256:%s","timetableRevision":"sha256:%s"}
                """.formatted("1".repeat(64), "2".repeat(64)));
        var manifest = json.readTree("""
                {"manifestVersion":1,"definitionSchemaVersion":1,"resultSchemaVersion":1,"catalogVersion":1,
                 "schoolId":"other","inputRevision":"sha256:%s","timetableRevision":"sha256:%s","locks":[]}
                """.formatted("1".repeat(64), "2".repeat(64)));

        WorkspaceProblem problem = assertThrows(WorkspaceProblem.class, () -> manifests.validatedOrGenerated(
                new ImportDocuments(definition, result, manifest, ImportDocuments.ImportMode.ACCEPTED_BASELINE)));

        assertEquals("INVALID_WORKSPACE_MANIFEST", problem.code());
    }

    @Test
    @DisplayName("UC-7 step 7/G4: a later repair can remove a prior attempt-scoped lock dimension")
    void laterRepairDropsPriorAttemptScopedLockWithoutManifestFailure() throws Exception {
        var definition = json.readTree("""
                {"schemaVersion":1,"catalogVersion":1,"schoolId":"school","lessons":[{"id":"a"}]}
                """);
        var result = json.readTree("""
                {"schemaVersion":1,"inputRevision":"sha256:%s","timetableRevision":"sha256:%s"}
                """.formatted("1".repeat(64), "2".repeat(64)));
        var previousManifest = json.readTree("""
                {"locks":[{"lessonId":"a","roomLockOrigin":"ATTEMPT_SCOPED"}]}
                """);
        var repairDraft = json.readTree("""
                {"intent":{"pins":[]}}
                """);

        var manifest = manifests.forAcceptedRepair(definition, result, previousManifest, repairDraft);

        assertTrue(manifest.path("locks").isEmpty());
        assertEquals("sha256:" + "1".repeat(64), manifest.path("inputRevision").stringValue());
        assertEquals("sha256:" + "2".repeat(64), manifest.path("timetableRevision").stringValue());
    }
}
