package org.schoolkernel.application;

import tools.jackson.databind.node.ObjectNode;

/** Exhaustive application outcome set; the CLI alone maps these outcomes to transport effects. */
public sealed interface CommandOutcome
        permits CommandOutcome.DocumentOutcome,
                CommandOutcome.TransportFailure,
                CommandOutcome.Interrupted {

    sealed interface DocumentOutcome extends CommandOutcome
            permits Succeeded, InvalidInput, NoFeasibleSolution, InternalError {
        ObjectNode document();
    }

    record Succeeded(ObjectNode document) implements DocumentOutcome {}

    record InvalidInput(ObjectNode document) implements DocumentOutcome {}

    record NoFeasibleSolution(ObjectNode document) implements DocumentOutcome {}

    record InternalError(ObjectNode document, RuntimeException cause) implements DocumentOutcome {}

    record TransportFailure(String safeMessage) implements CommandOutcome {}

    record Interrupted() implements CommandOutcome {}
}
