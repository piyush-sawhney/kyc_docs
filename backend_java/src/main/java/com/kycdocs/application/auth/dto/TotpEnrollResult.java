package com.kycdocs.application.auth.dto;

import java.util.List;

public record TotpEnrollResult(
    boolean success,
    String token,
    String userId,
    List<String> recoveryCodes
) {}
