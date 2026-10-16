package org.schoolkernel.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
@Profile("demo")
class DemoSamples {
    @org.springframework.beans.factory.annotation.Autowired(required = false) private DemoEvents events;
    private static final List<String> IDS = List.of("small", "two-class", "full");
    private static final List<String> TITLES = List.of("Small school", "Two-class school", "Full school");
    private final Map<String, Sample> samples = new LinkedHashMap<>();
    private final ObjectMapper json;
    private final KernelVerifier verifier;
    private final String artifact;
    private final ManifestService manifests;
    private final WorkspaceStore store;
    private final WorkspaceMutation mutation;

    DemoSamples(ObjectMapper json, KernelVerifier verifier, ManifestService manifests, WorkspaceStore store,
            WorkspaceMutation mutation, @Value("${demo.kernel-artifact:kernel-cli/target/school-kernel.jar}") String artifact) {
        this.json = json;
        this.verifier = verifier;
        this.manifests = manifests;
        this.store = store;
        this.mutation = mutation;
        this.artifact = artifact;
    }

    @PostConstruct void verifyPackagedSamples() throws Exception {
        JsonNode evidence = read("scoring-evidence.json");
        if (!kernelBuild(Path.of(KernelVerifier.resolveExecutable(artifact))).equals(evidence.path("kernelBuild").stringValue())) {
            throw new IllegalStateException("Packaged samples need current kernel scoring evidence");
        }
        Map<String, Sample> verifiedSamples = new LinkedHashMap<>();
        for (int index = 0; index < IDS.size(); index++) {
            String id = IDS.get(index);
            JsonNode definition = read(id + "-definition.json");
            JsonNode result = read(id + "-result.json");
            JsonNode expected = evidence.path("samples").path(id);
            if (definition.path("catalogVersion").intValue() != evidence.path("catalogVersion").intValue()
                    || !result.path("score").equals(expected.path("score"))
                    || !hash(CanonicalJson.bytes(definition)).equals(expected.path("definitionSha256").stringValue())
                    || !hash(CanonicalJson.bytes(result)).equals(expected.path("resultSha256").stringValue())) {
                throw new IllegalStateException("Packaged sample evidence does not match " + id);
            }
            var documents = new ImportDocuments(definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE);
            var verified = verifier.verify(documents);
            verifiedSamples.put(id, new Sample(id, TITLES.get(index), definition, result,
                    manifests.validatedOrGenerated(documents), verified));
        }
        samples.clear();
        samples.putAll(verifiedSamples);
    }

    ArrayNode catalog() {
        ArrayNode catalog = json.createArrayNode();
        for (Sample sample : samples.values()) {
            ObjectNode row = catalog.addObject();
            row.put("identifier", sample.id());
            row.put("title", sample.title());
            row.put("description", "Fictional school and people for trying timetable planning.");
            row.put("displayOrder", catalog.size() - 1);
            for (String collection : List.of("lessons", "cohorts", "teachers", "rooms", "periods")) {
                row.put(collection, sample.definition().path(collection).size());
            }
        }
        return catalog;
    }

    ObjectNode preview(String id) {
        Sample sample = sample(id);
        ObjectNode preview = json.createObjectNode();
        preview.put("identifier", sample.id());
        preview.put("title", sample.title());
        preview.set("definition", sample.definition().deepCopy());
        return preview;
    }

    WorkspaceAggregate use(String id, String ifMatch, boolean confirmed) {
        Sample sample = sample(id);
        var current = store.load();
        long version = ImportService.requireMatchingVersion(ifMatch, current);
        if (current.state() == WorkspaceState.SOLVING_INITIAL || current.state() == WorkspaceState.SOLVING_REPAIR)
            throw new WorkspaceProblem(HttpStatus.CONFLICT, "INVALID_WORKSPACE_TRANSITION",
                    "Wait for or cancel the active run before opening a sample.");
        if (current.state() != WorkspaceState.EMPTY && !confirmed) throw new WorkspaceProblem(
                HttpStatus.PRECONDITION_REQUIRED, "REPLACEMENT_CONFIRMATION_REQUIRED", "Confirm replacing the current workspace first.");
        ObjectNode document = json.createObjectNode();
        document.putObject("school").put("id", sample.verification().schoolId()).put("displayName", sample.title());
        document.put("origin", id);
        document.put("importMode", "PACKAGED_SAMPLE");
        document.put("definitionRevision", sample.verification().definitionRevision());
        document.put("timetableRevision", sample.verification().timetableRevision());
        ObjectNode baseline = document.putObject("acceptedBaseline");
        baseline.set("definition", sample.definition().deepCopy());
        baseline.set("result", sample.result().deepCopy());
        baseline.set("manifest", sample.manifest().deepCopy());
        WorkspaceAggregate opened = mutation.resetAndImport(version, WorkspaceState.ACCEPTED_BASELINE, document);
        if (events != null) events.product(DemoEvents.Product.SAMPLE_OPEN, id, sample.definition());
        return opened;
    }

    private Sample sample(String id) {
        Sample sample = samples.get(id);
        if (sample == null) throw new WorkspaceProblem(HttpStatus.NOT_FOUND, "SAMPLE_NOT_FOUND", "The sample does not exist.");
        return sample;
    }
    JsonNode read(String name) throws IOException {
        try (var input = new ClassPathResource("demo/samples/" + name).getInputStream()) { return json.readTree(input); }
    }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static String kernelBuild(Path artifact) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var jar = new ZipFile(artifact.toFile())) {
            var entries = jar.stream().filter(entry -> entry.getName().startsWith("org/schoolkernel/") && entry.getName().endsWith(".class")
                    || entry.getName().startsWith("schema/") && entry.getName().endsWith(".json"))
                    .sorted(java.util.Comparator.comparing(java.util.zip.ZipEntry::getName)).toList();
            if (entries.isEmpty()) throw new IllegalStateException("Kernel code is missing from configured artifact");
            for (var entry : entries) {
                digest.update(entry.getName().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                try (var input = jar.getInputStream(entry)) { digest.update(input.readAllBytes()); }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private record Sample(String id, String title, JsonNode definition, JsonNode result, JsonNode manifest,
            KernelVerifier.Verification verification) {}
}
