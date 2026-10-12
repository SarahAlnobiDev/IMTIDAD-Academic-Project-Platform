package com.imtidad.controller;

import com.imtidad.contract.ILineageService;
import com.imtidad.contract.IProjectService;
import com.imtidad.service.LineageService;
import com.imtidad.service.ProjectService;
import com.imtidad.util.SessionManager;
import com.imtidad.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.LineageProjectNode;
import model.Project;
import model.ProjectAttachmentInfo;
import model.ProjectType;
import model.PublicationStatus;
import model.StoredProject;
import model.UserAccount;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified project-details controller used for owners, other students, and reviewers.
 * The surrounding dashboard shell owns the sidebar; this controller renders content only.
 */
public class ProjectPassportController {

    private final IProjectService projectService;
    private final ILineageService lineageService;
    private Project project;

    @FXML private Label lblProjectTitle;
    @FXML private Label lblProjectType;
    @FXML private Label lblStatus;
    @FXML private Label lblProblemStatement;
    @FXML private Label lblOwner;
    @FXML private Label lblStudentNumberTitle;
    @FXML private Label lblStudentNumber;
    @FXML private Label lblReviewer;
    @FXML private Label lblDepartment;
    @FXML private Label lblAcademicYear;
    @FXML private Label lblCourseSemester;
    @FXML private Label lblTechStack;
    @FXML private Label lblTeamMembers;
    @FXML private Label lblActionHint;
    @FXML private Label lblNoAttachments;
    @FXML private Hyperlink lnkRepository;
    @FXML private ImageView imgProjectCover;
    @FXML private VBox coverPlaceholder;
    @FXML private FlowPane imageThumbnails;
    @FXML private FlowPane technologyChips;
    @FXML private VBox documentList;
    @FXML private Button btnSubmitForReview;
    @FXML private Button btnEditProject;
    @FXML private Button btnRequestContinuation;
    @FXML private VBox lineageSection;
    @FXML private TreeView<LineageProjectNode> treeLineage;

    public ProjectPassportController() {
        this(new ProjectService());
    }

    public ProjectPassportController(IProjectService projectService) {
        if (projectService == null) {
            throw new IllegalArgumentException("Project service cannot be null.");
        }
        this.projectService = projectService;
        this.lineageService = new LineageService();
    }

    @FXML
    public void initialize() {
        updateView();
    }

    public void setProject(Project project) {
        if (project == null) {
            this.project = null;
            clearView();
            return;
        }
        try {
            this.project = projectService.getProjectById(project.getProjectId());
        } catch (RuntimeException error) {
            this.project = project;
        }
        updateView();
    }

    private void updateView() {
        if (project == null || lblProjectTitle == null) {
            return;
        }

        lblProjectTitle.setText(safeOr(project.getTitle(), "بدون عنوان"));
        lblProjectType.setText(project.getProjectType() == ProjectType.GRADUATION_PROJECT
                ? "مشروع تخرج" : "مشروع مقرر دراسي");
        updateStatusBadge(project.getPublicationStatus());
        lblProblemStatement.setText(safeOr(project.getProblemStatement(), "لم تتم إضافة وصف بعد."));

        StoredProject stored = project instanceof StoredProject value ? value : null;
        if (stored != null) {
            lblOwner.setText(safeOr(stored.getOwnerName(), "غير محدد"));
            lblReviewer.setText(formatReviewer(stored.getReviewerName()));
            lblDepartment.setText(safeOr(stored.getDepartment(), "غير محدد"));
            lblAcademicYear.setText(formatAcademicYear(stored.getAcademicYear()));
            lblCourseSemester.setText(formatCourseSemester(stored));
            lblTeamMembers.setText(safeOr(stored.getTeamMembers(), "لا يوجد أعضاء إضافيون."));
            configureRepositoryLink(stored.getRepositoryUrl());
            updateTechnologyChips(stored.getTechStack());
            updateStudentNumberVisibility(stored);
        } else {
            lblOwner.setText("غير محدد");
            lblReviewer.setText("غير محدد");
            lblDepartment.setText("غير محدد");
            lblAcademicYear.setText("غير محدد");
            lblCourseSemester.setText("—");
            lblTeamMembers.setText("لا يوجد أعضاء إضافيون.");
            configureRepositoryLink(null);
            updateTechnologyChips(null);
            hideStudentNumber();
        }

        refreshAttachments();
        refreshLineage();
        updateActionPermissions();
    }

