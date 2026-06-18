package com.kycdocs.api.audit;

import com.kycdocs.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Audit Logs")
@RequestMapping("/api/audit-logs")
public interface AuditApi {

    @Operation(summary = "List audit logs")
    @GetMapping
    ResponseEntity<ApiResponse<Map<String, Object>>> listAuditLogs(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String entityId,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "false") boolean isAdmin);
}
