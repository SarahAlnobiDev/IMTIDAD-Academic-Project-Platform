package com.imtidad.dao;

import com.imtidad.util.DatabaseConnection;
import model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC repository for user account lookup.
 */
public class UserRepository {

    public Map<Integer, String> findDepartments() throws SQLException {
        Map<Integer, String> departments = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT dept_id, dept_name FROM DEPARTMENT ORDER BY dept_name");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) departments.put(result.getInt("dept_id"), result.getString("dept_name"));
        }
        return departments;
    }

    public void createStudent(String fullName, String studentNumber, String email, int departmentId, String passwordHash) throws SQLException {
        String accountSql = "INSERT INTO USER_ACCOUNT (username, email, password_hash, role_id) " +
                "VALUES (?, ?, ?, (SELECT role_id FROM ROLE WHERE UPPER(role_name) = 'STUDENT' FETCH FIRST 1 ROWS ONLY))";
        String lookupUserSql = "SELECT user_id FROM USER_ACCOUNT WHERE LOWER(email) = LOWER(?) FETCH FIRST 1 ROWS ONLY";
        String profileSql = "INSERT INTO STUDENT_PROFILE (user_id, full_name, student_number, department_id) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement account = connection.prepareStatement(accountSql)) {
                    account.setString(1, studentNumber);
                    account.setString(2, email);
                    account.setString(3, passwordHash);
                    if (account.executeUpdate() != 1) {
                        throw new SQLException("Could not create the account.");
                    }
                }

                // Oracle generated-key behaviour varies by driver. Read the identity back by the
                // unique email inside the same transaction so registration remains deterministic.
                int userId;
                try (PreparedStatement lookup = connection.prepareStatement(lookupUserSql)) {
                    lookup.setString(1, email);
                    try (ResultSet result = lookup.executeQuery()) {
                        if (!result.next()) throw new SQLException("Could not obtain the new account ID.");
                        userId = result.getInt("user_id");
                    }
                }

                try (PreparedStatement profile = connection.prepareStatement(profileSql)) {
                    profile.setInt(1, userId);
                    profile.setString(2, fullName);
                    profile.setString(3, studentNumber);
                    profile.setInt(4, departmentId);
                    if (profile.executeUpdate() != 1) {
                        throw new SQLException("Could not create the student profile.");
                    }
                }

                connection.commit();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public Optional<UserAccount> findByUsernameOrEmail(String usernameOrEmail) throws SQLException {
        String query = "SELECT u.user_id, u.username, u.email, u.password_hash, r.role_name, " +
                "COALESCE(sp.full_name, u.username) AS display_name, sp.student_number " +
                "FROM USER_ACCOUNT u " +
                "LEFT JOIN ROLE r ON u.role_id = r.role_id " +
                "LEFT JOIN STUDENT_PROFILE sp ON sp.user_id = u.user_id " +
                "WHERE LOWER(u.username) = LOWER(?) OR LOWER(u.email) = LOWER(?) " +
                "OR LOWER(sp.student_number) = LOWER(?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, usernameOrEmail);
            pstmt.setString(2, usernameOrEmail);
            pstmt.setString(3, usernameOrEmail);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserAccount user = new UserAccount(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("role_name"),
                            rs.getString("display_name"),
                            rs.getString("student_number")
                    );
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    public List<UserAccount> findReviewers() throws SQLException {
        String query = "SELECT u.user_id, u.username, u.email, r.role_name, u.username AS display_name " +
                "FROM USER_ACCOUNT u " +
                "JOIN ROLE r ON r.role_id = u.role_id " +
                "WHERE UPPER(r.role_name) IN ('REVIEWER','SUPERVISOR') " +
                "ORDER BY u.username";

        List<UserAccount> reviewers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                reviewers.add(new UserAccount(
                        result.getInt("user_id"),
                        result.getString("username"),
                        result.getString("email"),
                        result.getString("role_name"),
                        result.getString("display_name"),
                        null
                ));
            }
        }
        return reviewers;
    }

    public Optional<String> findPasswordHashByUsernameOrEmail(String usernameOrEmail) throws SQLException {
        String query = "SELECT u.password_hash FROM USER_ACCOUNT u " +
                "LEFT JOIN STUDENT_PROFILE sp ON sp.user_id = u.user_id " +
                "WHERE LOWER(u.username) = LOWER(?) OR LOWER(u.email) = LOWER(?) " +
                "OR LOWER(sp.student_number) = LOWER(?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, usernameOrEmail);
            pstmt.setString(2, usernameOrEmail);
            pstmt.setString(3, usernameOrEmail);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("password_hash"));
                }
            }
        }
        return Optional.empty();
    }
}