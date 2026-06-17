package com.kycdocs.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpEnrollCommand(
    @NotBlank String enrollToken,
    @NotBlank String totpCode
) {}
