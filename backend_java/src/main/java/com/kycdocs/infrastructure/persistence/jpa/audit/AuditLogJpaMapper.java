package com.kycdocs.infrastructure.persistence.jpa.audit;

import com.kycdocs.domain.audit.AuditLog;
import com.kycdocs.domain.user.UserId;
import org.springframework.stereotype.Component;

@Component
public class AuditLogJpaMapper {

    public AuditLogJpaEntity toJpa(AuditLog domain) {
        if (domain == null) return null;
        var entity = new AuditLogJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId() != null ? domain.getUserId().value() : null);
        entity.setAction(domain.getAction());
        entity.setEntityType(domain.getEntityType());
        entity.setEntityId(domain.getEntityId());
        entity.setDescription(domain.getDescription());
        entity.setOldValues(domain.getOldValues());
        entity.setNewValues(domain.getNewValues());
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public AuditLog toDomain(AuditLogJpaEntity entity) {
        if (entity == null) return null;
        return new AuditLog(
            entity.getId(),
            entity.getCreatedAt(),
            entity.getCreatedAt(),
            entity.getUserId() != null ? new UserId(entity.getUserId()) : null,
            entity.getAction(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getDescription(),
            entity.getOldValues(),
            entity.getNewValues(),
            entity.getIpAddress(),
            entity.getUserAgent()
        );
    }
}
