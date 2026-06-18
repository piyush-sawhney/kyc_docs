package com.kycdocs.domain.documenttype;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kycdocs.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class DocumentType extends BaseEntity {

    private String name;
    private boolean isActive;

    public DocumentType(UUID id, Instant createdAt, Instant updatedAt,
                        String name, boolean isActive) {
        super(id, createdAt, updatedAt);
        this.name = name;
        this.isActive = isActive;
    }

    public static DocumentType create(String name) {
        return new DocumentType(UUID.randomUUID(), Instant.now(), Instant.now(), name, true);
    }

    public String getName() { return name; }
    @JsonProperty("isActive")
    public boolean isActive() { return isActive; }

    public void updateName(String name) {
        this.name = name;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
