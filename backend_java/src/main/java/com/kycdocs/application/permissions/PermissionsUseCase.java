package com.kycdocs.application.permissions;

import com.kycdocs.domain.permission.Permission;

import java.util.List;

public interface PermissionsUseCase {

    List<Permission> listAll();

    List<Permission> getUserPermissions(String userId);

    void setUserPermissions(String userId, List<String> permissionIds);
}
