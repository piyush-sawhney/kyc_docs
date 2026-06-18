package com.kycdocs.api.permissions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SetUserPermissionsCommand(
    @NotEmpty List<@NotBlank String> permissionIds
) {}
