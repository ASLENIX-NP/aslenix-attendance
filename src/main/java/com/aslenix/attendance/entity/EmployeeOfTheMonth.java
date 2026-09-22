package com.aslenix.attendance.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * The permanently stored winner for a given evaluation month.
 * One row per month (unique on eval_year + eval_month).
 */
@Entity
@Table(
        name = "employee_of_the_month",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"eval_year", "eval_month"})
        }
)
public class EmployeeOfTheMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @Column(name = "department_name", length = 150)
    private String departmentName;

    @Column(name = "eval_year", nullable = false)
    private int evalYear;

    @Column(name = "eval_month", nullable = false)
    private int evalMonth;

    @Column(name = "final_score", nullable = false)
    private double finalScore;

    @Column(name = "attendance_score", nullable = false)
    private double attendanceScore;

    @Column(name = "task_score", nullable = false)
    private double taskScore;

    @Column(name = "tie_breaker_used", length = 100)
    private String tieBreakerUsed;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    public EmployeeOfTheMonth() {
        this.calculatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getEvalYear() {
        return evalYear;
    }

    public void setEvalYear(int evalYear) {
        this.evalYear = evalYear;
    }

    public int getEvalMonth() {
        return evalMonth;
    }

    public void setEvalMonth(int evalMonth) {
        this.evalMonth = evalMonth;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public double getAttendanceScore() {
        return attendanceScore;
    }

    public void setAttendanceScore(double attendanceScore) {
        this.attendanceScore = attendanceScore;
    }

    public double getTaskScore() {
        return taskScore;
    }

    public void setTaskScore(double taskScore) {
        this.taskScore = taskScore;
    }

    public String getTieBreakerUsed() {
        return tieBreakerUsed;
    }

    public void setTieBreakerUsed(String tieBreakerUsed) {
        this.tieBreakerUsed = tieBreakerUsed;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
