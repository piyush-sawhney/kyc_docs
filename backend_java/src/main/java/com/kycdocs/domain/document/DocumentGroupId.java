package com.kycdocs.domain.document;

import java.util.UUID;

public record DocumentGroupId(UUID value) {

    public DocumentGroupId {
        if (value == null) {
            throw new IllegalArgumentException("DocumentGroupId must not be null");
        }
    }

    public static DocumentGroupId generate() {
        return new DocumentGroupId(UUID.randomUUID());
    }

    public static DocumentGroupId fromString(String id) {
        return new DocumentGroupId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
