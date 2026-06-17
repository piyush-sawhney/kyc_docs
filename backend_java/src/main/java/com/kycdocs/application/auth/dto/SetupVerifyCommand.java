package com.kycdocs.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SetupVerifyCommand(
    @NotBlank String setupToken,
    @NotBlank String totpCode
) {}
