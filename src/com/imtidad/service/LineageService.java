package com.imtidad.service;

import com.imtidad.contract.IAuditLogService;
import com.imtidad.contract.ILineageRepository;
import com.imtidad.contract.ILineageService;
import com.imtidad.dao.LineageRepository;
import com.imtidad.exception.DatabaseException;
import com.imtidad.exception.ValidationException;
import com.imtidad.util.DatabaseConnection;
import com.imtidad.util.SessionManager;
import model.ContinuationRequestRecord;
import model.LineageProjectNode;
import model.Project;
import model.UserAccount;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/** Business rules for continuation requests and project lineage. */
public class LineageService implements ILineageService {
    private static final Set<String> RELATION_TYPES = Set.of("CONTINUATION", "EXTENSION", "DERIVATIVE");

    private final ILineageRepository repository;
    private final IAuditLogService auditLogService;

    public LineageService() {
        this(new LineageRepository(), new AuditLogService());
    }

    public LineageService(ILineageRepository repository, IAuditLogService auditLogService) {
        if (repository == null) throw new ValidationException("Lineage repository cannot be null.");
        this.repository = repository;
        this.auditLogService = auditLogService == null ? new AuditLogService() : auditLogService;
    }

    @Override
    public List<Project> getAvailableProjects() {
        requireLoggedIn();
        try (Connection connection = DatabaseConnection.getConnection()) {
            repository.ensureSupportTables(connection);
            return repository.findSelectableProjects(connection);
        } catch (SQLException error) {
            throw new DatabaseException("تعذر تحميل المشاريع لشجرة الامتداد.", error);
        }
    }

    @Override
    public List<LineageProjectNode> getLineage(int rootProjectId) {
        requireLoggedIn();
        if (rootProjectId <= 0) throw new ValidationException("اختر مشروعاً صالحاً.");
        try (Connection connection = DatabaseConnection.getConnection()) {
            repository.ensureSupportTables(connection);
            return repository.findLineage(connection, rootProjectId);
        } catch (SQLException error) {
            throw new DatabaseException("تعذر تحميل شجرة الامتداد.", error);
        }
    }

    @Override
    public int submitContinuationRequest(int projectId, String proposedTitle, String goals,
                                         String expectedTimeline, String skillSet, String relationType,
                                         String repositoryUrl, String attachmentPath) {
        UserAccount requester = requireStudent();
        if (projectId <= 0) throw new ValidationException("اختر المشروع الأصلي أولاً.");
        if (proposedTitle == null || proposedTitle.trim().length() < 3) {
            throw new ValidationException("اكتب عنواناً واضحاً للمشروع الممتد.");
        }
        if (goals == null || goals.trim().length() < 10) {
            throw new ValidationException("اكتب هدف وخطة الاستكمال بشكل أوضح.");
        }
        if (expectedTimeline == null || expectedTimeline.isBlank()) {
            throw new ValidationException("حدد المدة أو الخطة الزمنية المتوقعة.");
        }
        if (skillSet == null || skillSet.isBlank()) {
            throw new ValidationException("أدخل المهارات أو التقنيات المطلوبة.");
        }
        String normalizedRelation = relationType == null ? "CONTINUATION" : relationType.trim().toUpperCase();
        if (!RELATION_TYPES.contains(normalizedRelation)) {
            throw new ValidationException("نوع العلاقة غير مدعوم.");
        }

        int requesterId = requester.getUserId();
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                repository.ensureSupportTables(connection);
                Integer ownerId = repository.findProjectOwnerId(connection, projectId);
                String status = repository.findProjectStatus(connection, projectId);
                if (ownerId == null) {
                    throw new ValidationException("المشروع الأصلي غير موجود أو لا يملك صاحباً محدداً.");
                }
                boolean requesterIsOwner = ownerId == requesterId;
                // The project owner may start a new extension directly from their own project.
                // Other students still need a published project and the owner's approval.
                if (!requesterIsOwner && !"PUBLISHED".equalsIgnoreCase(status)) {
                    throw new ValidationException("يمكن طلب استكمال مشاريع الطلاب الآخرين بعد نشرها فقط.");
                }
                if (!requesterIsOwner && repository.hasPendingRequest(connection, projectId, requesterId)) {
                    throw new ValidationException("لديك طلب استكمال معلّق لهذا المشروع بالفعل.");
                }
                int requestId = repository.createContinuationRequest(connection, projectId, requesterId,
                        proposedTitle.trim(), goals.trim(), expectedTimeline.trim(), skillSet.trim(),
                        normalizedRelation, repositoryUrl, attachmentPath);

                if (requesterIsOwner) {
                    // No self-approval dialog is needed. Reuse the existing approval path so the
                    // child project and PROJECT_RELATION are created consistently.
                    repository.decideContinuationRequest(connection, requestId, requesterId, true,
                            "امتداد أنشأه صاحب المشروع الأصلي مباشرة.");
                }

                connection.commit();
                auditLogService.logAction(requesterId,
                        requesterIsOwner ? "OWNER_EXTENSION_CREATED" : "CONTINUATION_REQUEST_SUBMITTED",
                        "CONTINUATION_REQUEST", requestId);
                return requestId;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new DatabaseException("تعذر حفظ طلب الاستكمال.", error);
        }
    }

    @Override
    public List<ContinuationRequestRecord> getMyRequests() {
        requireLoggedIn();
        try (Connection connection = DatabaseConnection.getConnection()) {
            repository.ensureSupportTables(connection);
            return repository.findRequestsForUser(connection, SessionManager.getCurrentUser().getUserId());
        } catch (SQLException error) {
            throw new DatabaseException("تعذر تحميل طلبات الاستكمال الخاصة بك.", error);
        }
    }

    @Override
    public List<ContinuationRequestRecord> getIncomingRequests() {
        UserAccount current = requireStudent();
        try (Connection connection = DatabaseConnection.getConnection()) {
            repository.ensureSupportTables(connection);
            return repository.findRequestsForOwner(connection, current.getUserId());
        } catch (SQLException error) {
            throw new DatabaseException("تعذر تحميل طلبات الاستكمال الواردة لمشاريعك.", error);
        }
    }

    @Override
    public void decideIncomingRequest(int requestId, boolean approve, String notes) {
        UserAccount current = requireStudent();
        if (requestId <= 0) throw new ValidationException("اختر طلب استكمال صالحاً.");
        if (!approve && (notes == null || notes.trim().length() < 3)) {
            throw new ValidationException("أضف سبباً مختصراً عند رفض الطلب.");
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                repository.ensureSupportTables(connection);
                repository.decideContinuationRequest(connection, requestId, current.getUserId(), approve,
                        notes == null ? "" : notes.trim());
                connection.commit();
                auditLogService.logAction(current.getUserId(),
                        approve ? "CONTINUATION_REQUEST_APPROVED_BY_OWNER" : "CONTINUATION_REQUEST_REJECTED_BY_OWNER",
                        "CONTINUATION_REQUEST", requestId);
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new DatabaseException("تعذر حفظ قرار طلب الاستكمال.", error);
        }
    }

    private UserAccount requireStudent() {
        UserAccount current = SessionManager.getCurrentUser();
        if (current == null || !current.isStudent()) {
            throw new ValidationException("هذه العملية متاحة للطالب فقط.");
        }
        return current;
    }

    private void requireLoggedIn() {
        if (!SessionManager.isLoggedIn()) {
            throw new ValidationException("يجب تسجيل الدخول أولاً.");
        }
    }
}
