package com.imtidad.contract;

import model.Project;
import model.ProjectAttachmentInfo;

import java.nio.file.Path;
import java.util.List;

/** Service contract for project business operations. */
public interface IProjectService {

    List<Project> getAllProjects();

    /** Public exploration feed. Students see only published projects here. */
    List<Project> getPublishedProjects();

    /** Number of root projects that currently have at least one accepted extension relation. */
    long countProjectsWithExtensions();

    Project getProjectById(int projectId);

    /** Legacy creation entry point retained for existing callers. */
    boolean registerNewProject(Project project);

    /** Creates a student-owned draft and persists reviewer plus wizard metadata. */
    int saveDraft(Project project, int reviewerId, List<String> teamMembers);

    /** Updates an existing editable draft without creating a new project row. */
    void updateDraft(Project project, int reviewerId, List<String> teamMembers);

    int nextAvailableProjectId();

    /** Copies an allowed attachment into IMTIDAD local storage and records it in PROJECT_FILE. */
    String attachProjectFile(int projectId, Path sourceFile);

    List<String> getProjectAttachmentNames(int projectId);

    List<ProjectAttachmentInfo> getProjectAttachments(int projectId);

    void submitForReview(int projectId);
}
