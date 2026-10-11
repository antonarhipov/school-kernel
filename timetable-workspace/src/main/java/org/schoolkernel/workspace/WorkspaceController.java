package org.schoolkernel.workspace;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Controller
public class WorkspaceController {
    private final WorkspaceRepository repository;
    private final ImportService imports;
    private final InitialPlanningService planning;
    private final RepairPlanningService repairPlanning;
    private final RepairDraftService repairs;
    private final RepairProposalService repairProposals;
    private final ProposalReviewService proposalReviews;
    private final AcceptedBaselineExportService exports;
    private final ManualDraftService manualDrafts;
    private final DefinitionDraftService definitionDrafts;
    private final DefinitionGridService definitionGrid;
    private final DefinitionEntityService definitionEntities;
    private final DefinitionRequirementService definitionRequirements;
    private final DefinitionOccurrenceService definitionOccurrences;
    private final DefinitionPreferenceService definitionPreferences;
    private final DefinitionCatalogService definitionCatalog;
    private final DefinitionRemovalService definitionRemoval;
    private final DefinitionRestorationService definitionRestoration;
    private final ObjectMapper json;

    public WorkspaceController(
            WorkspaceRepository repository,
            ImportService imports,
            InitialPlanningService planning,
            RepairPlanningService repairPlanning,
            RepairDraftService repairs,
            RepairProposalService repairProposals,
            ProposalReviewService proposalReviews,
            AcceptedBaselineExportService exports,
            ManualDraftService manualDrafts,
            DefinitionDraftService definitionDrafts,
            DefinitionGridService definitionGrid,
            DefinitionEntityService definitionEntities,
            DefinitionRequirementService definitionRequirements,
            DefinitionOccurrenceService definitionOccurrences,
            DefinitionPreferenceService definitionPreferences,
            DefinitionCatalogService definitionCatalog,
            DefinitionRemovalService definitionRemoval,
            DefinitionRestorationService definitionRestoration,
            ObjectMapper json) {
        this.repository = repository;
        this.imports = imports;
        this.planning = planning;
        this.repairPlanning = repairPlanning;
        this.repairs = repairs;
        this.repairProposals = repairProposals;
        this.proposalReviews = proposalReviews;
        this.exports = exports;
        this.manualDrafts = manualDrafts;
        this.definitionDrafts = definitionDrafts;
        this.definitionGrid = definitionGrid;
        this.definitionEntities = definitionEntities;
        this.definitionRequirements = definitionRequirements;
        this.definitionOccurrences = definitionOccurrences;
        this.definitionPreferences = definitionPreferences;
        this.definitionCatalog = definitionCatalog;
        this.definitionRemoval = definitionRemoval;
        this.definitionRestoration = definitionRestoration;
        this.json = json;
    }

