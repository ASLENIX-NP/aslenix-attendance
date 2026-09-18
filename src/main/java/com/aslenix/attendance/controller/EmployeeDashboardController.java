package com.aslenix.attendance.controller;

import com.aslenix.attendance.dto.StreakDto;
import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.AttendanceService;
import com.aslenix.attendance.service.LeaveRequestService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeDashboardController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestService leaveRequestService;
    private final AttendanceService attendanceService;

    public EmployeeDashboardController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestService leaveRequestService,
            AttendanceService attendanceService) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestService = leaveRequestService;
        this.attendanceService = attendanceService;
    }

    // ============================================================
    // EMPLOYEE DASHBOARD
    // ============================================================

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        // --------------------------------------------------------
        // Make sure user is logged in
        // --------------------------------------------------------

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return "redirect:/login";
        }

        // --------------------------------------------------------
        // Get logged-in username
        // --------------------------------------------------------

        String username =
                authentication.getName();

        // --------------------------------------------------------
        // Find employee connected to this user account
        // --------------------------------------------------------

        Employee employee =
                employeeRepository
                        .findByUserUsername(username)
                        .orElse(null);

        // --------------------------------------------------------
        // Safety check
        // --------------------------------------------------------

        if (employee == null) {

            return "redirect:/login?error";
        }

        // --------------------------------------------------------
        // Today's date
        // --------------------------------------------------------

        LocalDate today =
                LocalDate.now();

        model.addAttribute(
                "today",
                today
        );

        // --------------------------------------------------------
        // Employee
        // --------------------------------------------------------

        model.addAttribute(
                "employee",
                employee
        );

        // --------------------------------------------------------
        // Active sidebar item
        // --------------------------------------------------------

        model.addAttribute(
                "activePage",
                "dashboard"
        );

        // ========================================================
        // TODAY'S ATTENDANCE
        // ========================================================

        Attendance attendance =
                attendanceRepository
                        .findByEmployeeAndAttendanceDate(
                                employee,
                                today
                        )
                        .orElse(null);

        model.addAttribute(
                "attendance",
                attendance
        );

        // ========================================================
        // TODAY'S WORKING HOURS
        // ========================================================

        String workingHours =
                calculateWorkingHours(attendance);

        model.addAttribute(
                "workingHours",
                workingHours
        );

        // ========================================================
        // CURRENT MONTH
        // ========================================================

        YearMonth currentMonth =
                YearMonth.now();

        LocalDate monthStart =
                currentMonth.atDay(1);

        LocalDate monthEnd =
                currentMonth.atEndOfMonth();

        List<Attendance> monthlyAttendance =
                attendanceRepository
                        .findByEmployeeAndAttendanceDateBetween(
                                employee,
                                monthStart,
                                monthEnd
                        );

        // ========================================================
        // PRESENT
        // ========================================================

        long presentCount =
                monthlyAttendance
                        .stream()
                        .filter(a ->
                                a.getCheckIn() != null)
                        .count();

        model.addAttribute(
                "presentCount",
                presentCount
        );

        // ========================================================
        // LATE
        // ========================================================

        long lateCount =
                monthlyAttendance
                        .stream()
                        .filter(Attendance::isLate)
                        .count();

        model.addAttribute(
                "lateCount",
                lateCount
        );

        // ========================================================
        // LEAVE
        // ========================================================

        long leaveCount =
                leaveRequestService
                        .countApprovedLeaves(employee);

        model.addAttribute(
                "leaveCount",
                leaveCount
        );

        // ========================================================
        // ABSENT
        // ========================================================

        long workingDays = 0;

        LocalDate date =
                monthStart;

        while (!date.isAfter(monthEnd)) {

            // Monday-Friday
            if (date.getDayOfWeek().getValue() <= 5) {
                workingDays++;
            }

            date = date.plusDays(1);
        }

        long absentCount =
                Math.max(
                        0,
                        workingDays
                                - presentCount
                                - leaveCount
                );

        model.addAttribute(
                "absentCount",
                absentCount
        );

        // ========================================================
        // ATTENDANCE STREAK
        // ========================================================

        StreakDto streak = attendanceService.calculateStreak(employee);

        model.addAttribute(
                "currentStreak",
                streak.getCurrentStreak()
        );

        model.addAttribute(
                "longestStreak",
                streak.getLongestStreak()
        );

        model.addAttribute(
                "streakMessage",
                streak.getMessage()
        );

        // ========================================================
        // RETURN DASHBOARD
        // ========================================================

        return "employee/dashboard";
    }

    // ============================================================
    // CALCULATE WORKING HOURS
    // ============================================================

    private String calculateWorkingHours(
            Attendance attendance) {

        if (attendance == null ||
                attendance.getCheckIn() == null) {

            return "0h 00m";
        }

        LocalDateTime checkIn =
                attendance.getCheckIn();

        LocalDateTime checkOut =
                attendance.getCheckOut();

        // --------------------------------------------------------
        // Still working
        // --------------------------------------------------------

        if (checkOut == null) {

            checkOut =
                    LocalDateTime.now();
        }

        // --------------------------------------------------------
        // Calculate duration
        // --------------------------------------------------------

        Duration duration =
                Duration.between(
                        checkIn,
                        checkOut
                );

        if (duration.isNegative()) {
            return "0h 00m";
        }

        long totalMinutes =
                duration.toMinutes();

        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;

        return String.format(
                "%dh %02dm",
                hours,
                minutes
        );
    }
}