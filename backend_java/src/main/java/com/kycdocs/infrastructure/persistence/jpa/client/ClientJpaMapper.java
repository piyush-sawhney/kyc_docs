package com.kycdocs.infrastructure.persistence.jpa.client;

import com.kycdocs.domain.client.Client;
import com.kycdocs.domain.user.UserId;
import org.springframework.stereotype.Component;

@Component
public class ClientJpaMapper {

    public ClientJpaEntity toJpa(Client domain) {
        if (domain == null) return null;
        var entity = new ClientJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setAvatar(domain.getAvatar());
        entity.setCreatedBy(domain.getCreatedBy().value());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setDeletedBy(domain.getDeletedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public Client toDomain(ClientJpaEntity entity) {
        if (entity == null) return null;
        var client = new Client(
            entity.getId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getName(),
            new UserId(entity.getCreatedBy())
        );
        if (entity.getAvatar() != null) client.updateAvatar(entity.getAvatar());
        if (entity.isDeleted()) client.softDelete(entity.getDeletedBy());
        return client;
    }
}
