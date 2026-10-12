package com.imtidad.service;

import com.imtidad.contract.IUserService;
import com.imtidad.dao.UserRepository;
import com.imtidad.exception.DatabaseException;
import com.imtidad.exception.ValidationException;
import model.UserAccount;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Business service for user authentication.
 * <p>
 * Passwords are hashed with SHA-256. In a production deployment this should be
 * replaced with a dedicated password-hashing library such as BCrypt or Argon2.
 */
public class UserService implements IUserService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;

    public UserService() {
        this(new UserRepository());
    }

    public UserService(UserRepository userRepository) {
        if (userRepository == null) {
            throw new ValidationException("User repository cannot be null.");
        }
        this.userRepository = userRepository;
    }

    @Override
    public UserAccount authenticate(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new ValidationException("اسم المستخدم أو البريد الإلكتروني إلزامي.");
        }
        if (password == null || password.isEmpty()) {
            throw new ValidationException("كلمة المرور إلزامية.");
        }

        String normalizedInput = usernameOrEmail.trim().toLowerCase();
        String inputHash = hashPassword(password);

        try {
            Optional<String> storedHash = userRepository.findPasswordHashByUsernameOrEmail(normalizedInput);
            if (storedHash.isEmpty() || !storedHash.get().equalsIgnoreCase(inputHash)) {
                throw new ValidationException("اسم المستخدم أو كلمة المرور غير صحيحة.");
            }

            Optional<UserAccount> user = userRepository.findByUsernameOrEmail(normalizedInput);
            return user.orElseThrow(() -> new ValidationException("اسم المستخدم أو كلمة المرور غير صحيحة."));
        } catch (SQLException e) {
            throw new DatabaseException("فشل المصادقة بسبب خطأ في قاعدة البيانات.", e);
        }
    }

    @Override
    public List<UserAccount> getReviewers() {
        try {
            return userRepository.findReviewers();
        } catch (SQLException error) {
            throw new DatabaseException("تعذر تحميل قائمة الدكاترة المراجعين.", error);
        }
    }

    @Override
    public String hashPassword(String password) {
        if (password == null) {
            throw new ValidationException("كلمة المرور لا يمكن أن تكون فارغة.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new DatabaseException("خوارزمية تجزئة كلمة المرور غير متوفرة.", e);
        }
    }

    @Override
    public void registerStudent(String fullName, String studentNumber, String email, int departmentId,
                                String password, String confirmPassword) {
        if (fullName == null || fullName.trim().length() < 3) throw new ValidationException("Please enter your full name.");
        if (studentNumber == null || !studentNumber.trim().matches("[A-Za-z0-9-]{4,30}")) throw new ValidationException("Enter a valid student number.");
        if (email == null || !EMAIL.matcher(email.trim()).matches()) throw new ValidationException("Enter a valid email address.");
        if (departmentId <= 0) throw new ValidationException("Please choose a department.");
        if (password == null || password.length() < 8) throw new ValidationException("Password must contain at least 8 characters.");
        if (!password.equals(confirmPassword)) throw new ValidationException("Password confirmation does not match.");
        try {
            Map<Integer, String> departments = userRepository.findDepartments();
            if (!departments.containsKey(departmentId)) throw new ValidationException("The selected department is not available.");
            userRepository.createStudent(fullName.trim(), studentNumber.trim(), email.trim().toLowerCase(), departmentId, hashPassword(password));
        } catch (SQLException error) {
            if (error.getErrorCode() == 1) throw new ValidationException("This student number or email is already registered.");
            throw new DatabaseException("Unable to create the student account.", error);
        }
    }
}