package com.imtidad.controller;

import com.imtidad.contract.IAuditLogService;
import com.imtidad.contract.IUserService;
import com.imtidad.service.AuditLogService;
import com.imtidad.service.UserService;
import com.imtidad.util.SessionManager;
import com.imtidad.util.ThemeManager;
import javafx.event.ActionEvent;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.UserAccount;

import java.io.IOException;

/**
 * متحكم تسجيل الدخول مع التحقق الصارم من الأدوار والمعالجة في الخلفية.
 */
public class LoginController {
    private final IUserService userService;
    private final IAuditLogService auditLogService;

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Button btnLogin;
    @FXML private Button btnGoogleLogin;
    @FXML private Label lblLoginState;

    public LoginController() {
        this(new UserService(), new AuditLogService());
    }

    public LoginController(IUserService userService, IAuditLogService auditLogService) {
        if (userService == null) throw new IllegalArgumentException("User service cannot be null.");
        this.userService = userService;
        this.auditLogService = auditLogService == null ? new AuditLogService() : auditLogService;
    }

    @FXML
    public void initialize() {
        cmbRole.getItems().setAll("طالب", "دكتور / مراجع", "أدمن", "زائر");
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();
        String selectedRole = cmbRole.getValue();

        if (username.isEmpty() || password.isBlank() || selectedRole == null) {
            showState("يرجى إدخال اسم المستخدم، كلمة المرور، واختيار نوع الحساب.");
            return;
        }

        // إيقاف الأدوار غير المفعلة في الإصدار الحالي
        if ("أدمن".equals(selectedRole) || "زائر".equals(selectedRole)) {
            showState("هذا الدور غير مفعل في الإصدار الحالي.");
            return;
        }

        setBusy(true, "جاري تسجيل الدخول...");

        Task<UserAccount> task = new Task<>() {
            @Override
            protected UserAccount call() {
                return userService.authenticate(username, password);
            }
        };

        task.setOnSucceeded(done -> {
            UserAccount user = task.getValue();
            String dbRole = user.getRoleName() == null ? "" : user.getRoleName().trim().toUpperCase();

            // طباعة للكونسول لمتابعة قيمة الدور المخزنة حقيقة في قاعدة البيانات
            System.out.println(">>> User Authenticated: " + user.getUsername() + " | DB Role: " + dbRole);

            // التحقق المرن والمكتمل من مطابقة الدور المختار مع دور قاعدة البيانات
            boolean roleMatches = false;

            if ("طالب".equals(selectedRole)) {
                roleMatches = dbRole.contains("STUDENT") || dbRole.contains("STUDENT_ROLE") || dbRole.contains("طالب");
            } else if ("دكتور / مراجع".equals(selectedRole)) {
                roleMatches = dbRole.contains("REVIEWER") || dbRole.contains("SUPERVISOR") ||
                        dbRole.contains("DOCTOR")   || dbRole.contains("INSTRUCTOR") ||
                        dbRole.contains("PROFESSOR")|| dbRole.contains("مراجع") || dbRole.contains("مشرف");
            }

            if (!roleMatches) {
                setBusy(false, "نوع الحساب المختار لا يطابق صلاحيات هذا الحساب (" + user.getRoleName() + ").");
                return;
            }

            // حفظ المستخدم في الجلسة والانتقال للواجهة
            SessionManager.setCurrentUser(user);
            auditLogService.logAction(user.getUserId(), "USER_LOGIN_SUCCESS", "USER_ACCOUNT", user.getUserId());
            navigateToProjectCatalog(event);
        });

        task.setOnFailed(done -> {
            auditLogService.logAction(null, "USER_LOGIN_FAILED: " + username, "USER_ACCOUNT", null);
            setBusy(false, message(task.getException()));
        });

        Thread worker = new Thread(task, "login-authentication");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    public void handleGoogleLogin() {
        showInfo("تسجيل الدخول عبر Google", "خدمة تسجيل الدخول عبر Google غير مفعلة في الإصدار الحالي.");
    }

    @FXML
    public void openStudentRegistration(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/imtidad/view/StudentRegistration.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("امتداد — إنشاء حساب طالب جديد");
            // Keep the existing Scene so the window dimensions never change while navigating.
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showState("تعذر فتح صفحة التسجيل: " + message(exception));
        }
    }

    private void setBusy(boolean busy, String state) {
        btnLogin.setDisable(busy);
        btnGoogleLogin.setDisable(busy);
        if (busy) {
            lblLoginState.setManaged(true);
            lblLoginState.setVisible(true);
            lblLoginState.setText(state);
        } else {
            showState(state);
        }
    }

    private void showState(String state) {
        lblLoginState.setText(state);
        lblLoginState.setManaged(true);
        lblLoginState.setVisible(true);
        btnLogin.setDisable(false);
        btnGoogleLogin.setDisable(false);
    }

    private void navigateToProjectCatalog(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/imtidad/view/ProjectListView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("امتداد — مساحة عمل المشاريع الأكاديمية");
            // Reuse the same Scene instead of creating a smaller/larger one.
            stage.getScene().setRoot(root);
            ThemeManager.install(stage.getScene());
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showState("تعذر فتح مساحة العمل: " + message(exception));
        }
    }

    private String message(Throwable error) {
        return error == null || error.getMessage() == null ? "فشل تسجيل الدخول. يرجى المحاولة مرة أخرى." : error.getMessage();
    }

    private void showInfo(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}