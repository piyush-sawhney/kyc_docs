package com.kycdocs.service.auth;

import com.kycdocs.domain.user.UserId;

import java.util.List;

/**
 * Interface: domain service for recovery code generation and verification.
 */
public interface RecoveryCodeService {

    List<String> generateRecoveryCodes(UserId userId);

    boolean verifyRecoveryCode(UserId userId, String code);

    int countUnusedCodes(UserId userId);

    boolean hasUnusedCodes(UserId userId);
}
