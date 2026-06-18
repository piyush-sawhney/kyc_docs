package com.kycdocs.application.users.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeUserRoleCommand(
    @NotBlank String role
) {}
