package com.kycdocs.application.permissions.impl;

import com.kycdocs.application.permissions.PermissionsUseCase;
import com.kycdocs.domain.permission.Permission;
import com.kycdocs.domain.permission.PermissionId;
import com.kycdocs.domain.permission.PermissionRepository;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.infrastructure.persistence.jpa.permission.SpringDataUserPermissionRepository;
import com.kycdocs.infrastructure.persistence.jpa.permission.UserPermissionJpaEntity;
import com.kycdocs.shared.exception.NotFoundException;

import java.util.List;

public class PermissionsUseCaseImpl implements PermissionsUseCase {

    private final PermissionRepository permissionRepository;
    private final SpringDataUserPermissionRepository userPermissionRepository;

    public PermissionsUseCaseImpl(PermissionRepository permissionRepository,
                                  SpringDataUserPermissionRepository userPermissionRepository) {
        this.permissionRepository = permissionRepository;
        this.userPermissionRepository = userPermissionRepository;
    }

    @Override
    public List<Permission> listAll() {
        return permissionRepository.findAll();
    }

    @Override
    public List<Permission> getUserPermissions(String userId) {
        return permissionRepository.findByUserId(PermissionId.fromString(userId));
    }

    @Override
    public void setUserPermissions(String userId, List<String> permissionIds) {
        var userIdUuid = UserId.fromString(userId).value();

        userPermissionRepository.deleteByUserId(userIdUuid);

        for (var permId : permissionIds) {
            var perm = permissionRepository.findById(PermissionId.fromString(permId))
                .orElseThrow(() -> new NotFoundException("Permission", permId));
            var jpaEntity = new UserPermissionJpaEntity(userIdUuid, perm.getId());
            userPermissionRepository.save(jpaEntity);
        }
    }
}
