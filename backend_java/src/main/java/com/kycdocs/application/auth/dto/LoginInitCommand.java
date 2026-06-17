package com.kycdocs.application.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginInitCommand(
    @NotBlank @Email String email
) {}
