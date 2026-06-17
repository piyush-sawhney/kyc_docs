package com.kycdocs.domain.auth;

import com.kycdocs.domain.user.UserId;

import java.util.List;
import java.util.Optional;

public interface RecoveryCodeRepository {

    List<RecoveryCode> findByUserId(UserId userId);

    List<RecoveryCode> findUnusedByUserId(UserId userId);

    Optional<RecoveryCode> findById(RecoveryCodeId id);

    RecoveryCode save(RecoveryCode recoveryCode);

    void delete(RecoveryCodeId id);
}
