package com.kycdocs.domain.document;

import java.util.UUID;

public record DocumentId(UUID value) {

    public DocumentId {
        if (value == null) {
            throw new IllegalArgumentException("DocumentId must not be null");
        }
    }

    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID());
    }

    public static DocumentId fromString(String id) {
        return new DocumentId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
