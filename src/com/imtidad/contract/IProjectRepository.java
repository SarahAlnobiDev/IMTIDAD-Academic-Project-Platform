package com.imtidad.contract;

import model.Project;
import model.ProjectAttachmentInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Repository contract for Project persistence. */
public interface IProjectRepository {

    void ensureTableExists(Connection connection) throws SQLException;

    List<Project> getAllProjects(Connection connection) throws SQLException;

    List<Project> getProjectsByOwner(Connection connection, int ownerId) throws SQLException;

    List<Project> getProjectsForReviewer(Connection connection, int reviewerId) throws SQLException;

    List<Project> getPublishedProjects(Connection connection) throws SQLException;

    long countProjectsWithExtensions(Connection connection) throws SQLException;

    Project findProjectById(Connection connection, int projectId) throws SQLException;

    boolean insertProject(Connection connection, Project project) throws SQLException;

    void updateProject(Connection connection, Project project) throws SQLException;

    int findNextProjectId(Connection connection) throws SQLException;

    void updatePublicationStatus(Connection connection, int projectId, String status) throws SQLException;

    void assignOwner(Connection connection, int projectId, int ownerId) throws SQLException;

    void saveProjectMetadata(Connection connection, Project project, int reviewerId,
                             List<String> teamMembers) throws SQLException;

    void insertProjectFile(Connection connection, int projectId, String fileName,
                           String filePath, String fileType, int uploadedBy) throws SQLException;

    List<String> findProjectFileNames(Connection connection, int projectId) throws SQLException;

    List<ProjectAttachmentInfo> findProjectAttachments(Connection connection, int projectId) throws SQLException;
}
