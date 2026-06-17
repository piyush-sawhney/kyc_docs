package com.kycdocs.domain.client;

import java.util.UUID;

public record ClientId(UUID value) {

    public ClientId {
        if (value == null) {
            throw new IllegalArgumentException("ClientId must not be null");
        }
    }

    public static ClientId generate() {
        return new ClientId(UUID.randomUUID());
    }

    public static ClientId fromString(String id) {
        return new ClientId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
