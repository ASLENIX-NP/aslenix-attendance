package com.aslenix.attendance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_assignments")
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    // ============================================================
    // SUBTASK / WEEK NUMBER (1..N)
    // ============================================================

    @Column(name = "subtask_number", nullable = false)
    private Integer subtaskNumber = 1;

    // ============================================================
    // TITLE & ADMIN-PROVIDED WORK DESCRIPTION
    // ============================================================

    @Column(nullable = false, length = 255)
    private String title = "Week 1";

    @Column(columnDefinition = "TEXT")
    private String description;

    // ============================================================
    // ASSIGNED TEAM MEMBER
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee assignee;

    // ============================================================
    // STATUS (TODO, IN_PROGRESS, READY_FOR_REVIEW, APPROVED, DECLINED)
    // ============================================================

    @Column(nullable = false, length = 30)
    private String status = "TODO";

    // ============================================================
    // COMPLEXITY / WEIGHT (SMALL, MEDIUM, LARGE, EPIC)
    // ============================================================

    @Column(length = 20)
    private String weight = "MEDIUM";

    // ============================================================
    // PROGRESS (0 to 100%, manually edited with mandatory note)
    // ============================================================

    @Column(name = "progress", nullable = false)
    private Integer progress = 0;

    // ============================================================
    // REVIEW & DECLINE REASON
    // ============================================================

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ============================================================
    // LEGACY / OPTIONAL DEADLINES
    // ============================================================

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "deadline_bs", length = 50)
    private String deadlineBs;

    @Column(name = "deadline_time", length = 20)
    private String deadlineTime;

    @Column(name = "overdue_notified", nullable = false)
    private boolean overdueNotified = false;

    @Column(columnDefinition = "TEXT")
    private String note;

    // ============================================================
    // AUDIT HISTORIES (All progress edits require note)
    // ============================================================

    @OneToMany(
            mappedBy = "assignment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt DESC")
    private List<TaskAssignmentHistory> histories = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TaskAssignment() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.status = "TODO";
        this.weight = "MEDIUM";
        this.progress = 0;
        this.subtaskNumber = 1;
        this.locked = false;
        this.overdueNotified = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOverdue() {
        if (progress != null && progress >= 100) return false;
        if ("APPROVED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "VERIFIED".equalsIgnoreCase(status)) return false;
        if (deadline == null) return false;

        LocalDate today = LocalDate.now();
        if (deadline.isBefore(today)) return true;
        if (deadline.isEqual(today) && deadlineTime != null && !deadlineTime.trim().isEmpty()) {
            try {
                LocalTime dt = LocalTime.parse(deadlineTime.trim());
                return LocalTime.now().isAfter(dt);
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public void addHistory(TaskAssignmentHistory history) {
        if (history != null) {
            this.histories.add(history);
            history.setAssignment(this);
        }
    }

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Integer getSubtaskNumber() {
        return subtaskNumber != null ? subtaskNumber : 1;
    }

    public void setSubtaskNumber(Integer subtaskNumber) {
        this.subtaskNumber = subtaskNumber != null ? subtaskNumber : 1;
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

    public Employee getAssignee() {
        return assignee;
    }

    public void setAssignee(Employee assignee) {
        this.assignee = assignee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
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

    public Integer getProgress() {
        return progress != null ? progress : 0;
    }

    public void setProgress(Integer progress) {
        this.progress = progress != null ? Math.max(0, Math.min(100, progress)) : 0;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public boolean isOverdueNotified() {
        return overdueNotified;
    }

    public void setOverdueNotified(boolean overdueNotified) {
        this.overdueNotified = overdueNotified;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<TaskAssignmentHistory> getHistories() {
        return histories;
    }

    public void setHistories(List<TaskAssignmentHistory> histories) {
        this.histories = histories;
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
}
