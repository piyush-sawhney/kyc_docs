package com.kycdocs.domain.auth;

import com.kycdocs.domain.user.UserId;
import com.kycdocs.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class RecoveryCode extends BaseEntity {

    private UserId userId;
    private String codeHash;
    private boolean isUsed;

    public RecoveryCode(UUID id, Instant createdAt, Instant updatedAt,
                        UserId userId, String codeHash) {
        super(id, createdAt, updatedAt);
        this.userId = userId;
        this.codeHash = codeHash;
        this.isUsed = false;
    }

    public UserId getUserId() { return userId; }
    public String getCodeHash() { return codeHash; }
    public boolean isUsed() { return isUsed; }

    public void markAsUsed() {
        if (isUsed) throw new IllegalStateException("Recovery code is already used");
        this.isUsed = true;
    }
}
