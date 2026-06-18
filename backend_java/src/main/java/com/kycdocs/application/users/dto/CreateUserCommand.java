package com.kycdocs.application.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserCommand(
    @NotBlank @Email String email,
    @NotBlank String fullName,
    String role
) {}
