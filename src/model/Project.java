package model;

import com.imtidad.exception.ValidationException;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Abstract base class for all academic projects.
 * Enforces encapsulation and status-transition validation.
 */
// [OOP - ABSTRACT CLASS]
// هذا هو الكلاس الأب لجميع أنواع المشاريع.
// لا ننشئ Project مباشرة؛ بل ننشئ نوعاً فعلياً مثل CourseProject أو GraduationProject.
public abstract class Project {

    // [OOP - ENCAPSULATION]
    // البيانات المشتركة بين جميع المشاريع مخفية بـ private،
    // ونصل إليها من خلال Getters/Setters حتى نستطيع التحقق من القيم قبل تخزينها.
    private int projectId;
    private String title;
    private String problemStatement;
    private PublicationStatus publicationStatus;

    // [JAVAFX PROPERTY]
    // خصائص مساعدة لربط بيانات الـ Model بعناصر JavaFX مثل TableView عند الحاجة.
    private IntegerProperty projectIdProperty;
    private StringProperty titleProperty;
    private StringProperty problemStatementProperty;

    // [CONSTRUCTOR]
    // نستخدم الـ Setters بدلاً من الإسناد المباشر حتى يمر إنشاء المشروع عبر الـ Validation.
    public Project(int projectId, String title, String problemStatement, PublicationStatus publicationStatus) {
        setProjectId(projectId);
        setTitle(title);
        setProblemStatement(problemStatement);
        setPublicationStatus(publicationStatus);
    }

    // --- Encapsulation: Validated Getters and Setters ---

    public int getProjectId() {
        return projectId;
    }

    // [VALIDATION] يمنع إنشاء مشروع برقم ID غير صالح.
    public void setProjectId(int projectId) {
        if (projectId <= 0) {
            throw new ValidationException("رقم المشروع يجب أن يكون موجباً.");
        }
        this.projectId = projectId;
        if (projectIdProperty != null) {
            projectIdProperty.set(projectId);
        }
    }

    public IntegerProperty projectIdProperty() {
        if (projectIdProperty == null) {
            projectIdProperty = new SimpleIntegerProperty(this, "projectId", projectId);
        }
        return projectIdProperty;
    }

    public String getTitle() {
        return title;
    }

    // [VALIDATION] عنوان المشروع إلزامي ولا يقبل null أو نصاً فارغاً.
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("عنوان المشروع لا يمكن أن يكون فارغاً.");
        }
        this.title = title;
        if (titleProperty != null) {
            titleProperty.set(title);
        }
    }

    public StringProperty titleProperty() {
        if (titleProperty == null) {
            titleProperty = new SimpleStringProperty(this, "title", title);
        }
        return titleProperty;
    }

    public String getProblemStatement() {
        return problemStatement;
    }

    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
        if (problemStatementProperty != null) {
            problemStatementProperty.set(problemStatement);
        }
    }

    public StringProperty problemStatementProperty() {
        if (problemStatementProperty == null) {
            problemStatementProperty = new SimpleStringProperty(this, "problemStatement", problemStatement);
        }
        return problemStatementProperty;
    }

    public PublicationStatus getPublicationStatus() {
        return publicationStatus;
    }

    public void setPublicationStatus(PublicationStatus publicationStatus) {
        if (publicationStatus == null) {
            throw new ValidationException("حالة النشر لا يمكن أن تكون فارغة.");
        }
        this.publicationStatus = publicationStatus;
    }

    /**
     * Transitions the publication status while enforcing the lifecycle rules.
     *
     * @param next the target status
     */
    // [BUSINESS RULE]
    // تغيير حالة المشروع لا يتم عشوائياً؛ أولاً نتحقق أن الانتقال للحالة الجديدة مسموح.
    public void transitionStatus(PublicationStatus next) {
        if (this.publicationStatus == null) {
            setPublicationStatus(next);
            return;
        }
        this.publicationStatus.validateTransition(next);
        setPublicationStatus(next);
    }

    // [OOP - POLYMORPHISM]
    // كل كلاس ابن يجب أن يحدد نوع المشروع بطريقته.
    // نفس الدالة getProjectType() تعطي نتيجة مختلفة حسب الكائن الحقيقي.
    public abstract ProjectType getProjectType();

    @Override
    public String toString() {
        return "Project{" +
                "projectId=" + projectId +
                ", title='" + title + '\'' +
                ", publicationStatus=" + publicationStatus +
                ", type=" + getProjectType() +
                '}';
    }
}
