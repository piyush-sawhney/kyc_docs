package com.kycdocs.infrastructure.config;

import com.kycdocs.application.audit.AuditUseCase;
import com.kycdocs.application.audit.impl.AuditUseCaseImpl;
import com.kycdocs.application.auth.AuthUseCase;
import com.kycdocs.application.auth.impl.AuthUseCaseImpl;
import com.kycdocs.application.clients.ClientsUseCase;
import com.kycdocs.application.clients.impl.ClientsUseCaseImpl;
import com.kycdocs.application.documenttypes.DocumentTypesUseCase;
import com.kycdocs.application.documenttypes.impl.DocumentTypesUseCaseImpl;
import com.kycdocs.application.documents.DocumentUseCase;
import com.kycdocs.application.documents.impl.DocumentUseCaseImpl;
import com.kycdocs.application.permissions.PermissionsUseCase;
import com.kycdocs.application.permissions.impl.PermissionsUseCaseImpl;
import com.kycdocs.application.setup.SetupUseCase;
import com.kycdocs.application.setup.impl.SetupUseCaseImpl;
import com.kycdocs.application.users.UsersUseCase;
import com.kycdocs.application.users.impl.UsersUseCaseImpl;
import com.kycdocs.domain.auth.RecoveryCodeRepository;
import com.kycdocs.domain.audit.AuditLogRepository;
import com.kycdocs.domain.client.ClientRepository;
import com.kycdocs.domain.document.DocumentRepository;
import com.kycdocs.domain.documenttype.DocumentTypeRepository;
import com.kycdocs.domain.permission.PermissionRepository;
import com.kycdocs.domain.user.UserRepository;
import com.kycdocs.infrastructure.persistence.jpa.permission.SpringDataUserPermissionRepository;
import com.kycdocs.infrastructure.QrCodeGenerator;
import com.kycdocs.infrastructure.security.JwtTokenProvider;
import com.kycdocs.infrastructure.security.TotpProvider;
import com.kycdocs.service.auth.RecoveryCodeService;
import com.kycdocs.service.compliance.AdminSafetyPolicy;
import com.kycdocs.service.document.DocumentEncryptionService;
import com.kycdocs.service.imaging.ImageProcessingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseBeanConfiguration {

    @Bean
    public AuthUseCase authUseCase(UserRepository userRepository,
                                   RecoveryCodeRepository recoveryCodeRepository,
                                   RecoveryCodeService recoveryCodeService,
                                   TotpProvider totpProvider,
                                   JwtTokenProvider jwtTokenProvider,
                                   QrCodeGenerator qrCodeGenerator) {
        return new AuthUseCaseImpl(
            userRepository, recoveryCodeRepository,
            recoveryCodeService, totpProvider, jwtTokenProvider, qrCodeGenerator
        );
    }

    @Bean
    public UsersUseCase usersUseCase(UserRepository userRepository,
                                     AdminSafetyPolicy adminSafetyPolicy) {
        return new UsersUseCaseImpl(userRepository, adminSafetyPolicy);
    }

    @Bean
    public ClientsUseCase clientsUseCase(ClientRepository clientRepository,
                                         DocumentRepository documentRepository) {
        return new ClientsUseCaseImpl(clientRepository, documentRepository);
    }

    @Bean
    public DocumentUseCase documentUseCase(DocumentRepository documentRepository,
                                           DocumentEncryptionService encryptionService,
                                           ImageProcessingService imageProcessingService) {
        return new DocumentUseCaseImpl(documentRepository, encryptionService, imageProcessingService);
    }

    @Bean
    public DocumentTypesUseCase documentTypesUseCase(DocumentTypeRepository documentTypeRepository) {
        return new DocumentTypesUseCaseImpl(documentTypeRepository);
    }

    @Bean
    public PermissionsUseCase permissionsUseCase(PermissionRepository permissionRepository,
                                                 SpringDataUserPermissionRepository userPermissionRepository) {
        return new PermissionsUseCaseImpl(permissionRepository, userPermissionRepository);
    }

    @Bean
    public AuditUseCase auditUseCase(AuditLogRepository auditLogRepository) {
        return new AuditUseCaseImpl(auditLogRepository);
    }

    @Bean
    public SetupUseCase setupUseCase(UserRepository userRepository,
                                     PermissionRepository permissionRepository,
                                     SpringDataUserPermissionRepository userPermissionRepository,
                                     DocumentTypeRepository documentTypeRepository,
                                     TotpProvider totpProvider,
                                     JwtTokenProvider jwtTokenProvider,
                                     RecoveryCodeService recoveryCodeService,
                                     QrCodeGenerator qrCodeGenerator) {
        return new SetupUseCaseImpl(
            userRepository, permissionRepository, userPermissionRepository,
            documentTypeRepository, totpProvider, jwtTokenProvider, recoveryCodeService, qrCodeGenerator
        );
    }
}
