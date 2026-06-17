package com.kycdocs.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
    @NotBlank String email,
    @NotBlank String totpCode
) {}
