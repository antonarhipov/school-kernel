package org.schoolkernel.cli;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Callable;

import org.schoolkernel.application.PlanRequest;
import org.schoolkernel.application.PlanService;
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
            subcommands = PlanCommand.class)
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
            return new PlanService().plan(
                    new PlanRequest(definition, output, controls, correlationId, force, debug),
                    errorWriter);
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
}
