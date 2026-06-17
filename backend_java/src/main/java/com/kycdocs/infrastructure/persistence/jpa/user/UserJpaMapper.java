package com.kycdocs.infrastructure.persistence.jpa.user;

import com.kycdocs.domain.user.Email;
import com.kycdocs.domain.user.User;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.domain.user.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserJpaMapper {

    public UserJpaEntity toJpa(User domain) {
        if (domain == null) return null;
        var entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail().value());
        entity.setFullName(domain.getFullName());
        entity.setRole(domain.getRole().getValue());
        entity.setTotpSecret(domain.getTotpSecret());
        entity.setTotpVerified(domain.isTotpVerified());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setDeletedBy(domain.getDeletedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) return null;
        var user = new User(
            entity.getId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            new Email(entity.getEmail()),
            entity.getFullName(),
            UserRole.fromString(entity.getRole())
        );
        if (entity.getTotpSecret() != null) {
            user.assignTotpSecret(entity.getTotpSecret());
            if (entity.isTotpVerified()) user.verifyTotp();
        }
        if (!entity.isActive()) user.deactivate();
        if (entity.isDeleted()) {
            user.softDelete(entity.getDeletedBy());
        }
        return user;
    }
}
