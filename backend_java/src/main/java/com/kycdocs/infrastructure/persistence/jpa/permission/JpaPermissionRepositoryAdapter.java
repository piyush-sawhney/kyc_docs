package com.kycdocs.infrastructure.persistence.jpa.permission;

import com.kycdocs.domain.permission.Permission;
import com.kycdocs.domain.permission.PermissionId;
import com.kycdocs.domain.permission.PermissionRepository;
import com.kycdocs.domain.user.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaPermissionRepositoryAdapter implements PermissionRepository {

    private final SpringDataPermissionRepository springRepo;
    private final PermissionJpaMapper mapper;

    public JpaPermissionRepositoryAdapter(SpringDataPermissionRepository springRepo, PermissionJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<Permission> findById(PermissionId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Permission> findByKey(String key) {
        return springRepo.findByKey(key).map(mapper::toDomain);
    }

    @Override
    public List<Permission> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Permission> findByUserId(UserId userId) {
        return springRepo.findByUserId(userId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Permission save(Permission permission) {
        var saved = springRepo.save(mapper.toJpa(permission));
        return mapper.toDomain(saved);
    }
}
