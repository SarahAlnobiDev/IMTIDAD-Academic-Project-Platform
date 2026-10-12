package com.imtidad.contract;

import model.ContinuationRequestRecord;
import model.LineageProjectNode;
import model.Project;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ILineageRepository {
    void ensureSupportTables(Connection connection) throws SQLException;
    List<Project> findSelectableProjects(Connection connection) throws SQLException;
    List<LineageProjectNode> findLineage(Connection connection, int rootProjectId) throws SQLException;
    int createContinuationRequest(Connection connection, int projectId, int requesterId,
                                  String proposedTitle, String goals, String expectedTimeline,
                                  String skillSet, String relationType, String repositoryUrl,
                                  String attachmentPath) throws SQLException;
    List<ContinuationRequestRecord> findRequestsForUser(Connection connection, int requesterId) throws SQLException;
    List<ContinuationRequestRecord> findRequestsForOwner(Connection connection, int ownerId) throws SQLException;
    Integer findProjectOwnerId(Connection connection, int projectId) throws SQLException;
    String findProjectStatus(Connection connection, int projectId) throws SQLException;
    boolean hasPendingRequest(Connection connection, int projectId, int requesterId) throws SQLException;
    void decideContinuationRequest(Connection connection, int requestId, int ownerId,
                                   boolean approve, String notes) throws SQLException;
}
