package com.imtidad.dao;

import com.imtidad.contract.ILineageRepository;
import model.ContinuationRequestRecord;
import model.LineageProjectNode;
import model.Project;
import model.ProjectType;
import model.PublicationStatus;
import model.RequestStatus;
import model.StoredProject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Oracle JDBC persistence for project lineage and continuation requests. */
public class LineageRepository implements ILineageRepository {

    public static final String DETAIL_TABLE = "CONTINUATION_REQUEST_DETAIL";

    @Override
    public void ensureSupportTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet ignored = statement.executeQuery("SELECT REQUEST_ID FROM " + DETAIL_TABLE + " WHERE 1 = 0")) {
            return;
        } catch (SQLException error) {
            if (error.getErrorCode() != 942) { // ORA-00942 table or view does not exist
                throw error;
            }
        }

        String ddl = "CREATE TABLE " + DETAIL_TABLE + " (" +
                "request_id NUMBER PRIMARY KEY, " +
                "proposed_title VARCHAR2(200) NOT NULL, " +
                "relation_type VARCHAR2(50) DEFAULT 'CONTINUATION' NOT NULL, " +
                "repository_url VARCHAR2(500), " +
                "attachment_path VARCHAR2(500), " +
                "review_notes CLOB, " +
                "reviewer_id NUMBER, " +
                "reviewed_at TIMESTAMP, " +
                "child_project_id NUMBER, " +
                "CONSTRAINT fk_cont_detail_request FOREIGN KEY (request_id) REFERENCES CONTINUATION_REQUEST(request_id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_cont_detail_reviewer FOREIGN KEY (reviewer_id) REFERENCES USER_ACCOUNT(user_id), " +
                "CONSTRAINT fk_cont_detail_child FOREIGN KEY (child_project_id) REFERENCES PROJECT(project_id), " +
                "CONSTRAINT chk_cont_detail_relation CHECK (relation_type IN ('CONTINUATION','EXTENSION','DERIVATIVE'))" +
                ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
        }
    }

    @Override
    public List<Project> findSelectableProjects(Connection connection) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT project_id, title, problem_statement, publication_status, project_type " +
                "FROM PROJECT WHERE publication_status = 'PUBLISHED' ORDER BY title";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                projects.add(new StoredProject(
                        result.getInt("project_id"),
                        result.getString("title"),
                        result.getString("problem_statement"),
                        parseStatus(result.getString("publication_status")),
                        ProjectType.valueOf(result.getString("project_type").trim().toUpperCase())
                ));
            }
        }
        return projects;
    }

    @Override
    public List<LineageProjectNode> findLineage(Connection connection, int rootProjectId) throws SQLException {
        List<LineageProjectNode> nodes = new ArrayList<>();
        String rootSql = "SELECT project_id, title, project_type, publication_status FROM PROJECT WHERE project_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(rootSql)) {
            statement.setInt(1, rootProjectId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    nodes.add(mapNode(result, null));
                }
            }
        }

        String childSql = "SELECT p.project_id, p.title, p.project_type, p.publication_status, r.relation_type " +
                "FROM PROJECT_RELATION r JOIN PROJECT p ON p.project_id = r.child_project_id " +
                "WHERE r.parent_project_id = ? ORDER BY r.relation_id";
        try (PreparedStatement statement = connection.prepareStatement(childSql)) {
            statement.setInt(1, rootProjectId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    nodes.add(mapNode(result, result.getString("relation_type")));
                }
            }
        }
        return nodes;
    }

    @Override
    public int createContinuationRequest(Connection connection, int projectId, int requesterId,
                                         String proposedTitle, String goals, String expectedTimeline,
                                         String skillSet, String relationType, String repositoryUrl,
                                         String attachmentPath) throws SQLException {
        ensureSupportTables(connection);
        String requestSql = "INSERT INTO CONTINUATION_REQUEST " +
                "(project_id, requester_id, goals, expected_timeline, skill_set, request_status) " +
                "VALUES (?, ?, ?, ?, ?, 'SUBMITTED')";
        int requestId;
        try (PreparedStatement statement = connection.prepareStatement(requestSql, new String[]{"REQUEST_ID"})) {
            statement.setInt(1, projectId);
            statement.setInt(2, requesterId);
            statement.setString(3, goals);
            statement.setString(4, expectedTimeline);
            statement.setString(5, skillSet);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not create continuation request.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Could not read continuation request ID.");
                }
                requestId = keys.getInt(1);
            }
        }

        String detailSql = "INSERT INTO " + DETAIL_TABLE +
                " (request_id, proposed_title, relation_type, repository_url, attachment_path) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(detailSql)) {
            statement.setInt(1, requestId);
            statement.setString(2, proposedTitle);
            statement.setString(3, relationType);
            statement.setString(4, blankToNull(repositoryUrl));
            statement.setString(5, blankToNull(attachmentPath));
            statement.executeUpdate();
        }
        return requestId;
    }

    @Override
    public List<ContinuationRequestRecord> findRequestsForUser(Connection connection, int requesterId) throws SQLException {
        ensureSupportTables(connection);
        String sql = baseRequestSelect() + " WHERE cr.requester_id = ? ORDER BY cr.submitted_at DESC";
        List<ContinuationRequestRecord> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requesterId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    requests.add(mapRequest(result));
                }
            }
        }
        return requests;
    }

    @Override
    public List<ContinuationRequestRecord> findRequestsForOwner(Connection connection, int ownerId) throws SQLException {
        ensureSupportTables(connection);
        String sql = baseRequestSelect() +
                " WHERE p.owner_id = ? AND cr.request_status IN ('SUBMITTED','UNDER_REVIEW') ORDER BY cr.submitted_at DESC";
        List<ContinuationRequestRecord> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    requests.add(mapRequest(result));
                }
            }
        }
        return requests;
    }

    @Override
    public Integer findProjectOwnerId(Connection connection, int projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT owner_id FROM PROJECT WHERE project_id = ?")) {
            statement.setInt(1, projectId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                int owner = result.getInt("owner_id");
                return result.wasNull() ? null : owner;
            }
        }
    }

    @Override
    public String findProjectStatus(Connection connection, int projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT publication_status FROM PROJECT WHERE project_id = ?")) {
            statement.setInt(1, projectId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("publication_status") : null;
            }
        }
    }

    @Override
    public boolean hasPendingRequest(Connection connection, int projectId, int requesterId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM CONTINUATION_REQUEST WHERE project_id = ? AND requester_id = ? " +
                        "AND request_status IN ('SUBMITTED','UNDER_REVIEW') FETCH FIRST 1 ROWS ONLY")) {
            statement.setInt(1, projectId);
            statement.setInt(2, requesterId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    @Override
    public void decideContinuationRequest(Connection connection, int requestId, int ownerId,
                                          boolean approve, String notes) throws SQLException {
        ensureSupportTables(connection);
        ContinuationRequestRecord request = findRequestById(connection, requestId);
        if (request == null) {
            throw new SQLException("Continuation request was not found: " + requestId);
        }
        Integer actualOwner = findProjectOwnerId(connection, request.getProjectId());
        if (actualOwner == null || actualOwner != ownerId) {
            throw new SQLException("Only the original project owner can decide this continuation request.");
        }
        if (request.getRequestStatus() != RequestStatus.SUBMITTED
                && request.getRequestStatus() != RequestStatus.UNDER_REVIEW) {
            throw new SQLException("Continuation request was already decided.");
        }

        Integer childProjectId = request.getChildProjectId();
        if (approve && childProjectId == null) {
            childProjectId = createChildProject(connection, request);
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE CONTINUATION_REQUEST SET request_status = ? WHERE request_id = ?")) {
            statement.setString(1, approve ? "APPROVED" : "REJECTED");
            statement.setInt(2, requestId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not update continuation request: " + requestId);
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + DETAIL_TABLE +
                        " SET review_notes = ?, reviewed_at = CURRENT_TIMESTAMP, child_project_id = ? WHERE request_id = ?")) {
            statement.setString(1, blankToNull(notes));
            if (childProjectId == null) statement.setNull(2, java.sql.Types.NUMERIC);
            else statement.setInt(2, childProjectId);
            statement.setInt(3, requestId);
            statement.executeUpdate();
        }
    }

    private ContinuationRequestRecord findRequestById(Connection connection, int requestId) throws SQLException {
        String sql = baseRequestSelect() + " WHERE cr.request_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapRequest(result) : null;
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
        Integer reviewerId = null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT project_type, reviewer_id FROM PROJECT WHERE project_id = ?")) {
            statement.setInt(1, request.getProjectId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Parent project was not found.");
                parentType = result.getString("project_type");
                int reviewer = result.getInt("reviewer_id");
                if (!result.wasNull()) reviewerId = reviewer;
            }
        }

        String insert = "INSERT INTO PROJECT " +
                "(project_id, title, problem_statement, publication_status, project_type, owner_id, reviewer_id, repository_url) " +
                "VALUES (?, ?, ?, 'DRAFT', ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setInt(1, nextId);
            statement.setString(2, request.getProposedTitle());
            statement.setString(3, request.getGoals());
            statement.setString(4, parentType);
            statement.setInt(5, request.getRequesterId());
            if (reviewerId == null) statement.setNull(6, java.sql.Types.NUMERIC);
            else statement.setInt(6, reviewerId);
            statement.setString(7, blankToNull(request.getRepositoryUrl()));
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

    public static String baseRequestSelect() {
        return "SELECT cr.request_id, cr.project_id, p.title AS project_title, cr.requester_id, " +
                "u.username AS requester_name, d.proposed_title, cr.goals, cr.expected_timeline, cr.skill_set, " +
                "d.relation_type, d.repository_url, d.attachment_path, cr.request_status, cr.submitted_at, " +
                "d.review_notes, d.child_project_id " +
                "FROM CONTINUATION_REQUEST cr " +
                "JOIN PROJECT p ON p.project_id = cr.project_id " +
                "LEFT JOIN USER_ACCOUNT u ON u.user_id = cr.requester_id " +
                "LEFT JOIN " + DETAIL_TABLE + " d ON d.request_id = cr.request_id";
    }

    public static ContinuationRequestRecord mapRequest(ResultSet result) throws SQLException {
        Timestamp submitted = result.getTimestamp("submitted_at");
        int child = result.getInt("child_project_id");
        Integer childId = result.wasNull() ? null : child;
        return new ContinuationRequestRecord(
                result.getInt("request_id"), result.getInt("project_id"), result.getString("project_title"),
                result.getInt("requester_id"), safe(result.getString("requester_name"), "مستخدم"),
                safe(result.getString("proposed_title"), "امتداد للمشروع"), result.getString("goals"),
                result.getString("expected_timeline"), result.getString("skill_set"),
                safe(result.getString("relation_type"), "CONTINUATION"), result.getString("repository_url"),
                result.getString("attachment_path"), parseRequestStatus(result.getString("request_status")),
                submitted == null ? null : submitted.toLocalDateTime(), result.getString("review_notes"), childId
        );
    }

    private LineageProjectNode mapNode(ResultSet result, String relation) throws SQLException {
        return new LineageProjectNode(result.getInt("project_id"), result.getString("title"),
                ProjectType.valueOf(result.getString("project_type").trim().toUpperCase()),
                parseStatus(result.getString("publication_status")), relation);
    }

    private static PublicationStatus parseStatus(String value) {
        try { return PublicationStatus.valueOf(value == null ? "DRAFT" : value.trim().toUpperCase()); }
        catch (IllegalArgumentException error) { return PublicationStatus.DRAFT; }
    }

    private static RequestStatus parseRequestStatus(String value) {
        try { return RequestStatus.valueOf(value == null ? "SUBMITTED" : value.trim().toUpperCase()); }
        catch (IllegalArgumentException error) { return RequestStatus.SUBMITTED; }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
