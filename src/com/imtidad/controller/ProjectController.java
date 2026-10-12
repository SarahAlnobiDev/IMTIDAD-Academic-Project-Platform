package com.imtidad.controller;

import com.imtidad.contract.ILineageService;
import com.imtidad.contract.IProjectService;
import com.imtidad.service.LineageService;
import com.imtidad.service.ProjectService;
import com.imtidad.util.SessionManager;
import com.imtidad.util.ThemeManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.ContinuationRequestRecord;
import model.Project;
import model.PublicationStatus;
import model.StoredProject;
import model.UserAccount;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Role-aware dashboard for the current IMTIDAD user.
 * Database work stays outside the JavaFX application thread.
 */
public class ProjectController {
    private static final double APP_WIDTH = 1180;
    private static final double APP_HEIGHT = 720;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final IProjectService projectService;
    private final ILineageService lineageService;
    private final List<Project> allProjects = new ArrayList<>();
    private final Map<String, PublicationStatus> statusFilters = new LinkedHashMap<>();
    private Project selectedProject;
    private volatile long projectsWithExtensionsCount;
    private boolean continuationNoticeShown;

    @FXML private FlowPane projectCards;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Label lblGreeting;
    @FXML private Label lblRole;
    @FXML private Label lblDashboardTitle;
    @FXML private Label lblDashboardSubtitle;
    @FXML private Label lblMetricOneTitle;
    @FXML private Label lblMetricOneValue;
    @FXML private Label lblMetricTwoTitle;
    @FXML private Label lblMetricTwoValue;
    @FXML private Label lblMetricThreeTitle;
    @FXML private Label lblMetricThreeValue;
    @FXML private Label lblMetricFourTitle;
    @FXML private Label lblMetricFourValue;
    @FXML private Label lblDashboardState;
    @FXML private Button btnAddProject;
    @FXML private Button btnReviewQueue;
    @FXML private Button btnThemeToggle;
    @FXML private VBox emptyProjects;

    public ProjectController() {
        this(new ProjectService());
    }

    public ProjectController(IProjectService projectService) {
        if (projectService == null) {
            throw new IllegalArgumentException("Project service cannot be null.");
        }
        this.projectService = projectService;
        this.lineageService = new LineageService();
    }

    @FXML
    public void initialize() {
        configureCurrentUser();
        configureStatusFilter();
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        cmbStatusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        updateThemeButton();
        loadProjectsIntoTable();
        Platform.runLater(this::showIncomingContinuationNotice);
    }

    private void configureCurrentUser() {
        UserAccount user = SessionManager.getCurrentUser();
        if (user == null) {
            lblGreeting.setText("مرحباً");
            lblRole.setText("لا توجد جلسة نشطة");
            lblDashboardTitle.setText("استكشف مشاريع الجامعة");
            lblDashboardSubtitle.setText("يرجى تسجيل الدخول للمتابعة.");
            return;
        }

        // displayName comes from the authenticated database record stored in SessionManager.
        lblGreeting.setText("مرحباً، " + user.getDisplayName());
        lblRole.setText(roleLabel(user));
        lblDashboardTitle.setText("استكشف مشاريع الجامعة");
        lblDashboardSubtitle.setText(
                "اكتشف مشاريع التخرج ومشاريع المواد، وابحث عن فرص الاستكمال في مختلف التخصصات.");

    }

