package org.schoolkernel.contract;

import org.schoolkernel.domain.ValidationReport;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class VerificationResultFactory {
    public ObjectNode verifiedInitial(
            String correlationId, long elapsedMillis, String schoolId, int catalogVersion, String revision) {
        ObjectNode result = envelope("VERIFIED", correlationId, elapsedMillis);
        result.put("mode", "INITIAL_DEFINITION");
        result.put("schoolId", schoolId);
        result.put("catalogVersion", catalogVersion);
        result.put("definitionRevision", revision);
        return result;
    }

    public ObjectNode verifiedBaseline(
            String correlationId,
            long elapsedMillis,
            String schoolId,
            int catalogVersion,
            String definitionRevision,
            String timetableRevision) {
        ObjectNode result = envelope("VERIFIED", correlationId, elapsedMillis);
        result.put("mode", "ACCEPTED_BASELINE");
        result.put("schoolId", schoolId);
        result.put("catalogVersion", catalogVersion);
        result.put("definitionRevision", definitionRevision);
        result.put("timetableRevision", timetableRevision);
        return result;
    }

    public ObjectNode invalid(String correlationId, long elapsedMillis, ValidationReport report) {
        ObjectNode result = envelope("INVALID_INPUT", correlationId, elapsedMillis);
        ObjectNode validation = result.putObject("validationReport");
        validation.put("totalErrors", report.totalErrors());
        validation.put("truncated", report.truncated());
        ArrayNode errors = validation.putArray("errors");
        report.errors().forEach(error -> {
            ObjectNode detail = errors.addObject();
            detail.put("location", error.location());
            ArrayNode ids = detail.putArray("entityIds");
            error.entityIds().forEach(ids::add);
            detail.put("message", error.message());
        });
        return result;
    }

    public ObjectNode internal(String correlationId, long elapsedMillis) {
        ObjectNode result = envelope("INTERNAL_ERROR", correlationId, elapsedMillis);
        result.put("safeMessage", "An unexpected internal error occurred.");
        return result;
    }

    private static ObjectNode envelope(String status, String correlationId, long elapsedMillis) {
        ObjectNode result = JsonSupport.mapper().createObjectNode();
        result.put("schemaVersion", 1);
        result.put("status", status);
        result.put("kernelVersion", KernelMetadata.version());
        result.put("correlationId", correlationId);
        result.put("elapsedTimeMs", elapsedMillis);
        return result;
    }
}
