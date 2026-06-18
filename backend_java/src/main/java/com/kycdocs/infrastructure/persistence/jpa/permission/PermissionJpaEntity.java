package com.kycdocs.infrastructure.persistence.jpa.permission;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class PermissionJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String key;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "permission_group", nullable = false, length = 100)
    private String permissionGroup;

    public PermissionJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getPermissionGroup() { return permissionGroup; }
    public void setPermissionGroup(String permissionGroup) { this.permissionGroup = permissionGroup; }
}
