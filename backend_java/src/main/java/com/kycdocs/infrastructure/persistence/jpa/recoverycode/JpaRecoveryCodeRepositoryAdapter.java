package com.kycdocs.infrastructure.persistence.jpa.recoverycode;

import com.kycdocs.domain.auth.RecoveryCode;
import com.kycdocs.domain.auth.RecoveryCodeId;
import com.kycdocs.domain.auth.RecoveryCodeRepository;
import com.kycdocs.domain.user.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaRecoveryCodeRepositoryAdapter implements RecoveryCodeRepository {

    private final SpringDataRecoveryCodeRepository springRepo;
    private final RecoveryCodeJpaMapper mapper;

    public JpaRecoveryCodeRepositoryAdapter(SpringDataRecoveryCodeRepository springRepo, RecoveryCodeJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public List<RecoveryCode> findByUserId(UserId userId) {
        return springRepo.findByUserId(userId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<RecoveryCode> findUnusedByUserId(UserId userId) {
        return springRepo.findByUserIdAndIsUsedFalse(userId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<RecoveryCode> findById(RecoveryCodeId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public RecoveryCode save(RecoveryCode recoveryCode) {
        var saved = springRepo.save(mapper.toJpa(recoveryCode));
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(RecoveryCodeId id) {
        springRepo.deleteById(id.value());
    }
}
