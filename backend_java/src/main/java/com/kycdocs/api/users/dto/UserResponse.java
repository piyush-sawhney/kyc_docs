package com.kycdocs.api.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kycdocs.domain.user.User;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
    String id,
    String email,
    String fullName,
    String role,
    boolean isActive,
    String createdAt,
    String updatedAt,
    String deletedAt
) {
    public static UserResponse from(User user) {
        Objects.requireNonNull(user, "user must not be null");
        return new UserResponse(
            Objects.toString(user.getId(), null),
            user.getEmail() != null ? user.getEmail().value() : null,
            user.getFullName(),
            user.getRole() != null ? user.getRole().getValue() : null,
            user.isActive(),
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
            user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null,
            user.getDeletedAt() != null ? user.getDeletedAt().toString() : null
        );
    }
}
