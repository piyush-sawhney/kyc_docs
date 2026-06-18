package com.kycdocs.application.clients.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClientCommand(
    @NotBlank String name
) {}
