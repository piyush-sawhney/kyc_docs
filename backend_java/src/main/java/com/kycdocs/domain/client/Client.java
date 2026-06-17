package com.kycdocs.domain.client;

import com.kycdocs.domain.user.UserId;
import com.kycdocs.shared.SoftDeletableEntity;

import java.time.Instant;
import java.util.UUID;

public class Client extends SoftDeletableEntity {

    private String name;
    private String avatar;
    private UserId createdBy;

    public Client(UUID id, Instant createdAt, Instant updatedAt,
                  String name, UserId createdBy) {
        super(id, createdAt, updatedAt);
        this.name = name;
        this.createdBy = createdBy;
    }

    public static Client create(String name, UserId createdBy) {
        return new Client(UUID.randomUUID(), Instant.now(), Instant.now(), name, createdBy);
    }

    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public UserId getCreatedBy() { return createdBy; }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name must not be blank");
        }
        this.name = name;
    }

    public void updateAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void removeAvatar() {
        this.avatar = null;
    }
}
