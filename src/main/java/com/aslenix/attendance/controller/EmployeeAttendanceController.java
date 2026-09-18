package com.aslenix.attendance.controller;

import com.aslenix.attendance.dto.StreakDto;
import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.AttendanceCorrectionRequest;
import com.aslenix.attendance.entity.CalendarEvent;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.LeaveRequest;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.AttendanceCorrectionService;
import com.aslenix.attendance.service.AttendanceService;
import com.aslenix.attendance.service.CalendarEventService;
import com.aslenix.attendance.service.LeaveRequestService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/employee/attendance")
public class EmployeeAttendanceController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final LeaveRequestService leaveRequestService;
    private final CalendarEventService calendarEventService;
    private final AttendanceCorrectionService attendanceCorrectionService;

    public EmployeeAttendanceController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            AttendanceService attendanceService,
            LeaveRequestService leaveRequestService,
            CalendarEventService calendarEventService,
            AttendanceCorrectionService attendanceCorrectionService) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
        this.leaveRequestService = leaveRequestService;
        this.calendarEventService = calendarEventService;
        this.attendanceCorrectionService = attendanceCorrectionService;
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
        // BUTTON STATES (CUTOFF AT 2:00 PM / 14:00)
        // --------------------------------------------------------

        boolean isAfter2Pm =
                LocalTime.now().isAfter(LocalTime.of(14, 0));

        boolean canCheckIn =
                (todayAttendance == null
                        || todayAttendance.getCheckIn() == null)
                        && !isAfter2Pm;

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

        for (Attendance a : attendanceRecords) {

            if ("PRESENT".equals(a.getStatus())
                    || "LATE".equals(a.getStatus())) {

                presentDays++;
            }

            if (a.getCheckIn() == null) {
                absentDays++;
            }

            if (a.getCheckIn() != null
                    && a.getCheckOut() != null) {

                Duration duration =
                        Duration.between(
                                a.getCheckIn(),
                                a.getCheckOut()
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
        // ATTENDANCE STREAK
        // --------------------------------------------------------

        StreakDto streak = attendanceService.calculateStreak(employee);
        model.addAttribute("currentStreak", streak.getCurrentStreak());
        model.addAttribute("longestStreak", streak.getLongestStreak());
        model.addAttribute("streakMessage", streak.getMessage());

        // --------------------------------------------------------
        // ATTENDANCE CORRECTION REQUESTS
        // --------------------------------------------------------

        List<AttendanceCorrectionRequest> correctionRequests =
                attendanceCorrectionService.getEmployeeRequests(employee);
        model.addAttribute("correctionRequests", correctionRequests);

        long pendingCorrectionCount = correctionRequests.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                .count();
        model.addAttribute("pendingCorrectionCount", pendingCorrectionCount);

        // --------------------------------------------------------
        // MODEL ATTRIBUTES
        // --------------------------------------------------------

        model.addAttribute(
                "employee",
                employee
        );

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
                "isAfter2Pm",
                isAfter2Pm
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

        model.addAttribute(
                "settings",
                settings
        );

        // Attendance data list for interactive calendar
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        List<Map<String, Object>> attendanceJsonList = new ArrayList<>();
        for (Attendance a : attendanceRecords) {
            if (a.getAttendanceDate() == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("date", a.getAttendanceDate().toString());
            item.put("status", a.getStatus() != null ? a.getStatus() : "");
            item.put("late", a.isLate());
            item.put("earlyLeave", a.isEarlyLeave());
            item.put("halfDay", a.isHalfDay());
            if (a.getCheckIn() != null) {
                item.put("checkInTime", a.getCheckIn().toLocalTime().format(timeFormatter));
            }
            if (a.getCheckOut() != null) {
                item.put("checkOutTime", a.getCheckOut().toLocalTime().format(timeFormatter));
            }
            if (a.getCheckIn() != null && a.getCheckOut() != null) {
                Duration dur = Duration.between(a.getCheckIn(), a.getCheckOut());
                long hrs = dur.toHours();
                long mins = dur.toMinutes() % 60;
                item.put("workingHours", hrs + "h " + String.format("%02dm", mins));
            }
            attendanceJsonList.add(item);
        }
        model.addAttribute("attendanceJsonList", attendanceJsonList);

        // Approved leaves for calendar
        List<LeaveRequest> allLeaves = leaveRequestService.getEmployeeRequests(employee);
        List<Map<String, Object>> approvedLeavesList = new ArrayList<>();
        if (allLeaves != null) {
            for (LeaveRequest lr : allLeaves) {
                if ("APPROVED".equalsIgnoreCase(lr.getStatus()) && lr.getStartDate() != null && lr.getEndDate() != null) {
                    Map<String, Object> lMap = new HashMap<>();
                    lMap.put("startDate", lr.getStartDate().toString());
                    lMap.put("endDate", lr.getEndDate().toString());
                    lMap.put("leaveType", lr.getLeaveType() != null ? lr.getLeaveType().toString() : "Leave");
                    approvedLeavesList.add(lMap);
                }
            }
        }
        model.addAttribute("approvedLeavesList", approvedLeavesList);

        // All calendar events (government, holidays, custom)
        model.addAttribute("calendarEvents", calendarEventService.getAllEvents());

        return "employee/attendance";
    }

    // ============================================================
    // SUBMIT ATTENDANCE CORRECTION REQUEST (EMPLOYEE)
    // ============================================================

    @PostMapping("/corrections")
    public String submitCorrection(
            Authentication authentication,
            @RequestParam("attendanceDate") LocalDate attendanceDate,
            @RequestParam("requestedCheckIn") LocalTime requestedCheckIn,
            @RequestParam(value = "requestedCheckOut", required = false) LocalTime requestedCheckOut,
            @RequestParam("reason") String reason,
            RedirectAttributes redirectAttributes) {

        Employee employee = getLoggedInEmployee(authentication);
        if (employee == null) {
            return "redirect:/login";
        }

        try {
            attendanceCorrectionService.submitCorrectionRequest(
                    employee,
                    attendanceDate,
                    requestedCheckIn,
                    requestedCheckOut,
                    reason
            );
            redirectAttributes.addFlashAttribute("successMessage", "Correction request submitted successfully for " + attendanceDate + ".");
            return "redirect:/employee/attendance?correctionSubmitted";
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/attendance?correctionError=" + encodeMessage(e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit correction request.");
            return "redirect:/employee/attendance?correctionError=" + encodeMessage("Unable to submit correction request.");
        }
    }

    // ============================================================
    // CALENDAR DATA API
    // ============================================================

    @GetMapping("/calendar-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCalendarData(Authentication authentication) {
        Employee employee = getLoggedInEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).build();
        }
        OfficeSettings settings = attendanceService.getOfficeSettings();
        List<Attendance> attendanceRecords = attendanceRepository.findByEmployeeOrderByAttendanceDateDesc(employee);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        List<Map<String, Object>> recordsList = new ArrayList<>();
        for (Attendance a : attendanceRecords) {
            if (a.getAttendanceDate() == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("date", a.getAttendanceDate().toString());
            item.put("status", a.getStatus() != null ? a.getStatus() : "");
            item.put("late", a.isLate());
            item.put("earlyLeave", a.isEarlyLeave());
            item.put("halfDay", a.isHalfDay());
            if (a.getCheckIn() != null) {
                item.put("checkInTime", a.getCheckIn().toLocalTime().format(timeFormatter));
            }
            if (a.getCheckOut() != null) {
                item.put("checkOutTime", a.getCheckOut().toLocalTime().format(timeFormatter));
            }
            if (a.getCheckIn() != null && a.getCheckOut() != null) {
                Duration dur = Duration.between(a.getCheckIn(), a.getCheckOut());
                long hrs = dur.toHours();
                long mins = dur.toMinutes() % 60;
                item.put("workingHours", hrs + "h " + String.format("%02dm", mins));
            }
            recordsList.add(item);
        }

        List<LeaveRequest> allLeaves = leaveRequestService.getEmployeeRequests(employee);
        List<Map<String, Object>> leavesList = new ArrayList<>();
        if (allLeaves != null) {
            for (LeaveRequest lr : allLeaves) {
                if ("APPROVED".equalsIgnoreCase(lr.getStatus()) && lr.getStartDate() != null && lr.getEndDate() != null) {
                    Map<String, Object> lMap = new HashMap<>();
                    lMap.put("startDate", lr.getStartDate().toString());
                    lMap.put("endDate", lr.getEndDate().toString());
                    lMap.put("leaveType", lr.getLeaveType() != null ? lr.getLeaveType().toString() : "Leave");
                    leavesList.add(lMap);
                }
            }
        }

        Map<String, Object> workingDays = new HashMap<>();
        workingDays.put("sunday", settings.isSunday());
        workingDays.put("monday", settings.isMonday());
        workingDays.put("tuesday", settings.isTuesday());
        workingDays.put("wednesday", settings.isWednesday());
        workingDays.put("thursday", settings.isThursday());
        workingDays.put("friday", settings.isFriday());
        workingDays.put("saturday", settings.isSaturday());

        Map<String, Object> response = new HashMap<>();
        response.put("attendance", recordsList);
        response.put("leaves", leavesList);
        response.put("workingDays", workingDays);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // CALENDAR EVENTS API
    // ============================================================

    @GetMapping("/calendar-events")
    @ResponseBody
    public ResponseEntity<List<CalendarEvent>> getCalendarEvents(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (year != null && month != null) {
            return ResponseEntity.ok(calendarEventService.getEventsForBsMonth(year, month));
        }
        return ResponseEntity.ok(calendarEventService.getAllEvents());
    }

    @PostMapping("/calendar-events")
    @ResponseBody
    public ResponseEntity<?> addCalendarEvent(@RequestBody Map<String, Object> payload) {
        try {
            String title = (String) payload.get("title");
            String bsDate = (String) payload.get("bsDate");
            boolean isHoliday = Boolean.TRUE.equals(payload.get("holiday")) || Boolean.TRUE.equals(payload.get("isHoliday"));
            String category = payload.get("category") != null ? (String) payload.get("category") : "GOVERNMENT";

            CalendarEvent event = calendarEventService.addEvent(title, bsDate, isHoliday, category);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error saving event"));
        }
    }

    @DeleteMapping("/calendar-events/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCalendarEvent(@PathVariable Long id) {
        boolean deleted = calendarEventService.deleteEvent(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.notFound().build();
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