package com.kycdocs.application.documenttypes.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentTypeCommand(
    @NotBlank String name
) {}
