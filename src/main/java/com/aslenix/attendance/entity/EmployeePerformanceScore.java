package com.aslenix.attendance.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A permanently stored monthly performance result for one employee.
 * Rows are snapshotted at evaluation time so later settings changes
 * never alter historical results.
 */
@Entity
@Table(
        name = "employee_performance_scores",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"employee_id", "eval_year", "eval_month"})
        }
)
public class EmployeePerformanceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // Snapshots so history stays readable even if the employee/department changes later.
    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @Column(name = "department_name", length = 150)
    private String departmentName;

    @Column(name = "eval_year", nullable = false)
    private int evalYear;

    @Column(name = "eval_month", nullable = false)
    private int evalMonth;

    // ============================================================
    // SCORES
    // ============================================================

    @Column(name = "attendance_score", nullable = false)
    private double attendanceScore;

    @Column(name = "task_score", nullable = false)
    private double taskScore;

    @Column(name = "final_score", nullable = false)
    private double finalScore;

    @Column(name = "raw_attendance_points", nullable = false)
    private double rawAttendancePoints;

    @Column(name = "raw_task_points", nullable = false)
    private double rawTaskPoints;

    // ============================================================
    // ATTENDANCE METRICS
    // ============================================================

    @Column(name = "late_check_ins", nullable = false)
    private int lateCheckIns;

    @Column(name = "early_check_ins", nullable = false)
    private int earlyCheckIns;

    @Column(name = "overtime_hours", nullable = false)
    private double overtimeHours;

    @Column(name = "days_present", nullable = false)
    private int daysPresent;

    // ============================================================
    // TASK METRICS
    // ============================================================

    @Column(name = "tasks_completed", nullable = false)
    private int tasksCompleted;

    @Column(name = "small_completed", nullable = false)
    private int smallCompleted;

    @Column(name = "medium_completed", nullable = false)
    private int mediumCompleted;

    @Column(name = "large_completed", nullable = false)
    private int largeCompleted;

    @Column(name = "epic_completed", nullable = false)
    private int epicCompleted;

    @Column(name = "tasks_early", nullable = false)
    private int tasksEarly;

    @Column(name = "tasks_on_time", nullable = false)
    private int tasksOnTime;

    @Column(name = "tasks_late", nullable = false)
    private int tasksLate;

    @Column(name = "tasks_no_deadline", nullable = false)
    private int tasksNoDeadline;

    // ============================================================
    // EVALUATION CONTEXT
    // ============================================================

    @Column(name = "attendance_weight_used", nullable = false)
    private int attendanceWeightUsed;

    @Column(name = "task_weight_used", nullable = false)
    private int taskWeightUsed;

    @Column(name = "score_rank", nullable = false)
    private int rank;

    @Column(name = "employee_of_the_month", nullable = false)
    private boolean employeeOfTheMonth;

    @Column(name = "tie_breaker_used", length = 100)
    private String tieBreakerUsed;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @OneToMany(
            mappedBy = "score",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("id ASC")
    private List<PerformanceTaskBreakdown> taskBreakdowns = new ArrayList<>();

    public EmployeePerformanceScore() {
        this.calculatedAt = LocalDateTime.now();
    }

    public void addTaskBreakdown(PerformanceTaskBreakdown breakdown) {
        if (breakdown != null) {
            breakdown.setScore(this);
            this.taskBreakdowns.add(breakdown);
        }
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

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public double getRawAttendancePoints() {
        return rawAttendancePoints;
    }

    public void setRawAttendancePoints(double rawAttendancePoints) {
        this.rawAttendancePoints = rawAttendancePoints;
    }

    public double getRawTaskPoints() {
        return rawTaskPoints;
    }

    public void setRawTaskPoints(double rawTaskPoints) {
        this.rawTaskPoints = rawTaskPoints;
    }

    public int getLateCheckIns() {
        return lateCheckIns;
    }

    public void setLateCheckIns(int lateCheckIns) {
        this.lateCheckIns = lateCheckIns;
    }

    public int getEarlyCheckIns() {
        return earlyCheckIns;
    }

    public void setEarlyCheckIns(int earlyCheckIns) {
        this.earlyCheckIns = earlyCheckIns;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public int getDaysPresent() {
        return daysPresent;
    }

    public void setDaysPresent(int daysPresent) {
        this.daysPresent = daysPresent;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(int tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public int getSmallCompleted() {
        return smallCompleted;
    }

    public void setSmallCompleted(int smallCompleted) {
        this.smallCompleted = smallCompleted;
    }

    public int getMediumCompleted() {
        return mediumCompleted;
    }

    public void setMediumCompleted(int mediumCompleted) {
        this.mediumCompleted = mediumCompleted;
    }

    public int getLargeCompleted() {
        return largeCompleted;
    }

    public void setLargeCompleted(int largeCompleted) {
        this.largeCompleted = largeCompleted;
    }

    public int getEpicCompleted() {
        return epicCompleted;
    }

    public void setEpicCompleted(int epicCompleted) {
        this.epicCompleted = epicCompleted;
    }

    public int getTasksEarly() {
        return tasksEarly;
    }

    public void setTasksEarly(int tasksEarly) {
        this.tasksEarly = tasksEarly;
    }

    public int getTasksOnTime() {
        return tasksOnTime;
    }

    public void setTasksOnTime(int tasksOnTime) {
        this.tasksOnTime = tasksOnTime;
    }

    public int getTasksLate() {
        return tasksLate;
    }

    public void setTasksLate(int tasksLate) {
        this.tasksLate = tasksLate;
    }

    public int getTasksNoDeadline() {
        return tasksNoDeadline;
    }

    public void setTasksNoDeadline(int tasksNoDeadline) {
        this.tasksNoDeadline = tasksNoDeadline;
    }

    public int getAttendanceWeightUsed() {
        return attendanceWeightUsed;
    }

    public void setAttendanceWeightUsed(int attendanceWeightUsed) {
        this.attendanceWeightUsed = attendanceWeightUsed;
    }

    public int getTaskWeightUsed() {
        return taskWeightUsed;
    }

    public void setTaskWeightUsed(int taskWeightUsed) {
        this.taskWeightUsed = taskWeightUsed;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public boolean isEmployeeOfTheMonth() {
        return employeeOfTheMonth;
    }

    public void setEmployeeOfTheMonth(boolean employeeOfTheMonth) {
        this.employeeOfTheMonth = employeeOfTheMonth;
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

    public List<PerformanceTaskBreakdown> getTaskBreakdowns() {
        return taskBreakdowns;
    }

    public void setTaskBreakdowns(List<PerformanceTaskBreakdown> taskBreakdowns) {
        this.taskBreakdowns = taskBreakdowns;
    }
}
