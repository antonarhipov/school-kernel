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
    private final WorkspaceRepository repository;
    private final WorkspaceMutation mutation;
    private final KernelVerifier verifier;
    private final ManifestService manifests;
    private final ProposalReviewService reviews;
    private final ObjectMapper json;

    public RepairProposalService(
            WorkspaceRepository repository,
            WorkspaceMutation mutation,
            KernelVerifier verifier,
            ManifestService manifests,
            ProposalReviewService reviews,
            ObjectMapper json) {
        this.repository = repository;
        this.mutation = mutation;
        this.verifier = verifier;
        this.manifests = manifests;
        this.reviews = reviews;
        this.json = json;
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
            verified = verifier.verify(new ImportDocuments(
                    definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
        } catch (WorkspaceProblem problem) {
            if ("KERNEL_VERIFICATION_FAILED".equals(problem.code())) {
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
        acceptedDocument.set("school", current.document().path("school").deepCopy());
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
        return mutation.acceptRepair(expectedVersion, acceptedDocument);
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
        JsonNode report = result.path("changeReport");
        if (!report.isObject() || report.size() != ProposalReviewService.CATEGORIES.size()) return false;
        for (String category : ProposalReviewService.CATEGORIES) {
            if (!report.path(category).isArray()
                    || proposal.path("changeCounts").path(category).intValue() != report.path(category).size()) {
                return false;
            }
        }
        return Arrays.equals(
                CanonicalJson.bytes(proposal.path("review")),
                CanonicalJson.bytes(reviews.create(accepted, draft, definition, result)));
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
