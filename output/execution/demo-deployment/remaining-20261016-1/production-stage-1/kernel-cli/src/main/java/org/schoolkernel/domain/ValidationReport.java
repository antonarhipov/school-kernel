package org.schoolkernel.domain;

import java.util.List;

public record ValidationReport(int totalErrors, boolean truncated, List<ValidationError> errors) {
    public static final int DETAIL_LIMIT = 1_000;

    public static ValidationReport from(List<ValidationError> detected) {
        var ordered = detected.stream().sorted().toList();
        return new ValidationReport(
                ordered.size(),
                ordered.size() > DETAIL_LIMIT,
                ordered.stream().limit(DETAIL_LIMIT).toList());
    }

    public boolean isValid() {
        return totalErrors == 0;
    }
}
