package org.schoolkernel.workspace;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Opening is one compare-and-swap write. Source data and accepted provenance are never transformed. */
@Service
public class DefinitionDraftService {
    private final WorkspaceStore repository;
    private final ObjectMapper json;
    private final Schema schema;
    private final ManifestService manifests;
    private final KernelVerifier verifier;

    public DefinitionDraftService(WorkspaceStore repository, ObjectMapper json, ManifestService manifests, KernelVerifier verifier) {
        this.repository = repository;
        this.json = json;
        this.manifests = manifests;
        this.verifier = verifier;
        try (var input = getClass().getResourceAsStream("/schema/school-definition-v1.schema.json")) {
            if (input == null) throw new IllegalStateException("Definition schema missing");
            schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(input);
            schema.initializeValidators();
        } catch (IOException exception) {
            throw new IllegalStateException("Definition schema could not be loaded", exception);
        }
    }

    public ObjectNode preview(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        ImportService.requireMatchingVersion(ifMatch, current);
        requireSource(current);
        ObjectNode preview = json.createObjectNode();
        preview.put("sourceState", current.state().name());
        preview.put("upgradeRequired", false);
        ArrayNode adjustments = preview.putArray("adjustments");
        preview.putArray("expiredPins");
        if (current.state() == WorkspaceState.DEFINITION_DRAFT) return preview;
        if (current.state() != WorkspaceState.EMPTY) {
            ObjectNode definition = sourceDefinition(current);
            validateSource(definition);
            preview.put("sourceName", definition.path("displayName").stringValue());
            preview.put("sourceCatalog", definition.path("catalogVersion").intValue());
            if (current.state() == WorkspaceState.ACCEPTED_BASELINE) {
                JsonNode accepted = current.document().path("acceptedBaseline");
                validateRevision(accepted.path("result").path("inputRevision"));
                try {
                    manifests.validatedOrGenerated(new ImportDocuments(definition, accepted.path("result"),
                            accepted.has("manifest") ? accepted.path("manifest") : null,
                            ImportDocuments.ImportMode.ACCEPTED_BASELINE));
                } catch (WorkspaceProblem problem) {
                    throw unsupported("acceptedBaseline.manifest", problem.getMessage());
                }
                int catalog = definition.path("catalogVersion").intValue();
                preview.put("upgradeRequired", catalog < 8);
                preview.put("targetCatalog", Math.max(8, catalog));
                upgrade(definition, adjustments);
                expirePins(definition, current.document().path("acceptedBaseline").path("manifest"),
                        (ArrayNode) preview.path("expiredPins"));
            }
        }
        return preview;
    }

