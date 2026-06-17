package com.kycdocs.domain.documenttype;

import java.util.UUID;

public record DocumentTypeId(UUID value) {

    public DocumentTypeId {
        if (value == null) {
            throw new IllegalArgumentException("DocumentTypeId must not be null");
        }
    }

    public static DocumentTypeId generate() {
        return new DocumentTypeId(UUID.randomUUID());
    }

    public static DocumentTypeId fromString(String id) {
        return new DocumentTypeId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
