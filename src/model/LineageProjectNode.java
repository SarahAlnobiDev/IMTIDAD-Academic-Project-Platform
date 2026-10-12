package model;

/** Lightweight project node used to build the project lineage tree. */
public final class LineageProjectNode {
    private final int projectId;
    private final String title;
    private final ProjectType projectType;
    private final PublicationStatus status;
    private final String relationType;

    public LineageProjectNode(int projectId, String title, ProjectType projectType,
                              PublicationStatus status, String relationType) {
        this.projectId = projectId;
        this.title = title;
        this.projectType = projectType;
        this.status = status;
        this.relationType = relationType;
    }

    public int getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public ProjectType getProjectType() { return projectType; }
    public PublicationStatus getStatus() { return status; }
    public String getRelationType() { return relationType; }

    @Override
    public String toString() {
        String relation = relationType == null || relationType.isBlank() ? "الأصل" : relationType;
        return title + "  ·  #" + projectId + "  ·  " + relation;
    }
}
