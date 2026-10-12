package com.imtidad.controller;

import com.imtidad.dao.UserRepository;
import com.imtidad.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * التسجيل الذاتي للطلاب فقط؛ لا يمكن اختيار أي دور آخر هنا.
 */
public class StudentRegistrationController {
    @FXML private TextField txtFullName;
    @FXML private TextField txtStudentNumber;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<DepartmentOption> cmbDepartment;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblState;
    private final UserService userService = new UserService();

    @FXML public void initialize() {
        try {
            for (Map.Entry<Integer, String> department : new UserRepository().findDepartments().entrySet())
                cmbDepartment.getItems().add(new DepartmentOption(department.getKey(), department.getValue()));
        } catch (SQLException error) {
            showState("الأقسام غير متوفرة. يرجى التحقق من الاتصال بقاعدة البيانات.");
        }
    }

    @FXML public void register(ActionEvent event) {
        try {
            DepartmentOption department = cmbDepartment.getValue();
            if (department == null || department.id() == 0) {
                showState("يرجى اختيار القسم الأكاديمي أولاً.");
                return;
            }

            userService.registerStudent(
                    txtFullName.getText(),
                    txtStudentNumber.getText(),
                    txtEmail.getText(),
                    department.id(),
                    txtPassword.getText(),
                    txtConfirmPassword.getText()
            );
            showState("تم إنشاء الحساب بنجاح. سجل الدخول كـ طالب باستخدام الرقم الجامعي أو البريد الإلكتروني.");
            clearSensitiveFields();
        } catch (Exception error) {
            error.printStackTrace(); // طباعة السطر والمشكلة الدقيقة في Terminal
            showState(error.getMessage() == null ? "تعذر إنشاء الحساب." : error.getMessage());
        }
    }

    @FXML public void backToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/imtidad/view/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("امتداد — تسجيل الدخول");
            // Preserve the current full-screen Scene while returning to login.
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException error) {
            showState("تعذر العودة لشاشة تسجيل الدخول.");
        }
    }

    private void clearSensitiveFields() {
        txtPassword.clear();
        txtConfirmPassword.clear();
    }

    private void showState(String message) {
        lblState.setText(message);
        lblState.setVisible(true);
        lblState.setManaged(true);
    }

    private record DepartmentOption(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}