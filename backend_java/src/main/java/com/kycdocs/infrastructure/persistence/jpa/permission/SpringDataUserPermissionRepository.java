package com.kycdocs.infrastructure.persistence.jpa.permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataUserPermissionRepository extends JpaRepository<UserPermissionJpaEntity, UserPermissionJpaEntity.UserPermissionId> {

    void deleteByIdUserId(UUID userId);
}