    private void clearView() {
        if (lblProjectTitle == null) return;
        lblProjectTitle.setText("لا يوجد مشروع محدد");
        lblProjectType.setText("");
        lblStatus.setText("");
        lblProblemStatement.setText("");
        lblOwner.setText("—");
        lblReviewer.setText("—");
        lblDepartment.setText("—");
        lblAcademicYear.setText("—");
        lblCourseSemester.setText("—");
        lblTeamMembers.setText("—");
        hideStudentNumber();
        configureRepositoryLink(null);
        technologyChips.getChildren().clear();
        imageThumbnails.getChildren().clear();
        documentList.getChildren().clear();
        showCoverPlaceholder();
        hideLineage();
        btnEditProject.setDisable(true);
        btnRequestContinuation.setDisable(true);
        btnSubmitForReview.setDisable(true);
    }

    private void updateStatusBadge(PublicationStatus status) {
        lblStatus.setText(formatStatus(status));
        lblStatus.getStyleClass().removeAll(
                "passport-status-draft", "passport-status-pending",
                "passport-status-changes", "passport-status-published");
        lblStatus.getStyleClass().add(statusStyle(status));
    }

    private void updateStudentNumberVisibility(StoredProject stored) {
        UserAccount current = SessionManager.getCurrentUser();
        boolean allowed = current != null && (
                (stored.getOwnerId() != null && stored.getOwnerId() == current.getUserId())
                        || current.isReviewer()
                        || current.isAdmin());
        lblStudentNumberTitle.setVisible(allowed);
        lblStudentNumberTitle.setManaged(allowed);
        lblStudentNumber.setVisible(allowed);
        lblStudentNumber.setManaged(allowed);
        lblStudentNumber.setText(allowed
                ? safeOr(stored.getOwnerStudentNumber(), "غير متوفر")
                : "");
    }

    private void hideStudentNumber() {
        lblStudentNumberTitle.setVisible(false);
        lblStudentNumberTitle.setManaged(false);
        lblStudentNumber.setVisible(false);
        lblStudentNumber.setManaged(false);
    }

    private void updateTechnologyChips(String techStack) {
        technologyChips.getChildren().clear();
        List<String> technologies = splitValues(techStack);
        if (technologies.isEmpty()) {
            Label empty = new Label("لم تُحدد تقنيات.");
            empty.getStyleClass().add("text-muted");
            technologyChips.getChildren().add(empty);
            lblTechStack.setText("لم تُحدد تقنيات.");
            return;
        }
        lblTechStack.setText(String.join(", ", technologies));
        for (String technology : technologies) {
            Label chip = new Label(technology);
            chip.getStyleClass().add("project-chip");
            technologyChips.getChildren().add(chip);
        }
    }

    private List<String> splitValues(String value) {
        if (value == null || value.isBlank()) return List.of();
        String normalized = value.replace("\n", ",");
        List<String> result = new ArrayList<>();
        for (String part : normalized.split(",")) {
            String item = part.trim();
            if (!item.isEmpty() && !result.contains(item)) result.add(item);
        }
        return result;
    }

    private void configureRepositoryLink(String repositoryUrl) {
        String url = safe(repositoryUrl);
        if (url.isBlank()) {
            lnkRepository.setText("لم يتم إضافة رابط للمستودع");
            lnkRepository.setDisable(true);
            lnkRepository.setUserData(null);
        } else {
            lnkRepository.setText(url);
            lnkRepository.setDisable(false);
            lnkRepository.setUserData(url);
        }
    }

