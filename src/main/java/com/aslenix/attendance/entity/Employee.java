package com.aslenix.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // BASIC EMPLOYEE INFORMATION
    // ============================================================

    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 150
    )
    private String email;

    @Column(
            name = "employee_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String employeeCode;

    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false,
            length = 100
    )
    private String lastName;

    @Column(
            name = "joining_date",
            nullable = false
    )
    private LocalDate joiningDate;

    @Column(
            name = "phone",
            length = 20
    )
    private String phone;

    @Column(
            name = "position",
            nullable = false,
            length = 100
    )
    private String position;

    // ============================================================
    // QR CODE
    // ============================================================

    @Column(
            name = "qr_token",
            unique = true,
            length = 100
    )
    private String qrToken;

    // ============================================================
    // DEPARTMENT
    // ============================================================

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    // ============================================================
    // USER ACCOUNT
    // ============================================================

    @OneToOne
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    // ============================================================
    // ACCOUNT STATUS
    // ============================================================

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled = true;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public Employee() {
    }

    // ============================================================
    // ID
    // ============================================================

    public Long getId() {
        return id;
    }

    // ============================================================
    // EMAIL
    // ============================================================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // ============================================================
    // EMPLOYEE CODE
    // ============================================================

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    // ============================================================
    // FIRST NAME
    // ============================================================

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    // ============================================================
    // LAST NAME
    // ============================================================

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    // ============================================================
    // JOINING DATE
    // ============================================================

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    // ============================================================
    // PHONE
    // ============================================================

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // ============================================================
    // POSITION
    // ============================================================

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    // ============================================================
    // QR TOKEN
    // ============================================================

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    // ============================================================
    // DEPARTMENT
    // ============================================================

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    // ============================================================
    // USER
    // ============================================================

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // ============================================================
    // ENABLED
    // ============================================================

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}