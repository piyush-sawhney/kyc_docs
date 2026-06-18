package com.kycdocs.api.users.impl;

import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.api.common.annotation.AuditAction;
import com.kycdocs.api.users.UsersApi;
import com.kycdocs.api.users.dto.UserResponse;
import com.kycdocs.application.users.UsersUseCase;
import com.kycdocs.application.users.dto.ChangeUserRoleCommand;
import com.kycdocs.application.users.dto.CreateUserCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UsersRestController implements UsersApi {

    private final UsersUseCase usersUseCase;

    public UsersRestController(UsersUseCase usersUseCase) {
        this.usersUseCase = usersUseCase;
    }

    @Override
    public ResponseEntity<ApiResponse<List<UserResponse>>> listActiveUsers() {
        var users = usersUseCase.listActiveUsers();
        var response = users.stream().map(UserResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<List<UserResponse>>> listDeletedUsers() {
        var users = usersUseCase.listDeletedUsers();
        var response = users.stream().map(UserResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<UserResponse>> getUser(String id) {
        var user = usersUseCase.getUser(id);
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @Override
    @AuditAction(entityType = "user", action = "CREATE")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(CreateUserCommand command) {
        var user = usersUseCase.createUser(
            command.email(),
            command.fullName(),
            command.role() != null ? command.role() : "user"
        );
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @Override
    @AuditAction(entityType = "user", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(String id) {
        usersUseCase.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.message("User deactivated"));
    }

    @Override
    @AuditAction(entityType = "user", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(String id) {
        usersUseCase.reactivateUser(id);
        return ResponseEntity.ok(ApiResponse.message("User reactivated"));
    }

    @Override
    @AuditAction(entityType = "user", action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(String id) {
        usersUseCase.softDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.message("User deleted"));
    }

    @Override
    @AuditAction(entityType = "user", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> restoreUser(String id) {
        usersUseCase.restoreUser(id);
        return ResponseEntity.ok(ApiResponse.message("User restored"));
    }

    @Override
    @AuditAction(entityType = "user", action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(String id, ChangeUserRoleCommand command) {
        usersUseCase.changeUserRole(id, command.role());
        return ResponseEntity.ok(ApiResponse.message("Role updated"));
    }
}
