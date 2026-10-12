package com.imtidad.service;

import com.imtidad.contract.IAuditLogService;
import com.imtidad.dao.AuditLogRepository;

import java.sql.SQLException;

/**
 * Service implementation for audit logging.
 * Failures are written to stderr but do not propagate, so the application
 * remains usable even if the AUDIT_LOG table is temporarily unavailable.
 */
public class AuditLogService implements IAuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService() {
        this(new AuditLogRepository());
    }

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void logAction(Integer userId, String action, String entityType, Integer entityId) {
        if (action == null || action.trim().isEmpty()) {
            return;
        }
        try {
            auditLogRepository.insertAuditLog(userId, action, entityType, entityId);
        } catch (SQLException e) {
            System.err.println("⚠️ Failed to write audit log [" + action + "]: " + e.getMessage());
        }
    }
}
