package com.kycdocs.api.users;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.users.dto.UserResponse;
import com.kycdocs.application.users.dto.ChangeUserRoleCommand;
import com.kycdocs.application.users.dto.CreateUserCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users")
@RequestMapping("/api/users")
public interface UsersApi {

    @Operation(summary = "List active users")
    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    ResponseEntity<ApiResponse<List<UserResponse>>> listActiveUsers();

    @Operation(summary = "List deleted users")
    @GetMapping("/deleted")
    @PreAuthorize("hasAuthority('user:view')")
    ResponseEntity<ApiResponse<List<UserResponse>>> listDeletedUsers();

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:view')")
    ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String id);

    @Operation(summary = "Create user")
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserCommand command);

    @Operation(summary = "Deactivate user")
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('user:manage')")
    ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable String id);

    @Operation(summary = "Reactivate user")
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('user:manage')")
    ResponseEntity<ApiResponse<Void>> reactivateUser(@PathVariable String id);

    @Operation(summary = "Soft-delete user")
    @PostMapping("/{id}/soft-delete")
    @PreAuthorize("hasAuthority('user:manage')")
    ResponseEntity<ApiResponse<Void>> softDeleteUser(@PathVariable String id);

    @Operation(summary = "Restore soft-deleted user")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('user:manage')")
    ResponseEntity<ApiResponse<Void>> restoreUser(@PathVariable String id);

    @Operation(summary = "Change user role")
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('user:manage')")
    ResponseEntity<ApiResponse<Void>> changeUserRole(@PathVariable String id,
                                                      @Valid @RequestBody ChangeUserRoleCommand command);
}
