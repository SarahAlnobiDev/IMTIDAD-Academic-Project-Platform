package com.imtidad.util;

import model.UserAccount;

/**
 * Simple in-memory session manager for the currently authenticated user.
 * <p>
 * This is intentionally lightweight for a desktop application. A web or
 * multi-user deployment would use a more robust session/token mechanism.
 */
public final class SessionManager {

    private static UserAccount currentUser;

    private SessionManager() {
        // Utility class
    }

    public static void setCurrentUser(UserAccount user) {
        currentUser = user;
    }

    public static UserAccount getCurrentUser() {
        return currentUser;
    }

    public static void clearSession() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }
}
