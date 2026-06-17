package com.kycdocs.application.audit;

import java.util.Map;

public interface AuditUseCase {

    Map<String, Object> listAuditLogs(int page, int limit, String entityType, String action,
                                      String userId, String entityId, String search, boolean isAdmin);
}