    @PostMapping("/api/definition-draft/preview")
    @ResponseBody
    public ResponseEntity<JsonNode> previewDefinitionDraft(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionDrafts.preview(ifMatch));
    }

    @PostMapping("/api/definition-draft")
    @ResponseBody
    public ResponseEntity<JsonNode> openDefinitionDraft(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody(required = false) JsonNode request) {
        return response(definitionDrafts.open(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/school")
    @ResponseBody
    public ResponseEntity<JsonNode> saveSchoolIdentity(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(definitionDrafts.saveSchool(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/grid/preview")
    @ResponseBody
    public ResponseEntity<JsonNode> previewGrid(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionGrid.preview(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/grid")
    @ResponseBody
    public ResponseEntity<JsonNode> saveGrid(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(definitionGrid.save(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/occurrences/preview")
    @ResponseBody
    public ResponseEntity<JsonNode> previewOccurrences(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionOccurrences.preview(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/occurrences")
    @ResponseBody
    public ResponseEntity<JsonNode> saveOccurrences(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(definitionOccurrences.save(ifMatch, request));
    }

    @GetMapping("/api/definition-draft/requirements")
    @ResponseBody
    public ResponseEntity<JsonNode> definitionRequirements() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionRequirements.view());
    }

    @PostMapping("/api/definition-draft/requirements/preview")
    @ResponseBody
    public ResponseEntity<JsonNode> previewRequirement(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionRequirements.preview(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/requirements")
    @ResponseBody
    public ResponseEntity<JsonNode> saveRequirement(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(definitionRequirements.save(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/restoration/inspect")
    @ResponseBody
    public ResponseEntity<JsonNode> inspectRestoration(@RequestHeader("If-Match") String ifMatch) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionRestoration.inspect(ifMatch));
    }
    @PostMapping("/api/definition-draft/restoration/preview")
    @ResponseBody
    public ResponseEntity<JsonNode> previewRestoration(@RequestHeader("If-Match") String ifMatch, @RequestBody JsonNode request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionRestoration.preview(ifMatch,request));
    }
    @PostMapping("/api/definition-draft/restoration")
    @ResponseBody
    public ResponseEntity<JsonNode> saveRestoration(@RequestHeader("If-Match") String ifMatch, @RequestBody JsonNode request) {
        return response(definitionRestoration.save(ifMatch,request));
    }
    @PostMapping("/api/definition-draft/removal/inspect")
    @ResponseBody
    public ResponseEntity<JsonNode> inspectRemoval(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch, @RequestBody JsonNode target) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionRemoval.inspect(ifMatch,target));
    }
    @PostMapping("/api/definition-draft/removal/preview")
    @ResponseBody
    public ResponseEntity<JsonNode> previewRemoval(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch, @RequestBody JsonNode request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionRemoval.preview(ifMatch,request));
    }
    @PostMapping("/api/definition-draft/removal")
    @ResponseBody
    public ResponseEntity<JsonNode> saveRemoval(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch, @RequestBody JsonNode request) {
        return response(definitionRemoval.save(ifMatch,request));
    }

    @PostMapping("/api/definition-draft/catalog/preview")
    @ResponseBody
    public ResponseEntity<JsonNode> previewCatalog(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(definitionCatalog.preview(ifMatch,request));
    }

    @PostMapping("/api/definition-draft/catalog")
    @ResponseBody
    public ResponseEntity<JsonNode> saveCatalog(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(definitionCatalog.save(ifMatch,request));
    }

    @PostMapping("/api/definition-draft/preferences")
    @ResponseBody
    public ResponseEntity<JsonNode> saveDefinitionPreference(@RequestHeader(value=HttpHeaders.IF_MATCH,required=false)String ifMatch,@RequestBody JsonNode request){return response(definitionPreferences.save(ifMatch,request));}

    @PostMapping("/api/definition-draft/room-policies")
    @ResponseBody
    public ResponseEntity<JsonNode> saveDefinitionRoomPolicy(@RequestHeader(value=HttpHeaders.IF_MATCH,required=false)String ifMatch,@RequestBody JsonNode request){return response(definitionEntities.saveRoomPolicy(ifMatch,request));}

    @PostMapping("/api/definition-draft/rooms")
    @ResponseBody
    public ResponseEntity<JsonNode> saveDefinitionRoom(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody(required = false) JsonNode request) {
        return response(definitionEntities.saveRoom(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/cohorts")
    @ResponseBody
    public ResponseEntity<JsonNode> saveDefinitionCohort(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody(required = false) JsonNode request) {
        return response(definitionEntities.saveCohort(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/teachers")
    @ResponseBody
    public ResponseEntity<JsonNode> saveDefinitionTeacher(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody(required = false) JsonNode request) {
        return response(definitionEntities.saveTeacher(ifMatch, request));
    }

    @PostMapping("/api/definition-draft/subjects")
    @ResponseBody
    public ResponseEntity<JsonNode> saveSubject(@RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(definitionEntities.saveSubject(ifMatch, request));
    }

    @PostMapping("/api/repair-draft")
    @ResponseBody
    public ResponseEntity<JsonNode> startRepair(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(repairs.start(ifMatch, request));
    }

    @PatchMapping("/api/repair-draft")
    @ResponseBody
    public ResponseEntity<JsonNode> updateRepair(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = "Prefer", required = false) String prefer,
            @RequestBody JsonNode request) {
        if ("return=minimal".equals(prefer)
                && request.path("action").isTextual()
                && java.util.Set.of("PIN", "UNPIN").contains(request.path("action").stringValue())) {
            RepairDraftService.MinimalUpdate updated = repairs.updateMinimalPin(ifMatch, request);
            ObjectNode body = json.createObjectNode();
            body.set("repairDraft", updated.repairDraft());
            return ResponseEntity.ok()
                    .eTag(updated.etag())
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(body);
        }
        return response(repairs.update(ifMatch, request));
    }

    @DeleteMapping("/api/repair-draft")
    @ResponseBody
    public ResponseEntity<JsonNode> discardRepair(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(repairs.discard(ifMatch, request));
    }

    @PostMapping("/api/repair-draft/bulk-pin-preview")
    @ResponseBody
    public ResponseEntity<JsonNode> previewBulkPins(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        RepairDraftService.BulkPinPreview preview = repairs.preview(ifMatch, request);
        return ResponseEntity.ok().eTag(preview.etag()).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(preview.document());
    }

    @PostMapping("/api/manual-draft")
    @ResponseBody
    public ResponseEntity<JsonNode> startManualDraft(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(manualDrafts.start(ifMatch));
    }

    @PatchMapping("/api/manual-draft")
    @ResponseBody
    public ResponseEntity<JsonNode> mutateManualDraft(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode request) {
        return response(manualDrafts.mutateDraft(ifMatch, request));
    }

    @DeleteMapping("/api/manual-draft")
    @ResponseBody
    public ResponseEntity<JsonNode> discardManualDraft(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody(required = false) JsonNode request) {
        return response(manualDrafts.discard(ifMatch, request));
    }

    @PostMapping("/api/manual-draft/publish")
    @ResponseBody
    public ResponseEntity<JsonNode> publishManualDraft(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(manualDrafts.publish(ifMatch));
    }

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(302).location(URI.create("/workspace/")).build();
    }

    @GetMapping("/workspace/")
    public String workspaceIndex() {
        return "forward:/workspace/index.html";
    }

    @GetMapping("/api/csrf")
    @ResponseBody
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/api/workspace")
    @ResponseBody
    public ResponseEntity<JsonNode> workspace() {
        return response(repository.load());
    }

    @PostMapping("/api/workspace/clear")
    @ResponseBody
    public ResponseEntity<JsonNode> clearWorkspace(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(imports.clearWorkspace(ifMatch));
    }

    @DeleteMapping("/api/workspace")
    @ResponseBody
    public ResponseEntity<JsonNode> deleteWorkspace(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(imports.clearWorkspace(ifMatch));
    }

    @PostMapping(path = "/api/workspace/upload-definition", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<JsonNode> uploadNewDefinition(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestParam(value = "definition", required = false) MultipartFile definition,
            @RequestParam(value = "result", required = false) MultipartFile result,
            @RequestParam(value = "archive", required = false) MultipartFile archive) {
        return response(imports.resetAndUploadDefinition(ifMatch, definition, result, archive));
    }

    @GetMapping("/api/accepted/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportAcceptedBaseline() {
        AcceptedBaselineExportService.ExportedBaseline exported = exports.export();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(exported.archive().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("accepted-baseline.zip").build().toString())
                .header(HttpHeaders.ETAG, exported.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(exported.archive());
    }

    @PostMapping(path = "/api/import", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<JsonNode> importSchool(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestParam(value = "definition", required = false) MultipartFile definition,
            @RequestParam(value = "result", required = false) MultipartFile result,
            @RequestParam(value = "archive", required = false) MultipartFile archive) {
        return response(imports.importSchool(ifMatch, definition, result, archive));
    }

    @PostMapping(path = "/api/initial-draft/replace", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<JsonNode> replaceInitialDraft(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestParam("definition") MultipartFile definition) {
        return response(imports.replaceInitial(ifMatch, definition));
    }

    @PostMapping("/api/runs")
    @ResponseBody
    public ResponseEntity<JsonNode> startRun(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody(required = false) JsonNode request) {
        WorkspaceAggregate current = repository.load();
        WorkspaceAggregate started = current.state() == WorkspaceState.REPAIR_DRAFT
                ? repairPlanning.start(ifMatch, request)
                : planning.start(ifMatch);
        return ResponseEntity.accepted()
                .eTag(started.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(snapshot(started));
    }

    @GetMapping("/api/runs/{runId}")
    @ResponseBody
    public ResponseEntity<JsonNode> run(@PathVariable UUID runId) {
        WorkspaceAggregate current = repository.load();
        return ResponseEntity.ok()
                .eTag(current.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(isRepairRun(current, runId) ? repairPlanning.run(runId) : planning.run(runId));
    }

    @DeleteMapping("/api/runs/{runId}")
    @ResponseBody
    public ResponseEntity<JsonNode> cancelRun(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @PathVariable UUID runId) {
        WorkspaceAggregate current = repository.load();
        return response(current.state() == WorkspaceState.SOLVING_REPAIR
                ? repairPlanning.cancel(ifMatch, runId)
                : planning.cancel(ifMatch, runId));
    }

    @GetMapping("/api/proposal")
    @ResponseBody
    public ResponseEntity<JsonNode> proposal() {
        WorkspaceAggregate current = repository.load();
        if (current.state() != WorkspaceState.INITIAL_PROPOSAL
                && current.state() != WorkspaceState.REPAIR_PROPOSAL) {
            throw new WorkspaceProblem(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "PROPOSAL_NOT_FOUND",
                    "There is no current proposal.");
        }
        return ResponseEntity.ok()
                .eTag(current.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(current.document().path("proposal"));
    }

    @PostMapping("/api/proposal/accept")
    @ResponseBody
    public ResponseEntity<JsonNode> acceptProposal(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(repository.load().state() == WorkspaceState.REPAIR_PROPOSAL
                ? repairProposals.accept(ifMatch) : planning.accept(ifMatch));
    }

    @DeleteMapping("/api/proposal")
    @ResponseBody
    public ResponseEntity<JsonNode> discardProposal(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(repository.load().state() == WorkspaceState.REPAIR_PROPOSAL
                ? repairProposals.discard(ifMatch) : planning.discard(ifMatch));
    }

    private ResponseEntity<JsonNode> response(WorkspaceAggregate aggregate) {
        return ResponseEntity.ok()
                .eTag(aggregate.etag())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(snapshot(aggregate));
    }

    private ObjectNode snapshot(WorkspaceAggregate aggregate) {
        ObjectNode body = json.createObjectNode();
        body.put("state", aggregate.state().name());
        body.put("version", aggregate.version());
        body.put("acceptedTimetable", switch (aggregate.state()) {
            case ACCEPTED_BASELINE, REPAIR_DRAFT, SOLVING_REPAIR, REPAIR_PROPOSAL, MANUAL_DRAFT -> true;
            case DEFINITION_DRAFT -> aggregate.document().path("acceptedBaseline").isObject();
            default -> false;
        });
        ObjectNode workspace = (ObjectNode) aggregate.document().deepCopy();
        if (aggregate.state() == WorkspaceState.REPAIR_PROPOSAL
                && workspace.path("proposal") instanceof ObjectNode proposal
                && proposal.path("review").isMissingNode()) {
            proposal.set("review", proposalReviews.create(
                    workspace.path("acceptedBaseline"),
                    workspace.path("repairDraft"),
                    proposal.path("definition"),
                    proposal.path("result")));
        }
        body.set("workspace", workspace);
        return body;
    }

    private static boolean isRepairRun(WorkspaceAggregate current, UUID runId) {
        for (String field : java.util.List.of("run", "lastRun", "proposal")) {
            JsonNode candidate = current.document().path(field);
            String idField = "proposal".equals(field) ? "runId" : "id";
            if (candidate.path(idField).isTextual()
                    && candidate.path("kind").isTextual()
                    && runId.toString().equals(candidate.path(idField).stringValue())
                    && "REPAIR".equals(candidate.path("kind").stringValue())) return true;
        }
        return false;
    }
}
