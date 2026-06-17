package com.kycdocs.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RecoveryLoginCommand(
    @NotBlank String email,
    @NotBlank String recoveryCode
) {}
