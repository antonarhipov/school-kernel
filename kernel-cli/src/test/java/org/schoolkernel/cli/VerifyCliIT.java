package org.schoolkernel.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.schoolkernel.contract.JsonSupport;

import tools.jackson.databind.JsonNode;

class VerifyCliIT {
    private static final Path JAR = Path.of("target", "school-kernel.jar").toAbsolutePath();

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("timetable-workspace UC-1 main definition mode: verify publishes non-solving structured evidence")
    void verifiesInitialDefinitionWithoutCandidateDisclosure() throws Exception {
        Path definition = Path.of("..", "examples", "initial-school.json").toAbsolutePath();
        Path output = temporaryDirectory.resolve("verification.json");

        ProcessResult process = run("verify", "--definition", definition.toString(), "--output", output.toString(),
                "--correlation-id", "workspace-uc1-initial");

        assertEquals(0, process.exitCode());
        assertEquals("", process.stdout());
        assertFalse(process.stderr().contains("Solving started"));
        JsonNode verification = JsonSupport.mapper().readTree(output);
        assertEquals("VERIFIED", verification.path("status").stringValue());
        assertEquals("INITIAL_DEFINITION", verification.path("mode").stringValue());
        assertEquals("demo-school", verification.path("schoolId").stringValue());
        assertEquals("1.0.0-SNAPSHOT", verification.path("kernelVersion").stringValue());
        assertFalse(verification.has("timetable"));
        assertFalse(verification.has("assignments"));
        assertTrue(verificationSchema().validate(verification).isEmpty());
    }

    @Test
    @DisplayName("timetable-workspace UC-1 accepted mode: verify accepts an exact complete FEASIBLE pair")
    void verifiesAcceptedBaseline() throws Exception {
        Path definition = Path.of("..", "examples", "initial-school.json").toAbsolutePath();
        Path result = temporaryDirectory.resolve("result.json");
        assertEquals(0, run("plan", "--definition", definition.toString(), "--output", result.toString(),
                "--step-limit", "100").exitCode());
        Path output = temporaryDirectory.resolve("verification.json");

        ProcessResult process = run("verify", "--definition", definition.toString(), "--result", result.toString(),
                "--output", output.toString());

        assertEquals(0, process.exitCode());
        assertFalse(process.stderr().contains("Solving started"));
        JsonNode verification = JsonSupport.mapper().readTree(output);
        JsonNode timetable = JsonSupport.mapper().readTree(result);
        assertEquals("ACCEPTED_BASELINE", verification.path("mode").stringValue());
        assertEquals(timetable.path("inputRevision"), verification.path("definitionRevision"));
        assertEquals(timetable.path("timetableRevision"), verification.path("timetableRevision"));
        assertTrue(verificationSchema().validate(verification).isEmpty());
    }

    @Test
    @DisplayName("timetable-workspace UC-1 extensions 2b and 2c: successor-only and mismatched pairs are rejected")
    void rejectsSuccessorOnlyAndMismatchedPair() throws Exception {
        Path definition = Path.of("..", "examples", "initial-school.json").toAbsolutePath();
        Path result = temporaryDirectory.resolve("result.json");
        assertEquals(0, run("plan", "--definition", definition.toString(), "--output", result.toString(),
                "--step-limit", "100").exitCode());

        Path successorOnly = temporaryDirectory.resolve("successor.json");
        var successor = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper().readTree(definition);
        successor.put("basedOnRevision", "sha256:" + "0".repeat(64));
        Files.write(successorOnly, JsonSupport.mapper().writeValueAsBytes(successor));
        Path successorOutput = temporaryDirectory.resolve("successor-verification.json");
        ProcessResult successorProcess = run("verify", "--definition", successorOnly.toString(),
                "--output", successorOutput.toString());
        assertEquals(2, successorProcess.exitCode());
        assertEquals("INVALID_INPUT", JsonSupport.mapper().readTree(successorOutput).path("status").stringValue());

        var changed = (tools.jackson.databind.node.ObjectNode) successor.deepCopy();
        changed.remove("basedOnRevision");
        ((tools.jackson.databind.node.ObjectNode) changed.path("subjects").get(0)).put("displayName", "Changed");
        Path changedDefinition = temporaryDirectory.resolve("changed.json");
        Files.write(changedDefinition, JsonSupport.mapper().writeValueAsBytes(changed));
        Path mismatchOutput = temporaryDirectory.resolve("mismatch-verification.json");
        ProcessResult mismatchProcess = run("verify", "--definition", changedDefinition.toString(),
                "--result", result.toString(), "--output", mismatchOutput.toString());
        assertEquals(2, mismatchProcess.exitCode());
        JsonNode mismatch = JsonSupport.mapper().readTree(mismatchOutput);
        assertEquals("INVALID_INPUT", mismatch.path("status").stringValue());
        assertFalse(mismatch.has("timetable"));
        assertTrue(verificationSchema().validate(mismatch).isEmpty());
    }

    @Test
    @DisplayName("timetable-workspace UC-1 extension 2a: malformed input is safe and verify exposes no solve controls")
    void malformedAndSolveControlsAreRejected() throws Exception {
        Path malformed = temporaryDirectory.resolve("malformed.json");
        Files.writeString(malformed, "{not-json", StandardCharsets.UTF_8);
        Path output = temporaryDirectory.resolve("verification.json");
        ProcessResult invalid = run("verify", "--definition", malformed.toString(), "--output", output.toString());
        assertEquals(2, invalid.exitCode());
        assertTrue(verificationSchema().validate(JsonSupport.mapper().readTree(output)).isEmpty());

        ProcessResult misuse = run("verify", "--definition", malformed.toString(),
                "--output", temporaryDirectory.resolve("unused.json").toString(), "--time-limit", "30s");
        assertEquals(64, misuse.exitCode());
    }

