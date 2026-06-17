package com.kycdocs.infrastructure.security;

import com.kycdocs.api.common.annotation.RequirePermissions;
import com.kycdocs.domain.permission.Permission;
import com.kycdocs.domain.permission.PermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionsGuard {

    private final PermissionRepository permissionRepository;

    public PermissionsGuard(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public boolean check(String userId, String[] requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }

        var userPermissions = permissionRepository.findAll();
        var userPermissionKeys = userPermissions.stream()
            .map(Permission::getKey)
            .collect(Collectors.toSet());

        return Arrays.stream(requiredPermissions).allMatch(userPermissionKeys::contains);
    }

    public static RequirePermissions getRequiredPermissions() {
        var request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
        if (request == null) return null;

        var handler = request.getRequest();
        var annotation = handler.getClass().getAnnotation(RequirePermissions.class);
        if (annotation != null) return annotation;

        try {
            var method = handler.getClass().getMethod(
                request.getRequest().getMethod().toLowerCase()
            );
            return method.getAnnotation(RequirePermissions.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
