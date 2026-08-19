package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import com.aslenix.attendance.util.GeoUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;

@Controller
@RequestMapping("/employee/attendance")
public class EmployeeAttendanceController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final OfficeSettingsRepository officeSettingsRepository;

    public EmployeeAttendanceController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            OfficeSettingsRepository officeSettingsRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.officeSettingsRepository = officeSettingsRepository;
    }

    // ============================================================
    // ATTENDANCE PAGE
    // ============================================================

    @GetMapping
    public String attendance(
            Authentication authentication,
            Model model) {

        Employee employee = getLoggedInEmployee(authentication);

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
        // TODAY
        // --------------------------------------------------------

        String todayCheckIn = "--:--";
        String todayCheckOut = "--:--";
        String todayStatus = null;

        if (todayAttendance != null) {

            if (todayAttendance.getCheckIn() != null) {

                todayCheckIn =
                        formatTime(todayAttendance.getCheckIn());

            }

            if (todayAttendance.getCheckOut() != null) {

                todayCheckOut =
                        formatTime(todayAttendance.getCheckOut());

            }

            if (todayAttendance.getCheckOut() != null) {

                todayStatus = "PRESENT";

            } else {

                todayStatus = "WORKING";
            }
        }

        // --------------------------------------------------------
        // SUMMARY
        // --------------------------------------------------------

        int totalDays = attendanceRecords.size();

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
                "todayStatus",
                todayStatus
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

        OfficeSettings settings =
                getOfficeSettings();

        LocalDate today = LocalDate.now();

        // --------------------------------------------------------
        // WORKING DAY
        // --------------------------------------------------------

        if (!isWorkingDay(today, settings)) {

            return "redirect:/employee/attendance?weekend";
        }

        // --------------------------------------------------------
        // ALREADY CHECKED IN
        // --------------------------------------------------------

        if (attendanceRepository
                .findByEmployeeAndAttendanceDate(
                        employee,
                        today
                )
                .isPresent()) {

            return "redirect:/employee/attendance?alreadyCheckedIn";
        }

        // --------------------------------------------------------
        // GPS DISTANCE
        // --------------------------------------------------------

        double distance =
                GeoUtils.distanceMeters(
                        latitude,
                        longitude,
                        settings.getLatitude(),
                        settings.getLongitude()
                );

        if (distance > settings.getAllowedRadiusMeters()) {

            return "redirect:/employee/attendance?outsideOffice";
        }

        // --------------------------------------------------------
        // CREATE ATTENDANCE
        // --------------------------------------------------------

        LocalDateTime now =
                LocalDateTime.now();

        LocalTime lateTime =
                settings.getWorkStartTime()
                        .plusMinutes(
                                settings.getLateGraceMinutes()
                        );

        boolean late =
                now.toLocalTime()
                        .isAfter(lateTime);

        Attendance attendance =
                new Attendance();

        attendance.setEmployee(employee);

        attendance.setAttendanceDate(today);

        attendance.setCheckIn(now);

        attendance.setCheckInLatitude(latitude);

        attendance.setCheckInLongitude(longitude);

        attendance.setCheckInDistanceMeters(distance);

        attendance.setLate(late);

        attendance.setEarlyLeave(false);

        attendance.setHalfDay(false);

        attendance.setStatus(
                late
                        ? "LATE"
                        : "PRESENT"
        );

        attendanceRepository.save(attendance);

        return "redirect:/employee/attendance?checkedIn";
    }

    // ============================================================
    // CHECK OUT
    // ============================================================

    @PostMapping("/check-out")
    public String checkOut(
            Authentication authentication) {

        Employee employee =
                getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        LocalDate today =
                LocalDate.now();

        Attendance attendance =
                attendanceRepository
                        .findByEmployeeAndAttendanceDate(
                                employee,
                                today
                        )
                        .orElse(null);

        if (attendance == null) {

            return "redirect:/employee/attendance?notCheckedIn";
        }

        if (attendance.getCheckOut() != null) {

            return "redirect:/employee/attendance?alreadyCheckedOut";
        }

        OfficeSettings settings =
                getOfficeSettings();

        LocalDateTime now =
                LocalDateTime.now();

        boolean earlyLeave =
                now.toLocalTime()
                        .isBefore(
                                settings.getWorkEndTime()
                        );

        attendance.setCheckOut(now);

        attendance.setEarlyLeave(earlyLeave);

        attendanceRepository.save(attendance);

        return "redirect:/employee/attendance?checkedOut";
    }

    // ============================================================
    // OFFICE SETTINGS
    // ============================================================

    private OfficeSettings getOfficeSettings() {

        return officeSettingsRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() ->
                        new IllegalStateException(
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

        DayOfWeek day =
                date.getDayOfWeek();

        return switch (day) {

            case SUNDAY ->
                    settings.isSunday();

            case MONDAY ->
                    settings.isMonday();

            case TUESDAY ->
                    settings.isTuesday();

            case WEDNESDAY ->
                    settings.isWednesday();

            case THURSDAY ->
                    settings.isThursday();

            case FRIDAY ->
                    settings.isFriday();

            case SATURDAY ->
                    settings.isSaturday();
        };
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
    // FORMAT TIME
    // ============================================================

    private String formatTime(
            LocalDateTime dateTime) {

        return dateTime
                .toLocalTime()
                .withSecond(0)
                .withNano(0)
                .toString();
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

        return hours + "h " + minutes + "m";
    }


}