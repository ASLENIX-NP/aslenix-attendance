package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.AttendanceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee/attendance")
public class EmployeeAttendanceController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;

    public EmployeeAttendanceController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            AttendanceService attendanceService) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
    }

    // ============================================================
    // ATTENDANCE PAGE
    // ============================================================

    @GetMapping
    public String attendance(
            Authentication authentication,
            Model model) {

        Employee employee =
                getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        LocalDate today = LocalDate.now();

        Attendance todayAttendance =
                attendanceRepository
                        .findByEmployeeAndAttendanceDate(
                                employee,
                                today
                        )
                        .orElse(null);

        List<Attendance> attendanceRecords =
                attendanceRepository
                        .findByEmployeeOrderByAttendanceDateDesc(
                                employee
                        );

        // --------------------------------------------------------
        // TODAY'S ATTENDANCE
        // --------------------------------------------------------

        String todayCheckIn =
                attendanceService.getTodayCheckInTime(employee);

        String todayCheckOut =
                attendanceService.getTodayCheckOutTime(employee);

        String todayStatus =
                attendanceService.getTodayStatus(employee);

        String todayWorkingHours =
                attendanceService.getTodayWorkingHours(employee);

        // --------------------------------------------------------
        // BUTTON STATES
        // --------------------------------------------------------

        boolean canCheckIn =
                todayAttendance == null
                        || todayAttendance.getCheckIn() == null;

        boolean canCheckOut =
                todayAttendance != null
                        && todayAttendance.getCheckIn() != null
                        && todayAttendance.getCheckOut() == null;

        boolean checkedIn =
                todayAttendance != null
                        && todayAttendance.getCheckIn() != null;

        boolean checkedOut =
                todayAttendance != null
                        && todayAttendance.getCheckOut() != null;

        // --------------------------------------------------------
        // TODAY'S TIMELINE DATA
        // --------------------------------------------------------

        String checkInDistance = "--";

        if (todayAttendance != null
                && todayAttendance.getCheckInDistanceMeters() != null) {

            checkInDistance =
                    String.format(
                            "%.1f m",
                            todayAttendance.getCheckInDistanceMeters()
                    );
        }

        boolean late =
                todayAttendance != null
                        && todayAttendance.isLate();

        boolean earlyLeave =
                todayAttendance != null
                        && todayAttendance.isEarlyLeave();

        // --------------------------------------------------------
        // SUMMARY
        // --------------------------------------------------------

        int totalDays =
                attendanceRecords.size();

        int presentDays = 0;

        int absentDays = 0;

        long totalWorkingMinutes = 0;

        for (Attendance attendance : attendanceRecords) {

            if ("PRESENT".equals(attendance.getStatus())
                    || "LATE".equals(attendance.getStatus())) {

                presentDays++;
            }

            if (attendance.getCheckIn() == null) {
                absentDays++;
            }

            if (attendance.getCheckIn() != null
                    && attendance.getCheckOut() != null) {

                Duration duration =
                        Duration.between(
                                attendance.getCheckIn(),
                                attendance.getCheckOut()
                        );

                totalWorkingMinutes +=
                        duration.toMinutes();
            }
        }

        String workingHours =
                formatWorkingHours(totalWorkingMinutes);

        // --------------------------------------------------------
        // OFFICE SETTINGS
        // --------------------------------------------------------

        OfficeSettings settings =
                attendanceService.getOfficeSettings();

        String officeStartTime = "--:--";
        String officeEndTime = "--:--";

        if (settings.getWorkStartTime() != null) {
            officeStartTime =
                    settings.getWorkStartTime().toString();
        }

        if (settings.getWorkEndTime() != null) {
            officeEndTime =
                    settings.getWorkEndTime().toString();
        }

        // --------------------------------------------------------
        // MODEL
        // --------------------------------------------------------

        model.addAttribute(
                "attendance",
                todayAttendance
        );

        model.addAttribute(
                "attendanceRecords",
                attendanceRecords
        );

        model.addAttribute(
                "totalDays",
                totalDays
        );

        model.addAttribute(
                "presentDays",
                presentDays
        );

        model.addAttribute(
                "absentDays",
                absentDays
        );

        model.addAttribute(
                "workingHours",
                workingHours
        );

        model.addAttribute(
                "todayDate",
                today
        );

        model.addAttribute(
                "todayCheckIn",
                todayCheckIn
        );

        model.addAttribute(
                "todayCheckOut",
                todayCheckOut
        );

        model.addAttribute(
                "todayWorkingHours",
                todayWorkingHours
        );

        model.addAttribute(
                "todayStatus",
                todayStatus
        );

        model.addAttribute(
                "canCheckIn",
                canCheckIn
        );

        model.addAttribute(
                "canCheckOut",
                canCheckOut
        );

        model.addAttribute(
                "checkedIn",
                checkedIn
        );

        model.addAttribute(
                "checkedOut",
                checkedOut
        );

        model.addAttribute(
                "late",
                late
        );

        model.addAttribute(
                "earlyLeave",
                earlyLeave
        );

        model.addAttribute(
                "checkInDistance",
                checkInDistance
        );

        model.addAttribute(
                "officeStartTime",
                officeStartTime
        );

        model.addAttribute(
                "officeEndTime",
                officeEndTime
        );

        return "employee/attendance";
    }

    // ============================================================
    // CHECK IN
    // ============================================================

    @PostMapping("/check-in")
    public String checkIn(
            Authentication authentication,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        Employee employee =
                getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        try {

            attendanceService.checkIn(
                    employee,
                    latitude,
                    longitude
            );

            return "redirect:/employee/attendance?checkedIn";

        } catch (IllegalArgumentException e) {

            return "redirect:/employee/attendance?error="
                    + encodeMessage(e.getMessage());

        } catch (IllegalStateException e) {

            return "redirect:/employee/attendance?error="
                    + encodeMessage(e.getMessage());
        }
    }

    // ============================================================
    // CHECK OUT
    // ============================================================

    @PostMapping("/check-out")
    public String checkOut(
            Authentication authentication,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        Employee employee =
                getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        try {

            attendanceService.checkOut(
                    employee,
                    latitude,
                    longitude
            );

            return "redirect:/employee/attendance?checkedOut";

        } catch (IllegalArgumentException e) {

            return "redirect:/employee/attendance?error="
                    + encodeMessage(e.getMessage());

        } catch (IllegalStateException e) {

            return "redirect:/employee/attendance?error="
                    + encodeMessage(e.getMessage());
        }
    }

    // ============================================================
    // GET LOGGED-IN EMPLOYEE
    // ============================================================

    private Employee getLoggedInEmployee(
            Authentication authentication) {

        if (authentication == null) {
            return null;
        }

        String username =
                authentication.getName();

        return employeeRepository
                .findByUserUsername(username)
                .orElse(null);
    }

    // ============================================================
    // FORMAT WORKING HOURS
    // ============================================================

    private String formatWorkingHours(
            long totalMinutes) {

        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;

        return hours
                + "h "
                + minutes
                + "m";
    }

    // ============================================================
    // ERROR MESSAGE
    // ============================================================

    private String encodeMessage(
            String message) {

        if (message == null) {
            return "Attendance operation failed.";
        }

        return message
                .replace(" ", "%20")
                .replace(":", "%3A")
                .replace(",", "%2C");
    }
}