package com.imtidad.dao;

import com.imtidad.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for loading reference data (departments, courses, technologies, etc.)
 * dynamically from Oracle. Never hardcodes reference values.
 */
public class ReferenceDataRepository {

    /**
     * Loads all department names from the DEPARTMENT table.
     * Returns an empty list if the table is missing or empty.
     *
     * @return list of department names
     */
    public List<String> getDepartments() {
        List<String> departments = new ArrayList<>();
        String query = "SELECT dept_name FROM DEPARTMENT ORDER BY dept_name";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String name = resultSet.getString("dept_name");
                if (name != null && !name.trim().isEmpty()) {
                    departments.add(name);
                }
            }
        } catch (SQLException e) {
            // Table may not exist yet during early development; return empty list.
            System.err.println("⚠️ Could not load departments: " + e.getMessage());
        }
        return departments;
    }

    /**
     * Loads all course names from the COURSE table.
     *
     * @return list of course names
     */
    public List<String> getCourses() {
        List<String> courses = new ArrayList<>();
        String query = "SELECT course_name FROM COURSE ORDER BY course_name";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String name = resultSet.getString("course_name");
                if (name != null && !name.trim().isEmpty()) {
                    courses.add(name);
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Could not load courses: " + e.getMessage());
        }
        return courses;
    }

    /**
     * Loads all technology names from the TECHNOLOGY table.
     *
     * @return list of technology names
     */
    public List<String> getTechnologies() {
        List<String> technologies = new ArrayList<>();
        String query = "SELECT tech_name FROM TECHNOLOGY ORDER BY tech_name";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String name = resultSet.getString("tech_name");
                if (name != null && !name.trim().isEmpty()) {
                    technologies.add(name);
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Could not load technologies: " + e.getMessage());
        }
        return technologies;
    }
}
