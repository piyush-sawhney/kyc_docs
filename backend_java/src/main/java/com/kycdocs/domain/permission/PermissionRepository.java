package com.kycdocs.domain.permission;

import com.kycdocs.domain.user.UserId;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {

    Optional<Permission> findById(PermissionId id);

    Optional<Permission> findByKey(String key);

    List<Permission> findAll();

    List<Permission> findByUserId(UserId userId);

    Permission save(Permission permission);
}
