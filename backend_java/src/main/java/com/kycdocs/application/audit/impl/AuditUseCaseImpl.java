package com.kycdocs.application.audit.impl;

import com.kycdocs.application.audit.AuditUseCase;
import com.kycdocs.domain.audit.AuditLogRepository;
import com.kycdocs.domain.user.UserId;

import java.util.HashMap;
import java.util.Map;

public class AuditUseCaseImpl implements AuditUseCase {

    private final AuditLogRepository auditLogRepository;

    public AuditUseCaseImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public Map<String, Object> listAuditLogs(int page, int limit, String entityType,
                                             String action, String userId, String entityId,
                                             String search, boolean isAdmin) {
        var result = new HashMap<String, Object>();

        if (!isAdmin && userId != null) {
            var logs = auditLogRepository.findByUserId(UserId.fromString(userId));
            result.put("data", logs);
            result.put("total", logs.size());
        } else {
            var logs = auditLogRepository.findAll(page, limit);
            result.put("data", logs);
            result.put("total", auditLogRepository.count());
        }

        result.put("page", page);
        result.put("limit", limit);
        return result;
    }
}
