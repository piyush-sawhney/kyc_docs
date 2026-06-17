package com.kycdocs.infrastructure.persistence.jpa.recoverycode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataRecoveryCodeRepository extends JpaRepository<RecoveryCodeJpaEntity, UUID> {

    List<RecoveryCodeJpaEntity> findByUserId(UUID userId);

    List<RecoveryCodeJpaEntity> findByUserIdAndIsUsedFalse(UUID userId);
}
