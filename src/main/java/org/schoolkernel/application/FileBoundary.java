package org.schoolkernel.application;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.schoolkernel.contract.JsonSupport;

import tools.jackson.databind.JsonNode;

public final class FileBoundary implements PlanFiles {
    public static final long DEFAULT_MAX_INPUT_BYTES = 10L * 1024L * 1024L;

    public void requireDistinct(Path input, Path output) throws TransportException {
        Path normalizedInput = input.toAbsolutePath().normalize();
        Path normalizedOutput = output.toAbsolutePath().normalize();
        if (normalizedInput.equals(normalizedOutput)) {
            throw new TransportException("Input and output paths must be distinct");
        }
        try {
            if (Files.exists(normalizedInput) && Files.exists(normalizedOutput)
                    && Files.isSameFile(normalizedInput, normalizedOutput)) {
                throw new TransportException("Input and output paths must be distinct");
            }
        } catch (IOException exception) {
            throw new TransportException("Paths could not be compared safely", exception);
        }
    }

    public void prepareDestination(Path output, boolean force) throws TransportException {
        Path absolute = output.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null || !Files.isDirectory(parent) || !Files.isWritable(parent)) {
            throw new TransportException("Output directory is not writable");
        }
        if (Files.exists(absolute) && !force) {
            throw new TransportException("Output already exists; use --force to replace it");
        }
        if (Files.exists(absolute) && !Files.isRegularFile(absolute)) {
            throw new TransportException("Output path is not a regular file");
        }
    }

    public byte[] read(Path input, long maximumBytes) throws TransportException {
        Path absolute = input.toAbsolutePath().normalize();
        try {
            long size = Files.size(absolute);
            if (size > maximumBytes) {
                throw new TransportException("Input exceeds the configured pre-parse size safeguard");
            }
            byte[] bytes = Files.readAllBytes(absolute);
            if (bytes.length > maximumBytes) {
                throw new TransportException("Input exceeds the configured pre-parse size safeguard");
            }
            return bytes;
        } catch (TransportException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new TransportException("Input could not be read", exception);
        }
    }

    public void publish(JsonNode result, Path output, boolean force) throws TransportException {
        Path absolute = output.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        Path temporary = null;
        try {
            byte[] bytes = JsonSupport.canonicalBytes(result);
            temporary = Files.createTempFile(parent, "." + absolute.getFileName() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(
                    temporary,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Interrupted before publication");
            }
            if (force) {
                Files.move(temporary, absolute,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE);
            }
            temporary = null;
        } catch (AtomicMoveNotSupportedException exception) {
            throw new TransportException("Atomic publication is not supported for the output path", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TransportException("Interrupted before publication", exception);
        } catch (IOException | RuntimeException exception) {
            throw new TransportException("Result could not be serialized and atomically published", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // A temporary artifact is never treated as the requested result.
                }
            }
        }
    }
}