    private void configureStatusFilter() {
        statusFilters.clear();
        statusFilters.put("كل الحالات", null);
        statusFilters.put("مسودة", PublicationStatus.DRAFT);
        statusFilters.put("قيد المراجعة", PublicationStatus.PENDING_REVIEW);
        statusFilters.put("يحتاج تعديلات", PublicationStatus.NEEDS_CHANGES);
        statusFilters.put("معتمد", PublicationStatus.APPROVED);
        statusFilters.put("منشور", PublicationStatus.PUBLISHED);
        cmbStatusFilter.getItems().setAll(statusFilters.keySet());
        cmbStatusFilter.getSelectionModel().selectFirst();
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
    public void refreshProjects() {
        loadProjectsIntoTable();
    }

    public void loadProjectsIntoTable() {
        setLoading(true, "جاري تحديث لوحة التحكم...");
        Task<List<Project>> task = new Task<>() {
            @Override
            protected List<Project> call() {
                List<Project> projects = projectService.getAllProjects();
                projectsWithExtensionsCount = projectService.countProjectsWithExtensions();
                return projects;
            }
        };

        task.setOnSucceeded(event -> {
            allProjects.clear();
            allProjects.addAll(task.getValue());
            selectedProject = null;
            updateMetrics();
            applyFilters();
            setLoading(false, allProjects.isEmpty()
                    ? emptyStateMessage()
                    : "تم تحميل " + allProjects.size() + " مشروع.");
        });

        task.setOnFailed(event -> {
            setLoading(false, "تعذر تحميل بيانات لوحة التحكم.");
            showError("الاتصال بقاعدة البيانات", readableMessage(task.getException()));
        });

        Thread worker = new Thread(task, "project-dashboard-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyFilters() {
        if (projectCards == null) {
            return;
        }

        String query = txtSearch.getText() == null
                ? ""
                : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        PublicationStatus selectedStatus = statusFilters.get(cmbStatusFilter.getValue());

        List<Project> visibleProjects = allProjects.stream()
                .filter(project -> matches(project, query, selectedStatus))
                .toList();

        projectCards.getChildren().setAll(
                visibleProjects.stream().map(this::createProjectCard).toList());

        boolean showEmpty = visibleProjects.isEmpty();
        emptyProjects.setVisible(showEmpty);
        emptyProjects.setManaged(showEmpty);

        if (!allProjects.isEmpty()) {
            lblDashboardState.setText("المعروض الآن: " + visibleProjects.size() + " من " + allProjects.size());
        }
    }

    private boolean matches(Project project, String query, PublicationStatus status) {
        String title = project.getTitle() == null ? "" : project.getTitle().toLowerCase(Locale.ROOT);
        String problem = project.getProblemStatement() == null
                ? ""
                : project.getProblemStatement().toLowerCase(Locale.ROOT);
        boolean queryMatch = query.isEmpty() || title.contains(query) || problem.contains(query);
        boolean statusMatch = status == null || project.getPublicationStatus() == status;
        return queryMatch && statusMatch;
    }

    private VBox createProjectCard(Project project) {
        VBox card = new VBox(11);
        card.getStyleClass().add("home-project-status-card");
        card.setPrefWidth(300);

        HBox top = new HBox(8);
        Label type = new Label(formatProjectType(project));
        type.getStyleClass().add("home-card-eyebrow");

        Label badge = new Label(formatStatus(project.getPublicationStatus()));
        badge.getStyleClass().addAll(
                "home-status-badge",
                "home-status-" + project.getPublicationStatus().name().toLowerCase(Locale.ROOT));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(type, spacer, badge);

        Label title = new Label(project.getTitle());
        title.setWrapText(true);
        title.getStyleClass().add("home-project-card-title");

        Label person = new Label(projectPersonLine(project));
        person.getStyleClass().add("home-project-card-meta");

        String dateText = projectDateLine(project);
        Label date = new Label(dateText);
        date.getStyleClass().add("home-project-card-meta");
        date.setVisible(!dateText.isBlank());
        date.setManaged(!dateText.isBlank());

        Label footer = new Label("رقم المشروع: " + project.getProjectId());
        footer.getStyleClass().add("metric-note");

        card.getChildren().addAll(top, title, person, date, footer);
        card.setOnMouseClicked(event -> {
            selectedProject = project;
            projectCards.getChildren().forEach(node ->
                    node.getStyleClass().remove("home-project-card-selected"));
            card.getStyleClass().add("home-project-card-selected");
        });
        return card;
    }

    private String projectPersonLine(Project project) {
        if (!(project instanceof StoredProject stored)) {
            return "";
        }

        UserAccount currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && currentUser.isReviewer()) {
            return "صاحب المشروع: " + safeText(stored.getOwnerName(), "غير محدد");
        }
        return "المراجع: " + safeText(stored.getReviewerName(), "لم يحدد بعد");
    }

    private String projectDateLine(Project project) {
        if (project instanceof StoredProject stored && stored.getCreatedAt() != null) {
            return "تاريخ الإنشاء: " + stored.getCreatedAt().format(DATE_FORMAT);
        }
        return "";
    }

    private void updateMetrics() {
        UserAccount user = SessionManager.getCurrentUser();
        if (user != null && user.isReviewer()) {
            long pending = countStatus(PublicationStatus.PENDING_REVIEW);
            long reviewed = allProjects.stream()
                    .filter(project -> project.getPublicationStatus() != PublicationStatus.PENDING_REVIEW)
                    .count();
            setMetric(lblMetricOneTitle, lblMetricOneValue, "إجمالي المشاريع", allProjects.size());
            setMetric(lblMetricTwoTitle, lblMetricTwoValue, "بانتظار المراجعة", pending);
            setMetric(lblMetricThreeTitle, lblMetricThreeValue, "تمت مراجعتها", reviewed);
            setMetric(lblMetricFourTitle, lblMetricFourValue, "مشاريع لها امتدادات", projectsWithExtensionsCount);
            return;
        }

        long drafts = countStatus(PublicationStatus.DRAFT);
        long pending = countStatus(PublicationStatus.PENDING_REVIEW);
        setMetric(lblMetricOneTitle, lblMetricOneValue, "إجمالي المشاريع", allProjects.size());
        setMetric(lblMetricTwoTitle, lblMetricTwoValue, "المسودات", drafts);
        setMetric(lblMetricThreeTitle, lblMetricThreeValue, "قيد المراجعة", pending);
        setMetric(lblMetricFourTitle, lblMetricFourValue, "مشاريع لها امتدادات", projectsWithExtensionsCount);
    }

    private long countStatus(PublicationStatus status) {
        return allProjects.stream()
                .filter(project -> project.getPublicationStatus() == status)
                .count();
    }

    private void setMetric(Label titleLabel, Label valueLabel, String title, long value) {
        titleLabel.setText(title);
        valueLabel.setText(String.valueOf(value));
    }

    private void setLoading(boolean loading, String message) {
        lblDashboardState.setText(message);
        if (btnAddProject != null && SessionManager.getCurrentUser() != null
                && SessionManager.getCurrentUser().isStudent()) {
            btnAddProject.setDisable(loading);
        }
    }

    @FXML
    public void showHome() {
        txtSearch.clear();
        cmbStatusFilter.getSelectionModel().selectFirst();
        applyFilters();
    }

    @FXML
    public void showMyProjects() {
        showHome();
        lblDashboardState.setText(SessionManager.getCurrentUser() != null
                && SessionManager.getCurrentUser().isReviewer()
                ? "عرض جميع المشاريع المتاحة للدكتور."
                : "عرض مشاريعك فقط.");
    }

    @FXML
    public void openAddProjectWindow() {
        UserAccount user = SessionManager.getCurrentUser();
        if (user == null || !user.isStudent()) {
            showInfo("صلاحية الوصول", "غير مصرح لك بإضافة مشروع.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/imtidad/view/AddProjectWizard.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("امتداد — إضافة مشروع");
            stage.setScene(new Scene(root));
            ThemeManager.install(stage.getScene());
            stage.initOwner(currentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(event -> loadProjectsIntoTable());
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showError("إضافة مشروع", readableMessage(exception));
        }
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
    public void openContinuationRequests() {
        UserAccount user = SessionManager.getCurrentUser();
        if (user == null || !user.isStudent()) {
            showInfo("طلبات الاستكمال", "هذه الخاصية متاحة لحسابات الطلاب فقط.");
            return;
        }
        navigateTo("/com/imtidad/view/ProjectLineage.fxml", "امتداد — طلبات الاستكمال");
    }

    private void showIncomingContinuationNotice() {
        if (continuationNoticeShown) return;
        UserAccount current = SessionManager.getCurrentUser();
        if (current == null || !current.isStudent()) return;
        continuationNoticeShown = true;
        try {
            List<ContinuationRequestRecord> incoming = lineageService.getIncomingRequests();
            if (incoming == null || incoming.isEmpty()) return;

            ContinuationRequestRecord request = incoming.get(0);
            int remaining = incoming.size() - 1;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("طلب استكمال وارد");
            alert.setHeaderText("لديك طلب استكمال لمشروعك «" + request.getProjectTitle() + "»");
            alert.setContentText("مقدم الطلب: " + request.getRequesterName()
                    + "\nالامتداد المقترح: " + request.getProposedTitle()
                    + (remaining > 0 ? "\nوهناك " + remaining + " طلب إضافي." : "")
                    + "\n\nيمكنك اتخاذ القرار الآن أو اختيار «لاحقاً».");

            ButtonType approve = new ButtonType("قبول", ButtonBar.ButtonData.OK_DONE);
            ButtonType reject = new ButtonType("رفض", ButtonBar.ButtonData.NO);
            ButtonType later = new ButtonType("لاحقاً", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(approve, reject, later);

            alert.showAndWait().ifPresent(choice -> {
                try {
                    if (choice == approve) {
                        lineageService.decideIncomingRequest(request.getRequestId(), true,
                                "تمت الموافقة من صاحب المشروع.");
                        showInfo("طلب الاستكمال", "تم قبول الطلب وإنشاء مشروع امتداد مستقل لمقدم الطلب.");
                    } else if (choice == reject) {
                        lineageService.decideIncomingRequest(request.getRequestId(), false,
                                "رفض صاحب المشروع طلب الاستكمال.");
                        showInfo("طلب الاستكمال", "تم رفض طلب الاستكمال.");
                    }
                } catch (RuntimeException error) {
                    showError("طلب الاستكمال", readableMessage(error));
                }
            });
        } catch (RuntimeException ignored) {
            // Continuation notifications must never prevent the dashboard from opening.
        }
    }

    @FXML
    public void openAbout() {
        navigateTo("/com/imtidad/view/AboutView.fxml", "امتداد — عن امتداد");
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
            // Login always stays on its original light design.
            ThemeManager.uninstall(stage.getScene());
            // Reuse the same Scene to keep exactly the same window size.
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showError("تسجيل الخروج", readableMessage(exception));
        }
    }

    @FXML
    public void openProjectPassport() {
        if (selectedProject == null) {
            showInfo("اختيار مشروع", "اختر مشروعاً أولاً ثم افتح بطاقة المشروع.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/imtidad/view/ProjectPassport.fxml"));
            Parent root = loader.load();
            loader.<ProjectPassportController>getController().setProject(selectedProject);
            Stage stage = currentStage();
            stage.getScene().setRoot(root);
            stage.setTitle("امتداد — بطاقة المشروع");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showError("فتح المشروع", readableMessage(exception));
        }
    }

    private void navigateTo(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            Stage stage = currentStage();
            // Main navigation always swaps only the root; the Scene and Stage stay unchanged.
            stage.getScene().setRoot(root);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException exception) {
            showError("التنقل", readableMessage(exception));
        }
    }

    private Stage currentStage() {
        return (Stage) btnAddProject.getScene().getWindow();
    }

    private String formatStatus(PublicationStatus status) {
        return switch (status) {
            case DRAFT -> "مسودة";
            case PENDING_REVIEW -> "قيد المراجعة";
            case NEEDS_CHANGES -> "يحتاج تعديلات";
            case APPROVED -> "معتمد";
            case PUBLISHED -> "منشور";
        };
    }

    private String formatProjectType(Project project) {
        return switch (project.getProjectType()) {
            case COURSE_PROJECT -> "مشروع مقرر";
            case GRADUATION_PROJECT -> "مشروع تخرج";
        };
    }

    private String roleLabel(UserAccount user) {
        if (user.isStudent()) {
            return "طالب";
        }
        if (user.isReviewer()) {
            return "دكتور / مراجع";
        }
        if (user.isAdmin()) {
            return "مدير النظام — غير مفعل حالياً";
        }
        return "دور غير مفعل حالياً";
    }

    private String emptyStateMessage() {
        UserAccount user = SessionManager.getCurrentUser();
        return user != null && user.isReviewer()
                ? "لا توجد مشاريع متاحة حالياً."
                : "لا توجد مشاريع في حسابك حتى الآن.";
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String readableMessage(Throwable exception) {
        return exception == null || exception.getMessage() == null
                ? "حدث خطأ غير متوقع."
                : exception.getMessage();
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
