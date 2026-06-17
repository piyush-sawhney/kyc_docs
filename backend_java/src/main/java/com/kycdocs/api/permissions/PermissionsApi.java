package com.kycdocs.api.permissions;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.domain.permission.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Permissions")
@RequestMapping("/api/permissions")
public interface PermissionsApi {

    @Operation(summary = "List all permission definitions")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<List<Permission>>> listPermissions();

    @Operation(summary = "Get user's permissions")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<List<Permission>>> getUserPermissions(@PathVariable String userId);

    @Operation(summary = "Set user's permissions")
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<Void>> setUserPermissions(@PathVariable String userId,
                                                          @RequestBody List<String> permissionKeys);
}
