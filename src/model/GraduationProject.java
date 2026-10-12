package model;

import com.imtidad.exception.ValidationException;

import java.util.List;

// [OOP - INHERITANCE]
// GraduationProject هو Project متخصص لمشاريع التخرج، ويرث الخصائص والسلوك المشترك من Project.
public class GraduationProject extends Project {
    private String advisor;           // المشرف الأكاديمي
    private String graduationTerm;    // فصل التخرج
    private int academicYear;         // العام الأكاديمي
    private String defenseDate;       // تاريخ المناقشة
    private String department;        // القسم الأكاديمي
    private List<String> techStack;   // التقنيات المستخدمة
    private String repositoryUrl;     // رابط المستودع
    private int completionPercentage; // نسبة الاكتمال

    // [CONSTRUCTOR OVERLOADING] Constructor شامل يحتوي تفاصيل مشروع التخرج كاملة.
    public GraduationProject(int projectId, String title, String problemStatement, PublicationStatus publicationStatus,
                             String advisor, String graduationTerm, int academicYear, String defenseDate,
                             String department, List<String> techStack, String repositoryUrl, int completionPercentage) {
        // [OOP - super] تهيئة البيانات المشتركة الموروثة من Project.
        // [OOP - super] استدعاء Constructor الأب لتجهيز الجزء المشترك.
        super(projectId, title, problemStatement, publicationStatus);
        setAdvisor(advisor);
        setGraduationTerm(graduationTerm);
        setAcademicYear(academicYear);
        setDefenseDate(defenseDate);
        setDepartment(department);
        setTechStack(techStack);
        setRepositoryUrl(repositoryUrl);
        setCompletionPercentage(completionPercentage);
    }

    // Constructor مبسط ليتوافق مع الـ Repository الحالي.
    public GraduationProject(int projectId, String title, String problemStatement, PublicationStatus publicationStatus,
                             String advisor, int academicYear, String defenseDate, String department) {
        super(projectId, title, problemStatement, publicationStatus);
        setAdvisor(advisor);
        setAcademicYear(academicYear);
        setDefenseDate(defenseDate);
        setDepartment(department);
        this.graduationTerm = "Fall";
        this.completionPercentage = 0;
    }

    // [OOP - OVERRIDE / POLYMORPHISM]
    // تنفيذ الدالة المجردة في Project وتحديد أن هذا الكائن مشروع تخرج.
    @Override
    public ProjectType getProjectType() {
        return ProjectType.GRADUATION_PROJECT;
    }

    // --- Getters and Setters ---

    public String getAdvisor() {
        return advisor;
    }

    // [VALIDATION] اسم المشرف إلزامي.
    public void setAdvisor(String advisor) {
        if (advisor == null || advisor.trim().isEmpty()) {
            throw new ValidationException("اسم المشرف لا يمكن أن يكون فارغاً.");
        }
        this.advisor = advisor;
    }

    public String getGraduationTerm() {
        return graduationTerm;
    }

    public void setGraduationTerm(String graduationTerm) {
        this.graduationTerm = graduationTerm;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    // [VALIDATION] السنة الأكاديمية يجب أن تكون قيمة موجبة.
    public void setAcademicYear(int academicYear) {
        if (academicYear <= 0) {
            throw new ValidationException("العام الأكاديمي غير صالح.");
        }
        this.academicYear = academicYear;
    }

    public String getDefenseDate() {
        return defenseDate;
    }

    public void setDefenseDate(String defenseDate) {
        this.defenseDate = defenseDate;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<String> getTechStack() {
        return techStack;
    }

    public void setTechStack(List<String> techStack) {
        this.techStack = techStack;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    // [VALIDATION] نسبة الاكتمال يجب أن تكون من 0 إلى 100.
    public void setCompletionPercentage(int completionPercentage) {
        if (completionPercentage < 0 || completionPercentage > 100) {
            throw new ValidationException("نسبة الاكتمال يجب أن تكون بين 0 و 100.");
        }
        this.completionPercentage = completionPercentage;
    }
}