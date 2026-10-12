package com.imtidad.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Utility class to automatically initialize the database schema on startup.
 * Reads and executes the imtidad_schema.sql file, ignoring errors for objects
 * that already exist.
 */
public class SchemaInitializer {

    private static final String SCHEMA_FILE = "/sql/imtidad_schema.sql";

    /**
     * Initializes the database schema by executing all DDL statements from the schema file.
     * Safe to run multiple times - ignores "name already used" and constraint existence errors.
     */
    public static void initializeSchema() {
        System.out.println("🔧 Checking database schema...");

        try (Connection connection = DatabaseConnection.getConnection()) {

            // Keep the demo doctor accounts aligned with the login credentials used by the project.
            try (Statement fixStmt = connection.createStatement()) {
                fixStmt.executeUpdate("UPDATE USER_ACCOUNT SET password_hash = '1048274a0809e7ec6bc682ea5ed6707a6919e0d46ceb340577a6346a079189ab' WHERE username = 'dr.ahmed'");
                fixStmt.executeUpdate("UPDATE USER_ACCOUNT SET password_hash = '75289a9e553c8ecb3314f4bd6208e860ba2c9e2d5a97f8ce65dc7cf94ffbe19c' WHERE username = 'dr.sara'");
            } catch (SQLException ex) {
                // Ignore on first boot before USER_ACCOUNT exists.
            }

            String sqlContent = readSchemaFile();
            if (sqlContent == null || sqlContent.trim().isEmpty()) {
                System.err.println("⚠️ Schema file is empty or not found.");
                return;
            }

            String[] statements = sqlContent.split(";");
            int executed = 0;
            int skipped = 0;

            for (String sql : statements) {
                sql = sql.trim();

                sql = sql.replaceAll("(?m)^\\s*--.*$", "")
                        .replaceAll("(?s)/\\*.*?\\*/", "").trim();
                if (sql.isEmpty()) {
                    continue;
                }

                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(sql);
                    executed++;
                } catch (SQLException e) {
                    int errorCode = e.getErrorCode();
                    if (errorCode == 955 || errorCode == 2260 || errorCode == 2275 || errorCode == 1418) {
                        skipped++;
                    } else {
                        System.err.println("⚠️ Schema warning: " + e.getMessage());
                    }
                }
            }

            ensureProjectWorkflowColumns(connection);
            ensureDemoDoctorPasswords(connection);

            System.out.println("✅ Database schema ready. Executed: " + executed + ", Already exists: " + skipped);

        } catch (SQLException e) {
            System.err.println("❌ Failed to initialize database schema: " + e.getMessage());
            System.err.println("   Please ensure Oracle database is running and credentials are correct.");
        }
    }

    private static void ensureProjectWorkflowColumns(Connection connection) {
        addColumnIfMissing(connection, "PROJECT", "REVIEWER_ID", "NUMBER");
        addColumnIfMissing(connection, "PROJECT", "REPOSITORY_URL", "VARCHAR2(500)");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE PROJECT ADD CONSTRAINT fk_project_reviewer FOREIGN KEY (reviewer_id) REFERENCES USER_ACCOUNT(user_id)");
        } catch (SQLException ignored) {
            // Constraint already exists or legacy data prevents it; the columns remain usable.
        }
    }

    private static void addColumnIfMissing(Connection connection, String table, String column, String definition) {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            check.setString(1, table.toUpperCase());
            check.setString(2, column.toUpperCase());
            try (ResultSet result = check.executeQuery()) {
                if (result.next() && result.getInt(1) == 0) {
                    try (Statement alter = connection.createStatement()) {
                        alter.executeUpdate("ALTER TABLE " + table + " ADD " + column + " " + definition);
                    }
                }
            }
        } catch (SQLException error) {
            System.err.println("⚠️ Could not migrate " + table + "." + column + ": " + error.getMessage());
        }
    }

    private static void ensureDemoDoctorPasswords(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE USER_ACCOUNT SET password_hash = '1048274a0809e7ec6bc682ea5ed6707a6919e0d46ceb340577a6346a079189ab' WHERE username = 'dr.ahmed'");
            statement.executeUpdate("UPDATE USER_ACCOUNT SET password_hash = '75289a9e553c8ecb3314f4bd6208e860ba2c9e2d5a97f8ce65dc7cf94ffbe19c' WHERE username = 'dr.sara'");
        } catch (SQLException ignored) {
            // Demo accounts may not exist in a custom database.
        }
    }

    private static String readSchemaFile() {
        try (InputStream input = SchemaInitializer.class.getResourceAsStream(SCHEMA_FILE)) {
            if (input == null) {
                System.err.println("⚠️ Schema file not found: " + SCHEMA_FILE);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            System.err.println("⚠️ Could not read schema file: " + e.getMessage());
            return null;
        }
    }
}