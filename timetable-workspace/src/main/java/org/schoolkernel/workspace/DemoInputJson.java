package org.schoolkernel.workspace;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Shared safeguards for every visitor-supplied demo document, before tree construction. */
final class DemoInputJson {
    static final JsonMapper MAPPER = JsonMapper.builder(JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(64).maxStringLength(1024 * 1024).maxNumberLength(100)
                    .maxNameLength(128).maxTokenCount(150_000).build()).build())
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    private DemoInputJson() {}
}
