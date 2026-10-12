package com.imtidad.contract;

import model.ContinuationRequestRecord;
import model.LineageProjectNode;
import model.Project;

import java.util.List;

public interface ILineageService {
    List<Project> getAvailableProjects();
    List<LineageProjectNode> getLineage(int rootProjectId);
    int submitContinuationRequest(int projectId, String proposedTitle, String goals,
                                  String expectedTimeline, String skillSet, String relationType,
                                  String repositoryUrl, String attachmentPath);
    List<ContinuationRequestRecord> getMyRequests();
    List<ContinuationRequestRecord> getIncomingRequests();
    void decideIncomingRequest(int requestId, boolean approve, String notes);
}
