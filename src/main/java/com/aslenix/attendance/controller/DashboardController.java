
package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.LeaveRequestRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    private final OfficeSettingsRepository officeSettingsRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public DashboardController(
            OfficeSettingsRepository officeSettingsRepository,
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            LeaveRequestRepository leaveRequestRepository) {

        this.officeSettingsRepository = officeSettingsRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        // ========================================================
        // TODAY
        // ========================================================

        LocalDate today = LocalDate.now();

        // ========================================================
        // OFFICE SETTINGS
        // ========================================================

        OfficeSettings settings =
                officeSettingsRepository
                        .findFirstByOrderByIdAsc()
                        .orElse(null);

        model.addAttribute("settings", settings);

        // ========================================================
        // EMPLOYEES
        // ========================================================

        long totalEmployees = employeeRepository.count();

        model.addAttribute(
                "totalEmployees",
                totalEmployees
        );

        // ========================================================
        // TODAY'S ATTENDANCE
        // ========================================================

        List<Attendance> attendanceRecords =
                attendanceRepository.findByAttendanceDate(today);

        model.addAttribute(
                "attendanceRecords",
                attendanceRecords
        );

        // Number of employees who have an attendance record today
        long presentToday =
                attendanceRepository.countByAttendanceDate(today);

        model.addAttribute(
                "presentToday",
                presentToday
        );

        // Number of employees who were late today
        long lateToday =
                attendanceRepository.countByAttendanceDateAndLateTrue(today);

        model.addAttribute(
                "lateToday",
                lateToday
        );

        // ========================================================
        // TODAY'S LEAVE
        // ========================================================

        long employeesOnLeaveToday =
                leaveRequestRepository
                        .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                "APPROVED",
                                today,
                                today
                        );

        model.addAttribute(
                "employeesOnLeaveToday",
                employeesOnLeaveToday
        );

        // ========================================================
        // ABSENT TODAY
        // ========================================================
        //
        // An employee is considered absent when:
        //
        // Total Employees
        // - Present Employees
        // - Employees On Approved Leave
        //
        // Never allow the number to become negative.
        // ========================================================

        long absentToday =
                totalEmployees
                        - presentToday
                        - employeesOnLeaveToday;

        if (absentToday < 0) {
            absentToday = 0;
        }

        model.addAttribute(
                "absentToday",
                absentToday
        );

        // ========================================================
        // LEAVE REQUEST COUNTS
        // ========================================================

        long pendingLeaveCount =
                leaveRequestRepository.countByStatus("PENDING");

        long approvedLeaveCount =
                leaveRequestRepository.countByStatus("APPROVED");

        long rejectedLeaveCount =
                leaveRequestRepository.countByStatus("REJECTED");

        model.addAttribute(
                "pendingLeaveCount",
                pendingLeaveCount
        );

        model.addAttribute(
                "approvedLeaveCount",
                approvedLeaveCount
        );

        model.addAttribute(
                "rejectedLeaveCount",
                rejectedLeaveCount
        );

        // ========================================================
        // RETURN DASHBOARD
        // ========================================================

        return "admin/dashboard";
    }
}

