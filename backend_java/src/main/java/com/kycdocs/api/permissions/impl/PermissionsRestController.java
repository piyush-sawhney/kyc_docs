package com.kycdocs.api.permissions.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.permissions.PermissionsApi;
import com.kycdocs.api.permissions.dto.PermissionResponse;
import com.kycdocs.api.permissions.dto.SetUserPermissionsCommand;
import com.kycdocs.application.permissions.PermissionsUseCase;
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
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions() {
        var permissions = permissionsUseCase.listAll();
        var response = permissions.stream().map(PermissionResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getUserPermissions(String userId) {
        var permissions = permissionsUseCase.getUserPermissions(userId);
        var response = permissions.stream().map(PermissionResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> setUserPermissions(String userId, SetUserPermissionsCommand command) {
        permissionsUseCase.setUserPermissions(userId, command.permissionIds());
        return ResponseEntity.ok(ApiResponse.message("Permissions updated"));
    }
}
