package com.imtidad.controller;

import com.imtidad.util.SessionManager;
import com.imtidad.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.UserAccount;

import java.io.IOException;

/** Controller for the IMTIDAD About page. */
public class AboutController {

    @FXML private Label lblGreeting;
    @FXML private Label lblRole;
    @FXML private Button btnThemeToggle;
    @FXML private Button btnAbout;

    @FXML
    public void initialize() {
        UserAccount user = SessionManager.getCurrentUser();
        if (user == null) {
            lblGreeting.setText("مرحباً");
            lblRole.setText("لا توجد جلسة نشطة");
        } else {
            lblGreeting.setText("مرحباً، " + user.getDisplayName());
            lblRole.setText(roleLabel(user));
        }
        updateThemeButton();
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle();
        updateThemeButton();
    }

    private void updateThemeButton() {
        if (btnThemeToggle == null) return;
        boolean dark = ThemeManager.isDarkMode();
        btnThemeToggle.setText(dark ? "☀" : "☾");
        if (btnThemeToggle.getTooltip() != null) {
            btnThemeToggle.getTooltip().setText(dark ? "التبديل إلى الوضع الفاتح" : "التبديل إلى الوضع الليلي");
        }
    }

    @FXML
    public void showHome() {
        navigateToDashboard(false);
    }

    @FXML
    public void showMyProjects() {
        navigateToDashboard(true);
    }

    private void navigateToDashboard(boolean myProjects) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/imtidad/view/ProjectListView.fxml"));
            Parent root = loader.load();
            Stage stage = currentStage();
            stage.getScene().setRoot(root);
            stage.setTitle("امتداد — الصفحة الرئيسية");
            stage.setMaximized(true);
            stage.show();
            if (myProjects) {
                loader.<ProjectController>getController().showMyProjects();
            }
        } catch (IOException exception) {
            showError("التنقل", readableMessage(exception));
        }
    }

    @FXML
    public void openAddProjectWindow() {
        UserAccount user = SessionManager.getCurrentUser();
        if (user == null || !user.isStudent()) {
            showInfo("صلاحية الوصول", "غير مصرح لك بإضافة مشروع.");
            return;
        }

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/imtidad/view/AddProjectWizard.fxml"));
            Stage stage = new Stage();
            stage.setTitle("امتداد — إضافة مشروع");
            stage.setScene(new Scene(root));
            ThemeManager.install(stage.getScene());
            stage.initOwner(currentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showError("إضافة مشروع", readableMessage(exception));
        }
    }

    @FXML
    public void openContinuationRequests() {
        UserAccount user = SessionManager.getCurrentUser();
        if (user == null || !user.isStudent()) {
            showInfo("طلبات الاستكمال", "هذه الخاصية متاحة لحسابات الطلاب فقط.");
            return;
        }
        navigateTo("/com/imtidad/view/ProjectLineage.fxml", "امتداد — طلبات الاستكمال");
    }

    @FXML
    public void openReviewQueue() {
        UserAccount user = SessionManager.getCurrentUser();
        if (user == null || !user.isReviewer()) {
            showInfo("صلاحية الوصول", "هذه الصفحة مخصصة للدكتور.");
            return;
        }
        navigateTo("/com/imtidad/view/ReviewQueue.fxml", "امتداد — طلبات المراجعة");
    }

    @FXML
    public void showSettingsNotice() {
        showInfo("الإعدادات", "صفحة الإعدادات غير مفعلة حالياً.");
    }

    @FXML
    public void logout() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/imtidad/view/Login.fxml"));
            Stage stage = currentStage();
            stage.setTitle("امتداد — تسجيل الدخول");
            ThemeManager.uninstall(stage.getScene());
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showError("تسجيل الخروج", readableMessage(exception));
        }
    }

    private void navigateTo(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            Stage stage = currentStage();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showError("التنقل", readableMessage(exception));
        }
    }

    private Stage currentStage() {
        return (Stage) btnAbout.getScene().getWindow();
    }

    private String roleLabel(UserAccount user) {
        if (user.isStudent()) return "طالب";
        if (user.isReviewer()) return "دكتور / مراجع";
        if (user.isAdmin()) return "مدير النظام — غير مفعل حالياً";
        return "دور غير مفعل حالياً";
    }

    private String readableMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return "حدث خطأ غير متوقع.";
        }
        return error.getMessage();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
