package com.imtidad.dao;

import com.imtidad.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC repository for writing audit log entries.
 */
public class AuditLogRepository {

    /**
     * Persists a single audit log entry.
     *
     * @param userId     the user performing the action
     * @param action     description of the action
     * @param entityType type of affected entity
     * @param entityId   id of affected entity
     * @throws SQLException if persistence fails
     */
    public void insertAuditLog(Integer userId, String action, String entityType, Integer entityId) throws SQLException {
        String query = "INSERT INTO AUDIT_LOG (user_id, action_performed, entity_type, entity_id) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {
            if (userId != null) {
                pstmt.setInt(1, userId);
            } else {
                pstmt.setNull(1, java.sql.Types.NUMERIC);
            }
            pstmt.setString(2, action);
            pstmt.setString(3, entityType);
            if (entityId != null) {
                pstmt.setInt(4, entityId);
            } else {
                pstmt.setNull(4, java.sql.Types.NUMERIC);
            }
            pstmt.executeUpdate();
        }
    }
}
