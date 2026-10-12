package com.imtidad.service;

import com.imtidad.contract.IAuditLogService;
import com.imtidad.contract.IProjectRepository;
import com.imtidad.contract.IProjectService;
import com.imtidad.dao.ProjectRepository;
import com.imtidad.exception.DatabaseException;
import com.imtidad.exception.ValidationException;
import com.imtidad.util.DatabaseConnection;
import com.imtidad.util.SessionManager;
import model.Project;
import model.ProjectAttachmentInfo;
import model.PublicationStatus;
import model.StoredProject;
import model.UserAccount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Business service for project operations.
 * Manages transactions and delegates JDBC persistence to {@link IProjectRepository}.
 */
public class ProjectService implements IProjectService {

    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS =
            Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx");

    private final IProjectRepository projectRepository;
    private final IAuditLogService auditLogService;

    public ProjectService() {
        this(new ProjectRepository(), new AuditLogService());
    }

    public ProjectService(IProjectRepository projectRepository) {
        this(projectRepository, new AuditLogService());
    }

    public ProjectService(IProjectRepository projectRepository, IAuditLogService auditLogService) {
        if (projectRepository == null) {
            throw new ValidationException("Project repository cannot be null.");
        }
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService != null ? auditLogService : new AuditLogService();
    }

    @Override
    public List<Project> getAllProjects() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            projectRepository.ensureTableExists(connection);

