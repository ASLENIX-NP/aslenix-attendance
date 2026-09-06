package com.aslenix.attendance.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    // ASSIGNED EMPLOYEE
    // ============================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "employee_id",
            nullable = false
    )
    private Employee employee;

    // ============================================================
    // PRIORITY
    // ============================================================

    @Column(
            nullable = false,
            length = 20
    )
    private String priority = "MEDIUM";

    // ============================================================
    // STATUS
    // ============================================================

    /*
     * TODO
     * IN_PROGRESS
     * READY_FOR_REVIEW
     * APPROVED
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
    // DUE DATE
    // ============================================================

    @Column(
            name = "due_date"
    )
    private LocalDate dueDate;

    // ============================================================
    // DEADLINE
    // ============================================================

    @Column(
            name = "deadline"
    )
    private LocalDate deadline;

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
    // CONSTRUCTOR
    // ============================================================

    public Task() {

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
        this.status = "TODO";
        this.progress = 0;
        this.priority = "MEDIUM";
    }

    // ============================================================
    // AUTO UPDATE TIMESTAMP
    // ============================================================

    @PreUpdate
    public void preUpdate() {

        this.updatedAt = LocalDateTime.now();
    }

    // ============================================================
    // ID
    // ============================================================

    public Long getId() {
        return id;
    }

    // ============================================================
    // TASK CODE
    // ============================================================

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    // ============================================================
    // TITLE
    // ============================================================

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // ============================================================
    // DESCRIPTION
    // ============================================================

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ============================================================
    // EMPLOYEE
    // ============================================================

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    // ============================================================
    // PRIORITY
    // ============================================================

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    // ============================================================
    // STATUS
    // ============================================================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ============================================================
    // PROGRESS
    // ============================================================

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    // ============================================================
    // DUE DATE
    // ============================================================

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    // ============================================================
    // DEADLINE
    // ============================================================

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    // ============================================================
    // COMPLETION NOTE
    // ============================================================

    public String getCompletionNote() {
        return completionNote;
    }

    public void setCompletionNote(String completionNote) {
        this.completionNote = completionNote;
    }

    // ============================================================
    // COMPLETED AT
    // ============================================================

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    // ============================================================
    // REVIEW COMMENT
    // ============================================================

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    // ============================================================
    // REVIEW NOTE
    // ============================================================

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    // ============================================================
    // REVIEWED AT
    // ============================================================

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    // ============================================================
    // APPROVED AT
    // ============================================================

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    // ============================================================
    // CREATED AT
    // ============================================================

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ============================================================
    // UPDATED AT
    // ============================================================

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}