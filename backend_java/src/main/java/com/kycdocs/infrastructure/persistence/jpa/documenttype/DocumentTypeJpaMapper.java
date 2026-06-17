package com.kycdocs.infrastructure.persistence.jpa.documenttype;

import com.kycdocs.domain.documenttype.DocumentType;
import org.springframework.stereotype.Component;

@Component
public class DocumentTypeJpaMapper {

    public DocumentTypeJpaEntity toJpa(DocumentType domain) {
        if (domain == null) return null;
        var entity = new DocumentTypeJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public DocumentType toDomain(DocumentTypeJpaEntity entity) {
        if (entity == null) return null;
        return new DocumentType(
            entity.getId(),
            entity.getCreatedAt(),
            entity.getCreatedAt(),
            entity.getName(),
            entity.isActive()
        );
    }
}
