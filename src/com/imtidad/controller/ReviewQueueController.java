package com.imtidad.controller;

import com.imtidad.contract.IReviewService;
import com.imtidad.service.ReviewService;
import com.imtidad.util.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.ReviewQueueItem;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reviewer workspace dedicated only to academic project-review decisions. */
public class ReviewQueueController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final IReviewService reviewService;
    private final ObservableList<ReviewQueueItem> visibleProjects = FXCollections.observableArrayList();
    private List<ReviewQueueItem> allProjects = new ArrayList<>();

    @FXML private Label lblReviewerName;
    @FXML private Label lblReviewState;
    @FXML private TextField txtProjectSearch;
    @FXML private ComboBox<String> cmbProjectTypeFilter;
    @FXML private TableView<ReviewQueueItem> tblProjects;
    @FXML private TableColumn<ReviewQueueItem, Number> colProjectId;
    @FXML private TableColumn<ReviewQueueItem, String> colProjectTitle;
    @FXML private TableColumn<ReviewQueueItem, String> colProjectType;
    @FXML private TableColumn<ReviewQueueItem, String> colProjectOwner;
    @FXML private TableColumn<ReviewQueueItem, String> colProjectDate;
    @FXML private Label lblSelectedProjectTitle;
    @FXML private Label lblSelectedProjectMeta;
    @FXML private TextArea txtProjectProblem;
    @FXML private CheckBox chkProblemClarity;
    @FXML private CheckBox chkDatasetsValid;
    @FXML private CheckBox chkOwnershipProof;
    @FXML private CheckBox chkFileIntegrity;
    @FXML private TextArea txtReviewComments;
    @FXML private Button btnApproveProject;
    @FXML private Button btnRequestChanges;
    @FXML private Button btnRejectProject;

    public ReviewQueueController() { this(new ReviewService()); }

    public ReviewQueueController(IReviewService reviewService) {
        if (reviewService == null) throw new IllegalArgumentException("Review service cannot be null.");
        this.reviewService = reviewService;
    }

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() != null) {
            lblReviewerName.setText(SessionManager.getCurrentUser().getUsername() + " · "
                    + SessionManager.getCurrentUser().getRoleName());
        }
        configureTable();
        configureFilters();
        clearProjectDetails();
        refreshAll();
    }

    private void configureTable() {
        colProjectId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getProjectId()));
        colProjectTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colProjectType.setCellValueFactory(data -> new SimpleStringProperty(
                arabicProjectType(data.getValue().getProjectType().name())));
        colProjectOwner.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOwnerName()));
        colProjectDate.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getSubmittedAt())));
        tblProjects.setItems(visibleProjects);
        tblProjects.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, value) -> showProject(value));
    }

    private void configureFilters() {
        cmbProjectTypeFilter.getItems().setAll("الكل", "مشروع مقرر", "مشروع تخرج");
        cmbProjectTypeFilter.setValue("الكل");
        txtProjectSearch.textProperty().addListener((obs, oldValue, value) -> applyProjectFilter());
        cmbProjectTypeFilter.valueProperty().addListener((obs, oldValue, value) -> applyProjectFilter());
    }

    @FXML
    public void refreshAll() {
        try {
            allProjects = reviewService.getPendingProjectReviews();
            applyProjectFilter();
            lblReviewState.setText("جاهز · " + allProjects.size() + " مشروع بانتظار قرار المراجعة");
        } catch (RuntimeException error) {
            lblReviewState.setText("تعذر تحميل طابور المراجعة");
            showError("تحميل المراجعات", readableMessage(error));
        }
    }

    private void applyProjectFilter() {
        String search = normalized(txtProjectSearch.getText());
        String type = cmbProjectTypeFilter.getValue();
        visibleProjects.setAll(allProjects.stream().filter(item -> {
            boolean matchesSearch = search.isEmpty() || normalized(item.getTitle()).contains(search)
                    || normalized(item.getOwnerName()).contains(search)
                    || String.valueOf(item.getProjectId()).contains(search);
            boolean matchesType = type == null || "الكل".equals(type)
                    || ("مشروع مقرر".equals(type) && "COURSE_PROJECT".equals(item.getProjectType().name()))
                    || ("مشروع تخرج".equals(type) && "GRADUATION_PROJECT".equals(item.getProjectType().name()));
            return matchesSearch && matchesType;
        }).toList());
    }

    private void showProject(ReviewQueueItem item) {
        if (item == null) {
            clearProjectDetails();
            return;
        }
        lblSelectedProjectTitle.setText(item.getTitle());
        lblSelectedProjectMeta.setText("#" + item.getProjectId() + " · "
                + arabicProjectType(item.getProjectType().name()) + " · "
                + item.getDepartmentName() + " · " + item.getOwnerName());
        txtProjectProblem.setText(item.getProblemStatement() == null
                ? "لا يوجد وصف للمشكلة." : item.getProblemStatement());
        chkProblemClarity.setSelected(false);
        chkDatasetsValid.setSelected(false);
        chkOwnershipProof.setSelected(false);
        chkFileIntegrity.setSelected(false);
        txtReviewComments.clear();
        setProjectActionsDisabled(false);
    }

    private void clearProjectDetails() {
        lblSelectedProjectTitle.setText("اختر مشروعاً من القائمة");
        lblSelectedProjectMeta.setText("ستظهر بيانات المشروع هنا قبل اتخاذ القرار.");
        txtProjectProblem.clear();
        txtReviewComments.clear();
        chkProblemClarity.setSelected(false);
        chkDatasetsValid.setSelected(false);
        chkOwnershipProof.setSelected(false);
        chkFileIntegrity.setSelected(false);
        setProjectActionsDisabled(true);
    }

    @FXML public void approveProject() { submitProjectDecision("APPROVE"); }
    @FXML public void requestProjectChanges() { submitProjectDecision("REQUEST_CHANGES"); }
    @FXML public void rejectProject() { submitProjectDecision("REJECT"); }

    private void submitProjectDecision(String decision) {
        ReviewQueueItem selected = tblProjects.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("المراجعة", "اختر مشروعاً أولاً.");
            return;
        }
        try {
            reviewService.submitProjectDecision(selected.getProjectId(), decision, txtReviewComments.getText(),
                    chkProblemClarity.isSelected(), chkDatasetsValid.isSelected(),
                    chkOwnershipProof.isSelected(), chkFileIntegrity.isSelected());
            showInfo("تم حفظ القرار", decision.equals("APPROVE")
                    ? "تم قبول المشروع."
                    : "تم حفظ قرار المراجعة وملاحظاتك.");
            refreshAll();
            clearProjectDetails();
        } catch (RuntimeException error) {
            showError("حفظ قرار المراجعة", readableMessage(error));
        }
    }

    @FXML
    public void handleBackToCatalog(ActionEvent event) {
        navigateTo(event, "/com/imtidad/view/ProjectListView.fxml", "IMTIDAD — Project workspace");
    }

    private void setProjectActionsDisabled(boolean disabled) {
        btnApproveProject.setDisable(disabled);
        btnRequestChanges.setDisable(disabled);
        btnRejectProject.setDisable(disabled);
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException error) {
            showError("الانتقال", readableMessage(error));
        }
    }

    private String arabicProjectType(String type) {
        return "GRADUATION_PROJECT".equals(type) ? "مشروع تخرج" : "مشروع مقرر";
    }

    private String formatDate(java.time.LocalDateTime value) {
        return value == null ? "—" : DATE.format(value);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String readableMessage(Throwable error) {
        return error == null || error.getMessage() == null ? "حدث خطأ غير متوقع." : error.getMessage();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
