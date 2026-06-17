package com.kycdocs.api.users.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.common.annotation.AuditAction;
import com.kycdocs.api.common.annotation.RequirePermissions;
import com.kycdocs.api.users.UsersApi;
import com.kycdocs.application.users.UsersUseCase;
import com.kycdocs.domain.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class UsersRestController implements UsersApi {

    private final UsersUseCase usersUseCase;

    public UsersRestController(UsersUseCase usersUseCase) {
        this.usersUseCase = usersUseCase;
    }

    @Override
    public ResponseEntity<ApiResponse<List<User>>> listActiveUsers() {
        return ResponseEntity.ok(ApiResponse.ok(usersUseCase.listActiveUsers()));
    }

    @Override
    public ResponseEntity<ApiResponse<List<User>>> listDeletedUsers() {
        return ResponseEntity.ok(ApiResponse.ok(usersUseCase.listDeletedUsers()));
    }

    @Override
    public ResponseEntity<ApiResponse<User>> getUser(String id) {
        return ResponseEntity.ok(ApiResponse.ok(usersUseCase.getUser(id)));
    }

    @Override
    @AuditAction(entityType = "user", action = "CREATE")
    public ResponseEntity<ApiResponse<User>> createUser(Map<String, String> body) {
        var user = usersUseCase.createUser(
            body.get("email"),
            body.get("fullName"),
            body.getOrDefault("role", "user")
        );
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @Override
    @AuditAction(entityType = "user", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(String id) {
        usersUseCase.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated"));
    }

    @Override
    @AuditAction(entityType = "user", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(String id) {
        usersUseCase.reactivateUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User reactivated"));
    }

    @Override
    @AuditAction(entityType = "user", action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(String id) {
        usersUseCase.softDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted"));
    }

    @Override
    @AuditAction(entityType = "user", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> restoreUser(String id) {
        usersUseCase.restoreUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User restored"));
    }

    @Override
    @AuditAction(entityType = "user", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(String id, Map<String, String> body) {
        usersUseCase.changeUserRole(id, body.get("role"));
        return ResponseEntity.ok(ApiResponse.ok("Role updated"));
    }
}
