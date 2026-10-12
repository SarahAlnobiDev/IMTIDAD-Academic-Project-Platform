package com.imtidad.controller;

import com.imtidad.contract.ILineageService;
import com.imtidad.service.LineageService;
import com.imtidad.util.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.ContinuationRequestRecord;
import model.LineageProjectNode;
import model.Project;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Lineage visualization and continuation-request form. */
public class ProjectLineageController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private final ILineageService lineageService;
    private File selectedAttachment;

    @FXML private Label lblCurrentUser;
    @FXML private Label lblLineageState;
    @FXML private ComboBox<Project> cmbSourceProject;
    @FXML private Label lblSourceTitle;
    @FXML private Label lblSourceMeta;
    @FXML private TreeView<LineageProjectNode> treeLineage;
    @FXML private TextField txtProposedTitle;
    @FXML private ComboBox<String> cmbRelationType;
    @FXML private TextArea txtGoals;
    @FXML private TextField txtTimeline;
    @FXML private TextField txtSkills;
    @FXML private TextField txtRepositoryUrl;
    @FXML private Label lblAttachment;
    @FXML private Button btnSubmitRequest;
    @FXML private TableView<ContinuationRequestRecord> tblMyRequests;
    @FXML private TableColumn<ContinuationRequestRecord, Number> colMyRequestId;
    @FXML private TableColumn<ContinuationRequestRecord, String> colMyRequestProject;
    @FXML private TableColumn<ContinuationRequestRecord, String> colMyRequestTitle;
    @FXML private TableColumn<ContinuationRequestRecord, String> colMyRequestStatus;
    @FXML private TableColumn<ContinuationRequestRecord, String> colMyRequestDate;

    public ProjectLineageController() { this(new LineageService()); }
    public ProjectLineageController(ILineageService lineageService) {
        if (lineageService == null) throw new IllegalArgumentException("Lineage service cannot be null.");
        this.lineageService = lineageService;
    }

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() != null) {
            lblCurrentUser.setText(SessionManager.getCurrentUser().getUsername() + " · " + SessionManager.getCurrentUser().getRoleName());
        }
        configureProjectPicker();
        configureRequestTable();
        cmbRelationType.getItems().setAll("استكمال", "تطوير", "مشروع مشتق");
        cmbRelationType.setValue("استكمال");
        loadWorkspace();
    }

    private void configureProjectPicker() {
        cmbSourceProject.setConverter(new StringConverter<>() {
            @Override public String toString(Project project) { return project == null ? "" : "#" + project.getProjectId() + " · " + project.getTitle(); }
            @Override public Project fromString(String string) { return null; }
        });
        cmbSourceProject.valueProperty().addListener((obs, oldValue, value) -> loadSelectedLineage(value));
    }

    private void configureRequestTable() {
        colMyRequestId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRequestId()));
        colMyRequestProject.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProjectTitle()));
        colMyRequestTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProposedTitle()));
        colMyRequestStatus.setCellValueFactory(data -> new SimpleStringProperty(arabicRequestStatus(data.getValue().getRequestStatus().name())));
        colMyRequestDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubmittedAt() == null ? "—" : DATE.format(data.getValue().getSubmittedAt())));
    }

    @FXML public void refreshWorkspace() { loadWorkspace(); }

    /** Preselects a project when this page is opened from Project Passport. */
    public void setSourceProject(Project project) {
        if (project == null || cmbSourceProject == null) return;
        for (Project candidate : cmbSourceProject.getItems()) {
            if (candidate.getProjectId() == project.getProjectId()) {
                cmbSourceProject.setValue(candidate);
                return;
            }
        }
        // If the list was refreshed between navigation steps, keep the requested project visible.
        cmbSourceProject.getItems().add(0, project);
        cmbSourceProject.setValue(project);
    }


    private void loadWorkspace() {
        try {
            List<Project> projects = lineageService.getAvailableProjects();
            Project previous = cmbSourceProject.getValue();
            cmbSourceProject.setItems(FXCollections.observableArrayList(projects));
            if (previous != null) {
                projects.stream().filter(project -> project.getProjectId() == previous.getProjectId()).findFirst().ifPresent(cmbSourceProject::setValue);
            }
            if (cmbSourceProject.getValue() == null && !projects.isEmpty()) cmbSourceProject.setValue(projects.get(0));
            tblMyRequests.setItems(FXCollections.observableArrayList(lineageService.getMyRequests()));
            lblLineageState.setText("تم تحديث شجرة الامتداد وطلباتك.");
        } catch (RuntimeException error) {
            lblLineageState.setText("تعذر تحميل بيانات الامتداد.");
            showError("الامتداد", readableMessage(error));
        }
    }

    private void loadSelectedLineage(Project project) {
        if (project == null) {
            lblSourceTitle.setText("لا يوجد مشروع محدد"); lblSourceMeta.setText(""); treeLineage.setRoot(null); return;
        }
        lblSourceTitle.setText(project.getTitle());
        lblSourceMeta.setText("#" + project.getProjectId() + " · " + arabicProjectType(project.getProjectType().name()) + " · " + arabicProjectStatus(project.getPublicationStatus().name()));
        try {
            List<LineageProjectNode> nodes = lineageService.getLineage(project.getProjectId());
            if (nodes.isEmpty()) { treeLineage.setRoot(null); return; }
            TreeItem<LineageProjectNode> root = new TreeItem<>(nodes.get(0));
            root.setExpanded(true);
            for (int i = 1; i < nodes.size(); i++) root.getChildren().add(new TreeItem<>(nodes.get(i)));
            treeLineage.setRoot(root);
            treeLineage.setShowRoot(true);
        } catch (RuntimeException error) { showError("تحميل الشجرة", readableMessage(error)); }
    }

    @FXML public void chooseAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر ملفاً داعماً لطلب الاستكمال");
        File selected = chooser.showOpenDialog(btnSubmitRequest.getScene().getWindow());
        if (selected != null) {
            selectedAttachment = selected;
            lblAttachment.setText(selected.getName());
        }
    }

    @FXML public void clearAttachment() {
        selectedAttachment = null;
        lblAttachment.setText("لا يوجد ملف مرفق");
    }

    @FXML public void submitContinuationRequest() {
        Project source = cmbSourceProject.getValue();
        if (source == null) { showError("طلب الاستكمال", "اختر المشروع الأصلي."); return; }
        try {
            int requestId = lineageService.submitContinuationRequest(source.getProjectId(), txtProposedTitle.getText(),
                    txtGoals.getText(), txtTimeline.getText(), txtSkills.getText(), mapRelation(cmbRelationType.getValue()),
                    txtRepositoryUrl.getText(), selectedAttachment == null ? null : selectedAttachment.getAbsolutePath());
            showInfo("تم إرسال الطلب", "تم حفظ طلب/امتداد الاستكمال رقم #" + requestId + " بنجاح.");
            clearForm(); loadWorkspace();
        } catch (RuntimeException error) { showError("إرسال طلب الاستكمال", readableMessage(error)); }
    }

    private void clearForm() {
        txtProposedTitle.clear(); txtGoals.clear(); txtTimeline.clear(); txtSkills.clear(); txtRepositoryUrl.clear();
        cmbRelationType.setValue("استكمال"); clearAttachment();
    }

    @FXML public void handleBackToCatalog(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/imtidad/view/ProjectListView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root); stage.setTitle("IMTIDAD — Project workspace"); stage.setMaximized(true); stage.show();
        } catch (IOException error) { showError("الانتقال", readableMessage(error)); }
    }

    private String mapRelation(String display) {
        if ("تطوير".equals(display)) return "EXTENSION";
        if ("مشروع مشتق".equals(display)) return "DERIVATIVE";
        return "CONTINUATION";
    }
    private String arabicProjectType(String value) { return "GRADUATION_PROJECT".equals(value) ? "مشروع تخرج" : "مشروع مقرر"; }
    private String arabicProjectStatus(String value) {
        return switch (value) {
            case "PENDING_REVIEW" -> "قيد المراجعة";
            case "NEEDS_CHANGES" -> "يحتاج تعديلات";
            case "APPROVED" -> "معتمد";
            case "PUBLISHED" -> "منشور";
            default -> "مسودة";
        };
    }
    private String arabicRequestStatus(String value) {
        return switch (value) {
            case "UNDER_REVIEW" -> "قيد المراجعة";
            case "APPROVED" -> "مقبول";
            case "REJECTED" -> "مرفوض";
            case "WITHDRAWN" -> "مسحوب";
            default -> "مرسل";
        };
    }
    private String readableMessage(Throwable error) { return error == null || error.getMessage() == null ? "حدث خطأ غير متوقع." : error.getMessage(); }
    private void showError(String title, String message) { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait(); }
    private void showInfo(String title, String message) { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait(); }
}
