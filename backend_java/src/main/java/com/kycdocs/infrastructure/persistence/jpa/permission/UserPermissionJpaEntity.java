package com.kycdocs.infrastructure.persistence.jpa.permission;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_permissions")
public class UserPermissionJpaEntity {

    @EmbeddedId
    private UserPermissionId id;

    public UserPermissionJpaEntity() {}

    public UserPermissionJpaEntity(UUID userId, UUID permissionId) {
        this.id = new UserPermissionId(userId, permissionId);
    }

    public UUID getUserId() { return id != null ? id.userId : null; }
    public UUID getPermissionId() { return id != null ? id.permissionId : null; }

    @Embeddable
    public static class UserPermissionId implements Serializable {
        private UUID userId;
        private UUID permissionId;

        public UserPermissionId() {}

        public UserPermissionId(UUID userId, UUID permissionId) {
            this.userId = userId;
            this.permissionId = permissionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserPermissionId that)) return false;
            return Objects.equals(userId, that.userId) && Objects.equals(permissionId, that.permissionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, permissionId);
        }
    }
}
