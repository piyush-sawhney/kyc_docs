package com.kycdocs.infrastructure.persistence.jpa.documenttype;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDocumentTypeRepository extends JpaRepository<DocumentTypeJpaEntity, UUID> {

    Optional<DocumentTypeJpaEntity> findByName(String name);
}
