package model;

import java.time.LocalDateTime;

/**
 * A PROJECT record enriched with the persisted project metadata required by
 * the dashboard, project passport, and edit-project workflow.
 */
// [OOP - INHERITANCE]
// يمثل مشروعاً تم تحميله من التخزين/قاعدة البيانات مع Metadata إضافية تحتاجها الواجهات.
// final تعني أننا لا نسمح بإنشاء subclass آخر منه.
public final class StoredProject extends Project {
    // [IMMUTABLE METADATA]
    // أغلب هذه البيانات final: تُحدد عند إنشاء StoredProject ولا تتغير بعد ذلك.
    private final ProjectType projectType;
    private final Integer ownerId;
    private final String ownerName;
    private final String ownerStudentNumber;
    private final Integer reviewerId;
    private final String reviewerName;
    private final LocalDateTime createdAt;
    private final String repositoryUrl;
    private final String department;
    private final Integer academicYear;
    private final String supervisorName;
    private final String courseName;
    private final String semester;
    private final String techStack;
    private final String teamMembers;

    // [CONSTRUCTOR OVERLOADING]
    // Constructor مختصر؛ يمرر القيم إلى الـ Constructor الأشمل باستخدام this(...).
    public StoredProject(int projectId, String title, String problemStatement,
                         PublicationStatus status, ProjectType projectType) {
        this(projectId, title, problemStatement, status, projectType,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    // Constructor آخر للمحافظة على التوافق مع ميزة Project Lineage الموجودة.
    /** Backward-compatible constructor used by the existing lineage feature. */
    public StoredProject(int projectId, String title, String problemStatement,
                         PublicationStatus status, ProjectType projectType,
                         Integer ownerId, String ownerName,
                         Integer reviewerId, String reviewerName,
                         LocalDateTime createdAt) {
        this(projectId, title, problemStatement, status, projectType,
                ownerId, ownerName, null, reviewerId, reviewerName, createdAt,
                null, null, null, null, null, null, null, null);
    }

    // [MAIN CONSTRUCTOR]
    // هذا هو الـ Constructor الأشمل الذي يستقبل كل Metadata الخاصة بالمشروع المخزن.
    public StoredProject(int projectId, String title, String problemStatement,
                         PublicationStatus status, ProjectType projectType,
                         Integer ownerId, String ownerName, String ownerStudentNumber,
                         Integer reviewerId, String reviewerName,
                         LocalDateTime createdAt, String repositoryUrl,
                         String department, Integer academicYear, String supervisorName,
                         String courseName, String semester, String techStack,
                         String teamMembers) {
        // [OOP - super] تهيئة البيانات الأساسية الموجودة في Project.
        super(projectId, title, problemStatement, status);
        this.projectType = projectType;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerStudentNumber = ownerStudentNumber;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.createdAt = createdAt;
        this.repositoryUrl = repositoryUrl;
        this.department = department;
        this.academicYear = academicYear;
        this.supervisorName = supervisorName;
        this.courseName = courseName;
        this.semester = semester;
        this.techStack = techStack;
        this.teamMembers = teamMembers;
    }

    // [OOP - OVERRIDE / POLYMORPHISM]
    // يعيد نوع المشروع الحقيقي الذي تم تحميله من قاعدة البيانات.
    @Override
    public ProjectType getProjectType() {
        return projectType;
    }

    // [READ ONLY] Getters فقط لأن Metadata أعلاه final ولا نريد تعديلها بعد التحميل.
    public Integer getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerStudentNumber() { return ownerStudentNumber; }
    public Integer getReviewerId() { return reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public String getDepartment() { return department; }
    public Integer getAcademicYear() { return academicYear; }
    public String getSupervisorName() { return supervisorName; }
    public String getCourseName() { return courseName; }
    public String getSemester() { return semester; }
    public String getTechStack() { return techStack; }
    public String getTeamMembers() { return teamMembers; }
}
