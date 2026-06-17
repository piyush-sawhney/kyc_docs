package com.kycdocs.shared;

import java.time.Instant;
import java.util.UUID;

public abstract class SoftDeletableEntity extends BaseEntity {

    private boolean isDeleted;
    private Instant deletedAt;
    private UUID deletedBy;

    protected SoftDeletableEntity() {}

    protected SoftDeletableEntity(UUID id, Instant createdAt, Instant updatedAt) {
        super(id, createdAt, updatedAt);
        this.isDeleted = false;
    }

    public boolean isDeleted() { return isDeleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public UUID getDeletedBy() { return deletedBy; }

    public void softDelete(UUID deletedBy) {
        if (this.isDeleted) {
            throw new IllegalStateException("Entity is already deleted");
        }
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }

    public void restore() {
        if (!this.isDeleted) {
            throw new IllegalStateException("Entity is not deleted");
        }
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
