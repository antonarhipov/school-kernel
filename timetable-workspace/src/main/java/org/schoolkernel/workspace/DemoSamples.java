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
    private static final List<String> IDS = List.of("two-class", "full", "star-wars", "english");
    private static final List<String> TITLES = List.of("Ranniku õppehoone", "Kasevälja Põhikool", "Coruscant Imperial Academy", "Meadowbrook Community School");
    private final Map<String, Sample> samples = new LinkedHashMap<>();
    private final ObjectMapper json;
    private final String artifact;
    private final ManifestService manifests;
    private final WorkspaceStore store;
    private final WorkspaceMutation mutation;
    private String build;

    DemoSamples(ObjectMapper json, ManifestService manifests, WorkspaceStore store,
            WorkspaceMutation mutation, @Value("${demo.kernel-artifact:kernel-cli/target/school-kernel.jar}") String artifact) {
        this.json = json;
        this.manifests = manifests;
        this.store = store;
        this.mutation = mutation;
        this.artifact = artifact;
    }

    /** Bundled artifacts are prepared and checked by maintenance tooling, never reverified at startup. */
    @PostConstruct void loadPackagedSamples() throws Exception {
        build = kernelBuild(Path.of(KernelVerifier.resolveExecutable(artifact)));
        Map<String, Sample> loaded = new LinkedHashMap<>();
        for (int index = 0; index < IDS.size(); index++) {
            String id = IDS.get(index);
            JsonNode definition = read(id + "-definition.json");
            ArrayNode variants = json.createArrayNode();
            JsonNode firstManifest = null;
            for (String variant : List.of("a", "b")) {
                JsonNode result = read(id + "-" + variant + "-result.json");
                JsonNode manifest = manifests.validatedOrGenerated(new ImportDocuments(
                        definition, result, null, ImportDocuments.ImportMode.ACCEPTED_BASELINE));
                ObjectNode entry = variants.addObject().put("id", variant)
                        .put("name", "Variant " + variant.toUpperCase(java.util.Locale.ROOT));
                entry.set("result", result); entry.set("manifest", manifest);
                if (firstManifest == null) firstManifest = manifest;
            }
            loaded.put(id, new Sample(id, TITLES.get(index), definition, variants.get(0).path("result"),
                    firstManifest, variants));
        }
        samples.clear();
        samples.putAll(loaded);
    }

    ArrayNode catalog() {
        ArrayNode catalog = json.createArrayNode();
        for (Sample sample : samples.values()) {
            ObjectNode row = catalog.addObject();
            row.put("identifier", sample.id());
            row.put("title", sample.title());
            row.put("description", sample.id().equals("star-wars")
                    ? "Junior foundations, intermediate practical training and senior officer operations with Star Wars faculty."
                    : sample.id().equals("english")
                    ? "An English-language school with Years 1–9, fictional staff and its own weekly curriculum."
                    : "Estonian curriculum with fictional staff, for exploring timetable alternatives.");
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
        preview.set("timetables", sample.variants().deepCopy());
        return preview;
    }

    ObjectNode bundle() {
        ObjectNode result = json.createObjectNode().put("build", build);
        ArrayNode schools = result.putArray("schools");
        IDS.forEach(id -> schools.add(preview(id)));
        return result;
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
        document.putObject("school").put("id", sample.definition().path("schoolId").stringValue()).put("displayName", sample.title());
        document.put("origin", id);
        document.put("importMode", "PACKAGED_SAMPLE");
        document.put("definitionRevision", sample.result().path("inputRevision").stringValue());
        document.put("timetableRevision", sample.result().path("timetableRevision").stringValue());
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
        try (var input = new ClassPathResource("demo/timetables/" + name).getInputStream()) { return json.readTree(input); }
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
            JsonNode variants) {}
}
