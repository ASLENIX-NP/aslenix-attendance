package com.aslenix.attendance.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "weekly_working_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"week_start", "work_date"}
                )
        }
)
public class WeeklyWorkingSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private boolean workingDay;

    public WeeklyWorkingSchedule() {
    }

    public WeeklyWorkingSchedule(
            LocalDate weekStart,
            LocalDate workDate,
            boolean workingDay
    ) {
        this.weekStart = weekStart;
        this.workDate = workDate;
        this.workingDay = workingDay;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public boolean isWorkingDay() {
        return workingDay;
    }

    public void setWorkingDay(boolean workingDay) {
        this.workingDay = workingDay;
    }
}