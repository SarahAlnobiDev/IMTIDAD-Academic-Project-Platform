package model;

import java.time.LocalDateTime;

/** Read model for a project that is waiting in the review queue. */
public final class ReviewQueueItem {
    private final int projectId;
    private final String title;
    private final ProjectType projectType;
    private final String ownerName;
    private final String departmentName;
    private final String problemStatement;
    private final PublicationStatus publicationStatus;
    private final LocalDateTime submittedAt;

    public ReviewQueueItem(int projectId, String title, ProjectType projectType, String ownerName,
                           String departmentName, String problemStatement,
                           PublicationStatus publicationStatus, LocalDateTime submittedAt) {
        this.projectId = projectId;
        this.title = title;
        this.projectType = projectType;
        this.ownerName = ownerName;
        this.departmentName = departmentName;
        this.problemStatement = problemStatement;
        this.publicationStatus = publicationStatus;
        this.submittedAt = submittedAt;
    }

    public int getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public ProjectType getProjectType() { return projectType; }
    public String getOwnerName() { return ownerName; }
    public String getDepartmentName() { return departmentName; }
    public String getProblemStatement() { return problemStatement; }
    public PublicationStatus getPublicationStatus() { return publicationStatus; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
