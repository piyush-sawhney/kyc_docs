package com.kycdocs.infrastructure.persistence.jpa.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    List<AuditLogJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<AuditLogJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
