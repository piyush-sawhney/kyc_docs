package com.kycdocs.api.audit.impl;

import com.kycdocs.api.audit.AuditApi;
import com.kycdocs.api.common.ApiResponse;
import com.kycdocs.application.audit.AuditUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuditRestController implements AuditApi {

    private final AuditUseCase auditUseCase;

    public AuditRestController(AuditUseCase auditUseCase) {
        this.auditUseCase = auditUseCase;
    }

    @Override
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAuditLogs(int page, int size,
                                                                           String entityType, String action,
                                                                           String userId, String entityId,
                                                                           String search, boolean isAdmin) {
        return ResponseEntity.ok(ApiResponse.ok(
            auditUseCase.listAuditLogs(page, size, entityType, action, userId, entityId, search, isAdmin)
        ));
    }
}
