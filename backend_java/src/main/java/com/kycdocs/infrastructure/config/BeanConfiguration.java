package com.kycdocs.infrastructure.config;

import com.kycdocs.domain.auth.RecoveryCodeRepository;
import com.kycdocs.domain.audit.AuditLogRepository;
import com.kycdocs.domain.client.ClientRepository;
import com.kycdocs.domain.document.DocumentRepository;
import com.kycdocs.domain.documenttype.DocumentTypeRepository;
import com.kycdocs.domain.permission.PermissionRepository;
import com.kycdocs.domain.user.UserRepository;
import com.kycdocs.infrastructure.persistence.jpa.audit.JpaAuditLogRepositoryAdapter;
import com.kycdocs.infrastructure.persistence.jpa.client.JpaClientRepositoryAdapter;
import com.kycdocs.infrastructure.persistence.jpa.document.JpaDocumentRepositoryAdapter;
import com.kycdocs.infrastructure.persistence.jpa.documenttype.JpaDocumentTypeRepositoryAdapter;
import com.kycdocs.infrastructure.persistence.jpa.permission.JpaPermissionRepositoryAdapter;
import com.kycdocs.infrastructure.persistence.jpa.recoverycode.JpaRecoveryCodeRepositoryAdapter;
import com.kycdocs.infrastructure.persistence.jpa.user.JpaUserRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires domain repository interfaces to infrastructure adapter implementations.
 * This is the composition root — explicit dependency injection for hexagonal architecture.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public UserRepository userRepository(JpaUserRepositoryAdapter adapter) {
        return adapter;
    }

    @Bean
    public ClientRepository clientRepository(JpaClientRepositoryAdapter adapter) {
        return adapter;
    }

    @Bean
    public DocumentRepository documentRepository(JpaDocumentRepositoryAdapter adapter) {
        return adapter;
    }

    @Bean
    public DocumentTypeRepository documentTypeRepository(JpaDocumentTypeRepositoryAdapter adapter) {
        return adapter;
    }

    @Bean
    public PermissionRepository permissionRepository(JpaPermissionRepositoryAdapter adapter) {
        return adapter;
    }

    @Bean
    public AuditLogRepository auditLogRepository(JpaAuditLogRepositoryAdapter adapter) {
        return adapter;
    }

    @Bean
    public RecoveryCodeRepository recoveryCodeRepository(JpaRecoveryCodeRepositoryAdapter adapter) {
        return adapter;
    }
}
