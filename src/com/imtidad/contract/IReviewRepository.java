package com.imtidad.contract;

import model.ContinuationRequestRecord;
import model.ReviewQueueItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IReviewRepository {
    List<ReviewQueueItem> findProjectsAwaitingReview(Connection connection, int reviewerId) throws SQLException;
    void saveProjectReview(Connection connection, int projectId, int reviewerId, String decision,
                           String comments, boolean problemClarity, boolean datasetsValid,
                           boolean ownershipProof, boolean fileIntegrity) throws SQLException;
    List<ContinuationRequestRecord> findContinuationRequestsAwaitingReview(Connection connection) throws SQLException;
    void reviewContinuationRequest(Connection connection, int requestId, int reviewerId,
                                   boolean approve, String reviewNotes) throws SQLException;
}
