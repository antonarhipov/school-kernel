package org.schoolkernel.solver;

import java.util.List;

public record ConstraintDiagnostic(String constraintId, long matchCount, List<List<String>> examples) {}
