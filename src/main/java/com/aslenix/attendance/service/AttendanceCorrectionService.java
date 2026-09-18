package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.AttendanceAuditLog;
import com.aslenix.attendance.entity.AttendanceCorrectionRequest;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.AttendanceAuditLogRepository;
import com.aslenix.attendance.repository.AttendanceCorrectionRequestRepository;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class AttendanceCorrectionService {

    private final AttendanceCorrectionRequestRepository correctionRequestRepository;
    private final AttendanceAuditLogRepository auditLogRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;

    public AttendanceCorrectionService(
            AttendanceCorrectionRequestRepository correctionRequestRepository,
            AttendanceAuditLogRepository auditLogRepository,
            AttendanceRepository attendanceRepository,
            AttendanceService attendanceService,
            NotificationService notificationService,
            EmployeeRepository employeeRepository) {

        this.correctionRequestRepository = correctionRequestRepository;
        this.auditLogRepository = auditLogRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
        this.notificationService = notificationService;
        this.employeeRepository = employeeRepository;
    }

    // ============================================================
    // SUBMIT CORRECTION REQUEST (EMPLOYEE)
    // ============================================================

    public AttendanceCorrectionRequest submitCorrectionRequest(
            Employee employee,
            LocalDate attendanceDate,
            LocalTime requestedCheckIn,
            LocalTime requestedCheckOut,
            String reason) {

        if (employee == null) {
            throw new IllegalArgumentException("Employee is required.");
        }
        if (attendanceDate == null) {
            throw new IllegalArgumentException("Attendance date is required.");
        }
        if (requestedCheckIn == null) {
            throw new IllegalArgumentException("Requested check-in time is required.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for correction is required.");
        }

        // Check for duplicate pending requests for the same date
        boolean hasPending = correctionRequestRepository
                .existsByEmployeeAndAttendanceDateAndStatus(
                        employee,
                        attendanceDate,
                        "PENDING"
                );

        if (hasPending) {
            throw new IllegalStateException(
                    "You already have a pending correction request for " + attendanceDate + "."
            );
        }

        AttendanceCorrectionRequest request = new AttendanceCorrectionRequest();
        request.setEmployee(employee);
        request.setAttendanceDate(attendanceDate);
        request.setRequestedCheckIn(requestedCheckIn);
        request.setRequestedCheckOut(requestedCheckOut);
        request.setReason(reason.trim());
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());

        AttendanceCorrectionRequest saved = correctionRequestRepository.save(request);

        // Notify Admin(s)
        String empName = employee.getFirstName() + (employee.getLastName() != null ? " " + employee.getLastName() : "");
        notificationService.createAdminNotification(
                employee,
                "Attendance Correction Request",
                "New attendance correction request from " + empName + " for " + attendanceDate + ".",
                "CORRECTION_REQUEST"
        );

        return saved;
    }

    // ============================================================
    // APPROVE CORRECTION REQUEST (ADMIN)
    // ============================================================

    public AttendanceCorrectionRequest approveCorrectionRequest(
            Long requestId,
            String adminUsername,
            String reviewNote) {

        AttendanceCorrectionRequest request = correctionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Correction request not found: " + requestId));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending correction requests can be approved.");
        }

        Employee employee = request.getEmployee();
        LocalDate date = request.getAttendanceDate();

        // 1. Snapshot original values for audit
        Attendance existing = attendanceRepository
                .findByEmployeeAndAttendanceDate(employee, date)
                .orElse(null);

        LocalDateTime origCheckIn = existing != null ? existing.getCheckIn() : null;
        LocalDateTime origCheckOut = existing != null ? existing.getCheckOut() : null;
        String origStatus = existing != null ? existing.getStatus() : "ABSENT";

        // 2. Apply and recalculate attendance
        Attendance updatedAttendance = attendanceService.recalculateAndApplyAttendance(
                employee,
                date,
                request.getRequestedCheckIn(),
                request.getRequestedCheckOut(),
                null // Auto-calculate status
        );

        // 3. Mark request APPROVED
        request.setStatus("APPROVED");
        request.setReviewedBy(adminUsername);
        request.setAdminReviewNote(reviewNote != null && !reviewNote.trim().isEmpty() ? reviewNote.trim() : null);
        request.setReviewedAt(LocalDateTime.now());
        AttendanceCorrectionRequest savedRequest = correctionRequestRepository.save(request);

        // 4. Create immutable audit log
        String empName = employee.getFirstName() + (employee.getLastName() != null ? " " + employee.getLastName() : "");
        String auditReason = "Request Approved: " + request.getReason();
        if (reviewNote != null && !reviewNote.trim().isEmpty()) {
            auditReason += " | Admin Note: " + reviewNote.trim();
        }

        AttendanceAuditLog auditLog = new AttendanceAuditLog(
                adminUsername,
                employee,
                employee.getEmployeeCode(),
                empName,
                date,
                origCheckIn,
                updatedAttendance.getCheckIn(),
                origCheckOut,
                updatedAttendance.getCheckOut(),
                origStatus,
                updatedAttendance.getStatus(),
                auditReason,
                "REQUEST_APPROVAL",
                LocalDateTime.now()
        );
        auditLogRepository.save(auditLog);

        // 5. Notify employee
        String notifyMsg = "Your attendance correction request for " + date + " has been approved.";
        if (reviewNote != null && !reviewNote.trim().isEmpty()) {
            notifyMsg += " Review note: " + reviewNote.trim();
        }
        notificationService.createNotification(
                employee,
                "Attendance Correction Approved",
                notifyMsg,
                "CORRECTION_APPROVED"
        );

        return savedRequest;
    }

    // ============================================================
    // REJECT CORRECTION REQUEST (ADMIN)
    // ============================================================

    public AttendanceCorrectionRequest rejectCorrectionRequest(
            Long requestId,
            String adminUsername,
            String reviewNote) {

        AttendanceCorrectionRequest request = correctionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Correction request not found: " + requestId));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending correction requests can be rejected.");
        }

        request.setStatus("REJECTED");
        request.setReviewedBy(adminUsername);
        request.setAdminReviewNote(reviewNote != null && !reviewNote.trim().isEmpty() ? reviewNote.trim() : null);
        request.setReviewedAt(LocalDateTime.now());
        AttendanceCorrectionRequest savedRequest = correctionRequestRepository.save(request);

        // Attendance remains completely unchanged

        // Notify employee
        String notifyMsg = "Your attendance correction request for " + request.getAttendanceDate() + " has been rejected.";
        if (reviewNote != null && !reviewNote.trim().isEmpty()) {
            notifyMsg += " Review note: " + reviewNote.trim();
        }
        notificationService.createNotification(
                request.getEmployee(),
                "Attendance Correction Rejected",
                notifyMsg,
                "CORRECTION_REJECTED"
        );

        return savedRequest;
    }

    // ============================================================
    // DIRECT EDIT ATTENDANCE (ADMIN)
    // ============================================================

    public Attendance directEditAttendance(
            Long employeeId,
            LocalDate date,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            String statusOverride,
            String reason,
            String adminUsername) {

        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Attendance date is required.");
        }
        if (checkInTime == null) {
            throw new IllegalArgumentException("Check-in time is required.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason for this correction is required for the audit trail.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));

        // 1. Snapshot original state
        Attendance existing = attendanceRepository
                .findByEmployeeAndAttendanceDate(employee, date)
                .orElse(null);

        LocalDateTime origCheckIn = existing != null ? existing.getCheckIn() : null;
        LocalDateTime origCheckOut = existing != null ? existing.getCheckOut() : null;
        String origStatus = existing != null ? existing.getStatus() : "ABSENT";

        // 2. Apply and recalculate
        Attendance updatedAttendance = attendanceService.recalculateAndApplyAttendance(
                employee,
                date,
                checkInTime,
                checkOutTime,
                statusOverride
        );

        // 3. Create audit trail
        String empName = employee.getFirstName() + (employee.getLastName() != null ? " " + employee.getLastName() : "");
        AttendanceAuditLog auditLog = new AttendanceAuditLog(
                adminUsername,
                employee,
                employee.getEmployeeCode(),
                empName,
                date,
                origCheckIn,
                updatedAttendance.getCheckIn(),
                origCheckOut,
                updatedAttendance.getCheckOut(),
                origStatus,
                updatedAttendance.getStatus(),
                reason.trim(),
                "DIRECT_EDIT",
                LocalDateTime.now()
        );
        auditLogRepository.save(auditLog);

        return updatedAttendance;
    }

    // ============================================================
    // QUERIES
    // ============================================================

    @Transactional(readOnly = true)
    public List<AttendanceCorrectionRequest> getEmployeeRequests(Employee employee) {
        return correctionRequestRepository.findByEmployeeOrderByCreatedAtDesc(employee);
    }

    @Transactional(readOnly = true)
    public List<AttendanceCorrectionRequest> getAllRequests() {
        return correctionRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AttendanceCorrectionRequest> getRequestsByStatus(String status) {
        return correctionRequestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public AttendanceCorrectionRequest getRequest(Long id) {
        return correctionRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AttendanceAuditLog> getAllAuditLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public long getPendingRequestsCount() {
        return correctionRequestRepository.countByStatus("PENDING");
    }
}
