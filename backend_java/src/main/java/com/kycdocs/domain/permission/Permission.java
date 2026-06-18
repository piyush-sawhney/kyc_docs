package com.kycdocs.domain.permission;

import com.kycdocs.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class Permission extends BaseEntity {

    private String key;
    private String label;
    private String permissionGroup;

    public Permission(UUID id, Instant createdAt, Instant updatedAt,
                      String key, String label, String permissionGroup) {
        super(id, createdAt, updatedAt);
        this.key = key;
        this.label = label;
        this.permissionGroup = permissionGroup;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getPermissionGroup() { return permissionGroup; }
}
