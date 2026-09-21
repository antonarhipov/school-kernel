package org.schoolkernel.cli;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Callable;

import org.schoolkernel.application.PlanRequest;
import org.schoolkernel.application.PlanService;
import org.schoolkernel.application.ReplanRequest;
import org.schoolkernel.application.ReplanService;
import org.schoolkernel.application.VerifyRequest;
import org.schoolkernel.application.VerifyService;
import org.schoolkernel.application.CommandOutcome;
import org.schoolkernel.application.FileBoundary;
import org.schoolkernel.application.DefinitionLoader;
import org.schoolkernel.application.TransportException;
import org.schoolkernel.contract.CurrentTimetableReader;
import org.schoolkernel.contract.DefinitionSchemaValidator;
import org.schoolkernel.contract.ResultFactory;
import org.schoolkernel.contract.RevisionService;
import org.schoolkernel.contract.VerificationResultFactory;
import org.schoolkernel.domain.BaselineVerifier;
import org.schoolkernel.domain.DefinitionValidator;
import org.schoolkernel.solver.PreflightFeasibilityCheck;
import org.schoolkernel.solver.ScheduleEvaluator;
import org.schoolkernel.solver.SolverAdapter;
import org.schoolkernel.solver.SolverAdapter.ExecutionControls;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

