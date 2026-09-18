package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.AttendanceAuditLog;
import com.aslenix.attendance.entity.AttendanceCorrectionRequest;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.AttendanceCorrectionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminAttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceCorrectionService attendanceCorrectionService;
    private final EmployeeRepository employeeRepository;

    public AdminAttendanceController(
            AttendanceRepository attendanceRepository,
            AttendanceCorrectionService attendanceCorrectionService,
            EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceCorrectionService = attendanceCorrectionService;
        this.employeeRepository = employeeRepository;
    }

    // ============================================================
    // ADMIN ATTENDANCE PAGE
    // ============================================================

    @GetMapping("/attendance")
    public String attendance(Model model) {

        List<Attendance> attendanceRecords = attendanceRepository.findAll();

        // Newest attendance date first.
        // If two records have the same date, newest check-in appears first.
        attendanceRecords.sort(
                Comparator
                        .comparing(
                                Attendance::getAttendanceDate,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                        .thenComparing(
                                Attendance::getCheckIn,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
        );

        model.addAttribute("attendanceRecords", attendanceRecords);

        // Correction requests
        List<AttendanceCorrectionRequest> allRequests = attendanceCorrectionService.getAllRequests();
        model.addAttribute("correctionRequests", allRequests);

        long pendingCount = allRequests.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                .count();
        model.addAttribute("pendingCorrectionCount", pendingCount);

        // Audit logs
        List<AttendanceAuditLog> auditLogs = attendanceCorrectionService.getAllAuditLogs();
        model.addAttribute("auditLogs", auditLogs);

        // All employees for direct edit modal
        List<Employee> employees = employeeRepository.findAllByOrderByFirstNameAsc();
        model.addAttribute("employees", employees);

        return "admin/attendance";
    }

    // ============================================================
    // APPROVE CORRECTION REQUEST
    // ============================================================

    @PostMapping("/attendance/corrections/{id}/approve")
    public String approveCorrection(
            @PathVariable Long id,
            @RequestParam(value = "reviewNote", required = false) String reviewNote,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String adminName = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "admin";

        try {
            attendanceCorrectionService.approveCorrectionRequest(id, adminName, reviewNote);
            redirectAttributes.addFlashAttribute("successMessage", "Correction request approved successfully.");
            return "redirect:/admin/attendance?approved";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve correction: " + e.getMessage());
            return "redirect:/admin/attendance?error=" + encodeMessage(e.getMessage());
        }
    }

    // ============================================================
    // REJECT CORRECTION REQUEST
    // ============================================================

    @PostMapping("/attendance/corrections/{id}/reject")
    public String rejectCorrection(
            @PathVariable Long id,
            @RequestParam(value = "reviewNote", required = false) String reviewNote,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String adminName = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "admin";

        try {
            attendanceCorrectionService.rejectCorrectionRequest(id, adminName, reviewNote);
            redirectAttributes.addFlashAttribute("successMessage", "Correction request rejected.");
            return "redirect:/admin/attendance?rejected";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject correction: " + e.getMessage());
            return "redirect:/admin/attendance?error=" + encodeMessage(e.getMessage());
        }
    }

    // ============================================================
    // DIRECT EDIT ATTENDANCE (ADMIN)
    // ============================================================

    @PostMapping("/attendance/edit")
    public String directEditAttendance(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("attendanceDate") LocalDate attendanceDate,
            @RequestParam("checkInTime") LocalTime checkInTime,
            @RequestParam(value = "checkOutTime", required = false) LocalTime checkOutTime,
            @RequestParam(value = "statusOverride", required = false) String statusOverride,
            @RequestParam("reason") String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String adminName = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "admin";

        try {
            attendanceCorrectionService.directEditAttendance(
                    employeeId,
                    attendanceDate,
                    checkInTime,
                    checkOutTime,
                    statusOverride,
                    reason,
                    adminName
            );
            redirectAttributes.addFlashAttribute("successMessage", "Attendance successfully updated and audit entry recorded.");
            return "redirect:/admin/attendance?edited";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to edit attendance: " + e.getMessage());
            return "redirect:/admin/attendance?error=" + encodeMessage(e.getMessage());
        }
    }

    private String encodeMessage(String message) {
        if (message == null) return "Operation failed.";
        return message.replace(" ", "%20").replace(":", "%3A").replace(",", "%2C");
    }
}