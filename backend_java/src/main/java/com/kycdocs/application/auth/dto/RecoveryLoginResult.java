package com.kycdocs.application.auth.dto;

public record RecoveryLoginResult(
    String token,
    String userId,
    String email,
    String role,
    Boolean recoveryCodesMissing
) {}
