package com.kycdocs.infrastructure.persistence.jpa.audit;

import com.kycdocs.domain.audit.AuditLog;
import com.kycdocs.domain.audit.AuditLogRepository;
import com.kycdocs.domain.user.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaAuditLogRepositoryAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository springRepo;
    private final AuditLogJpaMapper mapper;

    public JpaAuditLogRepositoryAdapter(SpringDataAuditLogRepository springRepo, AuditLogJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public List<AuditLog> findByUserId(UserId userId) {
        return springRepo.findByUserIdOrderByCreatedAtDesc(userId.value()).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findAll(int page, int size) {
        var pageIndex = Math.max(0, page - 1);
        return springRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(pageIndex, size))
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return springRepo.count();
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        var saved = springRepo.save(mapper.toJpa(auditLog));
        return mapper.toDomain(saved);
    }
}
