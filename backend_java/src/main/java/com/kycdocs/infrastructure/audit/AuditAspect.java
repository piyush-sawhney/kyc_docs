package com.kycdocs.infrastructure.audit;

import com.kycdocs.api.common.annotation.AuditAction;
import com.kycdocs.domain.audit.AuditLog;
import com.kycdocs.domain.audit.AuditLogRepository;
import com.kycdocs.domain.user.UserId;
import com.kycdocs.infrastructure.security.JwtAuthenticationFilter.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Around("@annotation(auditAction)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        var startTime = System.currentTimeMillis();

        try {
            var result = joinPoint.proceed();
            logAudit(auditAction, joinPoint, null, result, null);
            return result;
        } catch (Exception e) {
            logAudit(auditAction, joinPoint, null, null, e.getMessage());
            throw e;
        }
    }

    private void logAudit(AuditAction auditAction, ProceedingJoinPoint joinPoint,
                          Object oldValues, Object newValues, String errorMessage) {
        try {
            var request = getCurrentRequest();
            if (request == null) return;

            var userId = extractUserId(request);
            var ipAddress = request.getRemoteAddr();
            var userAgent = request.getHeader("User-Agent");
            var entityId = extractEntityId(request);
            var description = generateDescription(auditAction, joinPoint, request, newValues);

            var auditLog = new AuditLog(
                UUID.randomUUID(), Instant.now(), Instant.now(),
                userId != null ? UserId.fromString(userId) : null,
                auditAction.action(),
                auditAction.entityType(),
                entityId,
                description,
                null,
                newValues != null ? newValues.toString() : null,
                ipAddress,
                userAgent
            );

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to persist audit log", e);
        }
    }

    private String extractUserId(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser.userId();
        }
        return null;
    }

    private String extractEntityId(HttpServletRequest request) {
        var path = request.getRequestURI();
        var segments = path.split("/");
        for (int i = 1; i < segments.length; i++) {
            if (i + 1 < segments.length && (
                segments[i - 1].equals("users") ||
                segments[i - 1].equals("clients") ||
                segments[i - 1].equals("documents") ||
                segments[i - 1].equals("document-types")
            )) {
                return segments[i];
            }
        }
        return null;
    }

    private String generateDescription(AuditAction auditAction, ProceedingJoinPoint joinPoint,
                                       HttpServletRequest request, Object result) {
        var path = request.getRequestURI();
        var action = auditAction.action();
        var entityType = auditAction.entityType();

        var userName = extractUserId(request);
        if (userName == null) userName = "System";

        if (entityType.equals("client")) {
            if (action.equals("CREATE")) return "%s created client".formatted(userName);
            if (action.equals("UPDATE")) {
                if (path.contains("/merge")) return "%s merged client".formatted(userName);
                return "%s updated client".formatted(userName);
            }
            if (action.equals("DELETE")) return "%s deleted client".formatted(userName);
        }

        if (entityType.equals("document")) {
            if (action.equals("CREATE")) return "%s uploaded document".formatted(userName);
            if (action.equals("UPDATE")) {
                if (path.contains("/clear-image")) return "%s removed image".formatted(userName);
                if (path.contains("/metadata")) return "%s updated document details".formatted(userName);
                if (path.contains("/rotate")) return "%s rotated image".formatted(userName);
                if (path.contains("/crop")) return "%s cropped image".formatted(userName);
                if (path.contains("/optimize")) return "%s optimized image".formatted(userName);
                var method = request.getMethod();
                if (method.equals("PUT")) return "%s re-uploaded document".formatted(userName);
            }
            if (action.equals("DELETE")) return "%s deleted document".formatted(userName);
        }

        return "%s %s %s".formatted(userName, action, entityType);
    }

    private HttpServletRequest getCurrentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
