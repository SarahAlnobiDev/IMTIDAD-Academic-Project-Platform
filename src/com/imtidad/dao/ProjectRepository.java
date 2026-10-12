package com.imtidad.dao;

import com.imtidad.contract.IProjectRepository;
import model.CourseProject;
import model.GraduationProject;
import model.Project;
import model.ProjectAttachmentInfo;
import model.ProjectType;
import model.PublicationStatus;
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

/**
 * JDBC implementation of {@link IProjectRepository}.
 * Keeps the existing normalized project tables and adds only the workflow data
 * required for owner/reviewer assignment, repository links, and attachments.
 */
public class ProjectRepository implements IProjectRepository {

    private static final String TABLE_NAME = "PROJECT";

    private static final String DASHBOARD_SELECT =
            "SELECT p.project_id, p.title, p.problem_statement, p.publication_status, p.project_type, " +
            "p.owner_id, p.created_at, p.reviewer_id, p.repository_url, " +
            "COALESCE(sp.full_name, owner_user.username, 'غير محدد') AS owner_name, " +
            "sp.student_number AS owner_student_number, " +
            "COALESCE(reviewer_user.username, 'غير محدد') AS reviewer_name, " +
            "COALESCE(" +
            " (SELECT MAX(gpd.advisor) FROM GRADUATION_PROJECT_DETAILS gpd WHERE gpd.project_id = p.project_id), " +
            " (SELECT MAX(cpd.course_instructor) FROM COURSE_PROJECT_DETAILS cpd WHERE cpd.project_id = p.project_id)" +
            ") AS supervisor_name, " +
            "COALESCE(" +
            " (SELECT MAX(gpd.academic_year) FROM GRADUATION_PROJECT_DETAILS gpd WHERE gpd.project_id = p.project_id), " +
            " (SELECT MAX(cpd.academic_year) FROM COURSE_PROJECT_DETAILS cpd WHERE cpd.project_id = p.project_id)" +
            ") AS academic_year, " +
            "COALESCE(" +
            " (SELECT MAX(d.dept_name) FROM GRADUATION_PROJECT_DETAILS gpd JOIN DEPARTMENT d ON d.dept_id = gpd.department_id WHERE gpd.project_id = p.project_id), " +
            " (SELECT MAX(d.dept_name) FROM COURSE_PROJECT_DETAILS cpd JOIN COURSE c ON c.course_id = cpd.course_id JOIN DEPARTMENT d ON d.dept_id = c.dept_id WHERE cpd.project_id = p.project_id)" +
            ") AS department_name, " +
            "(SELECT MAX(c.course_name) FROM COURSE_PROJECT_DETAILS cpd JOIN COURSE c ON c.course_id = cpd.course_id WHERE cpd.project_id = p.project_id) AS course_name, " +
            "(SELECT MAX(cpd.semester) FROM COURSE_PROJECT_DETAILS cpd WHERE cpd.project_id = p.project_id) AS semester, " +
            "(SELECT LISTAGG(t.tech_name, ', ') WITHIN GROUP (ORDER BY t.tech_name) FROM PROJECT_TECHNOLOGY pt JOIN TECHNOLOGY t ON t.tech_id = pt.tech_id WHERE pt.project_id = p.project_id) AS tech_stack, " +
            "(SELECT LISTAGG(pm.member_name, CHR(10)) WITHIN GROUP (ORDER BY pm.member_id) FROM PROJECT_MEMBER pm WHERE pm.project_id = p.project_id) AS team_members " +
            "FROM PROJECT p " +
            "LEFT JOIN USER_ACCOUNT owner_user ON owner_user.user_id = p.owner_id " +
            "LEFT JOIN STUDENT_PROFILE sp ON sp.user_id = p.owner_id " +
            "LEFT JOIN USER_ACCOUNT reviewer_user ON reviewer_user.user_id = p.reviewer_id ";

    @Override
    public void ensureTableExists(Connection connection) throws SQLException {
        try (Statement verification = connection.createStatement();
             ResultSet ignored = verification.executeQuery(
                     "SELECT PROJECT_ID, TITLE, PROBLEM_STATEMENT, PUBLICATION_STATUS, PROJECT_TYPE, " +
                             "OWNER_ID, REVIEWER_ID, REPOSITORY_URL FROM PROJECT WHERE 1 = 0")) {
            // Successful query means the required core columns are available.
        }
    }

    @Override
    public List<Project> getAllProjects(Connection connection) throws SQLException {
        return readProjects(connection, DASHBOARD_SELECT +
                " ORDER BY p.created_at DESC, p.project_id DESC", null);
    }

