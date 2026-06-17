package com.kycdocs.application.setup.impl;

import com.kycdocs.application.setup.SetupUseCase;
import com.kycdocs.domain.documenttype.DocumentType;
import com.kycdocs.domain.documenttype.DocumentTypeRepository;
import com.kycdocs.domain.permission.Permission;
import com.kycdocs.domain.permission.PermissionRepository;
import com.kycdocs.domain.user.*;
import com.kycdocs.infrastructure.persistence.jpa.permission.SpringDataUserPermissionRepository;
import com.kycdocs.infrastructure.persistence.jpa.permission.UserPermissionJpaEntity;
import com.kycdocs.infrastructure.security.JwtTokenProvider;
import com.kycdocs.infrastructure.security.TotpProvider;
import com.kycdocs.service.auth.RecoveryCodeService;
import com.kycdocs.shared.exception.ValidationException;

import java.util.*;

public class SetupUseCaseImpl implements SetupUseCase {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final SpringDataUserPermissionRepository userPermissionRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final TotpProvider totpProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final RecoveryCodeService recoveryCodeService;

    public SetupUseCaseImpl(UserRepository userRepository,
                            PermissionRepository permissionRepository,
                            SpringDataUserPermissionRepository userPermissionRepository,
                            DocumentTypeRepository documentTypeRepository,
                            TotpProvider totpProvider,
                            JwtTokenProvider jwtTokenProvider,
                            RecoveryCodeService recoveryCodeService) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.totpProvider = totpProvider;
        this.jwtTokenProvider = jwtTokenProvider;
        this.recoveryCodeService = recoveryCodeService;
    }

    @Override
    public Map<String, Boolean> needsSetup() {
        var activeUsers = userRepository.findAllActive();
        return Map.of("needsSetup", activeUsers.isEmpty());
    }

    @Override
    public Map<String, Object> init(String email, String fullName) {
        if (!userRepository.findAllActive().isEmpty()) {
            throw new ValidationException("System is already set up");
        }

        var emailVo = new Email(email);
        var user = User.create(emailVo, fullName, UserRole.ADMIN);
        var secret = totpProvider.generateSecret();
        user.assignTotpSecret(secret);
        userRepository.save(user);

        seedDefaultPermissions();
        seedDefaultDocumentTypes();

        var qrDataUrl = totpProvider.generateTotpUri(secret, email);
        var setupToken = jwtTokenProvider.generateToken(
            user.getId().toString(), email, "SETUP"
        );

        var result = new HashMap<String, Object>();
        result.put("qrDataUrl", qrDataUrl);
        result.put("setupToken", setupToken);
        return result;
    }

    @Override
    public Map<String, Object> verify(String setupToken, String totpCode) {
        var claims = jwtTokenProvider.validateToken(setupToken);
        var userId = UserId.fromString(claims.getSubject());

        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ValidationException("User not found"));

        if (!totpProvider.verify(totpCode, user.getTotpSecret())) {
            throw new ValidationException("Invalid TOTP code");
        }

        user.verifyTotp();
        userRepository.save(user);

        assignAllPermissions(userId);
        var recoveryCodes = recoveryCodeService.generateRecoveryCodes(user.getId());

        var token = jwtTokenProvider.generateToken(
            user.getId(), user.getEmail(), user.getRole()
        );

        var result = new HashMap<String, Object>();
        result.put("token", token);
        result.put("user", Map.of(
            "id", user.getId().toString(),
            "email", user.getEmail().value(),
            "fullName", user.getFullName(),
            "role", user.getRole().getValue()
        ));
        result.put("recoveryCodes", recoveryCodes);
        return result;
    }

    private void seedDefaultPermissions() {
        var existing = permissionRepository.findAll();
        if (!existing.isEmpty()) return;

        var permissions = List.of(
            Map.of("key", "client:create", "label", "Create Client", "group", "Clients"),
            Map.of("key", "client:read", "label", "Read Client", "group", "Clients"),
            Map.of("key", "client:update", "label", "Update Client", "group", "Clients"),
            Map.of("key", "client:delete", "label", "Delete Client", "group", "Clients"),
            Map.of("key", "client:merge", "label", "Merge Clients", "group", "Clients"),
            Map.of("key", "document:upload", "label", "Upload Document", "group", "Documents"),
            Map.of("key", "document:view", "label", "View Document", "group", "Documents"),
            Map.of("key", "document:view_file", "label", "View Document File", "group", "Documents"),
            Map.of("key", "document:edit", "label", "Edit Document", "group", "Documents"),
            Map.of("key", "document:delete", "label", "Delete Document", "group", "Documents"),
            Map.of("key", "document:decrypt_number", "label", "Decrypt Number", "group", "Documents"),
            Map.of("key", "user:create", "label", "Create User", "group", "Users"),
            Map.of("key", "user:view", "label", "View User", "group", "Users"),
            Map.of("key", "user:manage", "label", "Manage User", "group", "Users"),
            Map.of("key", "audit:view", "label", "View Audit", "group", "System"),
            Map.of("key", "permission:assign", "label", "Assign Permissions", "group", "System"),
            Map.of("key", "setup:manage", "label", "Manage Setup", "group", "System"),
            Map.of("key", "document_type:manage", "label", "Manage Document Types", "group", "System")
        );

        for (var p : permissions) {
            permissionRepository.save(new Permission(
                UUID.randomUUID(), null, null,
                p.get("key"), p.get("label"), p.get("group")
            ));
        }
    }

    private void seedDefaultDocumentTypes() {
        var names = List.of("PAN", "Aadhar", "Passport", "Driving License", "Voter ID", "OCI");
        for (var name : names) {
            if (documentTypeRepository.findByName(name).isEmpty()) {
                documentTypeRepository.save(DocumentType.create(name));
            }
        }
    }

    private void assignAllPermissions(UserId userId) {
        var allPermissions = permissionRepository.findAll();
        var userIdUuid = userId.value();

        for (var perm : allPermissions) {
            userPermissionRepository.save(new UserPermissionJpaEntity(userIdUuid, perm.getId()));
        }
    }
}
