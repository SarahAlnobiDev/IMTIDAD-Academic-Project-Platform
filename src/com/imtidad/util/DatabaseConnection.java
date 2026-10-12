package com.imtidad.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class for obtaining Oracle database connections.
 * <p>
 * Configuration is loaded from {@code /config.properties} on the classpath.
 * Environment variables ({@code ORACLE_URL}, {@code ORACLE_USER}, {@code ORACLE_PASSWORD})
 * override the file values so that real credentials never need to be committed.
 */
public class DatabaseConnection {

    private static final String CONFIG_FILE = "/config.properties";
    private static final Properties CONFIG = loadConfiguration();

    private static Properties loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("⚠️ Could not load " + CONFIG_FILE + ": " + e.getMessage());
        }
        return properties;
    }

    private static String getConfig(String key, String defaultValue) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return propertiesGetOrDefault(key, defaultValue);
    }

    private static String propertiesGetOrDefault(String key, String defaultValue) {
        String value = CONFIG.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    /**
     * Creates and returns a connection to the Oracle database.
     *
     * @return a valid {@link Connection}
     * @throws SQLException if the driver is not found or the connection fails
     */
    public static Connection getConnection() throws SQLException {
        String url = getConfig("db.url", null);
        String user = getConfig("db.user", null);
        String password = getConfig("db.password", null);

        if (url == null || user == null || password == null) {
            throw new SQLException(
                    "Database credentials are not configured. " +
                    "Set ORACLE_URL, ORACLE_USER, ORACLE_PASSWORD environment variables " +
                    "or provide them in " + CONFIG_FILE);
        }

        try {
            Class.forName("oracle.jdbc.OracleDriver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC Driver not found on classpath.", e);
        }
    }

    /**
     * Quietly closes a connection without throwing.
     *
     * @param connection the connection to close; may be null
     */
    public static void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Best-effort close
            }
        }
    }
}
