package com.kycdocs.application.auth.dto;

import java.util.List;

public record LoginResult(
    String token,
    String userId,
    String email,
    String role,
    Boolean recoveryCodesMissing,
    List<String> recoveryCodes
) {}
