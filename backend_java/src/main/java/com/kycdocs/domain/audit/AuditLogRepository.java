package com.kycdocs.domain.audit;

import com.kycdocs.domain.user.UserId;

import java.util.List;

public interface AuditLogRepository {

    List<AuditLog> findByUserId(UserId userId);

    List<AuditLog> findAll(int page, int size);

    AuditLog save(AuditLog auditLog);
}
