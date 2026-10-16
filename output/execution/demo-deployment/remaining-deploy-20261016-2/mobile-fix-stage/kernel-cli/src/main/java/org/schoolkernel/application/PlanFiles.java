package org.schoolkernel.application;

import java.nio.file.Path;

import tools.jackson.databind.JsonNode;

public interface PlanFiles {
    void requireDistinct(Path input, Path output) throws TransportException;

    void prepareDestination(Path output, boolean force) throws TransportException;

    byte[] read(Path input, long maximumBytes) throws TransportException;

    void publish(JsonNode result, Path output, boolean force) throws TransportException;
}
