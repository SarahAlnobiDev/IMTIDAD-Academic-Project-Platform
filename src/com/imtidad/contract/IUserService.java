package com.imtidad.contract;

import model.UserAccount;

import java.util.List;

/**
 * Service contract for user authentication and account lookup.
 */
public interface IUserService {

    /**
     * Authenticates a user by username/email and password.
     */
    UserAccount authenticate(String usernameOrEmail, String password);

    /** Hashes a raw password for storage or comparison. */
    String hashPassword(String password);

    /** Creates a self-registered account with the Student role only. */
    void registerStudent(String fullName, String studentNumber, String email,
                         int departmentId, String password, String confirmPassword);

    /** Returns the academic reviewer/supervisor accounts available for project assignment. */
    List<UserAccount> getReviewers();
}
