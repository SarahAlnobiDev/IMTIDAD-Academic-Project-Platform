package model;

/**
 * Represents the lifecycle status of a continuation/extension request.
 */
public enum RequestStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    WITHDRAWN
}
