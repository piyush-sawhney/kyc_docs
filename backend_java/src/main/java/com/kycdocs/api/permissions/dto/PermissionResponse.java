package com.kycdocs.api.permissions.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kycdocs.domain.permission.Permission;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PermissionResponse(
    String id,
    String key,
    String label,
    String group
) {
    public static PermissionResponse from(Permission permission) {
        Objects.requireNonNull(permission, "permission must not be null");
        return new PermissionResponse(
            Objects.toString(permission.getId(), null),
            permission.getKey(),
            permission.getLabel(),
            permission.getPermissionGroup()
        );
    }
}