    @FXML
    public void openRepository() {
        Object value = lnkRepository.getUserData();
        if (!(value instanceof String url) || url.isBlank()) return;
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("رابط GitHub غير صالح.");
            }
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("فتح المتصفح غير مدعوم على هذا الجهاز.");
            }
            Desktop.getDesktop().browse(uri);
        } catch (Exception error) {
            showError("فتح GitHub", readableMessage(error));
        }
    }

    private void refreshAttachments() {
        imageThumbnails.getChildren().clear();
        documentList.getChildren().clear();
        showCoverPlaceholder();

        if (project == null) {
            lblNoAttachments.setVisible(true);
            lblNoAttachments.setManaged(true);
            return;
        }

        List<ProjectAttachmentInfo> attachments;
        try {
            attachments = projectService.getProjectAttachments(project.getProjectId());
        } catch (RuntimeException error) {
            attachments = List.of();
        }

        boolean hasVisibleAttachment = false;
        boolean coverAssigned = false;
        for (ProjectAttachmentInfo attachment : attachments) {
            Path path = safePath(attachment.filePath());
            if (path == null || !Files.isRegularFile(path)) {
                continue;
            }
            hasVisibleAttachment = true;
            if (attachment.isImage()) {
                if (!coverAssigned) {
                    showCover(path);
                    coverAssigned = true;
                }
                imageThumbnails.getChildren().add(createImageThumbnail(path, attachment.fileName()));
            } else {
                documentList.getChildren().add(createDocumentRow(attachment, path));
            }
        }

        lblNoAttachments.setVisible(!hasVisibleAttachment);
        lblNoAttachments.setManaged(!hasVisibleAttachment);
    }

    private Path safePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return null;
        try {
            return Path.of(rawPath);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void showCover(Path path) {
        try {
            imgProjectCover.setImage(new Image(path.toUri().toString(), true));
            imgProjectCover.setVisible(true);
            imgProjectCover.setManaged(true);
            coverPlaceholder.setVisible(false);
            coverPlaceholder.setManaged(false);
        } catch (RuntimeException error) {
            showCoverPlaceholder();
        }
    }

    private void showCoverPlaceholder() {
        imgProjectCover.setImage(null);
        imgProjectCover.setVisible(false);
        imgProjectCover.setManaged(false);
        coverPlaceholder.setVisible(true);
        coverPlaceholder.setManaged(true);
    }

    private StackPane createImageThumbnail(Path path, String fileName) {
        ImageView view = new ImageView(new Image(path.toUri().toString(), 104, 74, true, true, true));
        view.setFitWidth(104);
        view.setFitHeight(74);
        view.setPreserveRatio(true);
        StackPane pane = new StackPane(view);
        pane.setAlignment(Pos.CENTER);
        pane.getStyleClass().add("project-thumbnail");
        pane.setOnMouseClicked(event -> openImagePreview(path, fileName));
        return pane;
    }

    private void openImagePreview(Path path, String fileName) {
        if (!Files.isRegularFile(path)) {
            showError("معاينة الصورة", "الصورة غير موجودة على الجهاز.");
            return;
        }
        try {
            ImageView imageView = new ImageView(new Image(path.toUri().toString()));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(900);
            imageView.setFitHeight(640);
            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setPannable(true);
            Stage stage = new Stage();
            stage.setTitle(fileName == null || fileName.isBlank() ? "معاينة الصورة" : fileName);
            stage.setScene(new Scene(scrollPane, 920, 680));
            if (btnEditProject.getScene() != null) stage.initOwner(btnEditProject.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (RuntimeException error) {
            showError("معاينة الصورة", readableMessage(error));
        }
    }

    private HBox createDocumentRow(ProjectAttachmentInfo attachment, Path path) {
        Label name = new Label(safeOr(attachment.fileName(), path.getFileName().toString()));
        name.setWrapText(true);
        name.getStyleClass().add("project-attachment-name");

        Button open = new Button("فتح");
        open.getStyleClass().addAll("btn-outline", "project-attachment-open");
        open.setOnAction(event -> openAttachment(path));

        HBox row = new HBox(10, name, open);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.getStyleClass().add("project-attachment-row");
        return row;
    }

    private void openAttachment(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            showError("فتح المرفق", "الملف غير موجود على الجهاز.");
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("فتح الملفات غير مدعوم على هذا الجهاز.");
            }
            Desktop.getDesktop().open(path.toFile());
        } catch (Exception error) {
            showError("فتح المرفق", readableMessage(error));
        }
    }

    private void refreshLineage() {
        if (lineageSection == null || treeLineage == null || project == null) return;
        try {
            List<LineageProjectNode> nodes = lineageService.getLineage(project.getProjectId());
            if (nodes == null || nodes.size() <= 1) {
                hideLineage();
                return;
            }
            TreeItem<LineageProjectNode> root = new TreeItem<>(nodes.get(0));
            root.setExpanded(true);
            for (int i = 1; i < nodes.size(); i++) {
                root.getChildren().add(new TreeItem<>(nodes.get(i)));
            }
            treeLineage.setRoot(root);
            treeLineage.setShowRoot(true);
            lineageSection.setVisible(true);
            lineageSection.setManaged(true);
        } catch (RuntimeException error) {
            hideLineage();
        }
    }

    private void hideLineage() {
        if (treeLineage != null) treeLineage.setRoot(null);
        if (lineageSection != null) {
            lineageSection.setVisible(false);
            lineageSection.setManaged(false);
        }
    }

    private void updateActionPermissions() {
        boolean editable = canCurrentUserEdit();
        boolean continuationAllowed = canCurrentUserRequestContinuation();
        boolean submitAllowed = canCurrentUserSubmitForReview();

        // Keep both primary action buttons visible so the layout is stable between users.
        btnEditProject.setVisible(true);
        btnEditProject.setManaged(true);
        btnEditProject.setDisable(!editable);

        btnRequestContinuation.setVisible(true);
        btnRequestContinuation.setManaged(true);
        btnRequestContinuation.setDisable(!continuationAllowed);

        btnSubmitForReview.setVisible(true);
        btnSubmitForReview.setManaged(true);
        btnSubmitForReview.setDisable(!submitAllowed);

        if (project != null && project.getPublicationStatus() == PublicationStatus.NEEDS_CHANGES) {
            btnSubmitForReview.setText("إعادة الإرسال للمراجعة");
        } else if (project != null && project.getPublicationStatus() == PublicationStatus.PENDING_REVIEW) {
            btnSubmitForReview.setText("قيد المراجعة");
        } else {
            btnSubmitForReview.setText("إرسال للمراجعة");
        }

        UserAccount current = SessionManager.getCurrentUser();
        if (current == null) {
            lblActionHint.setText("سجّل الدخول لعرض الإجراءات المتاحة.");
        } else if (editable) {
            lblActionHint.setText("يمكنك تعديل المشروع، أو فتح صفحة طلب الاستكمال، ثم إرساله للمراجعة عند اكتماله.");
        } else if (continuationAllowed) {
            lblActionHint.setText("يمكنك فتح صفحة طلب الاستكمال لهذا المشروع.");
        } else if (project != null && project.getPublicationStatus() == PublicationStatus.PENDING_REVIEW) {
            lblActionHint.setText("المشروع قيد المراجعة حالياً؛ التعديل متوقف حتى يصدر قرار المراجع.");
        } else if (project != null && project.getPublicationStatus() == PublicationStatus.PUBLISHED) {
            lblActionHint.setText("المشروع منشور. الصلاحيات تختلف حسب صاحب المشروع والمستخدم الحالي.");
        } else {
            lblActionHint.setText("لا توجد إجراءات متاحة لك على هذا المشروع في حالته الحالية.");
        }
    }

    private boolean canCurrentUserEdit() {
        if (!(project instanceof StoredProject stored)) return false;
        UserAccount current = SessionManager.getCurrentUser();
        if (current == null || !current.isStudent()) return false;
        if (stored.getOwnerId() == null || stored.getOwnerId() != current.getUserId()) return false;
        return project.getPublicationStatus() == PublicationStatus.DRAFT
                || project.getPublicationStatus() == PublicationStatus.NEEDS_CHANGES;
    }

    private boolean canCurrentUserSubmitForReview() {
        return canCurrentUserEdit();
    }

    private boolean canCurrentUserRequestContinuation() {
        if (!(project instanceof StoredProject)) return false;
        UserAccount current = SessionManager.getCurrentUser();
        return current != null && current.isStudent();
    }

    private boolean isCurrentUserProjectOwner() {
        if (!(project instanceof StoredProject stored)) return false;
        UserAccount current = SessionManager.getCurrentUser();
        return current != null && stored.getOwnerId() != null
                && stored.getOwnerId() == current.getUserId();
    }

    @FXML
    public void handleEditProject() {
        if (!canCurrentUserEdit()) {
            showError("تعديل المشروع", "لا تملك صلاحية تعديل المشروع في حالته الحالية.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/imtidad/view/AddProjectWizard.fxml"));
            Parent root = loader.load();
            AddProjectWizardController controller = loader.getController();
            controller.setEditProject(project);

            Stage stage = new Stage();
            stage.setTitle("امتداد — تعديل المشروع");
            stage.setScene(new Scene(root, 900, 720));
            ThemeManager.install(stage.getScene());
            if (btnEditProject.getScene() != null) stage.initOwner(btnEditProject.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(event -> refreshProject());
            stage.show();
        } catch (IOException | RuntimeException error) {
            showError("تعديل المشروع", readableMessage(error));
        }
    }

    @FXML
    public void handleRequestContinuation() {
        if (!canCurrentUserRequestContinuation()) {
            showError("طلب استكمال المشروع", "طلب الاستكمال متاح لحسابات الطلاب فقط.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/imtidad/view/ProjectLineage.fxml"));
            Parent root = loader.load();
            ProjectLineageController controller = loader.getController();
            controller.setSourceProject(project);

            Stage stage = (Stage) btnRequestContinuation.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("امتداد — طلب استكمال مشروع");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException | RuntimeException error) {
            showError("طلب الاستكمال", readableMessage(error));
        }
    }

    @FXML
    public void handleBackToCatalog() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/imtidad/view/ProjectListView.fxml"));
            Stage stage = (Stage) btnEditProject.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("امتداد — مساحة عمل المشاريع الأكاديمية");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException error) {
            showError("العودة للمشاريع", readableMessage(error));
        }
    }

    @FXML
    public void handleSubmitForReview() {
        if (!canCurrentUserSubmitForReview()) {
            showError("إرسال للمراجعة", "لا تملك صلاحية إرسال هذا المشروع للمراجعة في حالته الحالية.");
            return;
        }
        try {
            projectService.submitForReview(project.getProjectId());
            refreshProject();
            showInfo("تم الإرسال", "تم إرسال المشروع إلى المراجع المحدد بنجاح.");
        } catch (RuntimeException error) {
            showError("إرسال للمراجعة", readableMessage(error));
        }
    }

    private void refreshProject() {
        if (project == null) return;
        try {
            project = projectService.getProjectById(project.getProjectId());
            updateView();
        } catch (RuntimeException error) {
            showError("تحديث المشروع", readableMessage(error));
        }
    }

    private String formatAcademicYear(Integer year) {
        if (year == null) return "غير محدد";
        return year + "/" + (year + 1);
    }

    private String formatCourseSemester(StoredProject stored) {
        String course = safe(stored.getCourseName());
        String semester = safe(stored.getSemester());
        if (course.isBlank() && semester.isBlank()) return "غير مطبق على مشروع التخرج";
        if (course.isBlank()) return semester;
        return semester.isBlank() ? course : course + " — الفصل " + semester;
    }

    private String formatStatus(PublicationStatus status) {
        if (status == null) return "غير محدد";
        return switch (status) {
            case DRAFT -> "مسودة";
            case PENDING_REVIEW -> "قيد المراجعة";
            case NEEDS_CHANGES -> "يحتاج تعديلات";
            case APPROVED -> "معتمد";
            case PUBLISHED -> "منشور";
        };
    }

    private String statusStyle(PublicationStatus status) {
        if (status == null) return "passport-status-draft";
        return switch (status) {
            case DRAFT -> "passport-status-draft";
            case PENDING_REVIEW -> "passport-status-pending";
            case NEEDS_CHANGES -> "passport-status-changes";
            case APPROVED, PUBLISHED -> "passport-status-published";
        };
    }

    private String formatReviewer(String reviewer) {
        if (reviewer == null || reviewer.isBlank() || "غير محدد".equals(reviewer)) return "غير محدد";
        return reviewer.startsWith("د.") || reviewer.toLowerCase().startsWith("dr.") ? reviewer : "د. " + reviewer;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String readableMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank() ? "حدث خطأ غير متوقع." : message;
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
