package org.schoolkernel.domain;

import java.util.List;

public record ValidationError(String location, List<String> entityIds, String message) implements Comparable<ValidationError> {
    @Override
    public int compareTo(ValidationError other) {
        int byLocation = location.compareTo(other.location);
        if (byLocation != 0) {
            return byLocation;
        }
        int byMessage = message.compareTo(other.message);
        if (byMessage != 0) {
            return byMessage;
        }
        return String.join("\u0000", entityIds).compareTo(String.join("\u0000", other.entityIds));
    }
}
