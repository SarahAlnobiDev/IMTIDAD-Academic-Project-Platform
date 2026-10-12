package com.imtidad.controller;

import com.imtidad.contract.IProjectService;
import com.imtidad.contract.IReferenceDataService;
import com.imtidad.contract.IUserService;
import com.imtidad.exception.ValidationException;
import com.imtidad.service.ProjectService;
import com.imtidad.service.ReferenceDataService;
import com.imtidad.service.UserService;
import com.imtidad.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.CourseProject;
import model.GraduationProject;
import model.Project;
import model.ProjectType;
import model.PublicationStatus;
import model.StoredProject;
import model.UserAccount;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/*
 * ==================== خريطة سريعة للمناقشة ====================
 * [STATE]        متغيرات تحفظ حالة الـ Wizard الحالي.
 * [OOP]          أمثلة مباشرة على مفاهيم البرمجة الكائنية.
 * [DATA]         تحميل أو تجهيز البيانات المعروضة في الواجهة.
 * [VALIDATION]   التحقق من صحة البيانات قبل الانتقال أو الحفظ.
 * [SAVE FLOW]    مسار إنشاء/تحديث المشروع في قاعدة البيانات.
 * [NAVIGATION]   الانتقال بين الخطوات أو الصفحات.
 * [HELPER]       دوال مساعدة صغيرة لتقليل التكرار.
 * ===============================================================
 */

/** Controller for the shared create/edit six-step project wizard. */
public class AddProjectWizardController {

    // [STATE] رقم الخطوة الحالية داخل الـ Wizard (من 1 إلى 6).
    private int currentStep = 1;

    // [OOP - Has-A] الـ Controller يعتمد على Services لتنفيذ منطق المشروع
    // بدل كتابة منطق قاعدة البيانات أو الأعمال مباشرة داخل الواجهة.
    private final IProjectService projectService;
    private final IReferenceDataService referenceDataService;
    private final IUserService userService;

    // [STATE] editMode=true يعني تعديل مشروع موجود، false يعني إنشاء مشروع جديد.
    private boolean editMode;

    // [SAVE FLOW] يمنع إنشاء INSERT جديد كل مرة؛ بعد أول حفظ يصبح true ونستخدم UPDATE.
    private boolean projectPersisted;

    // [STATE] رقم المشروع الذي نعمل عليه حالياً، وتُربط به المرفقات والتعديلات.
    private int workingProjectId;

    // [STATE] نحفظ الحالة الأصلية عند التعديل حتى لا تتغير حالة المشروع بالخطأ.
    private PublicationStatus originalStatus = PublicationStatus.DRAFT;

    @FXML private Label lblWizardTitle;
    @FXML private Label lblWizardSubtitle;
    @FXML private Label step1Indicator, step2Indicator, step3Indicator, step4Indicator, step5Indicator, step6Indicator;
    @FXML private Label lblCurrentStudentName, lblCurrentStudentNumber, lblDraftSaveState;
    @FXML private ScrollPane wizardScrollPane;

    @FXML private VBox step1Container, step2Container, step3Container, step4Container, step5Container, step6Container;
    @FXML private VBox courseFieldsBox;

    @FXML private Button btnBack, btnNext, btnSubmitDraft, btnUploadFile, btnCancel;

    @FXML private RadioButton rbtnGraduationProject, rbtnCourseProject;

    @FXML private TextField txtProjectId, txtProjectTitle;
    @FXML private TextArea txtProjectProblem;
    @FXML private ComboBox<String> cmbDepartment, cmbAcademicYear, cmbCourse, cmbSemester;
    @FXML private ComboBox<UserAccount> cmbReviewer;

    @FXML private TextField txtTeamMembers;
    @FXML private TextArea txtTeamListDisplay;
    @FXML private TextField txtTechStack;
    @FXML private TextField txtRepoUrl;
    @FXML private ListView<String> lstAttachments;
    @FXML private TextArea txtPreviewSummary;

    // [OOP] تهيئة الخدمات التي يحتاجها الـ Controller عند إنشاء الكائن.
    public AddProjectWizardController() {
        this.projectService = new ProjectService();
        this.referenceDataService = new ReferenceDataService();
        this.userService = new UserService();
    }

