package com.aslenix.attendance.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    // ============================================================
    // ID
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // EMPLOYEE (OPTIONAL TO ALLOW ADMIN BROADCAST NOTIFICATIONS)
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "employee_id",
            nullable = true
    )
    private Employee employee;

    // ============================================================
    // TARGET ROLE (E.G. "ADMIN" FOR ADMIN NOTIFICATIONS)
    // ============================================================

    @Column(
            name = "target_role",
            length = 50
    )
    private String targetRole;

    // ============================================================
    // NOTIFICATION TITLE
    // ============================================================

    @Column(
            name = "title",
            nullable = false,
            length = 200
    )
    private String title;

    // ============================================================
    // NOTIFICATION MESSAGE
    // ============================================================

    @Column(
            name = "message",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String message;

    // ============================================================
    // NOTIFICATION TYPE
    // ============================================================

    @Column(
            name = "type",
            nullable = false,
            length = 50
    )
    private String type;

    // ============================================================
    // READ STATUS
    // ============================================================

    @Column(
            name = "is_read",
            nullable = false
    )
    private boolean read = false;

    // ============================================================
    // CREATED TIME
    // ============================================================

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public Notification() {
    }

    // ============================================================
    // GETTERS / SETTERS
    // ============================================================

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ============================================================
    // PRE-PERSIST
    // ============================================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}