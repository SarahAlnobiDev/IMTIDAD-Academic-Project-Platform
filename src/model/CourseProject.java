package model;

import com.imtidad.exception.ValidationException;

import java.util.List;

// [OOP - INHERITANCE]
// CourseProject هو Project متخصص لمشاريع المقررات، ويرث الخصائص والسلوك المشترك من Project.
public class CourseProject extends Project {
    private String courseName;
    private String courseCode;
    private String semester;
    private int academicYear;
    private String courseInstructor;
    private String department;
    private List<String> techStack;
    private String repositoryUrl;
    private int completionPercentage;

    // [CONSTRUCTOR OVERLOADING] Constructor شامل يحتوي جميع تفاصيل مشروع المقرر.
    // وجود أكثر من Constructor بنفس الاسم وبـ Parameters مختلفة يسمى Constructor Overloading.
    public CourseProject(int projectId, String title, String problemStatement, PublicationStatus publicationStatus,
                         String courseName, String courseCode, String semester, int academicYear,
                         String courseInstructor, String department, List<String> techStack,
                         String repositoryUrl, int completionPercentage) {
        // [OOP - super] تهيئة الجزء الموروث من الكلاس الأب Project.
        // [OOP - super] استدعاء Constructor الأب لتجهيز البيانات المشتركة.
        super(projectId, title, problemStatement, publicationStatus);
        setCourseName(courseName);
        setCourseCode(courseCode);
        setSemester(semester);
        setAcademicYear(academicYear);
        setCourseInstructor(courseInstructor);
        setDepartment(department);
        setTechStack(techStack);
        setRepositoryUrl(repositoryUrl);
        setCompletionPercentage(completionPercentage);
    }

    // Constructor مبسط ليتوافق مع البيانات التي يعيدها الـ Repository الحالي.
    public CourseProject(int projectId, String title, String problemStatement, PublicationStatus publicationStatus,
                         String courseName, String semester, int academicYear, String courseInstructor,
                         String department) {
        super(projectId, title, problemStatement, publicationStatus);
        setCourseName(courseName);
        setSemester(semester);
        setAcademicYear(academicYear);
        setCourseInstructor(courseInstructor);
        setDepartment(department);
        this.courseCode = "N/A";
        this.completionPercentage = 0;
    }

    // [OOP - OVERRIDE / POLYMORPHISM]
    // تنفيذ الدالة المجردة الموجودة في Project وتحديد أن هذا الكائن مشروع مقرر.
    @Override
    public ProjectType getProjectType() {
        return ProjectType.COURSE_PROJECT;
    }

    // --- Getters and Setters الكاملة ---

    public String getCourseName() {
        return courseName;
    }

    // [VALIDATION] اسم المقرر إلزامي.
    public void setCourseName(String courseName) {
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new ValidationException("اسم المقرر لا يمكن أن يكون فارغاً.");
        }
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
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

    public String getCourseInstructor() {
        return courseInstructor;
    }

    public void setCourseInstructor(String courseInstructor) {
        this.courseInstructor = courseInstructor;
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

    // [VALIDATION] نسبة الاكتمال محصورة بين 0 و100.
    public void setCompletionPercentage(int completionPercentage) {
        if (completionPercentage < 0 || completionPercentage > 100) {
            throw new ValidationException("نسبة الاكتمال يجب أن تكون بين 0 و 100.");
        }
        this.completionPercentage = completionPercentage;
    }
}