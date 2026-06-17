package com.kycdocs.domain.auth;

import java.util.UUID;

public record RecoveryCodeId(UUID value) {

    public RecoveryCodeId {
        if (value == null) {
            throw new IllegalArgumentException("RecoveryCodeId must not be null");
        }
    }

    public static RecoveryCodeId generate() {
        return new RecoveryCodeId(UUID.randomUUID());
    }

    public static RecoveryCodeId fromString(String id) {
        return new RecoveryCodeId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
