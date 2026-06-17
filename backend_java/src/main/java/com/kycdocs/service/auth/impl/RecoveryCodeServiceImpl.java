package com.kycdocs.service.auth.impl;

import com.kycdocs.domain.auth.RecoveryCode;
import com.kycdocs.domain.auth.RecoveryCodeId;
import com.kycdocs.domain.auth.RecoveryCodeRepository;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.service.auth.RecoveryCodeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class RecoveryCodeServiceImpl implements RecoveryCodeService {

    private static final int CODE_COUNT = 5;
    private static final int CODE_BYTE_LENGTH = 10;

    private final RecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public RecoveryCodeServiceImpl(RecoveryCodeRepository recoveryCodeRepository, PasswordEncoder passwordEncoder) {
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public List<String> generateRecoveryCodes(UserId userId) {
        var codes = new ArrayList<String>();
        var now = Instant.now();

        for (int i = 0; i < CODE_COUNT; i++) {
            var rawBytes = new byte[CODE_BYTE_LENGTH];
            secureRandom.nextBytes(rawBytes);
            var rawCode = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
            var codeHash = passwordEncoder.encode(rawCode);

            var recoveryCode = new RecoveryCode(
                UUID.randomUUID(), now, now, userId, codeHash
            );
            recoveryCodeRepository.save(recoveryCode);
            codes.add(rawCode);
        }

        return codes;
    }

    @Override
    public boolean verifyRecoveryCode(UserId userId, String code) {
        var recoveryCodes = recoveryCodeRepository.findUnusedByUserId(userId);
        for (var recoveryCode : recoveryCodes) {
            if (passwordEncoder.matches(code, recoveryCode.getCodeHash())) {
                recoveryCode.markAsUsed();
                recoveryCodeRepository.save(recoveryCode);
                return true;
            }
        }
        return false;
    }

    @Override
    public int countUnusedCodes(UserId userId) {
        return recoveryCodeRepository.findUnusedByUserId(userId).size();
    }

    @Override
    public boolean hasUnusedCodes(UserId userId) {
        return countUnusedCodes(userId) > 0;
    }
}
