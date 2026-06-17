package com.kycdocs.domain.permission;

import java.util.UUID;

public record PermissionId(UUID value) {

    public PermissionId {
        if (value == null) {
            throw new IllegalArgumentException("PermissionId must not be null");
        }
    }

    public static PermissionId generate() {
        return new PermissionId(UUID.randomUUID());
    }

    public static PermissionId fromString(String id) {
        return new PermissionId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