    @Override
    public List<Project> getProjectsByOwner(Connection connection, int ownerId) throws SQLException {
        return readProjects(connection, DASHBOARD_SELECT +
                " WHERE p.owner_id = ? ORDER BY p.created_at DESC, p.project_id DESC",
                statement -> statement.setInt(1, ownerId));
    }

    @Override
    public List<Project> getProjectsForReviewer(Connection connection, int reviewerId) throws SQLException {
        return readProjects(connection, DASHBOARD_SELECT +
                " WHERE p.reviewer_id = ? AND p.publication_status <> 'DRAFT' " +
                "ORDER BY p.created_at DESC, p.project_id DESC",
                statement -> statement.setInt(1, reviewerId));
    }

    @Override
    public List<Project> getPublishedProjects(Connection connection) throws SQLException {
        return readProjects(connection, DASHBOARD_SELECT +
                " WHERE p.publication_status = 'PUBLISHED' ORDER BY p.created_at DESC, p.project_id DESC", null);
    }

    @Override
    public long countProjectsWithExtensions(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT parent_project_id) FROM PROJECT_RELATION";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : 0L;
        } catch (SQLException error) {
            // Older demo databases may not have the lineage table yet.
            if (error.getErrorCode() == 942) return 0L;
            throw error;
        }
    }

    @Override
    public Project findProjectById(Connection connection, int projectId) throws SQLException {
        List<Project> projects = readProjects(connection, DASHBOARD_SELECT +
                " WHERE p.project_id = ?", statement -> statement.setInt(1, projectId));
        return projects.isEmpty() ? null : projects.get(0);
    }

    @Override
    public boolean insertProject(Connection connection, Project project) throws SQLException {
        String query = "INSERT INTO \"" + TABLE_NAME + "\" " +
                "(PROJECT_ID, TITLE, PROBLEM_STATEMENT, PUBLICATION_STATUS, PROJECT_TYPE) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, project.getProjectId());
            statement.setString(2, project.getTitle());
            statement.setString(3, project.getProblemStatement());
            statement.setString(4, project.getPublicationStatus().name());
            statement.setString(5, project.getProjectType().name());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public void updateProject(Connection connection, Project project) throws SQLException {
        String sql = "UPDATE PROJECT SET title = ?, problem_statement = ?, publication_status = ?, " +
                "project_type = ? WHERE project_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, project.getTitle());
            statement.setString(2, project.getProblemStatement());
            statement.setString(3, project.getPublicationStatus().name());
            statement.setString(4, project.getProjectType().name());
            statement.setInt(5, project.getProjectId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Project was not found: " + project.getProjectId());
            }
        }
    }

    @Override
    public int findNextProjectId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT NVL(MAX(PROJECT_ID), 0) + 1 AS NEXT_ID FROM PROJECT")) {
            return result.next() ? result.getInt("NEXT_ID") : 1;
        }
    }

    @Override
    public void updatePublicationStatus(Connection connection, int projectId, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE PROJECT SET publication_status = ? WHERE project_id = ?")) {
            statement.setString(1, status);
            statement.setInt(2, projectId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Project was not found: " + projectId);
            }
        }
    }

    @Override
    public void assignOwner(Connection connection, int projectId, int ownerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE PROJECT SET owner_id = ? WHERE project_id = ?")) {
            statement.setInt(1, ownerId);
            statement.setInt(2, projectId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Project was not found: " + projectId);
            }
        }
    }

    @Override
    public void saveProjectMetadata(Connection connection, Project project, int reviewerId,
                                    List<String> teamMembers) throws SQLException {
        String repositoryUrl = repositoryUrl(project);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE PROJECT SET reviewer_id = ?, repository_url = ? WHERE project_id = ?")) {
            statement.setInt(1, reviewerId);
            statement.setString(2, blankToNull(repositoryUrl));
            statement.setInt(3, project.getProjectId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Project was not found: " + project.getProjectId());
            }
        }

        replaceTypeDetails(connection, project);
        replaceTeamMembers(connection, project.getProjectId(), teamMembers);
        replaceTechnologies(connection, project.getProjectId(), technologies(project));
    }

    @Override
    public void insertProjectFile(Connection connection, int projectId, String fileName,
                                  String filePath, String fileType, int uploadedBy) throws SQLException {
        String sql = "INSERT INTO PROJECT_FILE (project_id, file_name, file_path, file_type, uploaded_by) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            statement.setString(2, fileName);
            statement.setString(3, filePath);
            statement.setString(4, fileType);
            statement.setInt(5, uploadedBy);
            statement.executeUpdate();
        }
    }

    @Override
    public List<String> findProjectFileNames(Connection connection, int projectId) throws SQLException {
        List<String> files = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT file_name FROM PROJECT_FILE WHERE project_id = ? ORDER BY uploaded_at, file_id")) {
            statement.setInt(1, projectId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    files.add(result.getString("file_name"));
                }
            }
        }
        return files;
    }

    @Override
    public List<ProjectAttachmentInfo> findProjectAttachments(Connection connection, int projectId) throws SQLException {
        List<ProjectAttachmentInfo> files = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT file_name, file_path, file_type FROM PROJECT_FILE WHERE project_id = ? ORDER BY uploaded_at, file_id")) {
            statement.setInt(1, projectId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    files.add(new ProjectAttachmentInfo(
                            result.getString("file_name"),
                            result.getString("file_path"),
                            result.getString("file_type")));
                }
            }
        }
        return files;
    }

    private void replaceTypeDetails(Connection connection, Project project) throws SQLException {
        try (PreparedStatement deleteCourse = connection.prepareStatement(
                "DELETE FROM COURSE_PROJECT_DETAILS WHERE project_id = ?");
             PreparedStatement deleteGraduation = connection.prepareStatement(
                     "DELETE FROM GRADUATION_PROJECT_DETAILS WHERE project_id = ?")) {
            deleteCourse.setInt(1, project.getProjectId());
            deleteCourse.executeUpdate();
            deleteGraduation.setInt(1, project.getProjectId());
            deleteGraduation.executeUpdate();
        }

        if (project instanceof GraduationProject graduation) {
            Integer departmentId = findDepartmentId(connection, graduation.getDepartment());
            if (departmentId == null) {
                throw new SQLException("Department was not found: " + graduation.getDepartment());
            }
            String sql = "INSERT INTO GRADUATION_PROJECT_DETAILS " +
                    "(project_id, advisor, academic_year, graduation_term, defense_date, department_id) " +
                    "VALUES (?, ?, ?, ?, NULL, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, project.getProjectId());
                statement.setString(2, graduation.getAdvisor());
                statement.setInt(3, graduation.getAcademicYear());
                statement.setString(4, blankToNull(graduation.getGraduationTerm()));
                statement.setInt(5, departmentId);
                statement.executeUpdate();
            }
        } else if (project instanceof CourseProject courseProject) {
            Integer courseId = findCourseId(connection, courseProject.getCourseName());
            if (courseId == null) {
                throw new SQLException("Course was not found: " + courseProject.getCourseName());
            }
            String sql = "INSERT INTO COURSE_PROJECT_DETAILS " +
                    "(project_id, course_id, course_instructor, semester, academic_year) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, project.getProjectId());
                statement.setInt(2, courseId);
                statement.setString(3, blankToNull(courseProject.getCourseInstructor()));
                statement.setString(4, blankToNull(courseProject.getSemester()));
                statement.setInt(5, courseProject.getAcademicYear());
                statement.executeUpdate();
            }
        }
    }

    private void replaceTeamMembers(Connection connection, int projectId, List<String> teamMembers) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM PROJECT_MEMBER WHERE project_id = ?")) {
            delete.setInt(1, projectId);
            delete.executeUpdate();
        }
        if (teamMembers == null || teamMembers.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO PROJECT_MEMBER (project_id, user_id, member_name, member_role) VALUES (?, NULL, ?, 'Student')")) {
            for (String member : teamMembers) {
                if (member == null || member.isBlank()) continue;
                insert.setInt(1, projectId);
                insert.setString(2, member.trim());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void replaceTechnologies(Connection connection, int projectId, List<String> technologies) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM PROJECT_TECHNOLOGY WHERE project_id = ?")) {
            delete.setInt(1, projectId);
            delete.executeUpdate();
        }
        if (technologies == null || technologies.isEmpty()) {
            return;
        }
        for (String technology : technologies) {
            if (technology == null || technology.isBlank()) continue;
            int techId = findOrCreateTechnology(connection, technology.trim());
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO PROJECT_TECHNOLOGY (project_id, tech_id) VALUES (?, ?)")) {
                insert.setInt(1, projectId);
                insert.setInt(2, techId);
                insert.executeUpdate();
            }
        }
    }

    private int findOrCreateTechnology(Connection connection, String name) throws SQLException {
        try (PreparedStatement find = connection.prepareStatement(
                "SELECT tech_id FROM TECHNOLOGY WHERE UPPER(tech_name) = UPPER(?) FETCH FIRST 1 ROWS ONLY")) {
            find.setString(1, name);
            try (ResultSet result = find.executeQuery()) {
                if (result.next()) return result.getInt(1);
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO TECHNOLOGY (tech_name) VALUES (?)")) {
            insert.setString(1, name);
            insert.executeUpdate();
        } catch (SQLException insertError) {
            // A concurrent/previous insert may already have created the same technology.
            if (insertError.getErrorCode() != 1) throw insertError;
        }
        try (PreparedStatement findAgain = connection.prepareStatement(
                "SELECT tech_id FROM TECHNOLOGY WHERE UPPER(tech_name) = UPPER(?) FETCH FIRST 1 ROWS ONLY")) {
            findAgain.setString(1, name);
            try (ResultSet result = findAgain.executeQuery()) {
                if (result.next()) return result.getInt(1);
            }
        }
        throw new SQLException("Could not create technology: " + name);
    }

    private Integer findDepartmentId(Connection connection, String departmentName) throws SQLException {
        if (departmentName == null || departmentName.isBlank()) return null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT dept_id FROM DEPARTMENT WHERE UPPER(dept_name) = UPPER(?) FETCH FIRST 1 ROWS ONLY")) {
            statement.setString(1, departmentName.trim());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : null;
            }
        }
    }

    private Integer findCourseId(Connection connection, String courseName) throws SQLException {
        if (courseName == null || courseName.isBlank()) return null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT course_id FROM COURSE WHERE UPPER(course_name) = UPPER(?) FETCH FIRST 1 ROWS ONLY")) {
            statement.setString(1, courseName.trim());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : null;
            }
        }
    }

    private String repositoryUrl(Project project) {
        if (project instanceof GraduationProject graduation) return graduation.getRepositoryUrl();
        if (project instanceof CourseProject course) return course.getRepositoryUrl();
        return project instanceof StoredProject stored ? stored.getRepositoryUrl() : null;
    }

    private List<String> technologies(Project project) {
        if (project instanceof GraduationProject graduation && graduation.getTechStack() != null) {
            return graduation.getTechStack();
        }
        if (project instanceof CourseProject course && course.getTechStack() != null) {
            return course.getTechStack();
        }
        if (project instanceof StoredProject stored && stored.getTechStack() != null) {
            return splitCommaList(stored.getTechStack());
        }
        return List.of();
    }

    private List<String> splitCommaList(String value) {
        List<String> values = new ArrayList<>();
        if (value == null || value.isBlank()) return values;
        for (String part : value.split("[,،]")) {
            if (!part.isBlank()) values.add(part.trim());
        }
        return values;
    }

    private List<Project> readProjects(Connection connection, String sql, StatementBinder binder) throws SQLException {
        List<Project> projects = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) projects.add(mapRowToProject(resultSet));
            }
        }
        return projects;
    }

    private Project mapRowToProject(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("PROJECT_ID");
        String typeStr = resultSet.getString("PROJECT_TYPE");
        Timestamp createdTimestamp = resultSet.getTimestamp("CREATED_AT");
        LocalDateTime createdAt = createdTimestamp == null ? null : createdTimestamp.toLocalDateTime();
        try {
            return new StoredProject(
                    id,
                    resultSet.getString("TITLE"),
                    resultSet.getString("PROBLEM_STATEMENT"),
                    safeParseStatus(resultSet.getString("PUBLICATION_STATUS")),
                    ProjectType.valueOf(typeStr.trim().toUpperCase()),
                    nullableInt(resultSet, "OWNER_ID"),
                    resultSet.getString("OWNER_NAME"),
                    resultSet.getString("OWNER_STUDENT_NUMBER"),
                    nullableInt(resultSet, "REVIEWER_ID"),
                    resultSet.getString("REVIEWER_NAME"),
                    createdAt,
                    resultSet.getString("REPOSITORY_URL"),
                    resultSet.getString("DEPARTMENT_NAME"),
                    nullableInt(resultSet, "ACADEMIC_YEAR"),
                    resultSet.getString("SUPERVISOR_NAME"),
                    resultSet.getString("COURSE_NAME"),
                    resultSet.getString("SEMESTER"),
                    resultSet.getString("TECH_STACK"),
                    resultSet.getString("TEAM_MEMBERS")
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SQLException("Unsupported PROJECT.PROJECT_TYPE value: " + typeStr, exception);
        }
    }

    private Integer nullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private PublicationStatus safeParseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) return PublicationStatus.DRAFT;
        try {
            return PublicationStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Unknown publication status in database: " + statusStr + ", defaulting to DRAFT.");
            return PublicationStatus.DRAFT;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
