package com.kycdocs.infrastructure.persistence.jpa.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPermissionRepository extends JpaRepository<PermissionJpaEntity, UUID> {

    Optional<PermissionJpaEntity> findByKey(String key);

    @Query("SELECT p FROM PermissionJpaEntity p JOIN UserPermissionJpaEntity up ON p.id = up.id.permissionId WHERE up.id.userId = :userId")
    List<PermissionJpaEntity> findByUserId(@Param("userId") UUID userId);
}