    // [DATA] تعمل تلقائياً عند فتح الصفحة:
    // تجهز القوائم، تعرض الطالب الحالي، تجهز Project ID وتظهر الخطوة الأولى.
    @FXML
    public void initialize() {
        configureReviewerCombo();
        populateReferenceData();
        populateCurrentStudent();

        if (rbtnGraduationProject != null) rbtnGraduationProject.setSelected(true);
        if (cmbSemester != null && !cmbSemester.getItems().isEmpty()) cmbSemester.getSelectionModel().selectFirst();
        prepareNewProjectId();
        handleProjectTypeChanged(null);
        updateWizardView();
    }

    // [EDIT MODE] نعيد استخدام نفس صفحة الإضافة لتعديل المشروع بدلاً من إنشاء صفحة أخرى.
    // يسمح بالتعديل فقط لصاحب المشروع وفي حالتي DRAFT أو NEEDS_CHANGES.
    /** Reuses this same wizard for editing an existing DRAFT / NEEDS_CHANGES project. */
    public void setEditProject(Project selectedProject) {
        if (selectedProject == null) throw new ValidationException("لا يوجد مشروع للتعديل.");

        Project loaded = projectService.getProjectById(selectedProject.getProjectId());
        if (!(loaded instanceof StoredProject stored)) {
            throw new ValidationException("تعذر تحميل بيانات المشروع للتعديل.");
        }

        UserAccount current = SessionManager.getCurrentUser();
        if (current == null || !current.isStudent()
                || stored.getOwnerId() == null || stored.getOwnerId() != current.getUserId()) {
            throw new ValidationException("لا تملك صلاحية تعديل هذا المشروع.");
        }
        if (stored.getPublicationStatus() != PublicationStatus.DRAFT
                && stored.getPublicationStatus() != PublicationStatus.NEEDS_CHANGES) {
            throw new ValidationException("لا يمكن تعديل المشروع في حالته الحالية.");
        }

        editMode = true;
        projectPersisted = true;
        workingProjectId = stored.getProjectId();
        originalStatus = stored.getPublicationStatus();

        lblWizardTitle.setText("تعديل المشروع — امتداد");
        lblWizardSubtitle.setText("عدّل بيانات المسودة الحالية ثم احفظ التغييرات على نفس رقم المشروع.");
        btnSubmitDraft.setText("حفظ التعديلات");

        txtProjectId.setText(String.valueOf(stored.getProjectId()));
        txtProjectTitle.setText(safe(stored.getTitle()));
        txtProjectProblem.setText(safe(stored.getProblemStatement()));
        cmbDepartment.setValue(stored.getDepartment());
        if (stored.getAcademicYear() != null) cmbAcademicYear.setValue(formatAcademicYear(stored.getAcademicYear()));
        cmbCourse.setValue(stored.getCourseName());
        cmbSemester.setValue(stored.getSemester());
        txtTechStack.setText(safe(stored.getTechStack()));
        txtTeamListDisplay.setText(safe(stored.getTeamMembers()));
        txtRepoUrl.setText(safe(stored.getRepositoryUrl()));

        boolean graduation = stored.getProjectType() == ProjectType.GRADUATION_PROJECT;
        rbtnGraduationProject.setSelected(graduation);
        rbtnCourseProject.setSelected(!graduation);
        selectReviewer(stored.getReviewerId());

        lblCurrentStudentName.setText(safeOr(stored.getOwnerName(), current.getDisplayName()));
        lblCurrentStudentNumber.setText(safeOr(stored.getOwnerStudentNumber(),
                safeOr(current.getStudentNumber(), current.getUsername())));

        currentStep = 1;
        handleProjectTypeChanged(null);
        refreshAttachments();
        updateWizardView();
    }

