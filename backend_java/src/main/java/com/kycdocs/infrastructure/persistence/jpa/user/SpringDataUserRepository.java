package com.kycdocs.infrastructure.persistence.jpa.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.isDeleted = false")
    List<UserJpaEntity> findAllActive();

    @Query("SELECT u FROM UserJpaEntity u WHERE u.isDeleted = true")
    List<UserJpaEntity> findAllDeleted();

    @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.role = :role AND u.isDeleted = false AND u.isActive = true")
    long countActiveByRole(@Param("role") String role);
}
