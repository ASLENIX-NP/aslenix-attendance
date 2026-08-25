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

import java.time.LocalDate;
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

        String username =
                authentication.getName();

        Employee employee =
                employeeRepository
                        .findByUserUsername(username)
                        .orElse(null);

        if (employee == null) {

            return "redirect:/login?employeeNotFound=true";
        }

        // ========================================================
        // EMPLOYEE INFORMATION
        // ========================================================

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

        // ========================================================
        // ATTENDANCE
        // ========================================================

        List<Attendance> attendanceRecords =
                attendanceService
                        .getEmployeeAttendance(employee);

        model.addAttribute(
                "attendanceRecords",
                attendanceRecords
        );

        // ========================================================
        // MONTHLY STATISTICS
        // ========================================================

        model.addAttribute(
                "attendanceThisMonth",
                attendanceService
                        .getAttendanceThisMonth(employee)
        );

        model.addAttribute(
                "presentCount",
                attendanceService
                        .getPresentCount(employee)
        );

        model.addAttribute(
                "absentCount",
                attendanceService
                        .getAbsentCount(employee)
        );

        model.addAttribute(
                "lateCount",
                attendanceService
                        .getLateCount(employee)
        );

        model.addAttribute(
                "leaveCount",
                attendanceService
                        .getLeaveCount(employee)
        );

        // ========================================================
        // TODAY
        // ========================================================

        model.addAttribute(
                "todayStatus",
                attendanceService
                        .getTodayStatus(employee)
        );

        model.addAttribute(
                "checkInTime",
                attendanceService
                        .getTodayCheckInTime(employee)
        );

        model.addAttribute(
                "checkOutTime",
                attendanceService
                        .getTodayCheckOutTime(employee)
        );

        model.addAttribute(
                "workingHours",
                attendanceService
                        .getTodayWorkingHours(employee)
        );

        // ========================================================
        // DATE
        // ========================================================

        model.addAttribute(
                "todayDate",
                LocalDate.now()
        );

        return "employee/dashboard";
    }
}