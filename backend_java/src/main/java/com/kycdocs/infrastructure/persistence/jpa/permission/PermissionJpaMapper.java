package com.kycdocs.infrastructure.persistence.jpa.permission;

import com.kycdocs.domain.permission.Permission;
import com.kycdocs.domain.permission.PermissionId;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PermissionJpaMapper {

    public PermissionJpaEntity toJpa(Permission domain) {
        if (domain == null) return null;
        var entity = new PermissionJpaEntity();
        entity.setId(domain.getId());
        entity.setKey(domain.getKey());
        entity.setLabel(domain.getLabel());
        entity.setGroup(domain.getGroup());
        return entity;
    }

    public Permission toDomain(PermissionJpaEntity entity) {
        if (entity == null) return null;
        return new Permission(
            entity.getId(),
            Instant.now(),
            Instant.now(),
            entity.getKey(),
            entity.getLabel(),
            entity.getGroup()
        );
    }
}
