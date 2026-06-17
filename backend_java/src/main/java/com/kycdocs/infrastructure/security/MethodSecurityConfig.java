package com.kycdocs.infrastructure.security;

import com.kycdocs.api.common.annotation.RequirePermissions;
import com.kycdocs.domain.permission.PermissionRepository;
import com.kycdocs.domain.user.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public MethodSecurityConfig(PermissionRepository permissionRepository, UserRepository userRepository) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }
}
