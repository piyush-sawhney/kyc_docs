package com.kycdocs.application.setup.dto;

import com.kycdocs.domain.user.User;

import java.util.List;
import java.util.Objects;

public record SetupVerifyResult(
    String token,
    User user,
    List<String> recoveryCodes
) {
    public SetupVerifyResult {
        Objects.requireNonNull(recoveryCodes, "recoveryCodes must not be null");
    }
}