public final class SchoolKernelMain {
    private SchoolKernelMain() {}

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new RootCommand());
        commandLine.setOut(commandLine.getErr());
        commandLine.setParameterExceptionHandler((exception, parsedArgs) -> {
            exception.getCommandLine().getErr().println(exception.getMessage());
            return 64;
        });
        commandLine.setExecutionExceptionHandler((exception, command, parseResult) -> {
            command.getErr().println("Internal command failure");
            return 4;
        });
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Command(
            name = "school-kernel",
            mixinStandardHelpOptions = true,
            description = "Stateless school timetable planning kernel.",
            subcommands = {PlanCommand.class, ReplanCommand.class, VerifyCommand.class})
    static final class RootCommand implements Callable<Integer> {
        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            spec.commandLine().usage(spec.commandLine().getErr());
            return 64;
        }
    }

    static final class LimitGroup {
        @Option(names = "--time-limit", paramLabel = "DURATION", description = "Positive duration, for example 30s or PT30S.")
        private String timeLimit;

        @Option(names = "--step-limit", paramLabel = "STEPS", description = "Positive deterministic solver step limit.")
        private Integer stepLimit;
    }

    @Command(name = "plan", mixinStandardHelpOptions = true, description = "Generate an initial timetable.")
    static final class PlanCommand implements Callable<Integer> {
        @Option(names = "--definition", required = true, paramLabel = "PATH")
        private Path definition;

        @Option(names = "--output", required = true, paramLabel = "PATH")
        private Path output;

        @ArgGroup(exclusive = true, multiplicity = "0..1")
        private LimitGroup limit;

        @Option(names = "--seed", defaultValue = "0")
        private long seed;

        @Option(names = "--correlation-id")
        private String correlationId;

        @Option(names = "--force")
        private boolean force;

        @Option(names = "--debug")
        private boolean debug;

        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            PrintWriter errorWriter = spec.commandLine().getErr();
            if (correlationId != null && (correlationId.isBlank() || correlationId.length() > 128)) {
                errorWriter.println("--correlation-id must be nonblank and at most 128 characters");
                return 64;
            }
            ExecutionControls controls;
            try {
                controls = controls();
            } catch (IllegalArgumentException exception) {
                errorWriter.println(exception.getMessage());
                return 64;
            }
            FileBoundary files = new FileBoundary();
            Integer preflight = prepare(files, errorWriter, output, force, definition);
            if (preflight != null) {
                return preflight;
            }
            DefinitionLoader definitions = new DefinitionLoader(
                    files,
                    new DefinitionSchemaValidator(),
                    new DefinitionValidator(),
                    new RevisionService());
            PlanService handler = new PlanService(
                    definitions,
                    new PreflightFeasibilityCheck(),
                    new SolverAdapter(),
                    new ScheduleEvaluator(),
                    new ResultFactory());
            return mapOutcome(
                    handler.handle(new PlanRequest(definition, output, controls, correlationId, force, debug)),
                    files, output, force, debug, errorWriter);
        }

        private ExecutionControls controls() {
            if (limit == null) {
                return new ExecutionControls(Duration.ofSeconds(30), null, seed);
            }
            if (limit.stepLimit != null) {
                return new ExecutionControls(null, limit.stepLimit, seed);
            }
            return new ExecutionControls(parseDuration(limit.timeLimit), null, seed);
        }
    }

    static Duration parseDuration(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("--time-limit must be a positive duration");
        }
        try {
            Duration duration;
            if (value.startsWith("P") || value.startsWith("p")) {
                duration = Duration.parse(value.toUpperCase(java.util.Locale.ROOT));
            } else if (value.endsWith("ms")) {
                duration = Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
            } else {
                long amount = Long.parseLong(value.substring(0, value.length() - 1));
                duration = switch (value.charAt(value.length() - 1)) {
                    case 's' -> Duration.ofSeconds(amount);
                    case 'm' -> Duration.ofMinutes(amount);
                    case 'h' -> Duration.ofHours(amount);
                    default -> throw new IllegalArgumentException("Unsupported duration suffix");
                };
            }
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("--time-limit must be a positive duration");
            }
            return duration;
        } catch (DateTimeParseException | NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("--time-limit must be a positive duration such as 30s or PT30S");
        }
    }

    @Command(name = "replan", mixinStandardHelpOptions = true, description = "Replan a current timetable.")
    static final class ReplanCommand implements Callable<Integer> {
        @Option(names = "--current-definition", required = true, paramLabel = "PATH")
        private Path currentDefinition;

        @Option(names = "--definition", required = true, paramLabel = "PATH")
        private Path definition;

        @Option(names = "--current", required = true, paramLabel = "PATH")
        private Path current;

        @Option(names = "--output", required = true, paramLabel = "PATH")
        private Path output;

        @ArgGroup(exclusive = true, multiplicity = "0..1")
        private LimitGroup limit;

        @Option(names = "--seed", defaultValue = "0")
        private long seed;

        @Option(names = "--correlation-id")
        private String correlationId;

        @Option(names = "--force")
        private boolean force;

        @Option(names = "--debug")
        private boolean debug;

        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            PrintWriter errorWriter = spec.commandLine().getErr();
            if (correlationId != null && (correlationId.isBlank() || correlationId.length() > 128)) {
                errorWriter.println("--correlation-id must be nonblank and at most 128 characters");
                return 64;
            }
            ExecutionControls controls;
            try {
                controls = controls(limit, seed);
            } catch (IllegalArgumentException exception) {
                errorWriter.println(exception.getMessage());
                return 64;
            }
            FileBoundary files = new FileBoundary();
            Integer preflight = prepare(files, errorWriter, output, force, currentDefinition, definition, current);
            if (preflight != null) {
                return preflight;
            }
            DefinitionLoader definitions = new DefinitionLoader(
                    files,
                    new DefinitionSchemaValidator(),
                    new DefinitionValidator(),
                    new RevisionService());
            ReplanService handler = new ReplanService(
                    definitions,
                    files,
                    new CurrentTimetableReader(),
                    new BaselineVerifier(),
                    new PreflightFeasibilityCheck(),
                    new SolverAdapter(),
                    new ScheduleEvaluator(),
                    new ResultFactory());
            return mapOutcome(
                    handler.handle(new ReplanRequest(
                            currentDefinition, definition, current, output,
                            controls, correlationId, force, debug)),
                    files, output, force, debug, errorWriter);
        }
    }

    @Command(name = "verify", mixinStandardHelpOptions = true, description = "Verify a definition or accepted baseline without solving.")
    static final class VerifyCommand implements Callable<Integer> {
        @Option(names = "--definition", required = true, paramLabel = "PATH")
        private Path definition;

        @Option(names = "--result", paramLabel = "PATH")
        private Path result;

        @Option(names = "--output", required = true, paramLabel = "PATH")
        private Path output;

        @Option(names = "--correlation-id")
        private String correlationId;

        @Option(names = "--force")
        private boolean force;

        @Option(names = "--debug")
        private boolean debug;

        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            PrintWriter errorWriter = spec.commandLine().getErr();
            if (correlationId != null && (correlationId.isBlank() || correlationId.length() > 128)) {
                errorWriter.println("--correlation-id must be nonblank and at most 128 characters");
                return 64;
            }
            FileBoundary files = new FileBoundary();
            Integer preflight = result == null
                    ? prepare(files, errorWriter, output, force, definition)
                    : prepare(files, errorWriter, output, force, definition, result);
            if (preflight != null) {
                return preflight;
            }
            DefinitionLoader definitions = new DefinitionLoader(
                    files,
                    new DefinitionSchemaValidator(),
                    new DefinitionValidator(),
                    new RevisionService());
            VerifyService handler = new VerifyService(
                    definitions,
                    files,
                    new CurrentTimetableReader(),
                    new BaselineVerifier(),
                    new VerificationResultFactory());
            return mapOutcome(
                    handler.handle(new VerifyRequest(definition, result, output, correlationId, force, debug)),
                    files, output, force, debug, errorWriter);
        }
    }

    private static Integer prepare(
            FileBoundary files,
            PrintWriter errorWriter,
            Path output,
            boolean force,
            Path... inputs) {
        try {
            for (Path input : inputs) {
                files.requireDistinct(input, output);
            }
            for (int left = 0; left < inputs.length; left++) {
                for (int right = left + 1; right < inputs.length; right++) {
                    files.requireDistinct(inputs[left], inputs[right]);
                }
            }
        } catch (TransportException exception) {
            errorWriter.println(exception.getMessage());
            return 64;
        }
        try {
            files.prepareDestination(output, force);
            return null;
        } catch (TransportException exception) {
            errorWriter.println(exception.getMessage());
            return 74;
        }
    }

    private static int mapOutcome(
            CommandOutcome outcome,
            FileBoundary files,
            Path output,
            boolean force,
            boolean debug,
            PrintWriter errorWriter) {
        if (outcome instanceof CommandOutcome.TransportFailure failure) {
            errorWriter.println(failure.safeMessage());
            return 74;
        }
        if (outcome instanceof CommandOutcome.Interrupted) {
            return 130;
        }
        CommandOutcome.DocumentOutcome documentOutcome = (CommandOutcome.DocumentOutcome) outcome;
        if (documentOutcome instanceof CommandOutcome.InternalError failure) {
            if (debug) {
                failure.cause().printStackTrace(errorWriter);
            } else {
                errorWriter.println("Internal failure; correlation ID: "
                        + failure.document().path("correlationId").stringValue());
            }
        }
        try {
            files.publish(documentOutcome.document(), output, force);
        } catch (TransportException exception) {
            errorWriter.println(exception.getMessage());
            return 74;
        }
        return switch (documentOutcome) {
            case CommandOutcome.Succeeded ignored -> 0;
            case CommandOutcome.InvalidInput ignored -> 2;
            case CommandOutcome.NoFeasibleSolution ignored -> 3;
            case CommandOutcome.InternalError ignored -> 4;
        };
    }

    private static ExecutionControls controls(LimitGroup limit, long seed) {
        if (limit == null) {
            return new ExecutionControls(Duration.ofSeconds(30), null, seed);
        }
        if (limit.stepLimit != null) {
            return new ExecutionControls(null, limit.stepLimit, seed);
        }
        return new ExecutionControls(parseDuration(limit.timeLimit), null, seed);
    }
}
