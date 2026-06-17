package com.kycdocs.domain.audit;

import com.kycdocs.domain.user.UserId;
import com.kycdocs.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class AuditLog extends BaseEntity {

    private UserId userId;
    private String action;
    private String entityType;
    private String entityId;
    private String description;
    private String oldValues;
    private String newValues;
    private String ipAddress;
    private String userAgent;

    public AuditLog(UUID id, Instant createdAt, Instant updatedAt,
                    UserId userId, String action, String entityType,
                    String entityId, String description, String oldValues,
                    String newValues, String ipAddress, String userAgent) {
        super(id, createdAt, updatedAt);
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UserId getUserId() { return userId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getDescription() { return description; }
    public String getOldValues() { return oldValues; }
    public String getNewValues() { return newValues; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
}
