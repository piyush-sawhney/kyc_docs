package com.kycdocs.infrastructure.security;

import com.kycdocs.domain.permission.PermissionRepository;
import com.kycdocs.domain.user.UserRole;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.stream.Collectors;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final PermissionRepository permissionRepository;

    public CustomPermissionEvaluator(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return hasPermission(auth, permission.toString());
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return hasPermission(auth, permission.toString());
    }

    private boolean hasPermission(Authentication auth, String permissionKey) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        if (auth.getPrincipal() instanceof JwtAuthenticationFilter.AuthUser authUser) {
            var permissions = permissionRepository.findAll();
            var keys = permissions.stream().map(p -> p.getKey()).collect(Collectors.toSet());
            return keys.contains(permissionKey);
        }

        return false;
    }
}
