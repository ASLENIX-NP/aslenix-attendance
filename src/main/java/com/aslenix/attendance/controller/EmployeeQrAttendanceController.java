package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import com.aslenix.attendance.util.GeoUtils;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/employee/attendance")
public class EmployeeQrAttendanceController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final OfficeSettingsRepository officeSettingsRepository;

    public EmployeeQrAttendanceController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            OfficeSettingsRepository officeSettingsRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.officeSettingsRepository = officeSettingsRepository;
    }

    // ============================================================
    // QR SCANNER PAGE
    // ============================================================

    @GetMapping("/scanner")
    public String scanner(
            Authentication authentication,
            Model model) {

        if (authentication == null) {
            return "redirect:/login";
        }

        Employee employee = getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        if (!employee.isEnabled()) {
            return "redirect:/login";
        }

        if (employee.getQrToken() == null
                || employee.getQrToken().isBlank()) {

            return "redirect:/employee/profile";
        }

        model.addAttribute("employee", employee);

        model.addAttribute(
                "employeeName",
                employee.getFirstName() + " " + employee.getLastName()
        );

        return "employee/attendance-scanner";
    }

    // ============================================================
    // QR ATTENDANCE
    // ============================================================

    @PostMapping("/qr")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> qrAttendance(
            Authentication authentication,
            @RequestParam String qrToken,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        // --------------------------------------------------------
        // Authentication
        // --------------------------------------------------------

        if (authentication == null) {

            return response(
                    false,
                    "Your session has expired. Please log in again.",
                    "LOGIN_REQUIRED"
            );
        }

        Employee loggedInEmployee =
                getLoggedInEmployee(authentication);

        if (loggedInEmployee == null) {

            return response(
                    false,
                    "Employee account not found.",
                    "EMPLOYEE_NOT_FOUND"
            );
        }

        if (!loggedInEmployee.isEnabled()) {

            return response(
                    false,
                    "Your employee account is disabled.",
                    "ACCOUNT_DISABLED"
            );
        }

        // --------------------------------------------------------
        // Validate QR
        // --------------------------------------------------------

        if (qrToken == null || qrToken.isBlank()) {

            return response(
                    false,
                    "Invalid QR code.",
                    "INVALID_QR"
            );
        }

        qrToken = qrToken.trim();

        // --------------------------------------------------------
        // Find employee from QR token
        // --------------------------------------------------------

        Employee qrEmployee =
                employeeRepository
                        .findByQrToken(qrToken)
                        .orElse(null);

        if (qrEmployee == null) {

            return response(
                    false,
                    "This QR code is not registered.",
                    "QR_NOT_REGISTERED"
            );
        }

        // --------------------------------------------------------
        // QR employee enabled
        // --------------------------------------------------------

        if (!qrEmployee.isEnabled()) {

            return response(
                    false,
                    "This employee account is disabled.",
                    "ACCOUNT_DISABLED"
            );
        }

        // --------------------------------------------------------
        // Prevent proxy attendance
        // --------------------------------------------------------

        if (!loggedInEmployee.getId()
                .equals(qrEmployee.getId())) {

            return response(
                    false,
                    "This QR code does not belong to your account.",
                    "QR_EMPLOYEE_MISMATCH"
            );
        }

        // --------------------------------------------------------
        // Office settings
        // --------------------------------------------------------

        OfficeSettings settings;

        try {

            settings = getOfficeSettings();

        } catch (Exception e) {

            return response(
                    false,
                    "Office attendance settings are not configured.",
                    "SETTINGS_ERROR"
            );
        }

        // --------------------------------------------------------
        // Today
        // --------------------------------------------------------

        LocalDate today = LocalDate.now();

        // --------------------------------------------------------
        // Working day
        // --------------------------------------------------------

        if (!isWorkingDay(today, settings)) {

            return response(
                    false,
                    "Today is not a working day.",
                    "NOT_WORKING_DAY"
            );
        }

        // --------------------------------------------------------
        // GPS
        // --------------------------------------------------------

        double distance;

        try {

            distance =
                    GeoUtils.distanceMeters(
                            latitude,
                            longitude,
                            settings.getLatitude(),
                            settings.getLongitude()
                    );

        } catch (Exception e) {

            return response(
                    false,
                    "Unable to verify your office location.",
                    "GPS_ERROR"
            );
        }

        // --------------------------------------------------------
        // Office radius
        // --------------------------------------------------------

        if (distance > settings.getAllowedRadiusMeters()) {

            return response(
                    false,
                    "You are outside the allowed office attendance area.",
                    "OUTSIDE_OFFICE"
            );
        }

        // --------------------------------------------------------
        // Today's attendance
        // --------------------------------------------------------

        Attendance attendance =
                attendanceRepository
                        .findByEmployeeAndAttendanceDate(
                                loggedInEmployee,
                                today
                        )
                        .orElse(null);

        // ========================================================
        // CHECK IN
        // ========================================================

        if (attendance == null) {

            LocalDateTime now = LocalDateTime.now();

            LocalTime lateTime =
                    settings.getWorkStartTime()
                            .plusMinutes(
                                    settings.getLateGraceMinutes()
                            );

            boolean late =
                    now.toLocalTime().isAfter(lateTime);

            attendance = new Attendance();

            attendance.setEmployee(loggedInEmployee);
            attendance.setAttendanceDate(today);
            attendance.setCheckIn(now);

            attendance.setCheckInLatitude(latitude);
            attendance.setCheckInLongitude(longitude);
            attendance.setCheckInDistanceMeters(distance);

            attendance.setLate(late);
            attendance.setEarlyLeave(false);
            attendance.setHalfDay(false);

            attendance.setStatus(
                    late ? "LATE" : "PRESENT"
            );

            attendance =
                    attendanceRepository.save(attendance);

            // ----------------------------------------------------
            // Return UPDATED attendance
            // ----------------------------------------------------

            Map<String, Object> response = new HashMap<>();

            response.put("success", true);
            response.put("action", "CHECK_IN");

            response.put(
                    "message",
                    late
                            ? "Check-in successful. You are marked late."
                            : "Check-in successful."
            );

            response.put(
                    "time",
                    formatTime(now)
            );

            response.put(
                    "date",
                    today.toString()
            );

            response.put(
                    "status",
                    attendance.getStatus()
            );

            response.put(
                    "employeeName",
                    loggedInEmployee.getFirstName()
                            + " "
                            + loggedInEmployee.getLastName()
            );

            response.put(
                    "employeeCode",
                    loggedInEmployee.getEmployeeCode()
            );

            response.put(
                    "distance",
                    Math.round(distance)
            );

            response.put(
                    "dashboardUrl",
                    "/employee/dashboard"
            );

            return ResponseEntity.ok(response);
        }

        // ========================================================
        // ALREADY CHECKED OUT
        // ========================================================

        if (attendance.getCheckOut() != null) {

            return response(
                    false,
                    "You have already checked out today.",
                    "ALREADY_CHECKED_OUT"
            );
        }

        // ========================================================
        // CHECK OUT
        // ========================================================

        LocalDateTime now = LocalDateTime.now();

        boolean earlyLeave =
                now.toLocalTime()
                        .isBefore(
                                settings.getWorkEndTime()
                        );

        attendance.setCheckOut(now);
        attendance.setEarlyLeave(earlyLeave);

        attendance =
                attendanceRepository.save(attendance);

        // --------------------------------------------------------
        // Return UPDATED attendance
        // --------------------------------------------------------

        Map<String, Object> response = new HashMap<>();

        response.put("success", true);
        response.put("action", "CHECK_OUT");

        response.put(
                "message",
                earlyLeave
                        ? "Check-out successful. You are marked as early leave."
                        : "Check-out successful."
        );

        response.put(
                "time",
                formatTime(now)
        );

        response.put(
                "date",
                today.toString()
        );

        response.put(
                "status",
                attendance.getStatus()
        );

        response.put(
                "employeeName",
                loggedInEmployee.getFirstName()
                        + " "
                        + loggedInEmployee.getLastName()
        );

        response.put(
                "employeeCode",
                loggedInEmployee.getEmployeeCode()
        );

        response.put(
                "distance",
                Math.round(distance)
        );

        response.put(
                "dashboardUrl",
                "/employee/dashboard"
        );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // RESPONSE HELPER
    // ============================================================

    private ResponseEntity<Map<String, Object>> response(
            boolean success,
            String message,
            String errorCode) {

        Map<String, Object> response =
                new HashMap<>();

        response.put("success", success);
        response.put("message", message);
        response.put("errorCode", errorCode);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // FORMAT TIME
    // ============================================================

    private String formatTime(LocalDateTime dateTime) {

        return dateTime
                .toLocalTime()
                .withSecond(0)
                .withNano(0)
                .toString();
    }

    // ============================================================
    // OFFICE SETTINGS
    // ============================================================

    private OfficeSettings getOfficeSettings() {

        return officeSettingsRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Office settings not configured."
                        )
                );
    }

    // ============================================================
    // WORKING DAY
    // ============================================================

    private boolean isWorkingDay(
            LocalDate date,
            OfficeSettings settings) {

        DayOfWeek day = date.getDayOfWeek();

        return switch (day) {

            case SUNDAY -> settings.isSunday();
            case MONDAY -> settings.isMonday();
            case TUESDAY -> settings.isTuesday();
            case WEDNESDAY -> settings.isWednesday();
            case THURSDAY -> settings.isThursday();
            case FRIDAY -> settings.isFriday();
            case SATURDAY -> settings.isSaturday();
        };
    }

    // ============================================================
    // LOGGED-IN EMPLOYEE
    // ============================================================

    private Employee getLoggedInEmployee(
            Authentication authentication) {

        if (authentication == null) {
            return null;
        }

        return employeeRepository
                .findByUserUsername(authentication.getName())
                .orElse(null);
    }
}