package com.kycdocs.api.permissions;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.permissions.dto.PermissionResponse;
import com.kycdocs.api.permissions.dto.SetUserPermissionsCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions();

    @Operation(summary = "Get user's permissions")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<List<PermissionResponse>>> getUserPermissions(@PathVariable String userId);

    @Operation(summary = "Set user's permissions")
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ApiResponse<Void>> setUserPermissions(@PathVariable String userId,
                                                           @Valid @RequestBody SetUserPermissionsCommand command);
}
