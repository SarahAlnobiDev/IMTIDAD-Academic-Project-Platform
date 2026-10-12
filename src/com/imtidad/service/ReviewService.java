package com.imtidad.service;

import com.imtidad.contract.IAuditLogService;
import com.imtidad.contract.IReviewRepository;
import com.imtidad.contract.IReviewService;
import com.imtidad.dao.ReviewRepository;
import com.imtidad.exception.DatabaseException;
import com.imtidad.exception.ValidationException;
import com.imtidad.util.DatabaseConnection;
import com.imtidad.util.SessionManager;
import model.ContinuationRequestRecord;
import model.ReviewQueueItem;
import model.UserAccount;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/** Reviewer workflow for academic project-quality decisions. Continuation decisions belong to project owners. */
public class ReviewService implements IReviewService {
    private static final Set<String> DECISIONS = Set.of("APPROVE", "REQUEST_CHANGES", "REJECT");

    private final IReviewRepository repository;
    private final IAuditLogService auditLogService;

    public ReviewService() {
        this(new ReviewRepository(), new AuditLogService());
    }

    public ReviewService(IReviewRepository repository, IAuditLogService auditLogService) {
        if (repository == null) throw new ValidationException("Review repository cannot be null.");
        this.repository = repository;
        this.auditLogService = auditLogService == null ? new AuditLogService() : auditLogService;
    }

    @Override
    public List<ReviewQueueItem> getPendingProjectReviews() {
        UserAccount reviewer = requireReviewer();
        try (Connection connection = DatabaseConnection.getConnection()) {
            return repository.findProjectsAwaitingReview(connection, reviewer.getUserId());
        } catch (SQLException error) {
            throw new DatabaseException("تعذر تحميل طابور مراجعة المشاريع.", error);
        }
    }

    @Override
    public void submitProjectDecision(int projectId, String decision, String comments,
                                      boolean problemClarity, boolean datasetsValid,
                                      boolean ownershipProof, boolean fileIntegrity) {
        UserAccount reviewer = requireReviewer();
        if (projectId <= 0) throw new ValidationException("اختر مشروعاً للمراجعة.");
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        if (!DECISIONS.contains(normalized)) throw new ValidationException("قرار المراجعة غير صالح.");
        if (!normalized.equals("APPROVE") && (comments == null || comments.trim().length() < 5)) {
            throw new ValidationException("أضف ملاحظة توضّح سبب الإرجاع أو الرفض.");
        }
        if (normalized.equals("APPROVE") && !(problemClarity && datasetsValid && ownershipProof && fileIntegrity)) {
            throw new ValidationException("لا يمكن قبول المشروع قبل اكتمال قائمة التحقق.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                repository.saveProjectReview(connection, projectId, reviewer.getUserId(), normalized,
                        comments == null ? "" : comments.trim(), problemClarity, datasetsValid,
                        ownershipProof, fileIntegrity);
                connection.commit();
                auditLogService.logAction(reviewer.getUserId(), "PROJECT_REVIEW_" + normalized, "PROJECT", projectId);
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new DatabaseException("تعذر حفظ قرار المراجعة.", error);
        }
    }

    @Override
    public List<ContinuationRequestRecord> getPendingContinuationRequests() {
        requireReviewer();
        // Continuation requests are intentionally not part of the doctor's review workflow.
        return List.of();
    }

    @Override
    public void submitContinuationDecision(int requestId, boolean approve, String reviewNotes) {
        requireReviewer();
        throw new ValidationException("طلبات الاستكمال من صلاحية صاحب المشروع الأصلي وليست من صلاحية الدكتور.");
    }

    private UserAccount requireReviewer() {
        UserAccount current = SessionManager.getCurrentUser();
        if (current == null || !(current.isReviewer() || current.isAdmin())) {
            throw new ValidationException("هذه الشاشة متاحة للمراجع أو مدير النظام فقط.");
        }
        return current;
    }
}
