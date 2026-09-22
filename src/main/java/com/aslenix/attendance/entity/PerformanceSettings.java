package com.aslenix.attendance.entity;

import jakarta.persistence.*;

/**
 * Singleton configuration for the Employee of the Month scoring engine.
 * Every value here is admin-configurable; nothing is hard-coded in the engine.
 */
@Entity
@Table(name = "performance_settings")
public class PerformanceSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // WEIGHTS (must always sum to exactly 100)
    // ============================================================

    @Column(name = "attendance_weight", nullable = false)
    private int attendanceWeight = 30;

    @Column(name = "task_weight", nullable = false)
    private int taskWeight = 70;

    // ============================================================
    // ATTENDANCE POINTS
    // ============================================================

    @Column(name = "late_deduction_points", nullable = false)
    private double lateDeductionPoints = 5.0;

    @Column(name = "early_check_in_bonus_points", nullable = false)
    private double earlyCheckInBonusPoints = 2.0;

    @Column(name = "overtime_bonus_per_hour", nullable = false)
    private double overtimeBonusPerHour = 1.0;

    /**
     * Cap on the number of overtime hours that earn a bonus.
     * 0 (or negative) means "no cap".
     */
    @Column(name = "max_overtime_bonus_hours", nullable = false)
    private double maxOvertimeBonusHours = 0.0;

    // ============================================================
    // TASK COMPLEXITY POINTS (existing 4-level scheme)
    // ============================================================

    @Column(name = "small_points", nullable = false)
    private double smallPoints = 5.0;

    @Column(name = "medium_points", nullable = false)
    private double mediumPoints = 10.0;

    @Column(name = "large_points", nullable = false)
    private double largePoints = 15.0;

    @Column(name = "epic_points", nullable = false)
    private double epicPoints = 20.0;

    // ============================================================
    // DEADLINE PERFORMANCE POINTS
    // ============================================================

    @Column(name = "early_completion_bonus_points", nullable = false)
    private double earlyCompletionBonusPoints = 5.0;

    @Column(name = "on_time_completion_points", nullable = false)
    private double onTimeCompletionPoints = 0.0;

    @Column(name = "late_completion_deduction_points", nullable = false)
    private double lateCompletionDeductionPoints = 5.0;

    /**
     * Raw task points that map to a perfect (100) normalized task score.
     * taskScore = clamp(rawTaskPoints / taskPerfectScorePoints * 100, 0, 100).
     */
    @Column(name = "task_perfect_score_points", nullable = false)
    private double taskPerfectScorePoints = 100.0;

    public PerformanceSettings() {
    }

    public Long getId() {
        return id;
    }

    public int getAttendanceWeight() {
        return attendanceWeight;
    }

    public void setAttendanceWeight(int attendanceWeight) {
        this.attendanceWeight = attendanceWeight;
    }

    public int getTaskWeight() {
        return taskWeight;
    }

    public void setTaskWeight(int taskWeight) {
        this.taskWeight = taskWeight;
    }

    public double getLateDeductionPoints() {
        return lateDeductionPoints;
    }

    public void setLateDeductionPoints(double lateDeductionPoints) {
        this.lateDeductionPoints = lateDeductionPoints;
    }

    public double getEarlyCheckInBonusPoints() {
        return earlyCheckInBonusPoints;
    }

    public void setEarlyCheckInBonusPoints(double earlyCheckInBonusPoints) {
        this.earlyCheckInBonusPoints = earlyCheckInBonusPoints;
    }

    public double getOvertimeBonusPerHour() {
        return overtimeBonusPerHour;
    }

    public void setOvertimeBonusPerHour(double overtimeBonusPerHour) {
        this.overtimeBonusPerHour = overtimeBonusPerHour;
    }

    public double getMaxOvertimeBonusHours() {
        return maxOvertimeBonusHours;
    }

    public void setMaxOvertimeBonusHours(double maxOvertimeBonusHours) {
        this.maxOvertimeBonusHours = maxOvertimeBonusHours;
    }

    public double getSmallPoints() {
        return smallPoints;
    }

    public void setSmallPoints(double smallPoints) {
        this.smallPoints = smallPoints;
    }

    public double getMediumPoints() {
        return mediumPoints;
    }

    public void setMediumPoints(double mediumPoints) {
        this.mediumPoints = mediumPoints;
    }

    public double getLargePoints() {
        return largePoints;
    }

    public void setLargePoints(double largePoints) {
        this.largePoints = largePoints;
    }

    public double getEpicPoints() {
        return epicPoints;
    }

    public void setEpicPoints(double epicPoints) {
        this.epicPoints = epicPoints;
    }

    public double getEarlyCompletionBonusPoints() {
        return earlyCompletionBonusPoints;
    }

    public void setEarlyCompletionBonusPoints(double earlyCompletionBonusPoints) {
        this.earlyCompletionBonusPoints = earlyCompletionBonusPoints;
    }

    public double getOnTimeCompletionPoints() {
        return onTimeCompletionPoints;
    }

    public void setOnTimeCompletionPoints(double onTimeCompletionPoints) {
        this.onTimeCompletionPoints = onTimeCompletionPoints;
    }

    public double getLateCompletionDeductionPoints() {
        return lateCompletionDeductionPoints;
    }

    public void setLateCompletionDeductionPoints(double lateCompletionDeductionPoints) {
        this.lateCompletionDeductionPoints = lateCompletionDeductionPoints;
    }

    public double getTaskPerfectScorePoints() {
        return taskPerfectScorePoints;
    }

    public void setTaskPerfectScorePoints(double taskPerfectScorePoints) {
        this.taskPerfectScorePoints = taskPerfectScorePoints;
    }

    /**
     * Points awarded for a given complexity/weight value (SMALL/MEDIUM/LARGE/EPIC).
     * Unknown values fall back to the medium points.
     */
    public double complexityPoints(String complexity) {
        if (complexity == null) {
            return mediumPoints;
        }
        return switch (complexity.trim().toUpperCase()) {
            case "SMALL" -> smallPoints;
            case "LARGE" -> largePoints;
            case "EPIC" -> epicPoints;
            default -> mediumPoints;
        };
    }
}
