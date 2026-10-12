package com.imtidad.contract;

/**
 * Service contract for audit logging of sensitive actions.
 */
public interface IAuditLogService {

    /**
     * Records an action in the audit log.
     *
     * @param userId     the id of the user performing the action; may be null if unknown
     * @param action     a short description of the action performed
     * @param entityType the type of entity affected (e.g., PROJECT, USER_ACCOUNT)
     * @param entityId   the id of the entity affected; may be null
     */
    void logAction(Integer userId, String action, String entityType, Integer entityId);

    /**
     * Convenience overload for actions without a specific entity.
     */
    default void logAction(Integer userId, String action) {
        logAction(userId, action, null, null);
    }
}
