package com.kycdocs.infrastructure.persistence.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDocumentRepository extends JpaRepository<ClientDocumentJpaEntity, UUID> {

    @Query("SELECT d FROM ClientDocumentJpaEntity d WHERE d.clientId = :clientId AND d.isDeleted = false")
    List<ClientDocumentJpaEntity> findActiveByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT d FROM ClientDocumentJpaEntity d WHERE d.documentGroupId = :groupId AND d.isDeleted = false")
    List<ClientDocumentJpaEntity> findActiveByDocumentGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT d FROM ClientDocumentJpaEntity d WHERE d.clientId = :clientId AND d.isDeleted = false")
    List<ClientDocumentJpaEntity> findAllActiveByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT d FROM ClientDocumentJpaEntity d WHERE d.documentNumber = :number AND d.isDeleted = false")
    Optional<ClientDocumentJpaEntity> findByDocumentNumber(@Param("number") String number);
}
