package com.aslenix.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_audit_logs")
public class AttendanceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_username", nullable = false, length = 100)
    private String adminUsername;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "employee_name", length = 150)
    private String employeeName;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "original_check_in")
    private LocalDateTime originalCheckIn;

    @Column(name = "new_check_in")
    private LocalDateTime newCheckIn;

    @Column(name = "original_check_out")
    private LocalDateTime originalCheckOut;

    @Column(name = "new_check_out")
    private LocalDateTime newCheckOut;

    @Column(name = "original_status", length = 50)
    private String originalStatus;

    @Column(name = "new_status", nullable = false, length = 50)
    private String newStatus;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "correction_type", nullable = false, length = 50)
    private String correctionType; // REQUEST_APPROVAL, DIRECT_EDIT

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public AttendanceAuditLog() {
    }

    public AttendanceAuditLog(
            String adminUsername,
            Employee employee,
            String employeeCode,
            String employeeName,
            LocalDate attendanceDate,
            LocalDateTime originalCheckIn,
            LocalDateTime newCheckIn,
            LocalDateTime originalCheckOut,
            LocalDateTime newCheckOut,
            String originalStatus,
            String newStatus,
            String reason,
            String correctionType,
            LocalDateTime timestamp) {
        this.adminUsername = adminUsername;
        this.employee = employee;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.attendanceDate = attendanceDate;
        this.originalCheckIn = originalCheckIn;
        this.newCheckIn = newCheckIn;
        this.originalCheckOut = originalCheckOut;
        this.newCheckOut = newCheckOut;
        this.originalStatus = originalStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.correctionType = correctionType;
        this.timestamp = timestamp;
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public LocalDateTime getOriginalCheckIn() {
        return originalCheckIn;
    }

    public void setOriginalCheckIn(LocalDateTime originalCheckIn) {
        this.originalCheckIn = originalCheckIn;
    }

    public LocalDateTime getNewCheckIn() {
        return newCheckIn;
    }

    public void setNewCheckIn(LocalDateTime newCheckIn) {
        this.newCheckIn = newCheckIn;
    }

    public LocalDateTime getOriginalCheckOut() {
        return originalCheckOut;
    }

    public void setOriginalCheckOut(LocalDateTime originalCheckOut) {
        this.originalCheckOut = originalCheckOut;
    }

    public LocalDateTime getNewCheckOut() {
        return newCheckOut;
    }

    public void setNewCheckOut(LocalDateTime newCheckOut) {
        this.newCheckOut = newCheckOut;
    }

    public String getOriginalStatus() {
        return originalStatus;
    }

    public void setOriginalStatus(String originalStatus) {
        this.originalStatus = originalStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCorrectionType() {
        return correctionType;
    }

    public void setCorrectionType(String correctionType) {
        this.correctionType = correctionType;
    }

    public String getActionType() {
        return correctionType;
    }

    public void setActionType(String actionType) {
        this.correctionType = actionType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
