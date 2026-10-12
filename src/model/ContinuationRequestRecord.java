package model;

import java.time.LocalDateTime;

/** DTO used by the lineage screen and the reviewer continuation-request queue. */
public final class ContinuationRequestRecord {
    private final int requestId;
    private final int projectId;
    private final String projectTitle;
    private final int requesterId;
    private final String requesterName;
    private final String proposedTitle;
    private final String goals;
    private final String expectedTimeline;
    private final String skillSet;
    private final String relationType;
    private final String repositoryUrl;
    private final String attachmentPath;
    private final RequestStatus requestStatus;
    private final LocalDateTime submittedAt;
    private final String reviewNotes;
    private final Integer childProjectId;

    public ContinuationRequestRecord(int requestId, int projectId, String projectTitle,
                                     int requesterId, String requesterName, String proposedTitle,
                                     String goals, String expectedTimeline, String skillSet,
                                     String relationType, String repositoryUrl, String attachmentPath,
                                     RequestStatus requestStatus, LocalDateTime submittedAt,
                                     String reviewNotes, Integer childProjectId) {
        this.requestId = requestId;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.proposedTitle = proposedTitle;
        this.goals = goals;
        this.expectedTimeline = expectedTimeline;
        this.skillSet = skillSet;
        this.relationType = relationType;
        this.repositoryUrl = repositoryUrl;
        this.attachmentPath = attachmentPath;
        this.requestStatus = requestStatus;
        this.submittedAt = submittedAt;
        this.reviewNotes = reviewNotes;
        this.childProjectId = childProjectId;
    }

    public int getRequestId() { return requestId; }
    public int getProjectId() { return projectId; }
    public String getProjectTitle() { return projectTitle; }
    public int getRequesterId() { return requesterId; }
    public String getRequesterName() { return requesterName; }
    public String getProposedTitle() { return proposedTitle; }
    public String getGoals() { return goals; }
    public String getExpectedTimeline() { return expectedTimeline; }
    public String getSkillSet() { return skillSet; }
    public String getRelationType() { return relationType; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public String getAttachmentPath() { return attachmentPath; }
    public RequestStatus getRequestStatus() { return requestStatus; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getReviewNotes() { return reviewNotes; }
    public Integer getChildProjectId() { return childProjectId; }
}
