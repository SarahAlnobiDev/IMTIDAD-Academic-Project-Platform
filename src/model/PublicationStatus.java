package model;

import com.imtidad.exception.ValidationException;

/**
 * Represents the publication lifecycle of a project.
 * Lifecycle: DRAFT -> PENDING_REVIEW -> (NEEDS_CHANGES | APPROVED) -> PUBLISHED
 */
public enum PublicationStatus {
    DRAFT,
    PENDING_REVIEW,
    NEEDS_CHANGES,
    APPROVED,
    PUBLISHED;

    /**
     * Validates whether a transition from this status to the target status is allowed.
     *
     * @param next the requested next status
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(PublicationStatus next) {
        if (next == null) {
            return false;
        }
        switch (this) {
            case DRAFT:
                return next == PENDING_REVIEW;
            case PENDING_REVIEW:
                return next == NEEDS_CHANGES || next == APPROVED;
            case NEEDS_CHANGES:
                return next == PENDING_REVIEW;
            case APPROVED:
                return next == PUBLISHED;
            case PUBLISHED:
                return false;
            default:
                return false;
        }
    }

    /**
     * Performs the transition and throws a ValidationException if it is illegal.
     *
     * @param next the requested next status
     */
    public void validateTransition(PublicationStatus next) {
        if (!canTransitionTo(next)) {
            throw new ValidationException(
                    "Invalid status transition from " + this + " to " + next);
        }
    }
}
