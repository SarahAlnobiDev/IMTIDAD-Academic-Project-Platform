package com.imtidad.controller;

import com.imtidad.contract.IReferenceDataService;
import com.imtidad.service.ReferenceDataService;
import com.imtidad.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the Admin Dashboard.
 * Provides overview metrics and management of reference data.
 */
public class AdminDashboardController {

    private final IReferenceDataService referenceDataService;

    @FXML
    private Label lblAdminName;
    @FXML
    private Label lblDepartmentCount;
    @FXML
    private Label lblCourseCount;
    @FXML
    private Label lblTechnologyCount;
    @FXML
    private Button btnBackToCatalog;

    public AdminDashboardController() {
        this.referenceDataService = new ReferenceDataService();
    }

    public AdminDashboardController(IReferenceDataService referenceDataService) {
        if (referenceDataService == null) {
            throw new IllegalArgumentException("Reference data service cannot be null.");
        }
        this.referenceDataService = referenceDataService;
    }

    @FXML
    public void initialize() {
        if (lblAdminName != null && SessionManager.getCurrentUser() != null) {
            lblAdminName.setText(SessionManager.getCurrentUser().getUsername());
        }
        loadMetrics();
    }

    private void loadMetrics() {
        try {
            if (lblDepartmentCount != null) {
                lblDepartmentCount.setText(String.valueOf(referenceDataService.getDepartments().size()));
            }
            if (lblCourseCount != null) {
                lblCourseCount.setText(String.valueOf(referenceDataService.getCourses().size()));
            }
            if (lblTechnologyCount != null) {
                lblTechnologyCount.setText(String.valueOf(referenceDataService.getTechnologies().size()));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load admin metrics: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToCatalog(ActionEvent event) {
        navigateTo(event, "/com/imtidad/view/ProjectListView.fxml", "نظام امتداد - إدارة المشاريع");
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            // Preserve the current Scene dimensions when returning to the catalog.
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("فشل الانتقال", "تعذر فتح الشاشة المطلوبة: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