            UserAccount current = SessionManager.getCurrentUser();
            if (current == null) return List.of();
            if (current.isStudent()) {
                return projectRepository.getProjectsByOwner(connection, current.getUserId());
            }
            if (current.isReviewer()) {
                // Dashboard catalog for reviewers shows the complete project collection.
                // Review Queue remains separately restricted to projects assigned to this reviewer.
                return projectRepository.getAllProjects(connection);
            }
            return projectRepository.getAllProjects(connection);
        } catch (SQLException e) {
            throw new DatabaseException("فشل تحميل المشاريع من قاعدة البيانات.", e);
        }
    }

    @Override
    public List<Project> getPublishedProjects() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            projectRepository.ensureTableExists(connection);
            return projectRepository.getPublishedProjects(connection);
        } catch (SQLException e) {
            throw new DatabaseException("تعذر تحميل المشاريع المنشورة.", e);
        }
    }

    @Override
    public long countProjectsWithExtensions() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            projectRepository.ensureTableExists(connection);
            return projectRepository.countProjectsWithExtensions(connection);
        } catch (SQLException e) {
            throw new DatabaseException("تعذر حساب المشاريع التي لها امتدادات.", e);
        }
    }

    @Override
    public Project getProjectById(int projectId) {
        if (projectId <= 0) throw new ValidationException("رقم المشروع غير صالح.");
        try (Connection connection = DatabaseConnection.getConnection()) {
            projectRepository.ensureTableExists(connection);
            Project project = projectRepository.findProjectById(connection, projectId);
            if (project == null) throw new ValidationException("المشروع غير موجود.");
            return project;
        } catch (SQLException e) {
            throw new DatabaseException("تعذر تحميل تفاصيل المشروع.", e);
        }
    }

    @Override
    public boolean registerNewProject(Project project) {
        requireStudent();
        validateProject(project);
        try (Connection connection = DatabaseConnection.getConnection()) {
            projectRepository.ensureTableExists(connection);
            connection.setAutoCommit(false);
            try {
                boolean inserted = projectRepository.insertProject(connection, project);
                if (inserted) {
                    int userId = SessionManager.getCurrentUser().getUserId();
                    projectRepository.assignOwner(connection, project.getProjectId(), userId);
                }
                connection.commit();
                if (inserted) {
                    auditLogService.logAction(SessionManager.getCurrentUser().getUserId(),
                            "PROJECT_REGISTERED", "PROJECT", project.getProjectId());
                }
                return inserted;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("فشل حفظ المشروع في قاعدة البيانات.", e);
        }
    }

    @Override
    public int saveDraft(Project project, int reviewerId, List<String> teamMembers) {
        UserAccount current = requireStudent();
        validateProject(project);
        validateReviewer(reviewerId);
        if (project.getPublicationStatus() != PublicationStatus.DRAFT) {
            throw new ValidationException("المشروع الجديد يجب أن يبدأ كمسودة.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            projectRepository.ensureTableExists(connection);
            connection.setAutoCommit(false);
            try {
                if (!projectRepository.insertProject(connection, project)) {
                    throw new SQLException("تعذر إنشاء سجل المشروع.");
                }
                projectRepository.assignOwner(connection, project.getProjectId(), current.getUserId());
                projectRepository.saveProjectMetadata(connection, project, reviewerId,
                        teamMembers == null ? List.of() : teamMembers);
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
            auditLogService.logAction(current.getUserId(), "PROJECT_DRAFT_CREATED",
                    "PROJECT", project.getProjectId());
            return project.getProjectId();
        } catch (SQLException e) {
            throw new DatabaseException("تعذر حفظ المشروع كمسودة.", e);
        }
    }

    @Override
    public void updateDraft(Project project, int reviewerId, List<String> teamMembers) {
        UserAccount current = requireStudent();
        validateProject(project);
        validateReviewer(reviewerId);

        try (Connection connection = DatabaseConnection.getConnection()) {
            projectRepository.ensureTableExists(connection);
            StoredProject existing = requireEditableOwnedProject(connection, project.getProjectId(), current.getUserId());

            // Keep the current lifecycle state. Editing NEEDS_CHANGES must not silently reset to DRAFT.
            project.setPublicationStatus(existing.getPublicationStatus());

            connection.setAutoCommit(false);
            try {
                projectRepository.updateProject(connection, project);
                projectRepository.saveProjectMetadata(connection, project, reviewerId,
                        teamMembers == null ? List.of() : teamMembers);
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
            auditLogService.logAction(current.getUserId(), "PROJECT_DRAFT_UPDATED",
                    "PROJECT", project.getProjectId());
        } catch (SQLException e) {
            throw new DatabaseException("تعذر حفظ تعديلات المشروع.", e);
        }
    }

    @Override
    public int nextAvailableProjectId() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            projectRepository.ensureTableExists(connection);
            return projectRepository.findNextProjectId(connection);
        } catch (SQLException error) {
            throw new DatabaseException("تعذر تجهيز رقم المشروع.", error);
        }
    }

    @Override
    public String attachProjectFile(int projectId, Path sourceFile) {
        UserAccount current = requireStudent();
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new ValidationException("الملف المحدد غير صالح.");
        }
        String originalName = sourceFile.getFileName().toString();
        String extension = extensionOf(originalName);
        if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw new ValidationException("نوع الملف غير مدعوم. المسموح: PDF, PNG, JPG, JPEG, DOC, DOCX.");
        }

        Path copiedPath = null;
        try (Connection connection = DatabaseConnection.getConnection()) {
            requireEditableOwnedProject(connection, projectId, current.getUserId());

            Path uploadDirectory = Path.of(System.getProperty("user.home"), ".imtidad", "uploads",
                    String.valueOf(projectId));
            Files.createDirectories(uploadDirectory);

            String safeName = sanitizeFileName(originalName);
            Path destination = uniqueDestination(uploadDirectory, safeName);
            copiedPath = Files.copy(sourceFile, destination, StandardCopyOption.COPY_ATTRIBUTES);

            projectRepository.insertProjectFile(connection, projectId, originalName,
                    copiedPath.toAbsolutePath().toString(), extension.toUpperCase(Locale.ROOT), current.getUserId());
            auditLogService.logAction(current.getUserId(), "PROJECT_FILE_ATTACHED", "PROJECT", projectId);
            return originalName;
        } catch (SQLException e) {
            deleteQuietly(copiedPath);
            throw new DatabaseException("تعذر تسجيل المرفق في قاعدة البيانات.", e);
        } catch (IOException e) {
            deleteQuietly(copiedPath);
            throw new DatabaseException("تعذر نسخ المرفق إلى مجلد امتداد المحلي.", e);
        }
    }

    @Override
    public List<String> getProjectAttachmentNames(int projectId) {
        if (projectId <= 0) return List.of();
        try (Connection connection = DatabaseConnection.getConnection()) {
            return projectRepository.findProjectFileNames(connection, projectId);
        } catch (SQLException e) {
            throw new DatabaseException("تعذر تحميل مرفقات المشروع.", e);
        }
    }

    @Override
    public List<ProjectAttachmentInfo> getProjectAttachments(int projectId) {
        if (projectId <= 0) return List.of();
        try (Connection connection = DatabaseConnection.getConnection()) {
            return projectRepository.findProjectAttachments(connection, projectId);
        } catch (SQLException e) {
            throw new DatabaseException("تعذر تحميل مرفقات المشروع.", e);
        }
    }

    @Override
    public void submitForReview(int projectId) {
        UserAccount current = requireStudent();
        if (projectId <= 0) throw new ValidationException("رقم المشروع غير صالح.");

        try (Connection connection = DatabaseConnection.getConnection()) {
            StoredProject project = requireEditableOwnedProject(connection, projectId, current.getUserId());
            if (project.getReviewerId() == null) {
                throw new ValidationException("اختر الدكتور المراجع قبل إرسال المشروع للمراجعة.");
            }
            if (project.getRepositoryUrl() == null || project.getRepositoryUrl().isBlank()) {
                throw new ValidationException("أضف رابط مستودع GitHub قبل إرسال المشروع للمراجعة.");
            }
            projectRepository.updatePublicationStatus(connection, projectId, PublicationStatus.PENDING_REVIEW.name());
            auditLogService.logAction(current.getUserId(), "PROJECT_SUBMITTED_FOR_REVIEW", "PROJECT", projectId);
        } catch (SQLException error) {
            throw new DatabaseException("تعذر إرسال المشروع للمراجعة.", error);
        }
    }

    private StoredProject requireEditableOwnedProject(Connection connection, int projectId, int userId) throws SQLException {
        Project loaded = projectRepository.findProjectById(connection, projectId);
        if (!(loaded instanceof StoredProject stored)) {
            throw new ValidationException("المشروع غير موجود.");
        }
        if (stored.getOwnerId() == null || stored.getOwnerId() != userId) {
            throw new ValidationException("لا تملك صلاحية تعديل هذا المشروع.");
        }
        PublicationStatus status = stored.getPublicationStatus();
        if (status != PublicationStatus.DRAFT && status != PublicationStatus.NEEDS_CHANGES) {
            throw new ValidationException("لا يمكن تعديل المشروع في حالته الحالية.");
        }
        return stored;
    }

    private UserAccount requireStudent() {
        UserAccount current = SessionManager.getCurrentUser();
        if (current == null || !current.isStudent()) {
            throw new ValidationException("هذه العملية متاحة للطالب فقط.");
        }
        return current;
    }

    private void validateReviewer(int reviewerId) {
        if (reviewerId <= 0) throw new ValidationException("يجب اختيار الدكتور المراجع.");
    }

    private void validateProject(Project project) {
        if (project == null) throw new ValidationException("لا يمكن إضافة مشروع فارغ.");
        if (project.getTitle() == null || project.getTitle().trim().isEmpty()) {
            throw new ValidationException("عنوان المشروع إلزامي.");
        }
        if (project.getProjectId() <= 0) throw new ValidationException("رقم المشروع يجب أن يكون موجباً.");
        if (project.getPublicationStatus() == null) throw new ValidationException("حالة النشر إلزامية.");
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = fileName.replaceAll("[^A-Za-z0-9._\\-\\u0600-\\u06FF]", "_");
        return sanitized.isBlank() ? "attachment" : sanitized;
    }

    private Path uniqueDestination(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName);
        if (!Files.exists(candidate)) return candidate;
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        return directory.resolve(base + "_" + System.currentTimeMillis() + extension);
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }
}