    // [DATA] تحميل الأقسام، السنوات، المقررات والدكاترة من الخدمات إلى الـ ComboBox.
    private void populateReferenceData() {
        try {
            if (cmbDepartment != null) cmbDepartment.getItems().setAll(referenceDataService.getDepartments());
            if (cmbAcademicYear != null) cmbAcademicYear.getItems().setAll(referenceDataService.getAcademicYearOptions());
            if (cmbCourse != null) cmbCourse.getItems().setAll(referenceDataService.getCourses());
            if (cmbReviewer != null) cmbReviewer.getItems().setAll(userService.getReviewers());
        } catch (RuntimeException error) {
            showError("تعذر تحميل البيانات المرجعية", readableMessage(error));
        }
        if (cmbSemester != null) cmbSemester.getItems().setAll("الأول", "الثاني", "الصيفي");
        if (cmbAcademicYear != null && !cmbAcademicYear.getItems().isEmpty()) {
            cmbAcademicYear.getSelectionModel().selectLast();
        }
    }

    // [UI] يحدد فقط طريقة عرض اسم الدكتور داخل ComboBox، ولا يغير بياناته.
    private void configureReviewerCombo() {
        if (cmbReviewer == null) return;
        cmbReviewer.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(UserAccount user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : reviewerDisplayName(user));
            }
        });
        cmbReviewer.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(UserAccount user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : reviewerDisplayName(user));
            }
        });
    }

    private String reviewerDisplayName(UserAccount user) {
        String name = safeOr(user.getDisplayName(), user.getUsername());
        return name.startsWith("د.") || name.toLowerCase().startsWith("dr.") ? name : "د. " + name;
    }

    // [SESSION] جلب المستخدم المسجل حالياً من SessionManager وعرض اسمه ورقمه الجامعي.
    private void populateCurrentStudent() {
        UserAccount current = SessionManager.getCurrentUser();
        if (current == null) {
            lblCurrentStudentName.setText("غير مسجل");
            lblCurrentStudentNumber.setText("—");
            return;
        }
        lblCurrentStudentName.setText(safeOr(current.getDisplayName(), current.getUsername()));
        lblCurrentStudentNumber.setText(safeOr(current.getStudentNumber(), current.getUsername()));
    }

    // [SAVE FLOW] تجهيز رقم مشروع جديد فقط عند الإنشاء، وليس أثناء Edit Mode.
    private void prepareNewProjectId() {
        if (editMode || txtProjectId == null) return;
        try {
            workingProjectId = projectService.nextAvailableProjectId();
            txtProjectId.setText(String.valueOf(workingProjectId));
        } catch (RuntimeException error) {
            txtProjectId.clear();
            showError("رقم المشروع", readableMessage(error));
        }
    }

    // [UI LOGIC] إظهار حقول المقرر فقط إذا كان النوع Course Project.
    @FXML
    public void handleProjectTypeChanged(ActionEvent event) {
        boolean course = rbtnCourseProject != null && rbtnCourseProject.isSelected();
        if (courseFieldsBox != null) {
            courseFieldsBox.setVisible(course);
            courseFieldsBox.setManaged(course);
        }
    }

    // [TEAM] إضافة اسم عضو إلى القائمة مع منع تكرار نفس النص بالخطأ.
    // ملاحظة: في نظام إنتاجي الأفضل التمييز بالرقم الجامعي/User ID لأن الأسماء قد تتكرر.
    @FXML
    public void handleAddMember(ActionEvent event) {
        if (txtTeamMembers == null || txtTeamListDisplay == null) return;
        String member = safe(txtTeamMembers.getText()).trim();
        if (member.isEmpty()) return;

        List<String> members = teamMembers();
        boolean duplicate = members.stream().anyMatch(existing -> existing.equalsIgnoreCase(member));
        if (!duplicate) {
            members.add(member);
            txtTeamListDisplay.setText(String.join("\n", members));
        }
        txtTeamMembers.clear();
    }

    // [ATTACHMENTS] قبل رفع أي ملف نتأكد أن المشروع محفوظ وله Project ID
    // حتى يمكن ربط المرفق بالمشروع في قاعدة البيانات.
    @FXML
    public void handleUploadFile(ActionEvent event) {
        try {
            if (!ensureProjectPersisted()) return;

            FileChooser chooser = new FileChooser();
            chooser.setTitle("اختر تقريراً أو صورة للمشروع");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("المرفقات المدعومة", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.doc", "*.docx"),
                    new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                    new FileChooser.ExtensionFilter("الصور", "*.png", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("Word", "*.doc", "*.docx")
            );
            Stage stage = (Stage) btnUploadFile.getScene().getWindow();
            File selected = chooser.showOpenDialog(stage);
            if (selected == null) return;

            String addedName = projectService.attachProjectFile(workingProjectId, selected.toPath());
            refreshAttachments();
            showInfo("تم إرفاق الملف", "تمت إضافة «" + addedName + "» إلى المشروع.");
        } catch (RuntimeException error) {
            showError("إضافة المرفق", readableMessage(error));
        }
    }

    // [NAVIGATION + VALIDATION] زر التالي:
    // يتحقق من الخطوة الحالية، ويحفظ Draft قبل مرحلة المرفقات، ثم ينتقل للخطوة التالية.
    @FXML
    public void handleNextStep(ActionEvent event) {
        if (!validateCurrentStep()) return;

        // Before attachments, persist a real DRAFT once so PROJECT_FILE has a valid FK.
        if (currentStep == 4 && !ensureProjectPersisted()) return;

        // Leaving attachments: persist repository URL and any latest wizard edits.
        if (currentStep == 5 && !ensureProjectPersisted()) return;

        if (currentStep < 6) {
            currentStep++;
            updateWizardView();
        }
    }

    @FXML
    public void handleBackStep(ActionEvent event) {
        if (currentStep > 1) {
            currentStep--;
            updateWizardView();
        }
    }

    // [SAVE FLOW] الحفظ النهائي للـ Wizard بعد التحقق من كل الحقول المطلوبة.
    @FXML
    public void handleSaveButtonAction(ActionEvent event) {
        if (!validateAllRequiredFields()) return;
        try {
            if (!ensureProjectPersisted()) return;
            showInfo("تم الحفظ", editMode
                    ? "تم حفظ تعديلات المشروع على نفس المسودة بنجاح."
                    : "تم حفظ المشروع كمسودة بنجاح. يمكنك تعديله لاحقاً من بطاقة المشروع.");
            closeWindow();
        } catch (RuntimeException error) {
            showError("فشل الحفظ", readableMessage(error));
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        closeWindow();
    }

    // [SAVE FLOW - دالة محورية]
    // إذا لم يُحفظ المشروع بعد: saveDraft = INSERT.
    // إذا كان محفوظاً مسبقاً: updateDraft = UPDATE على نفس Project ID.
    private boolean ensureProjectPersisted() {
        if (!validatePersistableFields()) return false;
        try {
            Project project = buildProjectFromForm();
            UserAccount reviewer = cmbReviewer.getValue();
            if (reviewer == null) {
                showError("بيانات المشروع", "اختر الدكتور المراجع أولاً.");
                return false;
            }

            if (!projectPersisted) {
                workingProjectId = projectService.saveDraft(project, reviewer.getUserId(), teamMembers());
                projectPersisted = true;
                txtProjectId.setText(String.valueOf(workingProjectId));
            } else {
                projectService.updateDraft(project, reviewer.getUserId(), teamMembers());
            }

            if (lblDraftSaveState != null) {
                lblDraftSaveState.setText("✓ المسودة محفوظة — رقم المشروع " + workingProjectId);
            }
            return true;
        } catch (RuntimeException error) {
            showError("حفظ المسودة", readableMessage(error));
            return false;
        }
    }

    // [OOP - Polymorphism]
    // يحول بيانات الفورم إلى Project؛ الكائن الفعلي يكون CourseProject أو GraduationProject
    // بينما نوع الإرجاع العام يبقى Project.
    private Project buildProjectFromForm() {
        int id = workingProjectId > 0 ? workingProjectId : Integer.parseInt(txtProjectId.getText().trim());
        String title = txtProjectTitle.getText().trim();
        String problem = safe(txtProjectProblem.getText()).trim();
        String department = cmbDepartment.getValue();
        int year = parseAcademicYear();
        UserAccount reviewer = cmbReviewer.getValue();
        String supervisor = reviewer == null ? "" : reviewerDisplayName(reviewer);
        List<String> technologies = technologyList();
        String repositoryUrl = safe(txtRepoUrl.getText()).trim();
        PublicationStatus status = editMode ? originalStatus : PublicationStatus.DRAFT;

        if (rbtnCourseProject.isSelected()) {
            return new CourseProject(
                    id, title, problem, status,
                    cmbCourse.getValue(), "N/A", cmbSemester.getValue(), year,
                    supervisor, department, technologies, repositoryUrl, 0
            );
        }
        return new GraduationProject(
                id, title, problem, status,
                supervisor, "غير محدد", year, null,
                department, technologies, repositoryUrl, 0
        );
    }

    // [VALIDATION] كل خطوة تتحقق من حقولها فقط قبل السماح بالانتقال.
    private boolean validateCurrentStep() {
        return switch (currentStep) {
            case 1 -> validateType();
            case 2 -> validateBasicInformation();
            case 3 -> true;
            case 4 -> validateTechnology();
            case 5 -> validateRepositoryUrl();
            case 6 -> true;
            default -> true;
        };
    }

    // [VALIDATION] الحد الأدنى من البيانات التي تسمح بحفظ Draft حقيقي.
    private boolean validatePersistableFields() {
        return validateType() && validateBasicInformation() && validateTechnology();
    }

    // [VALIDATION] التحقق النهائي قبل إنهاء الحفظ، ويشمل رابط GitHub.
    private boolean validateAllRequiredFields() {
        return validatePersistableFields() && validateRepositoryUrl();
    }

    private boolean validateType() {
        if (!rbtnGraduationProject.isSelected() && !rbtnCourseProject.isSelected()) {
            showError("نوع المشروع", "اختر نوع المشروع.");
            return false;
        }
        return true;
    }

    private boolean validateBasicInformation() {
        if (workingProjectId <= 0 && safe(txtProjectId.getText()).isBlank()) {
            showError("رقم المشروع", "تعذر تجهيز رقم المشروع.");
            return false;
        }
        if (safe(txtProjectTitle.getText()).isBlank()) {
            showError("البيانات الأساسية", "عنوان المشروع إلزامي.");
            return false;
        }
        if (cmbDepartment.getValue() == null || cmbDepartment.getValue().isBlank()) {
            showError("البيانات الأساسية", "اختر القسم الأكاديمي.");
            return false;
        }
        if (cmbAcademicYear.getValue() == null || cmbAcademicYear.getValue().isBlank()) {
            showError("البيانات الأساسية", "اختر السنة الأكاديمية.");
            return false;
        }
        if (cmbReviewer.getValue() == null) {
            showError("البيانات الأساسية", "اختر الدكتور المراجع / المشرف.");
            return false;
        }
        if (rbtnCourseProject.isSelected()) {
            if (cmbCourse.getValue() == null || cmbCourse.getValue().isBlank()) {
                showError("بيانات المقرر", "اختر اسم المقرر.");
                return false;
            }
            if (cmbSemester.getValue() == null || cmbSemester.getValue().isBlank()) {
                showError("بيانات المقرر", "اختر الفصل الدراسي.");
                return false;
            }
        }
        return true;
    }

    private boolean validateTechnology() {
        if (safe(txtTechStack.getText()).isBlank()) {
            showError("التقنيات", "أدخل تقنية واحدة على الأقل مستخدمة في المشروع.");
            return false;
        }
        return true;
    }

    // [VALIDATION] يقبل رابط HTTP/HTTPS صحيحاً تابعاً لـ github.com فقط.
    private boolean validateRepositoryUrl() {
        String value = safe(txtRepoUrl.getText()).trim();
        if (value.isEmpty()) {
            showError("رابط GitHub", "أدخل رابط مستودع GitHub للمشروع قبل المتابعة.");
            return false;
        }
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host == null || !(host.equalsIgnoreCase("github.com") || host.toLowerCase().endsWith(".github.com"))) {
                throw new IllegalArgumentException();
            }
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
            return true;
        } catch (IllegalArgumentException error) {
            showError("رابط GitHub", "أدخل رابط GitHub صحيحاً، مثال: https://github.com/user/project");
            return false;
        }
    }

    // [HELPER] تحويل التقنيات المكتوبة بفواصل عربية/إنجليزية إلى List بدون تكرار.
    private List<String> technologyList() {
        List<String> values = new ArrayList<>();
        for (String part : safe(txtTechStack.getText()).split("[,،]")) {
            String value = part.trim();
            if (!value.isEmpty() && values.stream().noneMatch(v -> v.equalsIgnoreCase(value))) values.add(value);
        }
        return values;
    }

    // [HELPER] تحويل أسماء أعضاء الفريق (كل اسم في سطر) إلى List.
    private List<String> teamMembers() {
        List<String> values = new ArrayList<>();
        for (String line : safe(txtTeamListDisplay.getText()).split("\\R")) {
            String value = line.trim();
            if (!value.isEmpty() && values.stream().noneMatch(v -> v.equalsIgnoreCase(value))) values.add(value);
        }
        return values;
    }

    // [DATA] إعادة تحميل أسماء مرفقات المشروع الحالي وعرضها في ListView.
    private void refreshAttachments() {
        if (lstAttachments == null || workingProjectId <= 0) return;
        try {
            lstAttachments.getItems().setAll(projectService.getProjectAttachmentNames(workingProjectId));
        } catch (RuntimeException error) {
            lstAttachments.getItems().setAll("تعذر تحميل المرفقات الحالية");
        }
    }

    private void selectReviewer(Integer reviewerId) {
        if (reviewerId == null || cmbReviewer == null) return;
        cmbReviewer.getItems().stream()
                .filter(user -> user.getUserId() == reviewerId)
                .findFirst()
                .ifPresent(cmbReviewer::setValue);
    }

    // [UI STATE] إظهار خطوة واحدة فقط، وضبط أزرار السابق/التالي/الحفظ ومؤشرات الخطوات.
    private void updateWizardView() {
        setContainerVisible(step1Container, currentStep == 1);
        setContainerVisible(step2Container, currentStep == 2);
        setContainerVisible(step3Container, currentStep == 3);
        setContainerVisible(step4Container, currentStep == 4);
        setContainerVisible(step5Container, currentStep == 5);
        setContainerVisible(step6Container, currentStep == 6);

        if (currentStep == 5 && projectPersisted) refreshAttachments();
        if (currentStep == 6) updatePreviewContent();

        btnBack.setDisable(currentStep == 1);
        boolean last = currentStep == 6;
        btnNext.setVisible(!last);
        btnNext.setManaged(!last);
        btnSubmitDraft.setVisible(last);
        btnSubmitDraft.setManaged(last);

        updateIndicatorsVisuals();
        if (wizardScrollPane != null) Platform.runLater(() -> wizardScrollPane.setVvalue(0));
    }

    private void updatePreviewContent() {
        UserAccount reviewer = cmbReviewer.getValue();
        StringBuilder preview = new StringBuilder();
        preview.append("صاحب المشروع: ").append(lblCurrentStudentName.getText()).append("\n");
        preview.append("الرقم الجامعي: ").append(lblCurrentStudentNumber.getText()).append("\n");
        preview.append("رقم المشروع: ").append(txtProjectId.getText()).append("\n");
        preview.append("النوع: ").append(rbtnGraduationProject.isSelected() ? "مشروع تخرج" : "مشروع مقرر").append("\n");
        preview.append("العنوان: ").append(txtProjectTitle.getText()).append("\n");
        preview.append("القسم: ").append(cmbDepartment.getValue()).append("\n");
        preview.append("السنة الأكاديمية: ").append(cmbAcademicYear.getValue()).append("\n");
        preview.append("الدكتور المراجع: ").append(reviewer == null ? "غير محدد" : reviewerDisplayName(reviewer)).append("\n");
        if (rbtnCourseProject.isSelected()) {
            preview.append("المقرر: ").append(cmbCourse.getValue()).append("\n");
            preview.append("الفصل الدراسي: ").append(cmbSemester.getValue()).append("\n");
        }
        preview.append("المشكلة والأهداف: ").append(safeOr(txtProjectProblem.getText(), "غير مضافة")).append("\n");
        preview.append("أعضاء الفريق: ").append(teamMembers().isEmpty() ? "لا يوجد أعضاء إضافيون" : String.join("، ", teamMembers())).append("\n");
        preview.append("التقنيات: ").append(String.join("، ", technologyList())).append("\n");
        preview.append("GitHub: ").append(txtRepoUrl.getText()).append("\n");
        preview.append("المرفقات: ").append(lstAttachments.getItems().isEmpty()
                ? "لا توجد مرفقات" : String.join("، ", lstAttachments.getItems()));
        txtPreviewSummary.setText(preview.toString());
    }

    private void setContainerVisible(VBox container, boolean visible) {
        if (container == null) return;
        container.setVisible(visible);
        container.setManaged(visible);
    }

    // [OOP REQUIREMENT - ARRAY]
    // مصفوفة Label[] تجمع مؤشرات الخطوات ونمر عليها بـ loop بدل تكرار نفس الكود 6 مرات.
    private void updateIndicatorsVisuals() {
        Label[] indicators = {step1Indicator, step2Indicator, step3Indicator, step4Indicator, step5Indicator, step6Indicator};
        for (int i = 0; i < indicators.length; i++) {
            Label indicator = indicators[i];
            if (indicator == null) continue;
            indicator.getStyleClass().removeAll("step-indicator-active", "step-indicator-completed");
            if (i + 1 == currentStep) indicator.getStyleClass().add("step-indicator-active");
            else if (i + 1 < currentStep) indicator.getStyleClass().add("step-indicator-completed");
        }
    }

    private int parseAcademicYear() {
        String value = cmbAcademicYear.getValue();
        if (value == null || value.isBlank()) return java.time.Year.now().getValue();
        try {
            return Integer.parseInt(value.split("/")[0].trim());
        } catch (NumberFormatException error) {
            throw new ValidationException("السنة الأكاديمية غير صحيحة.");
        }
    }

    private String formatAcademicYear(int year) {
        String candidate = year + "/" + (year + 1);
        if (cmbAcademicYear.getItems().contains(candidate)) return candidate;
        if (!cmbAcademicYear.getItems().contains(candidate)) cmbAcademicYear.getItems().add(candidate);
        return candidate;
    }

    // [NAVIGATION] اختصار يخرج من Add Project ويفتح صفحة طلب استكمال مشروع سابق.
    @FXML
    public void openContinuationRequests(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/imtidad/view/ProjectLineage.fxml"));
            Parent root = loader.load();

            Stage wizardStage = event != null && event.getSource() instanceof javafx.scene.Node node
                    ? (Stage) node.getScene().getWindow()
                    : (btnCancel != null && btnCancel.getScene() != null
                    ? (Stage) btnCancel.getScene().getWindow() : null);

            Window owner = wizardStage == null ? null : wizardStage.getOwner();
            if (wizardStage != null) wizardStage.close();

            if (owner instanceof Stage mainStage) {
                if (mainStage.getScene() == null) mainStage.setScene(new Scene(root));
                else mainStage.getScene().setRoot(root);
                mainStage.setTitle("امتداد — طلب استكمال مشروع");
                mainStage.setMaximized(true);
                mainStage.show();
            } else if (wizardStage != null) {
                wizardStage.setScene(new Scene(root));
                wizardStage.setTitle("امتداد — طلب استكمال مشروع");
                wizardStage.setMaximized(true);
                wizardStage.show();
            }
        } catch (IOException error) {
            showError("طلب الاستكمال", "تعذر فتح صفحة طلب الاستكمال: " + readableMessage(error));
        }
    }

    private void closeWindow() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            ((Stage) btnCancel.getScene().getWindow()).close();
        }
    }

    // [HELPER] حماية بسيطة من null عند التعامل مع النصوص.
    private String safe(String value) { return value == null ? "" : value; }

    private String safeOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String readableMessage(Throwable error) {
        if (error == null) return "حدث خطأ غير متوقع.";
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "حدث خطأ غير متوقع.";
        return message;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // [EXCEPTION/UI] عرض الخطأ للمستخدم بدل ترك الاستثناء ينهار بالواجهة.
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