    @Transactional
    public WorkspaceAggregate open(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = repository.load();
        long version = ImportService.requireMatchingVersion(ifMatch, current);
        ObjectNode preview = preview(ifMatch);
        if (current.state() == WorkspaceState.DEFINITION_DRAFT) return current;
        if (preview.path("upgradeRequired").booleanValue()
                && (request == null || !request.path("confirmUpgrade").isBoolean()
                || !request.path("confirmUpgrade").booleanValue())) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "CATALOG_UPGRADE_CONFIRMATION_REQUIRED",
                    "Authoring this accepted source requires a confirmed upgrade to catalog 8. No draft was opened.");
        }
        ObjectNode document = ((ObjectNode) current.document()).deepCopy();
        ObjectNode draft = document.putObject("definitionDraft");
        draft.put("sourceState", current.state().name());
        draft.put("sourceVersion", version);
        draft.set("sourceDocument", current.document().deepCopy());
        ObjectNode definition;
        if (current.state() == WorkspaceState.EMPTY) {
            definition = json.createObjectNode().put("schemaVersion", 1).put("catalogVersion", 9);
            for (String collection : List.of("subjects", "teachers", "cohorts", "rooms", "periods", "lessons")) {
                definition.putArray(collection);
            }
        } else {
            definition = sourceDefinition(current);
            if (current.state() == WorkspaceState.ACCEPTED_BASELINE) {
                definition.put("basedOnRevision", current.document().path("acceptedBaseline")
                        .path("result").path("inputRevision").stringValue());
                upgrade(definition, json.createArrayNode());
                expirePins(definition, current.document().path("acceptedBaseline").path("manifest"), json.createArrayNode());
            } else definition.remove("basedOnRevision");
        }
        draft.set("definition", definition);
        draft.set("openingAdjustments", preview.path("adjustments"));
        draft.set("expiredPins", preview.path("expiredPins"));
        draft.set("editingContext", current.document().has("authoringContext")
                ? current.document().path("authoringContext").deepCopy() : json.createObjectNode());
        draft.putArray("removedIdentities");
        ArrayNode issues = draft.putArray("issues");
        if (current.state() == WorkspaceState.EMPTY) {
            for (String field : List.of("displayName", "schoolId")) {
                issues.addObject().put("severity", "BLOCKING").put("ownerType", "School").put("ownerId", "")
                        .put("field", field).put("message", "Enter the school " + (field.equals("schoolId") ? "identifier." : "name."))
                        .putArray("relatedIds");
            }
        }
        draft.put("draftRevision", revision(definition));
        draft.put("openedRevision", revision(definition));
        draft.put("entrySection", current.state() == WorkspaceState.EMPTY ? "School" : "Review");
        if (repository.replace(version, current.state(), WorkspaceState.DEFINITION_DRAFT, document).isEmpty()) {
            throw new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED, "STALE_WORKSPACE_VERSION",
                    "The workspace changed. No draft was opened; reload before retrying.");
        }
        return repository.load();
    }

    /** UC-19: restore the recorded source verbatim, including old catalog and provenance. */
    @Transactional
    public WorkspaceAggregate discard(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = repository.load();
        long version = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.document().path("repairDraft").has("retainedAuthoring")) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "SEEDED_REPAIR_ACTIVE",
                    "Return to authoring first: cancel an active solve or discard only its proposal, then explicitly abandon repair staging and pins. Discard authoring is a separate confirmation.");
        }
        if (current.state() != WorkspaceState.DEFINITION_DRAFT) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION",
                    "Discard authoring requires DEFINITION_DRAFT; " + current.state() + " is active. No change was committed.");
        }
        if (!(request instanceof ObjectNode fields) || fields.size() != 1
                || !fields.path("confirmed").isBoolean() || !fields.path("confirmed").booleanValue()) {
            throw new WorkspaceProblem(HttpStatus.BAD_REQUEST, "DISCARD_CONFIRMATION_REQUIRED",
                    "Explicitly confirm whole-authoring discard. Saved work and source are unchanged.");
        }
        JsonNode draft = current.document().path("definitionDraft");
        WorkspaceState sourceState;
        try { sourceState = WorkspaceState.valueOf(draft.path("sourceState").asString("")); }
        catch (IllegalArgumentException exception) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_AUTHORING_SOURCE", "Recorded source state is unavailable. Nothing was discarded.");
        }
        if (!List.of(WorkspaceState.EMPTY, WorkspaceState.INITIAL_DRAFT, WorkspaceState.ACCEPTED_BASELINE).contains(sourceState)) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_AUTHORING_SOURCE", "Recorded authoring source is unavailable. Nothing was discarded.");
        }
        ObjectNode source = discardSource(current, draft, sourceState);
        if (repository.replace(version, WorkspaceState.DEFINITION_DRAFT, sourceState, source.deepCopy()).isEmpty()) {
            throw new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED, "STALE_WORKSPACE_VERSION",
                    "The workspace changed. Nothing was discarded; review the newer saved draft before retrying.");
        }
        return repository.load();
    }

    /** Older drafts retained the unchanged source alongside the draft, identified by its revision and catalog. */
    private ObjectNode discardSource(WorkspaceAggregate current, JsonNode draft, WorkspaceState sourceState) {
        if (draft.path("sourceDocument") instanceof ObjectNode source) return source.deepCopy();
        JsonNode document = current.document();
        JsonNode definition = sourceState == WorkspaceState.ACCEPTED_BASELINE
                ? document.path("acceptedBaseline").path("definition") : document.path("initialDefinition");
        String recordedRevision = draft.path("sourceDefinitionRevision").asString("");
        boolean sourceMatches = !draft.has("sourceDocument")
                && List.of(WorkspaceState.INITIAL_DRAFT, WorkspaceState.ACCEPTED_BASELINE).contains(sourceState)
                && definition.isObject()
                && recordedRevision.matches("sha256:[0-9a-f]{64}")
                && recordedRevision.equals(document.path("definitionRevision").asString(""))
                && draft.path("sourceCatalogVersion").isIntegralNumber()
                && draft.path("sourceCatalogVersion").equals(definition.path("catalogVersion"))
                && (sourceState != WorkspaceState.ACCEPTED_BASELINE
                    || recordedRevision.equals(document.path("acceptedBaseline").path("result").path("inputRevision").asString("")))
                && (!document.has("school")
                    || document.path("school").path("id").equals(definition.path("schoolId"))
                        && document.path("school").path("displayName").equals(definition.path("displayName")))
                && !document.has("manualDraft") && !document.has("repairDraft")
                && !document.has("proposal") && !document.has("run");
        if (!sourceMatches) {
            throw invalidDiscardSource();
        }
        // Kernel source revisions normalize entity/set order; authoring JSON revisions intentionally do not.
        KernelVerifier.Verification verified;
        try {
            verified = verifier.verify(new ImportDocuments(definition,
                    sourceState == WorkspaceState.ACCEPTED_BASELINE ? document.path("acceptedBaseline").path("result") : null,
                    null, sourceState == WorkspaceState.ACCEPTED_BASELINE
                        ? ImportDocuments.ImportMode.ACCEPTED_BASELINE : ImportDocuments.ImportMode.INITIAL_DEFINITION));
        } catch (WorkspaceProblem problem) {
            if (problem.status() == HttpStatus.UNPROCESSABLE_ENTITY) throw invalidDiscardSource();
            throw problem;
        }
        if (!recordedRevision.equals(verified.definitionRevision())) throw invalidDiscardSource();
        ObjectNode source = ((ObjectNode) document).deepCopy();
        source.remove("definitionDraft");
        return source;
    }

    private static WorkspaceProblem invalidDiscardSource() {
        return new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_AUTHORING_SOURCE",
                "Recorded authoring source is unavailable or no longer matches its saved revision. Nothing was discarded.");
    }

    /** UC-2: change identity fields only, never the recorded source or Current. */
    @Transactional
    public WorkspaceAggregate saveSchool(String ifMatch, JsonNode request) {
        WorkspaceAggregate current = repository.load();
        long version = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() != WorkspaceState.DEFINITION_DRAFT) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION",
                    "School identity can only be saved in DEFINITION_DRAFT; " + current.state() + " is active. No change was committed.");
        }
        if (!(request instanceof ObjectNode fields) || fields.size() != 2
                || !fields.path("schoolId").isTextual() || !fields.path("displayName").isTextual()) {
            throw new WorkspaceProblem(HttpStatus.BAD_REQUEST, "INVALID_SCHOOL_FIELDS",
                    "Supply only schoolId and displayName as text. Schema, revisions and catalog are system-managed. No change was committed.");
        }
        ObjectNode document = ((ObjectNode) current.document()).deepCopy();
        ObjectNode draft = (ObjectNode) document.path("definitionDraft");
        ObjectNode definition = (ObjectNode) draft.path("definition");
        String id = fields.path("schoolId").stringValue();
        if (definition.has("schoolId") && !definition.path("schoolId").stringValue().equals(id)) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "SCHOOL_ID_IMMUTABLE",
                    "The established school identifier is read-only. No change was committed.");
        }
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_SCHOOL_ID",
                    "Use 1–128 ASCII letters, digits, dots, underscores or hyphens, starting with a letter or digit. No change was committed.");
        }
        if (!definition.has("schoolId")) {
            for (JsonNode reserved : draft.path("removedIdentities")) {
                if ("School".equals(reserved.path("ownerType").stringValue())
                        && id.equals(reserved.path("id").stringValue())) {
                    throw new WorkspaceProblem(HttpStatus.CONFLICT, "SCHOOL_ID_RESERVED",
                            "That school identifier is reserved in this authoring lineage. No change was committed.");
                }
            }
        }
        definition.put("schoolId", id).put("displayName", fields.path("displayName").stringValue());
        ArrayNode issues = json.createArrayNode();
        for (JsonNode issue : draft.path("issues")) {
            if (!("School".equals(issue.path("ownerType").stringValue())
                    && List.of("schoolId", "displayName").contains(issue.path("field").stringValue()))) issues.add(issue);
        }
        if (definition.path("displayName").stringValue().isBlank()) {
            issues.addObject().put("severity", "BLOCKING").put("ownerType", "School").put("ownerId", id)
                    .put("field", "displayName").put("message", "Enter a nonblank school name.").putArray("relatedIds");
        }
        draft.set("issues", issues);
        draft.put("draftRevision", revision(definition));
        if (repository.replace(version, WorkspaceState.DEFINITION_DRAFT, WorkspaceState.DEFINITION_DRAFT, document).isEmpty()) {
            throw new WorkspaceProblem(HttpStatus.PRECONDITION_FAILED, "STALE_WORKSPACE_VERSION",
                    "The workspace changed. No change was committed; review the newer saved draft before retrying.");
        }
        return repository.load();
    }

    static void requireNotAuthoring(WorkspaceAggregate current) {
        if (current.state() == WorkspaceState.DEFINITION_DRAFT) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "DEFINITION_DRAFT_ACTIVE",
                    "A school-definition draft is open. Resume authoring and finish or discard it first.");
        }
    }

    private static void requireSource(WorkspaceAggregate current) {
        if (!List.of(WorkspaceState.EMPTY, WorkspaceState.INITIAL_DRAFT, WorkspaceState.ACCEPTED_BASELINE,
                WorkspaceState.DEFINITION_DRAFT).contains(current.state())) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION",
                    "Authoring cannot open while " + current.state() + " is active. Finish or abandon that workflow first.");
        }
    }

    private static ObjectNode sourceDefinition(WorkspaceAggregate current) {
        JsonNode definition = current.state() == WorkspaceState.INITIAL_DRAFT
                ? current.document().path("initialDefinition") : current.document().path("acceptedBaseline").path("definition");
        if (!(definition instanceof ObjectNode object)) throw unsupported("definition", "Expected a complete source definition.");
        return object.deepCopy();
    }

    private void validateSource(ObjectNode definition) {
        var errors = schema.validate(definition);
        if (!errors.isEmpty()) {
            var error = errors.stream().sorted(java.util.Comparator.comparing(e -> e.getMessage())).findFirst().orElseThrow();
            throw unsupported(error.getInstanceLocation().toString(), error.getMessage());
        }
    }

    private static void validateRevision(JsonNode revision) {
        if (!revision.isTextual() || !revision.stringValue().matches("sha256:[0-9a-f]{64}")) {
            throw unsupported("acceptedBaseline.result.inputRevision", "Unsupported accepted input revision.");
        }
    }

    static void upgrade(ObjectNode definition, ArrayNode adjustments) {
        upgrade(definition, 8, adjustments);
    }

    static void upgrade(ObjectNode definition, int target, ArrayNode adjustments) {
        int source = definition.path("catalogVersion").intValue();
        if (source >= target) return;
        for (String id : List.of("soft.cohort-gap", "soft.cohort-week-balance", "soft.cohort-late-start")) {
            int since = id.equals("soft.cohort-late-start") ? 3 : 2;
            if (source >= since) continue;
            boolean explicit = false;
            for (JsonNode override : definition.path("softConstraintOverrides")) {
                if (id.equals(override.path("constraintId").stringValue())) explicit = true;
            }
            if (!explicit) {
                definition.withArray("softConstraintOverrides").addObject().put("constraintId", id).put("weight", 0);
                adjustments.add(id + " = 0 (preserve previously inactive preference)");
            }
        }
        if (source < 5) for (JsonNode cohort : definition.path("cohorts")) {
            if (!cohort.has("maxDailyGaps")) {
                ((ObjectNode) cohort).put("maxDailyGaps", Integer.MAX_VALUE);
                adjustments.add("Class " + cohort.path("id").stringValue() + ": maxDailyGaps = No limit (preserve unrestricted gaps)");
            }
        }
        definition.put("catalogVersion", target);
    }

    private static void expirePins(ObjectNode definition, JsonNode manifest, ArrayNode expired) {
        for (JsonNode lock : manifest.path("locks")) for (JsonNode lesson : definition.path("lessons")) {
            if (!lesson.path("id").equals(lock.path("lessonId"))) continue;
            for (String field : List.of("periodLock", "roomLock")) {
                if ("ATTEMPT_SCOPED".equals(lock.path(field + "Origin").asString("")) && lesson.has(field)) {
                    expired.addObject().put("lessonId", lesson.path("id").stringValue()).put("field", field)
                            .set("sourceValue", lesson.path(field).deepCopy());
                    ((ObjectNode) lesson).remove(field);
                }
            }
        }
    }

    static String revision(JsonNode definition) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(CanonicalJson.bytes(definition)));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private static WorkspaceProblem unsupported(String location, String message) {
        return new WorkspaceProblem(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_DEFINITION_SOURCE",
                "Cannot open authoring at " + location + ": " + message + " Source unchanged.");
    }
}
