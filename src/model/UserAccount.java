package model;

import com.imtidad.exception.ValidationException;

/**
 * Represents an authenticated user account in the IMTIDAD system.
 */
public class UserAccount {

    private int userId;
    private String username;
    private String email;
    private String roleName;
    private String displayName;
    private String studentNumber;

    public UserAccount(int userId, String username, String email, String roleName) {
        this(userId, username, email, roleName, username, null);
    }

    public UserAccount(int userId, String username, String email, String roleName, String displayName) {
        this(userId, username, email, roleName, displayName, null);
    }

    public UserAccount(int userId, String username, String email, String roleName,
                       String displayName, String studentNumber) {
        setUserId(userId);
        setUsername(username);
        setEmail(email);
        setRoleName(roleName);
        setDisplayName(displayName);
        setStudentNumber(studentNumber);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        if (userId <= 0) {
            throw new ValidationException("معرف المستخدم يجب أن يكون موجباً.");
        }
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("اسم المستخدم لا يمكن أن يكون فارغاً.");
        }
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("البريد الإلكتروني لا يمكن أن يكون فارغاً.");
        }
        this.email = email;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null || displayName.trim().isEmpty()
                ? username
                : displayName.trim();
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber == null || studentNumber.trim().isEmpty()
                ? null
                : studentNumber.trim();
    }

    public boolean isStudent() {
        return "Student".equalsIgnoreCase(roleName);
    }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(roleName);
    }

    /**
     * The current login groups Reviewer and Supervisor under the academic doctor/reviewer option.
     */
    public boolean isReviewer() {
        return "Reviewer".equalsIgnoreCase(roleName) || "Supervisor".equalsIgnoreCase(roleName);
    }

    public boolean isSupervisor() {
        return "Supervisor".equalsIgnoreCase(roleName);
    }

    @Override
    public String toString() {
        return displayName != null && !displayName.isBlank() ? displayName : username;
    }
}
