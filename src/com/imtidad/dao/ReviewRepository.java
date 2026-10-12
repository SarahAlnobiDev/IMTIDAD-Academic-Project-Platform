package com.imtidad.dao;

import com.imtidad.contract.IReviewRepository;
import model.ContinuationRequestRecord;
import model.ProjectType;
import model.PublicationStatus;
import model.ReviewQueueItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Oracle JDBC persistence for project and continuation-request reviews. */
public class ReviewRepository implements IReviewRepository {

    private final LineageRepository lineageRepository = new LineageRepository();

    @Override
    public List<ReviewQueueItem> findProjectsAwaitingReview(Connection connection, int reviewerId) throws SQLException {
        String sql = "SELECT p.project_id, p.title, p.project_type, p.problem_statement, p.publication_status, p.created_at, " +
                "NVL(u.username, 'غير محدد') AS owner_name, " +
                "NVL(gd.dept_name, cd.dept_name) AS department_name " +
                "FROM PROJECT p " +
                "LEFT JOIN USER_ACCOUNT u ON u.user_id = p.owner_id " +
                "LEFT JOIN GRADUATION_PROJECT_DETAILS gpd ON gpd.project_id = p.project_id " +
                "LEFT JOIN DEPARTMENT gd ON gd.dept_id = gpd.department_id " +
                "LEFT JOIN COURSE_PROJECT_DETAILS cpd ON cpd.project_id = p.project_id " +
                "LEFT JOIN COURSE c ON c.course_id = cpd.course_id " +
                "LEFT JOIN DEPARTMENT cd ON cd.dept_id = c.dept_id " +
                "WHERE p.publication_status = 'PENDING_REVIEW' AND p.reviewer_id = ? " +
                "ORDER BY p.created_at ASC, p.project_id ASC";
        List<ReviewQueueItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reviewerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                Timestamp timestamp = result.getTimestamp("created_at");
                LocalDateTime createdAt = timestamp == null ? null : timestamp.toLocalDateTime();
                items.add(new ReviewQueueItem(
                        result.getInt("project_id"), result.getString("title"),
                        ProjectType.valueOf(result.getString("project_type").trim().toUpperCase()),
                        result.getString("owner_name"), safe(result.getString("department_name"), "غير محدد"),
                        result.getString("problem_statement"),
                        PublicationStatus.valueOf(result.getString("publication_status").trim().toUpperCase()),
                        createdAt
                ));
                }
            }
        }
        return items;
    }

    @Override
    public void saveProjectReview(Connection connection, int projectId, int reviewerId, String decision,
                                  String comments, boolean problemClarity, boolean datasetsValid,
                                  boolean ownershipProof, boolean fileIntegrity) throws SQLException {
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        if (!normalized.equals("APPROVE") && !normalized.equals("REQUEST_CHANGES") && !normalized.equals("REJECT")) {
            throw new SQLException("Unsupported review decision: " + decision);
        }
        String insert = "INSERT INTO REVIEW (project_id, reviewer_id, decision, comments, problem_clarity, " +
                "datasets_valid, ownership_proof, file_integrity) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setInt(1, projectId);
            statement.setInt(2, reviewerId);
            statement.setString(3, normalized);
            statement.setString(4, comments);
            statement.setInt(5, problemClarity ? 1 : 0);
            statement.setInt(6, datasetsValid ? 1 : 0);
            statement.setInt(7, ownershipProof ? 1 : 0);
            statement.setInt(8, fileIntegrity ? 1 : 0);
            statement.executeUpdate();
        }
        String newStatus = normalized.equals("APPROVE") ? "APPROVED" : "NEEDS_CHANGES";
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE PROJECT SET publication_status = ? WHERE project_id = ? AND reviewer_id = ?")) {
            statement.setString(1, newStatus);
            statement.setInt(2, projectId);
            statement.setInt(3, reviewerId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Project is not assigned to this reviewer: " + projectId);
            }
        }
    }

    @Override
    public List<ContinuationRequestRecord> findContinuationRequestsAwaitingReview(Connection connection) throws SQLException {
        lineageRepository.ensureSupportTables(connection);
        String sql = LineageRepository.baseRequestSelect() +
                " WHERE cr.request_status IN ('SUBMITTED','UNDER_REVIEW') ORDER BY cr.submitted_at ASC";
        List<ContinuationRequestRecord> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                items.add(LineageRepository.mapRequest(result));
            }
        }
        return items;
    }

    @Override
    public void reviewContinuationRequest(Connection connection, int requestId, int reviewerId,
                                          boolean approve, String reviewNotes) throws SQLException {
        lineageRepository.ensureSupportTables(connection);
        ContinuationRequestRecord request = findRequest(connection, requestId);
        if (request == null) {
            throw new SQLException("Continuation request was not found: " + requestId);
        }

        Integer childProjectId = request.getChildProjectId();
        if (approve && childProjectId == null) {
            childProjectId = createChildProject(connection, request);
        }

        String status = approve ? "APPROVED" : "REJECTED";
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE CONTINUATION_REQUEST SET request_status = ? WHERE request_id = ?")) {
            statement.setString(1, status);
            statement.setInt(2, requestId);
            statement.executeUpdate();
        }
        String detailUpdate = "UPDATE " + LineageRepository.DETAIL_TABLE +
                " SET review_notes = ?, reviewer_id = ?, reviewed_at = CURRENT_TIMESTAMP, child_project_id = ? WHERE request_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(detailUpdate)) {
            statement.setString(1, reviewNotes);
            statement.setInt(2, reviewerId);
            if (childProjectId == null) statement.setNull(3, java.sql.Types.NUMERIC); else statement.setInt(3, childProjectId);
            statement.setInt(4, requestId);
            statement.executeUpdate();
        }
    }

    private ContinuationRequestRecord findRequest(Connection connection, int requestId) throws SQLException {
        String sql = LineageRepository.baseRequestSelect() + " WHERE cr.request_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? LineageRepository.mapRequest(result) : null;
            }
        }
    }

    private int createChildProject(Connection connection, ContinuationRequestRecord request) throws SQLException {
        int nextId;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT NVL(MAX(project_id), 0) + 1 AS next_id FROM PROJECT")) {
            result.next();
            nextId = result.getInt("next_id");
        }

        String parentType;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT project_type FROM PROJECT WHERE project_id = ?")) {
            statement.setInt(1, request.getProjectId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Parent project was not found.");
                parentType = result.getString("project_type");
            }
        }

        String insert = "INSERT INTO PROJECT (project_id, title, problem_statement, publication_status, project_type, owner_id) " +
                "VALUES (?, ?, ?, 'DRAFT', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setInt(1, nextId);
            statement.setString(2, request.getProposedTitle());
            statement.setString(3, request.getGoals());
            statement.setString(4, parentType);
            statement.setInt(5, request.getRequesterId());
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO PROJECT_RELATION (parent_project_id, child_project_id, relation_type) VALUES (?, ?, ?)")) {
            statement.setInt(1, request.getProjectId());
            statement.setInt(2, nextId);
            statement.setString(3, request.getRelationType());
            statement.executeUpdate();
        }
        return nextId;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
