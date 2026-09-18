package com.aslenix.attendance.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // TASK CODE
    // ============================================================

    @Column(
            name = "task_code",
            nullable = false,
            unique = true,
            length = 100
    )
    private String taskCode;

    // ============================================================
    // BASIC INFORMATION
    // ============================================================

    @Column(
            nullable = false,
            length = 200
    )
    private String title;

    @Column(
            columnDefinition = "TEXT"
    )
    private String description;

    // ============================================================
    // ASSIGNED PRIMARY EMPLOYEE (Retained for backwards compatibility)
    // ============================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = true
    )
    @JoinColumn(
            name = "employee_id",
            nullable = true
    )
    private Employee employee;

    // ============================================================
    // MULTIPLE ASSIGNEES / TEAM LEADS
    // ============================================================

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private Set<Employee> assignees = new HashSet<>();

    // ============================================================
    // PRIORITY
    // ============================================================

    @Column(
            nullable = false,
            length = 20
    )
    private String priority = "MEDIUM";

    // ============================================================
    // COMPLEXITY (SMALL, MEDIUM, LARGE, EPIC)
    // ============================================================

    @Column(
            nullable = false,
            length = 30
    )
    private String complexity = "MEDIUM";

    // ============================================================
    // STATUS
    // ============================================================

    /*
     * TODO
     * IN_PROGRESS
     * READY_FOR_REVIEW / UNDER_REVIEW
     * APPROVED / COMPLETED
     */

    @Column(
            nullable = false,
            length = 30
    )
    private String status = "TODO";

    // ============================================================
    // PROGRESS
    // ============================================================

    @Column(
            nullable = false
    )
    private Integer progress = 0;

    // ============================================================
    // DUE DATE & DEADLINES
    // ============================================================

    @Column(
            name = "due_date"
    )
    private LocalDate dueDate;

    @Column(
            name = "deadline"
    )
    private LocalDate deadline;

    @Column(
            name = "deadline_bs",
            length = 50
    )
    private String deadlineBs;

    @Column(
            name = "deadline_time",
            length = 20
    )
    private String deadlineTime;

    // ============================================================
    // TAGS (comma-separated, e.g. "frontend, urgent")
    // ============================================================

    @Column(
            length = 255
    )
    private String tags;

    // ============================================================
    // SUB-TASKS / WORK ASSIGNMENTS
    // ============================================================

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<TaskAssignment> assignments = new ArrayList<>();

    // ============================================================
    // DISCUSSION COMMENTS
    // ============================================================

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    private List<TaskComment> comments = new ArrayList<>();

    // ============================================================
    // EMPLOYEE COMPLETION NOTE
    // ============================================================

    @Column(
            name = "completion_note",
            columnDefinition = "TEXT"
    )
    private String completionNote;

    // ============================================================
    // COMPLETED AT
    // ============================================================

    @Column(
            name = "completed_at"
    )
    private LocalDateTime completedAt;

    // ============================================================
    // ADMIN REVIEW COMMENT
    // ============================================================

    @Column(
            name = "review_comment",
            columnDefinition = "TEXT"
    )
    private String reviewComment;

    // ============================================================
    // REVIEW NOTE
    // ============================================================

    @Column(
            name = "review_note",
            columnDefinition = "TEXT"
    )
    private String reviewNote;

    // ============================================================
    // REVIEWED AT
    // ============================================================

    @Column(
            name = "reviewed_at"
    )
    private LocalDateTime reviewedAt;

    // ============================================================
    // APPROVED AT
    // ============================================================

    @Column(
            name = "approved_at"
    )
    private LocalDateTime approvedAt;

    // ============================================================
    // CREATED / UPDATED
    // ============================================================

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    // ============================================================
    // LOCKED STATUS
    // ============================================================

    @Column(
            name = "locked",
            nullable = false
    )
    private boolean locked = false;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public Task() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.status = "TODO";
        this.progress = 0;
        this.priority = "MEDIUM";
        this.complexity = "MEDIUM";
    }

    // ============================================================
    // AUTO UPDATE TIMESTAMP
    // ============================================================

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================================
    // GETTERS & SETTERS
    // ============================================================

    public Long getId() {
        return id;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Employee getEmployee() {
        if (employee == null && assignees != null && !assignees.isEmpty()) {
            return assignees.iterator().next();
        }
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
        if (employee != null) {
            if (this.assignees == null) {
                this.assignees = new HashSet<>();
            }
            this.assignees.add(employee);
        }
    }

    public Set<Employee> getAssignees() {
        return assignees;
    }

    public void setAssignees(Set<Employee> assignees) {
        this.assignees = assignees;
        if (assignees != null && !assignees.isEmpty()) {
            if (this.employee == null || !assignees.contains(this.employee)) {
                this.employee = assignees.iterator().next();
            }
        }
    }

    public void addAssignee(Employee employee) {
        if (employee != null) {
            if (this.assignees == null) {
                this.assignees = new HashSet<>();
            }
            this.assignees.add(employee);
            if (this.employee == null) {
                this.employee = employee;
            }
        }
    }

    public void removeAssignee(Employee employee) {
        if (employee != null && this.assignees != null) {
            this.assignees.remove(employee);
            if (this.employee != null && this.employee.getId().equals(employee.getId())) {
                this.employee = this.assignees.isEmpty() ? null : this.assignees.iterator().next();
            }
        }
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getComplexity() {
        return complexity;
    }

    public void setComplexity(String complexity) {
        this.complexity = complexity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getDeadlineBs() {
        return deadlineBs;
    }

    public void setDeadlineBs(String deadlineBs) {
        this.deadlineBs = deadlineBs;
    }

    public String getDeadlineTime() {
        return deadlineTime;
    }

    public void setDeadlineTime(String deadlineTime) {
        this.deadlineTime = deadlineTime;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<TaskAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<TaskAssignment> assignments) {
        this.assignments = assignments;
    }

    public void addAssignment(TaskAssignment assignment) {
        if (assignment != null) {
            assignment.setTask(this);
            if (this.assignments == null) {
                this.assignments = new ArrayList<>();
            }
            this.assignments.add(assignment);
        }
    }

    public void removeAssignment(TaskAssignment assignment) {
        if (assignment != null && this.assignments != null) {
            this.assignments.remove(assignment);
            assignment.setTask(null);
        }
    }

    public List<TaskComment> getComments() {
        return comments;
    }

    public void setComments(List<TaskComment> comments) {
        this.comments = comments;
    }

    public void addComment(TaskComment comment) {
        if (comment != null) {
            comment.setTask(this);
            if (this.comments == null) {
                this.comments = new ArrayList<>();
            }
            this.comments.add(comment);
        }
    }

    public String getCompletionNote() {
        return completionNote;
    }

    public void setCompletionNote(String completionNote) {
        this.completionNote = completionNote;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isLocked() {
        return locked || "APPROVED".equalsIgnoreCase(status);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}