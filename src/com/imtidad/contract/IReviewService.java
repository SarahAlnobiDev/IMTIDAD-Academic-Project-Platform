package com.imtidad.contract;

import model.ContinuationRequestRecord;
import model.ReviewQueueItem;

import java.util.List;

public interface IReviewService {
    List<ReviewQueueItem> getPendingProjectReviews();
    void submitProjectDecision(int projectId, String decision, String comments,
                               boolean problemClarity, boolean datasetsValid,
                               boolean ownershipProof, boolean fileIntegrity);
    List<ContinuationRequestRecord> getPendingContinuationRequests();
    void submitContinuationDecision(int requestId, boolean approve, String reviewNotes);
}
