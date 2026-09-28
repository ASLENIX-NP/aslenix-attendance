package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.AdminAttendanceAudit;
import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AdminAttendanceAuditRepository;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin/card-reader-attendance")
public class AdminCardReaderController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final OfficeSettingsRepository officeSettingsRepository;
    private final AdminAttendanceAuditRepository auditRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public AdminCardReaderController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            OfficeSettingsRepository officeSettingsRepository,
            AdminAttendanceAuditRepository auditRepository) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.officeSettingsRepository = officeSettingsRepository;
        this.auditRepository = auditRepository;
    }

    // ============================================================
    // MAIN PAGE
    // ============================================================
    @GetMapping
    public String cardReaderPage(Authentication authentication, Model model) {
        String adminName = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "admin";

        OfficeSettings settings = officeSettingsRepository
                .findFirstByOrderByIdAsc()
                .orElse(null);

        String scheduled = "09:00 AM - 06:00 PM";
        if (settings != null && settings.getWorkStartTime() != null && settings.getWorkEndTime() != null) {
            scheduled = settings.getWorkStartTime().format(TIME_FMT) + " - " + settings.getWorkEndTime().format(TIME_FMT);
        }

        List<AdminAttendanceAudit> auditLogs = auditRepository.findTop30ByOrderByTimestampDesc();

        model.addAttribute("adminName", adminName);
        model.addAttribute("adminRole", "Super Admin");
        model.addAttribute("todayDate", LocalDate.now().format(DATE_FMT));
        model.addAttribute("currentTime", LocalDateTime.now().format(TIME_FMT));
        model.addAttribute("scheduledHours", scheduled);
        model.addAttribute("auditLogs", auditLogs);

        return "admin/card-reader-attendance";
    }

    // ============================================================
    // SCAN / IDENTIFY EMPLOYEE (QR TOKEN OR EMPLOYEE CODE)
    // ============================================================
    @PostMapping("/scan")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> scanEmployee(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {

        String query = payload != null ? payload.get("query") : "";
        if (query == null || query.isBlank()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please scan or enter an employee QR code or ID."));
        }

        query = query.trim();
        String adminName = (authentication != null) ? authentication.getName() : "admin";

        // Try lookup by QR token, Employee Code, Email, or Username
        Employee employee = employeeRepository.findByQrToken(query).orElse(null);
        if (employee == null) {
            employee = employeeRepository.findByEmployeeCode(query).orElse(null);
        }
        if (employee == null) {
            employee = employeeRepository.findByEmail(query).orElse(null);
        }
        if (employee == null) {
            employee = employeeRepository.findByUserUsername(query).orElse(null);
        }

        // If not found
        if (employee == null) {
            AdminAttendanceAudit audit = new AdminAttendanceAudit(
                    query.length() > 20 ? query.substring(0, 20) + "..." : query,
                    "Unknown employee",
                    adminName,
                    "UNKNOWN_CARD",
                    "Card not registered",
                    LocalDateTime.now()
            );
            auditRepository.save(audit);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", "Card or ID '" + query + "' not recognized. Employee not registered.");
            resp.put("auditLogs", formatAuditList(auditRepository.findTop30ByOrderByTimestampDesc()));
            return ResponseEntity.ok(resp);
        }

        // Found employee
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository
                .findByEmployeeAndAttendanceDate(employee, today)
                .orElse(null);

        OfficeSettings settings = officeSettingsRepository
                .findFirstByOrderByIdAsc()
                .orElse(null);

        String scheduled = "09:00 AM - 06:00 PM";
        if (settings != null && settings.getWorkStartTime() != null && settings.getWorkEndTime() != null) {
            scheduled = settings.getWorkStartTime().format(TIME_FMT) + " - " + settings.getWorkEndTime().format(TIME_FMT);
        }

        // Determine current attendance state
        String currentStatus = "Not Checked In";
        String checkInStr = "--:--";
        String checkOutStr = "-";
        String previousAttendance = "No records today";
        String recommendedAction = "CHECK_IN";

        if (attendance != null) {
            if (attendance.getCheckIn() != null) {
                checkInStr = attendance.getCheckIn().format(TIME_FMT);
                previousAttendance = "Checked in at " + checkInStr;
            }
            if (attendance.getCheckOut() != null) {
                checkOutStr = attendance.getCheckOut().format(TIME_FMT);
            }

            if (attendance.getCheckIn() != null && attendance.getCheckOut() == null) {
                currentStatus = "Checked In";
                recommendedAction = "CHECK_OUT";
            } else if (attendance.getCheckOut() != null) {
                currentStatus = "Checked Out";
                recommendedAction = "CHECK_OUT";
            }
        }

        boolean hasCheckedIn = (attendance != null && attendance.getCheckIn() != null);
        boolean hasCheckedOut = (attendance != null && attendance.getCheckOut() != null);

        // Masked token / card display
        String rawToken = (employee.getQrToken() != null && !employee.getQrToken().isBlank())
                ? employee.getQrToken()
                : employee.getEmployeeCode();
        String masked = "****" + (rawToken.length() >= 4 ? rawToken.substring(rawToken.length() - 4).toUpperCase() : "825A");

        // Initials avatar
        String initials = "";
        if (employee.getFirstName() != null && !employee.getFirstName().isBlank()) {
            initials += employee.getFirstName().substring(0, 1).toUpperCase();
        }
        if (employee.getLastName() != null && !employee.getLastName().isBlank()) {
            initials += employee.getLastName().substring(0, 1).toUpperCase();
        }
        if (initials.isBlank()) initials = "E";

        Map<String, Object> empData = new HashMap<>();
        empData.put("id", employee.getId());
        empData.put("employeeCode", employee.getEmployeeCode());
        empData.put("fullName", (employee.getFirstName() != null ? employee.getFirstName() : "") + " " + (employee.getLastName() != null ? employee.getLastName() : "").trim());
        empData.put("role", (employee.getPosition() != null && !employee.getPosition().isBlank()) ? employee.getPosition() : "Team Member");
        empData.put("department", employee.getDepartment() != null ? employee.getDepartment().getName() : "Development");
        empData.put("joinDate", employee.getJoiningDate() != null ? employee.getJoiningDate().format(DATE_FMT) : "28 Jul 2026");
        empData.put("status", employee.isEnabled() ? "Active" : "Disabled");
        empData.put("enabled", employee.isEnabled());
        empData.put("hasCheckedIn", hasCheckedIn);
        empData.put("hasCheckedOut", hasCheckedOut);
        empData.put("avatar", initials);
        empData.put("photoUrl", employee.getPhotoUrl());
        empData.put("maskedCard", masked);
        empData.put("lastVerifiedAt", LocalDateTime.now().format(DATETIME_FMT));

        Map<String, Object> attData = new HashMap<>();
        attData.put("status", currentStatus);
        attData.put("date", today.format(DATE_FMT));
        attData.put("scheduled", scheduled);
        attData.put("previousAttendance", previousAttendance);
        attData.put("checkIn", checkInStr);
        attData.put("checkOut", checkOutStr);
        attData.put("hasCheckedIn", hasCheckedIn);
        attData.put("hasCheckedOut", hasCheckedOut);
        attData.put("recommendedAction", recommendedAction);

        // Record scan event
        AdminAttendanceAudit scanAudit = new AdminAttendanceAudit(
                employee.getEmployeeCode(),
                empData.get("fullName").toString(),
                adminName,
                "CARD_SCAN",
                "Card verified | Secure scan completed",
                LocalDateTime.now()
        );
        auditRepository.save(scanAudit);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("employee", empData);
        resp.put("attendance", attData);
        resp.put("auditLogs", formatAuditList(auditRepository.findTop30ByOrderByTimestampDesc()));

        return ResponseEntity.ok(resp);
    }

    // ============================================================
    // PROCESS ATTENDANCE (CHECK IN / CHECK OUT / FORCE OVERRIDE)
    // ============================================================
    @PostMapping("/process")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processAttendance(
            Authentication authentication,
            @RequestBody Map<String, Object> payload) {

        String employeeCode = payload != null ? String.valueOf(payload.get("employeeCode")) : "";
        String action = payload != null ? String.valueOf(payload.get("action")).toUpperCase() : "CHECK_IN";
        boolean force = payload != null && Boolean.parseBoolean(String.valueOf(payload.get("force")));

        String adminName = (authentication != null) ? authentication.getName() : "admin";

        Employee employee = employeeRepository.findByEmployeeCode(employeeCode).orElse(null);
        if (employee == null) {
            employee = employeeRepository.findByQrToken(employeeCode).orElse(null);
        }

        if (employee == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Employee not found."));
        }

        if (!employee.isEnabled() && !force) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Employee account is disabled. Enable force attendance to override."));
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository
                .findByEmployeeAndAttendanceDate(employee, today)
                .orElse(null);

        OfficeSettings settings = officeSettingsRepository
                .findFirstByOrderByIdAsc()
                .orElse(null);

        String employeeFullName = (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
                (employee.getLastName() != null ? employee.getLastName() : "").trim();

        String auditActionType = action;
        String auditNotes = "";
        String successMessage = "";

        if ("CHECK_IN".equalsIgnoreCase(action)) {
            if (attendance != null && attendance.getCheckIn() != null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", employeeFullName + " has already checked in today at " + attendance.getCheckIn().format(TIME_FMT) + ". Check-in cannot be performed again."
                ));
            } else {
                // New check in
                if (attendance == null) {
                    attendance = new Attendance();
                    attendance.setEmployee(employee);
                    attendance.setAttendanceDate(today);
                }
                attendance.setCheckIn(now);

                boolean isLate = false;
                if (settings != null && settings.getWorkStartTime() != null) {
                    LocalTime lateThreshold = settings.getWorkStartTime().plusMinutes(settings.getLateGraceMinutes());
                    isLate = now.toLocalTime().isAfter(lateThreshold);
                }

                attendance.setLate(isLate);
                attendance.setStatus(isLate ? "LATE" : "PRESENT");
                attendance.setEarlyLeave(false);
                attendance.setHalfDay(false);
                attendanceRepository.save(attendance);

                if (force) {
                    auditActionType = "FORCE_CHECK_IN";
                    auditNotes = "Force Check-In processed | Admin: " + adminName;
                    successMessage = "Force Check-In successfully recorded for " + employeeFullName + " at " + now.format(TIME_FMT);
                } else {
                    auditActionType = "CHECK_IN";
                    auditNotes = (isLate ? "Marked Late" : "On Time") + " | Admin Card Reader | Admin: " + adminName;
                    successMessage = "Check-In successfully recorded for " + employeeFullName + " at " + now.format(TIME_FMT);
                }
            }
        } else if ("CHECK_OUT".equalsIgnoreCase(action)) {
            if (attendance == null || attendance.getCheckIn() == null) {
                if (!force) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", employeeFullName + " has not checked in today. Please check in first or enable Force Check-Out."
                    ));
                }
                // Force check-out without prior check-in
                if (attendance == null) {
                    attendance = new Attendance();
                    attendance.setEmployee(employee);
                    attendance.setAttendanceDate(today);
                }
                LocalTime defaultStart = (settings != null && settings.getWorkStartTime() != null) ? settings.getWorkStartTime() : LocalTime.of(9, 0);
                attendance.setCheckIn(today.atTime(defaultStart));
                attendance.setCheckOut(now);
                attendance.setStatus("PRESENT");
                attendanceRepository.save(attendance);

                auditActionType = "FORCE_CHECK_OUT";
                auditNotes = "Employee missed normal check-in | Force Check-out by " + adminName;
                successMessage = "Force Check-Out successfully completed for " + employeeFullName + " at " + now.format(TIME_FMT);
            } else if (attendance.getCheckOut() != null) {
                if (!force) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", employeeFullName + " already checked out at " + attendance.getCheckOut().format(TIME_FMT) + ". Enable Force Check-Out to update."
                    ));
                }
                attendance.setCheckOut(now);
                attendanceRepository.save(attendance);

                auditActionType = "FORCE_CHECK_OUT";
                auditNotes = "Check-out time updated | Force Check-out by " + adminName;
                successMessage = "Force Check-Out updated for " + employeeFullName + " at " + now.format(TIME_FMT);
            } else {
                // Normal check-out
                attendance.setCheckOut(now);
                boolean earlyLeave = false;
                if (settings != null && settings.getWorkEndTime() != null) {
                    earlyLeave = now.toLocalTime().isBefore(settings.getWorkEndTime());
                }
                attendance.setEarlyLeave(earlyLeave);
                attendanceRepository.save(attendance);

                if (force) {
                    auditActionType = "FORCE_CHECK_OUT";
                    auditNotes = "Force Check-out completed | Admin: " + adminName;
                    successMessage = "Force Check-Out completed for " + employeeFullName + " at " + now.format(TIME_FMT);
                } else {
                    auditActionType = "CHECK_OUT";
                    auditNotes = (earlyLeave ? "Early Departure" : "Standard Departure") + " | Admin Card Reader | Admin: " + adminName;
                    successMessage = "Check-Out completed for " + employeeFullName + " at " + now.format(TIME_FMT);
                }
            }
        }

        // Save audit log
        AdminAttendanceAudit audit = new AdminAttendanceAudit(
                employee.getEmployeeCode(),
                employeeFullName,
                adminName,
                auditActionType,
                auditNotes,
                LocalDateTime.now()
        );
        auditRepository.save(audit);

        // Updated attendance state
        String scheduled = "09:00 AM - 06:00 PM";
        if (settings != null && settings.getWorkStartTime() != null && settings.getWorkEndTime() != null) {
            scheduled = settings.getWorkStartTime().format(TIME_FMT) + " - " + settings.getWorkEndTime().format(TIME_FMT);
        }

        Map<String, Object> attData = new HashMap<>();
        attData.put("status", (attendance.getCheckOut() != null ? "Checked Out" : "Checked In"));
        attData.put("date", today.format(DATE_FMT));
        attData.put("scheduled", scheduled);
        attData.put("previousAttendance", "Checked in at " + attendance.getCheckIn().format(TIME_FMT));
        attData.put("checkIn", attendance.getCheckIn().format(TIME_FMT));
        attData.put("checkOut", attendance.getCheckOut() != null ? attendance.getCheckOut().format(TIME_FMT) : "-");
        attData.put("recommendedAction", attendance.getCheckOut() != null ? "CHECK_IN" : "CHECK_OUT");

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", successMessage);
        resp.put("attendance", attData);
        resp.put("auditLogs", formatAuditList(auditRepository.findTop30ByOrderByTimestampDesc()));

        return ResponseEntity.ok(resp);
    }

    // ============================================================
    // AUDIT LOGS REFRESH
    // ============================================================
    @GetMapping("/logs")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs() {
        return ResponseEntity.ok(formatAuditList(auditRepository.findTop30ByOrderByTimestampDesc()));
    }

    private List<Map<String, Object>> formatAuditList(List<AdminAttendanceAudit> list) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        if (list == null) return formatted;
        for (AdminAttendanceAudit item : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("employeeCode", item.getEmployeeCode() != null ? item.getEmployeeCode() : "Unknown");
            map.put("employeeName", item.getEmployeeName() != null ? item.getEmployeeName() : "Unknown employee");
            map.put("adminUsername", item.getAdminUsername() != null ? item.getAdminUsername() : "admin");
            map.put("actionType", item.getActionType());
            map.put("notes", item.getNotes() != null ? item.getNotes() : "");
            map.put("time", item.getTimestamp() != null ? item.getTimestamp().format(TIME_FMT) : "");
            map.put("date", item.getTimestamp() != null ? item.getTimestamp().format(DATE_FMT) : "");
            formatted.add(map);
        }
        return formatted;
    }
}
