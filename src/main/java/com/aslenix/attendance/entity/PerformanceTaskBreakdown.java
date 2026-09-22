package com.aslenix.attendance.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One line of the transparent task-score breakdown for a single completed
 * work item (TaskAssignment). Stored so admins can see exactly where every
 * task point came from.
 */
@Entity
@Table(name = "performance_task_breakdowns")
public class PerformanceTaskBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "score_id", nullable = false)
    private EmployeePerformanceScore score;

    @Column(name = "task_assignment_id")
    private Long taskAssignmentId;

    @Column(name = "task_title", length = 300)
    private String taskTitle;

    @Column(name = "assignment_title", length = 300)
    private String assignmentTitle;

    @Column(name = "complexity", length = 30)
    private String complexity;

    @Column(name = "base_points", nullable = false)
    private double basePoints;

    /**
     * EARLY, ON_TIME, LATE or NO_DEADLINE.
     */
    @Column(name = "deadline_outcome", length = 20)
    private String deadlineOutcome;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Positive when completed early, negative when late, 0 otherwise.
     */
    @Column(name = "days_difference", nullable = false)
    private long daysDifference;

    @Column(name = "deadline_adjustment", nullable = false)
    private double deadlineAdjustment;

    @Column(name = "total_points", nullable = false)
    private double totalPoints;

    public PerformanceTaskBreakdown() {
    }

    public Long getId() {
        return id;
    }

    public EmployeePerformanceScore getScore() {
        return score;
    }

    public void setScore(EmployeePerformanceScore score) {
        this.score = score;
    }

    public Long getTaskAssignmentId() {
        return taskAssignmentId;
    }

    public void setTaskAssignmentId(Long taskAssignmentId) {
        this.taskAssignmentId = taskAssignmentId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public String getComplexity() {
        return complexity;
    }

    public void setComplexity(String complexity) {
        this.complexity = complexity;
    }

    public double getBasePoints() {
        return basePoints;
    }

    public void setBasePoints(double basePoints) {
        this.basePoints = basePoints;
    }

    public String getDeadlineOutcome() {
        return deadlineOutcome;
    }

    public void setDeadlineOutcome(String deadlineOutcome) {
        this.deadlineOutcome = deadlineOutcome;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public long getDaysDifference() {
        return daysDifference;
    }

    public void setDaysDifference(long daysDifference) {
        this.daysDifference = daysDifference;
    }

    public double getDeadlineAdjustment() {
        return deadlineAdjustment;
    }

    public void setDeadlineAdjustment(double deadlineAdjustment) {
        this.deadlineAdjustment = deadlineAdjustment;
    }

    public double getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(double totalPoints) {
        this.totalPoints = totalPoints;
    }
}
