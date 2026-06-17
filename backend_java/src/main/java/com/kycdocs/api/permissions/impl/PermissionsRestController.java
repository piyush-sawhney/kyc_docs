package com.kycdocs.api.permissions.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.permissions.PermissionsApi;
import com.kycdocs.application.permissions.PermissionsUseCase;
import com.kycdocs.domain.permission.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PermissionsRestController implements PermissionsApi {

    private final PermissionsUseCase permissionsUseCase;

    public PermissionsRestController(PermissionsUseCase permissionsUseCase) {
        this.permissionsUseCase = permissionsUseCase;
    }

    @Override
    public ResponseEntity<ApiResponse<List<Permission>>> listPermissions() {
        return ResponseEntity.ok(ApiResponse.ok(permissionsUseCase.listAll()));
    }

    @Override
    public ResponseEntity<ApiResponse<List<Permission>>> getUserPermissions(String userId) {
        return ResponseEntity.ok(ApiResponse.ok(permissionsUseCase.getUserPermissions(userId)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> setUserPermissions(String userId, List<String> permissionKeys) {
        permissionsUseCase.setUserPermissions(userId, permissionKeys);
        return ResponseEntity.ok(ApiResponse.ok("Permissions updated"));
    }
}
