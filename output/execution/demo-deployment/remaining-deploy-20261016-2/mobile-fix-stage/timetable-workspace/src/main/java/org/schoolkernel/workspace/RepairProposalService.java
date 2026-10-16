package org.schoolkernel.workspace;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class RepairProposalService {
    @org.springframework.beans.factory.annotation.Autowired(required = false) private DemoEvents events;
    private final WorkspaceStore repository;
    private final WorkspaceMutation mutation;
    private final KernelVerifier verifier;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DemoVerificationService demoVerification;
    private final ManifestService manifests;
    private final ProposalReviewService reviews;
    private final ObjectMapper json;
    private final RepairDraftService drafts;

    public RepairProposalService(
            WorkspaceStore repository,
            WorkspaceMutation mutation,
            KernelVerifier verifier,
            ManifestService manifests,
            ProposalReviewService reviews,
            ObjectMapper json, RepairDraftService drafts) {
        this.repository = repository;
        this.mutation = mutation;
        this.verifier = verifier;
        this.manifests = manifests;
        this.reviews = reviews;
        this.json = json;
        this.drafts = drafts;
    }

    public WorkspaceAggregate discard(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireRepairProposal(current, "Only a repair proposal can be discarded.");
        ObjectNode document = copy(current.document());
        document.remove("proposal");
        return mutation.replaceRepair(
                expectedVersion, WorkspaceState.REPAIR_PROPOSAL, WorkspaceState.REPAIR_DRAFT, document);
    }

    @Transactional(noRollbackFor = WorkspaceProblem.class)
    public WorkspaceAggregate accept(String ifMatch) {
        WorkspaceAggregate current = repository.load();
        long expectedVersion = ImportService.requireMatchingVersion(ifMatch, current);
        requireRepairProposal(current, "Only a repair proposal can be accepted.");
        JsonNode proposal = current.document().path("proposal");
        JsonNode definition = proposal.path("definition");
        JsonNode result = proposal.path("result");
        if (!identityMatches(current, proposal, definition, result)) {
            invalidate(current);
            throw staleProposal();
        }

        KernelVerifier.Verification verified;
        try {
            if (RepairDraftService.seeded(current.document())) {
                JsonNode source = current.document().path("acceptedBaseline");
                verifier.verify(new ImportDocuments(source.path("definition"), source.path("result"), null,
                        ImportDocuments.ImportMode.ACCEPTED_BASELINE));
                KernelVerifier.Verification seed = verifier.verify(new ImportDocuments(
                        current.document().path("repairDraft").path("seedDefinition"), null, null,
                        ImportDocuments.ImportMode.SUCCESSOR_DEFINITION));
                if (!seed.definitionRevision().equals(current.document().path("repairDraft").path("seedDefinitionRevision").asString(""))) {
                    invalidate(current);
                    throw staleProposal();
                }
            }
            verified = verify(new ImportDocuments(
                    definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        } catch (WorkspaceProblem problem) {
            if (demoVerification == null && "KERNEL_VERIFICATION_FAILED".equals(problem.code())) {
                invalidate(current);
                throw staleProposal();
            }
            throw problem;
        }
        if (!verified.definitionRevision().equals(text(proposal.path("successorDefinitionRevision")))
                || !verified.timetableRevision().equals(text(proposal.path("proposedTimetableRevision")))) {
            invalidate(current);
            throw staleProposal();
        }

        ObjectNode acceptedDocument = json.createObjectNode();
        acceptedDocument.putObject("school").put("id", verified.schoolId())
                .put("displayName", definition.path("displayName").asString());
        if (RepairDraftService.seeded(current.document())) acceptedDocument.set("authoringContext",
                current.document().path("repairDraft").path("retainedAuthoring").path("editingContext").deepCopy());
        else if (current.document().has("authoringContext")) acceptedDocument.set("authoringContext",
                current.document().path("authoringContext").deepCopy());
        acceptedDocument.put("importMode", "REPAIR_ACCEPTANCE");
        acceptedDocument.put("definitionRevision", verified.definitionRevision());
        acceptedDocument.put("timetableRevision", verified.timetableRevision());
        ObjectNode accepted = acceptedDocument.putObject("acceptedBaseline");
        accepted.set("definition", definition.deepCopy());
        accepted.set("result", result.deepCopy());
        accepted.set("manifest", manifests.forAcceptedRepair(
                definition,
                result,
                current.document().path("acceptedBaseline").path("manifest"),
                current.document().path("repairDraft")));
        acceptedDocument.set("lastRun", current.document().path("lastRun").deepCopy());
        if (demoVerification != null) acceptedDocument.set("solveProvenance", proposal.path("solveProvenance").deepCopy());
        var installed = mutation.acceptRepair(expectedVersion, acceptedDocument);
        if (events != null) events.product(DemoEvents.Product.ACCEPT_REPAIR);
        return installed;
    }

    private boolean identityMatches(
            WorkspaceAggregate current, JsonNode proposal, JsonNode definition, JsonNode result) {
        JsonNode accepted = current.document().path("acceptedBaseline");
        JsonNode draft = current.document().path("repairDraft");
        JsonNode lastRun = current.document().path("lastRun");
        if (!proposal.isObject() || !definition.isObject() || !result.isObject()
                || proposal.path("sourceWorkspaceVersion").longValue() != current.version() - 2
                || !same(proposal, "acceptedTimetableRevision", accepted.path("result"), "timetableRevision")
                || !same(proposal, "intentRevision", draft, "intentRevision")
                || !same(proposal, "successorDefinitionRevision", result, "inputRevision")
                || !same(proposal, "proposedTimetableRevision", result, "timetableRevision")
                || !same(proposal, "runId", lastRun, "id")
                || !same(proposal, "limit", lastRun, "limit")
                || !same(proposal, "terminationReason", result, "terminationReason")
                || proposal.path("elapsedTimeMs").longValue() != result.path("elapsedTimeMs").longValue()
                || !"REPAIR".equals(text(proposal.path("kind")))
                || !"REPAIR".equals(text(lastRun.path("kind")))
                || !"FEASIBLE".equals(text(result.path("status")))
                || !accepted.path("result").path("inputRevision").stringValue()
                        .equals(text(definition.path("basedOnRevision")))) {
            return false;
        }
        if (demoVerification != null || RepairDraftService.seeded(current.document())) {
            try {
                ObjectNode checked = copy(current.document());
                drafts.refreshSavedDraft(checked);
                if (!draft.path("intentRevision").equals(checked.path("repairDraft").path("intentRevision"))
                        || !checked.path("repairDraft").path("readyToSolve").booleanValue()
                        || !definition.equals(drafts.compiledDefinition(checked))) return false;
            } catch (WorkspaceProblem invalid) { return false; }
        }
        if (demoVerification != null && (!proposal.path("limit").equals(result.path("limit").path("duration"))
                || !proposal.path("limit").equals(proposal.path("solveProvenance").path("duration"))
                || !demoVerification.build().equals(proposal.path("solveProvenance").path("kernelBuild").asString())
                || !"REPAIR".equals(proposal.path("solveProvenance").path("operation").asString()))) return false;
        JsonNode report = result.path("changeReport");
        if (!report.isObject() || report.size() != ProposalReviewService.CATEGORIES.size()) return false;
        for (String category : ProposalReviewService.CATEGORIES) {
            if (!report.path(category).isArray()
                    || proposal.path("changeCounts").path(category).intValue() != report.path(category).size()) {
                return false;
            }
        }
        JsonNode storedReview = proposal.path("review");
        return storedReview.isMissingNode() || Arrays.equals(
                CanonicalJson.bytes(storedReview),
                CanonicalJson.bytes(reviews.create(accepted, draft, definition, result)));
    }

    private KernelVerifier.Verification verify(ImportDocuments documents) {
        return demoVerification == null ? verifier.verify(documents) : demoVerification.verify(documents);
    }

    private void invalidate(WorkspaceAggregate current) {
        ObjectNode document = copy(current.document());
        document.remove("proposal");
        mutation.replaceRepair(
                current.version(), WorkspaceState.REPAIR_PROPOSAL, WorkspaceState.REPAIR_DRAFT, document);
    }

    private static void requireRepairProposal(WorkspaceAggregate current, String message) {
        if (current.state() != WorkspaceState.REPAIR_PROPOSAL) {
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION", message);
        }
    }

    private static boolean same(JsonNode left, String leftField, JsonNode right, String rightField) {
        return text(left.path(leftField)) != null && text(left.path(leftField)).equals(text(right.path(rightField)));
    }

    private static String text(JsonNode node) {
        return node.isTextual() ? node.stringValue() : null;
    }

    private static ObjectNode copy(JsonNode node) {
        return (ObjectNode) node.deepCopy();
    }

    private static WorkspaceProblem staleProposal() {
        return new WorkspaceProblem(
                HttpStatus.CONFLICT,
                "STALE_PROPOSAL",
                "The repair proposal no longer matches the accepted baseline and repair draft.");
    }
}
