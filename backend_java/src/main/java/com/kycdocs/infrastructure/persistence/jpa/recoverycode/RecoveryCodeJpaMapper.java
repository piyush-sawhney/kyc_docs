package com.kycdocs.infrastructure.persistence.jpa.recoverycode;

import com.kycdocs.domain.auth.RecoveryCode;
import com.kycdocs.domain.auth.RecoveryCodeId;
import com.kycdocs.domain.user.UserId;
import org.springframework.stereotype.Component;

@Component
public class RecoveryCodeJpaMapper {

    public RecoveryCodeJpaEntity toJpa(RecoveryCode domain) {
        if (domain == null) return null;
        var entity = new RecoveryCodeJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId().value());
        entity.setCodeHash(domain.getCodeHash());
        entity.setUsed(domain.isUsed());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public RecoveryCode toDomain(RecoveryCodeJpaEntity entity) {
        if (entity == null) return null;
        return new RecoveryCode(
            entity.getId(),
            entity.getCreatedAt(),
            entity.getCreatedAt(),
            new UserId(entity.getUserId()),
            entity.getCodeHash()
        );
    }
}
