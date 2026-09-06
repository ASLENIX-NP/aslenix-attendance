package com.aslenix.attendance.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // EMPLOYEE
    // ============================================================

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ============================================================
    // LEAVE TYPE
    // ============================================================

    @Column(
            name = "leave_type",
            nullable = false,
            length = 50
    )
    private String leaveType;

    // ============================================================
    // DURATION
    // FULL_DAY / HALF_DAY
    // ============================================================

    @Column(
            name = "duration",
            nullable = false,
            length = 30
    )
    private String duration;

    // ============================================================
    // TOTAL DAYS
    //
    // FULL DAY:
    // 1 day  = 1.0
    // 2 days = 2.0
    // etc.
    //
    // HALF DAY:
    // 0.5
    // ============================================================

    @Column(
            name = "total_days",
            nullable = false
    )
    private Double totalDays;

    // ============================================================
    // HALF-DAY SESSION
    // FIRST_HALF / SECOND_HALF
    //
    // NULL for FULL_DAY
    // ============================================================

    @Column(
            name = "half_day_session",
            length = 30
    )
    private String halfDaySession;

    // ============================================================
    // DATES
    // ============================================================

    @Column(
            name = "start_date",
            nullable = false
    )
    private LocalDate startDate;

    @Column(
            name = "end_date",
            nullable = false
    )
    private LocalDate endDate;

    // ============================================================
    // REASON
    // ============================================================

    @Column(
            name = "reason",
            nullable = false,
            length = 1000
    )
    private String reason;

    // ============================================================
    // STATUS
    // ============================================================

    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private String status = "PENDING";

    // ============================================================
    // ADMIN REVIEW NOTE
    // ============================================================

    @Column(
            name = "review_note",
            length = 1000
    )
    private String reviewNote;

    // ============================================================
    // CREATED AT
    // ============================================================

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    // ============================================================
    // REVIEWED AT
    // ============================================================

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public LeaveRequest() {
    }

    // ============================================================
    // ID
    // ============================================================

    public Long getId() {
        return id;
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
    // LEAVE TYPE
    // ============================================================

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    // ============================================================
    // DURATION
    // ============================================================

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    // ============================================================
    // TOTAL DAYS
    // ============================================================

    public Double getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Double totalDays) {
        this.totalDays = totalDays;
    }

    // ============================================================
    // HALF-DAY SESSION
    // ============================================================

    public String getHalfDaySession() {
        return halfDaySession;
    }

    public void setHalfDaySession(String halfDaySession) {
        this.halfDaySession = halfDaySession;
    }

    // ============================================================
    // START DATE
    // ============================================================

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    // ============================================================
    // END DATE
    // ============================================================

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    // ============================================================
    // REASON
    // ============================================================

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
    // REVIEW NOTE
    // ============================================================

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
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
    // REVIEWED AT
    // ============================================================

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}