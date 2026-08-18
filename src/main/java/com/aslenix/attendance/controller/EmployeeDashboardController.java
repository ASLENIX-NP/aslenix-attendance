package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.AttendanceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeDashboardController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceService attendanceService;

    public EmployeeDashboardController(
            EmployeeRepository employeeRepository,
            AttendanceService attendanceService) {

        this.employeeRepository = employeeRepository;
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
        // Get logged-in username
        // --------------------------------------------------------

        String username =
                authentication.getName();

        // --------------------------------------------------------
        // Find employee
        // --------------------------------------------------------

        Employee employee =
                employeeRepository
                        .findByUserUsername(username)
                        .orElse(null);

        if (employee == null) {

            return "redirect:/login?employeeNotFound=true";
        }

        // --------------------------------------------------------
        // Attendance records
        // --------------------------------------------------------

        List<Attendance> attendanceRecords =
                attendanceService
                        .getEmployeeAttendance(employee);

        // --------------------------------------------------------
        // Current month statistics
        // --------------------------------------------------------

        long attendanceThisMonth =
                attendanceService
                        .getAttendanceThisMonth(employee);

        long presentCount =
                attendanceService
                        .getPresentCount(employee);

        long absentCount =
                attendanceService
                        .getAbsentCount(employee);

        long lateCount =
                attendanceService
                        .getLateCount(employee);

        long leaveCount =
                attendanceService
                        .getLeaveCount(employee);

        // --------------------------------------------------------
        // Today's attendance
        // --------------------------------------------------------

        String todayStatus =
                attendanceService
                        .getTodayStatus(employee);

        String checkInTime =
                attendanceService
                        .getTodayCheckInTime(employee);

        String checkOutTime =
                attendanceService
                        .getTodayCheckOutTime(employee);

        String workingHours =
                attendanceService
                        .getTodayWorkingHours(employee);

        // --------------------------------------------------------
        // Employee information
        // --------------------------------------------------------

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "employeeName",
                employee.getFirstName()
                        + " "
                        + employee.getLastName()
        );

        model.addAttribute(
                "employeeCode",
                employee.getEmployeeCode()
        );

        model.addAttribute(
                "department",
                employee.getDepartment()
        );

        // --------------------------------------------------------
        // Dashboard statistics
        // --------------------------------------------------------

        model.addAttribute(
                "attendanceThisMonth",
                attendanceThisMonth
        );

        model.addAttribute(
                "presentCount",
                presentCount
        );

        model.addAttribute(
                "absentCount",
                absentCount
        );

        model.addAttribute(
                "lateCount",
                lateCount
        );

        model.addAttribute(
                "leaveCount",
                leaveCount
        );

        // --------------------------------------------------------
        // Today's information
        // --------------------------------------------------------

        model.addAttribute(
                "todayStatus",
                todayStatus
        );

        model.addAttribute(
                "checkInTime",
                checkInTime
        );

        model.addAttribute(
                "checkOutTime",
                checkOutTime
        );

        model.addAttribute(
                "workingHours",
                workingHours
        );

        // --------------------------------------------------------
        // Attendance history
        // --------------------------------------------------------

        model.addAttribute(
                "attendanceRecords",
                attendanceRecords
        );

        // --------------------------------------------------------
        // Today's date
        // --------------------------------------------------------

        model.addAttribute(
                "todayDate",
                java.time.LocalDate.now()
        );

        return "employee/dashboard";
    }
}