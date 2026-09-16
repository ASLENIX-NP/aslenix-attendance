package com.aslenix.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_attendance_audit")
public class AdminAttendanceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", length = 50)
    private String employeeCode;

    @Column(name = "employee_name", length = 150)
    private String employeeName;

    @Column(name = "admin_username", length = 100)
    private String adminUsername;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public AdminAttendanceAudit() {
    }

    public AdminAttendanceAudit(String employeeCode, String employeeName, String adminUsername, String actionType, String notes, LocalDateTime timestamp) {
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.adminUsername = adminUsername;
        this.actionType = actionType;
        this.notes = notes;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
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

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
