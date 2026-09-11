package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.LeaveRequest;
import com.aslenix.attendance.repository.LeaveRequestRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationService notificationService;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            NotificationService notificationService) {

        this.leaveRequestRepository =
                leaveRequestRepository;

        this.notificationService =
                notificationService;
    }

    // ============================================================
    // CREATE LEAVE REQUEST
    // ============================================================

    @Transactional
    public LeaveRequest createLeaveRequest(
            Employee employee,
            String leaveType,
            String duration,
            String halfDaySession,
            LocalDate startDate,
            LocalDate endDate,
            String reason) {

        // --------------------------------------------------------
        // VALIDATE EMPLOYEE
        // --------------------------------------------------------

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }

        // --------------------------------------------------------
        // VALIDATE LEAVE TYPE
        // --------------------------------------------------------

        if (leaveType == null ||
                leaveType.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Leave type is required."
            );
        }

        // --------------------------------------------------------
        // VALIDATE DURATION
        // --------------------------------------------------------

        if (duration == null ||
                duration.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Leave duration is required."
            );
        }

        // --------------------------------------------------------
        // VALIDATE START DATE
        // --------------------------------------------------------

        if (startDate == null) {
            throw new IllegalArgumentException(
                    "Start date is required."
            );
        }

        // --------------------------------------------------------
        // NORMALIZE VALUES
        // --------------------------------------------------------

        leaveType = leaveType.trim();

        duration = duration.trim().toUpperCase();

        // ========================================================
        // TOTAL DAYS
        // ========================================================

        double totalDays;

        // ========================================================
        // FULL DAY
        // ========================================================

        if ("FULL_DAY".equals(duration)) {

            // End date is mandatory for full-day leave.
            if (endDate == null) {
                throw new IllegalArgumentException(
                        "End date is required for full-day leave."
                );
            }

            // End date cannot be before start date.
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException(
                        "End date cannot be before start date."
                );
            }

            // Full-day leave does not use a half-day session.
            halfDaySession = null;

            // ----------------------------------------------------
            // Calculate total days.
            // ----------------------------------------------------

            totalDays =
                    ChronoUnit.DAYS.between(
                            startDate,
                            endDate
                    ) + 1.0;
        }

        // ========================================================
        // HALF DAY
        // ========================================================

        else if ("HALF_DAY".equals(duration)) {

            // ----------------------------------------------------
            // Half-day leave is ALWAYS one date.
            // ----------------------------------------------------

            endDate = startDate;

            // ----------------------------------------------------
            // Half-day session is mandatory.
            // ----------------------------------------------------

            if (halfDaySession == null ||
                    halfDaySession.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Half-day session is required."
                );
            }

            halfDaySession =
                    halfDaySession.trim().toUpperCase();

            // ----------------------------------------------------
            // Validate session.
            // ----------------------------------------------------

            if (!"FIRST_HALF".equals(halfDaySession)
                    && !"SECOND_HALF".equals(halfDaySession)) {

                throw new IllegalArgumentException(
                        "Invalid half-day session."
                );
            }

            // ----------------------------------------------------
            // Half day = 0.5 day.
            // ----------------------------------------------------

            totalDays = 0.5;
        }

        // ========================================================
        // INVALID DURATION
        // ========================================================

        else {

            throw new IllegalArgumentException(
                    "Invalid leave duration."
            );
        }

        // ========================================================
        // CREATE REQUEST
        // ========================================================

        LeaveRequest request =
                new LeaveRequest();

        // --------------------------------------------------------
        // Employee
        // --------------------------------------------------------

        request.setEmployee(employee);

        // --------------------------------------------------------
        // Leave type
        // --------------------------------------------------------

        request.setLeaveType(
                leaveType
        );

        // --------------------------------------------------------
        // Duration
        // --------------------------------------------------------

        request.setDuration(
                duration
        );

        // --------------------------------------------------------
        // Half-day session
        // --------------------------------------------------------

        request.setHalfDaySession(
                halfDaySession
        );

        // --------------------------------------------------------
        // Dates
        // --------------------------------------------------------

        request.setStartDate(
                startDate
        );

        request.setEndDate(
                endDate
        );

        // --------------------------------------------------------
        // Reason
        // --------------------------------------------------------

        request.setReason(
                reason == null
                        ? null
                        : reason.trim()
        );

        // --------------------------------------------------------
        // Status
        // --------------------------------------------------------

        request.setStatus(
                "PENDING"
        );

        // --------------------------------------------------------
        // Review information
        // --------------------------------------------------------

        request.setReviewNote(
                null
        );

        request.setReviewedAt(
                null
        );

        // --------------------------------------------------------
        // Created time
        // --------------------------------------------------------

        request.setCreatedAt(
                LocalDateTime.now()
        );

        // ========================================================
        // TOTAL DAYS
        // ========================================================

        request.setTotalDays(
                totalDays
        );

        // ========================================================
        // SAVE
        // ========================================================

        return leaveRequestRepository.save(
                request
        );
    }

    // ============================================================
    // EMPLOYEE REQUESTS
    // ============================================================

    public List<LeaveRequest> getEmployeeRequests(
            Employee employee) {

        return leaveRequestRepository
                .findByEmployeeOrderByCreatedAtDesc(
                        employee
                );
    }

    // ============================================================
    // ADMIN - ALL REQUESTS
    // ============================================================

    public List<LeaveRequest> getAllRequests() {

        return leaveRequestRepository
                .findAllByOrderByCreatedAtDesc();
    }

    // ============================================================
    // GET REQUEST
    // ============================================================

    public LeaveRequest getRequest(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Leave request ID is required."
            );
        }

        return leaveRequestRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Leave request not found."
                        )
                );
    }

    // ============================================================
    // APPROVE
    // ============================================================

    @Transactional
    public LeaveRequest approveLeave(
            Long id,
            String reviewNote) {

        LeaveRequest request =
                getRequest(id);

        // --------------------------------------------------------
        // Only pending requests can be approved.
        // --------------------------------------------------------

        if (!"PENDING".equals(request.getStatus())) {

            throw new IllegalStateException(
                    "Only pending leave requests can be approved."
            );
        }

        // --------------------------------------------------------
        // Update status.
        // --------------------------------------------------------

        request.setStatus(
                "APPROVED"
        );

        // --------------------------------------------------------
        // Save review note.
        // --------------------------------------------------------

        request.setReviewNote(
                reviewNote == null ||
                        reviewNote.trim().isEmpty()
                        ? null
                        : reviewNote.trim()
        );

        // --------------------------------------------------------
        // Save review time.
        // --------------------------------------------------------

        request.setReviewedAt(
                LocalDateTime.now()
        );

        // --------------------------------------------------------
        // SAVE LEAVE REQUEST
        // --------------------------------------------------------

        LeaveRequest savedRequest =
                leaveRequestRepository.save(
                        request
                );

        // ========================================================
        // CREATE EMPLOYEE NOTIFICATION
        // ========================================================

        String leaveName =
                formatLeaveType(
                        request.getLeaveType()
                );

        String message =
                "Your " +
                leaveName +
                " leave request has been approved.";

        if (request.getReviewNote() != null &&
                !request.getReviewNote().trim().isEmpty()) {

            message +=
                    " Review note: " +
                    request.getReviewNote().trim();
        }

        notificationService.createNotification(
                request.getEmployee(),
                "Leave Approved",
                message,
                "LEAVE_APPROVED"
        );

        return savedRequest;
    }

    // ============================================================
    // REJECT
    // ============================================================

    @Transactional
    public LeaveRequest rejectLeave(
            Long id,
            String reviewNote) {

        LeaveRequest request =
                getRequest(id);

        // --------------------------------------------------------
        // Only pending requests can be rejected.
        // --------------------------------------------------------

        if (!"PENDING".equals(request.getStatus())) {

            throw new IllegalStateException(
                    "Only pending leave requests can be rejected."
            );
        }

        // --------------------------------------------------------
        // Update status.
        // --------------------------------------------------------

        request.setStatus(
                "REJECTED"
        );

        // --------------------------------------------------------
        // Save review note.
        // --------------------------------------------------------

        request.setReviewNote(
                reviewNote == null ||
                        reviewNote.trim().isEmpty()
                        ? null
                        : reviewNote.trim()
        );

        // --------------------------------------------------------
        // Save review time.
        // --------------------------------------------------------

        request.setReviewedAt(
                LocalDateTime.now()
        );

        // --------------------------------------------------------
        // SAVE LEAVE REQUEST
        // --------------------------------------------------------

        LeaveRequest savedRequest =
                leaveRequestRepository.save(
                        request
                );

        // ========================================================
        // CREATE EMPLOYEE NOTIFICATION
        // ========================================================

        String leaveName =
                formatLeaveType(
                        request.getLeaveType()
                );

        String message =
                "Your " +
                leaveName +
                " leave request has been rejected.";

        if (request.getReviewNote() != null &&
                !request.getReviewNote().trim().isEmpty()) {

            message +=
                    " Review note: " +
                    request.getReviewNote().trim();
        }

        notificationService.createNotification(
                request.getEmployee(),
                "Leave Rejected",
                message,
                "LEAVE_REJECTED"
        );

        return savedRequest;
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Transactional
    public void deleteLeave(Long id) {

        LeaveRequest request =
                getRequest(id);

        leaveRequestRepository.delete(
                request
        );
    }

    // ============================================================
    // EMPLOYEE COUNTS
    // ============================================================

    public long countApprovedLeaves(
            Employee employee) {

        return leaveRequestRepository
                .countByEmployeeAndStatus(
                        employee,
                        "APPROVED"
                );
    }

    public long countPendingLeaves(
            Employee employee) {

        return leaveRequestRepository
                .countByEmployeeAndStatus(
                        employee,
                        "PENDING"
                );
    }

    public long countRejectedLeaves(
            Employee employee) {

        return leaveRequestRepository
                .countByEmployeeAndStatus(
                        employee,
                        "REJECTED"
                );
    }

    // ============================================================
    // ADMIN COUNTS
    // ============================================================

    public long countAllPendingLeaves() {

        return leaveRequestRepository
                .countByStatus(
                        "PENDING"
                );
    }

    public long countAllApprovedLeaves() {

        return leaveRequestRepository
                .countByStatus(
                        "APPROVED"
                );
    }

    public long countAllRejectedLeaves() {

        return leaveRequestRepository
                .countByStatus(
                        "REJECTED"
                );
    }

    // ============================================================
    // CHECK EMPLOYEE ON LEAVE TODAY
    // ============================================================

    public boolean isEmployeeOnLeaveToday(
            Employee employee) {

        LocalDate today =
                LocalDate.now();

        return leaveRequestRepository
                .existsByEmployeeAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee,
                        "APPROVED",
                        today,
                        today
                );
    }

    // ============================================================
    // COUNT EMPLOYEES ON LEAVE TODAY
    // ============================================================

    public long countEmployeesOnLeaveToday() {

        LocalDate today =
                LocalDate.now();

        return leaveRequestRepository
                .countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        "APPROVED",
                        today,
                        today
                );
    }

    // ============================================================
    // FORMAT LEAVE TYPE
    // ============================================================

    private String formatLeaveType(
            String leaveType) {

        if (leaveType == null ||
                leaveType.trim().isEmpty()) {

            return "leave";
        }

        String value =
                leaveType
                        .trim()
                        .toUpperCase();

        return switch (value) {

            case "SICK" ->
                    "sick";

            case "CASUAL" ->
                    "casual";

            case "VACATION" ->
                    "vacation";

            case "EMERGENCY" ->
                    "emergency";

            case "WORK_FROM_HOME" ->
                    "work from home";

            default ->
                    leaveType
                            .trim()
                            .toLowerCase();
        };
    }
}