    @Test
    @DisplayName("RULE-28: packaged verify enforces byte and token limits without replacing a destination")
    void boundedInputsPreserveDestination() throws Exception {
        Path output = temporaryDirectory.resolve("preserved.json");
        byte[] prior = "prior-result".getBytes(StandardCharsets.UTF_8);
        Files.write(output, prior);

        List<String> oversizedDocuments = List.of(
                "[".repeat(65) + "0" + "]".repeat(65),
                "{\"value\":\"" + "x".repeat(1024 * 1024 + 1) + "\"}",
                "{\"value\":" + "1" + "0".repeat(100) + "}");
        int index = 0;
        for (String document : oversizedDocuments) {
            Path input = temporaryDirectory.resolve("oversized-" + index++ + ".json");
            Files.writeString(input, document, StandardCharsets.UTF_8);
            ProcessResult process = run("verify", "--definition", input.toString(),
                    "--output", output.toString(), "--force");
            assertEquals(74, process.exitCode());
            assertArrayEquals(prior, Files.readAllBytes(output));
        }

        Path below = sizedJson("below-byte-limit.json", 10 * 1024 * 1024 - 1);
        Path at = sizedJson("at-byte-limit.json", 10 * 1024 * 1024);
        Path above = sizedJson("above-byte-limit.json", 10 * 1024 * 1024 + 1);
        assertEquals(2, run("verify", "--definition", below.toString(),
                "--output", temporaryDirectory.resolve("below-result.json").toString()).exitCode());
        assertEquals(2, run("verify", "--definition", at.toString(),
                "--output", temporaryDirectory.resolve("at-result.json").toString()).exitCode());
        assertEquals(74, run("verify", "--definition", above.toString(),
                "--output", temporaryDirectory.resolve("above-result.json").toString()).exitCode());
    }

    @Test
    @DisplayName("RULE-28: concurrent packaged publishers have one unforced winner and a complete forced winner")
    void concurrentPackagedPublicationIsRaceSafe() throws Exception {
        Path definition = Path.of("..", "examples", "initial-school.json").toAbsolutePath();
        Path unforced = temporaryDirectory.resolve("unforced.json");
        Process first = start("verify", "--definition", definition.toString(), "--output", unforced.toString(),
                "--correlation-id", "first");
        Process second = start("verify", "--definition", definition.toString(), "--output", unforced.toString(),
                "--correlation-id", "second");
        assertTrue(first.waitFor(30, TimeUnit.SECONDS));
        assertTrue(second.waitFor(30, TimeUnit.SECONDS));
        assertEquals(List.of(0, 74), java.util.stream.Stream.of(first.exitValue(), second.exitValue()).sorted().toList());
        assertTrue(verificationSchema().validate(JsonSupport.mapper().readTree(unforced)).isEmpty());

        var slowDefinition = (tools.jackson.databind.node.ObjectNode) JsonSupport.mapper().readTree(definition);
        ((tools.jackson.databind.node.ObjectNode) slowDefinition.path("subjects").get(0))
                .put("displayName", "x".repeat(1024 * 1024));
        Path slowInput = temporaryDirectory.resolve("slow-definition.json");
        Files.write(slowInput, JsonSupport.mapper().writeValueAsBytes(slowDefinition));
        Path forced = temporaryDirectory.resolve("forced.json");
        Process slow = start("verify", "--definition", slowInput.toString(), "--output", forced.toString(),
                "--force", "--correlation-id", "slow");
        Process fast = start("verify", "--definition", definition.toString(), "--output", forced.toString(),
                "--force", "--correlation-id", "fast");
        Process firstCompleted = (Process) java.util.concurrent.CompletableFuture.anyOf(
                        slow.onExit(), fast.onExit())
                .get(30, TimeUnit.SECONDS);
        Process lastCompleted = firstCompleted == slow ? fast : slow;
        assertTrue(lastCompleted.waitFor(30, TimeUnit.SECONDS));
        assertEquals(0, slow.exitValue());
        assertEquals(0, fast.exitValue());
        JsonNode forcedResult = JsonSupport.mapper().readTree(forced);
        assertEquals(lastCompleted == slow ? "slow" : "fast",
                forcedResult.path("correlationId").stringValue());
        assertTrue(verificationSchema().validate(forcedResult).isEmpty());
    }

    private Path sizedJson(String name, int bytes) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        byte[] content = new byte[bytes];
        java.util.Arrays.fill(content, (byte) ' ');
        content[0] = '{';
        content[1] = '}';
        Files.write(path, content);
        return path;
    }

    private static com.networknt.schema.Schema verificationSchema() throws Exception {
        try (InputStream input = VerifyCliIT.class.getResourceAsStream("/schema/verification-result-v1.schema.json")) {
            return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(input);
        }
    }

    private static ProcessResult run(String... arguments) throws Exception {
        Process process = start(arguments);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "packaged CLI did not terminate");
        return new ProcessResult(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static Process start(String... arguments) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(JAR.toString());
        command.addAll(List.of(arguments));
        return new ProcessBuilder(command).start();
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
