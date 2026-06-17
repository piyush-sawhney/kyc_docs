package com.kycdocs.infrastructure.persistence.jpa.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SpringDataClientRepository extends JpaRepository<ClientJpaEntity, UUID> {

    @Query("SELECT c FROM ClientJpaEntity c WHERE c.isDeleted = false")
    List<ClientJpaEntity> findAllActive();

    @Query("SELECT c FROM ClientJpaEntity c WHERE c.isDeleted = true")
    List<ClientJpaEntity> findAllDeleted();
}
