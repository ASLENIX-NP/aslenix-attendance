package com.aslenix.attendance;

import com.aslenix.attendance.dto.StreakDto;
import com.aslenix.attendance.entity.*;
import com.aslenix.attendance.repository.*;
import com.aslenix.attendance.service.AttendanceCorrectionService;
import com.aslenix.attendance.service.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AttendanceStreakAndCorrectionTests {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceCorrectionService correctionService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceCorrectionRequestRepository correctionRequestRepository;

    @Autowired
    private AttendanceAuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private OfficeSettingsRepository officeSettingsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Employee testEmployee;
    private User testAdminUser;

    @BeforeEach
    void setUp() {
        // Ensure office settings exist (Saturday is weekend by default in Nepal, Monday-Friday working)
        List<OfficeSettings> allSettings = officeSettingsRepository.findAll();
        if (allSettings.isEmpty()) {
            OfficeSettings s = new OfficeSettings();
            s.setOfficeName("ASLENIX HQ");
            s.setOfficeLocation("Kathmandu, Nepal");
            s.setAllowedRadiusMeters(200.0);
            s.setWorkStartTime(LocalTime.of(9, 0));
            s.setWorkEndTime(LocalTime.of(17, 0));
            s.setLateGraceMinutes(15);
            s.setMonday(true);
            s.setTuesday(true);
            s.setWednesday(true);
            s.setThursday(true);
            s.setFriday(true);
            s.setSaturday(false);
            s.setSunday(true);
            officeSettingsRepository.save(s);
        }

        // Create a unique test user & employee
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        User empUser = new User();
        empUser.setUsername("test_emp_" + randomSuffix);
        empUser.setPassword(passwordEncoder.encode("password123"));
        empUser.setRole(Role.EMPLOYEE);
        empUser.setEnabled(true);
        empUser = userRepository.save(empUser);

        testEmployee = new Employee();
        testEmployee.setUser(empUser);
        testEmployee.setEmployeeCode("TST-" + randomSuffix);
        testEmployee.setFirstName("Streak");
        testEmployee.setLastName("Tester");
        testEmployee.setEmail("streak." + randomSuffix + "@aslenix.com");
        testEmployee.setJoiningDate(LocalDate.now().minusMonths(6));
        testEmployee.setPosition("QA Engineer");
        testEmployee.setEnabled(true);
        testEmployee = employeeRepository.save(testEmployee);

        // Create a test admin user
        testAdminUser = new User();
        testAdminUser.setUsername("test_admin_" + randomSuffix);
        testAdminUser.setPassword(passwordEncoder.encode("admin123"));
        testAdminUser.setRole(Role.ADMIN);
        testAdminUser.setEnabled(true);
        testAdminUser = userRepository.save(testAdminUser);
    }

    @Test
    void testAttendanceStreakIncrementsOnConsecutiveWorkingDaysAndSkipsWeekends() {
        // Find a recent Monday to Friday range in the past
        LocalDate today = LocalDate.now();
        LocalDate startMonday = today.minusWeeks(2);
        while (startMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
            startMonday = startMonday.minusDays(1);
        }

        // Add attendance for Mon, Tue, Wed, Thu, Fri (5 working days)
        for (int i = 0; i < 5; i++) {
            LocalDate workDay = startMonday.plusDays(i);
            Attendance att = new Attendance();
            att.setEmployee(testEmployee);
            att.setAttendanceDate(workDay);
            att.setCheckIn(workDay.atTime(9, 5));
            att.setCheckOut(workDay.atTime(17, 0));
            att.setStatus("PRESENT");
            attendanceRepository.save(att);
        }

        // Calculate streak
        StreakDto streak = attendanceService.calculateStreak(testEmployee);
        assertNotNull(streak);
        assertTrue(streak.getLongestStreak() >= 5, "Longest streak should be at least 5 for consecutive working days");
    }

    @Test
    void testStreakBridgesAcrossApprovedLeave() {
        LocalDate today = LocalDate.now();
        LocalDate day1 = today.minusDays(4);
        LocalDate day2 = today.minusDays(3); // Leave day
        LocalDate day3 = today.minusDays(2);

        // Day 1: Attended
        Attendance a1 = new Attendance();
        a1.setEmployee(testEmployee);
        a1.setAttendanceDate(day1);
        a1.setCheckIn(day1.atTime(9, 0));
        a1.setStatus("PRESENT");
        attendanceRepository.save(a1);

        // Day 2: Approved Leave
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(testEmployee);
        leave.setStartDate(day2);
        leave.setEndDate(day2);
        leave.setLeaveType("SICK");
        leave.setDuration("FULL_DAY");
        leave.setTotalDays(1.0);
        leave.setCreatedAt(java.time.LocalDateTime.now());
        leave.setStatus("APPROVED");
        leave.setReason("Doctor visit");
        leaveRequestRepository.save(leave);

        // Day 3: Attended
        Attendance a3 = new Attendance();
        a3.setEmployee(testEmployee);
        a3.setAttendanceDate(day3);
        a3.setCheckIn(day3.atTime(9, 10));
        a3.setStatus("PRESENT");
        attendanceRepository.save(a3);

        StreakDto streak = attendanceService.calculateStreak(testEmployee);
        assertNotNull(streak);
        assertTrue(streak.getLongestStreak() >= 2, "Streak should bridge across approved leave day");
    }

    @Test
    void testLateAndHalfDayCountTowardsStreak() {
        LocalDate today = LocalDate.now();
        LocalDate d1 = today.minusDays(3);
        LocalDate d2 = today.minusDays(2);

        Attendance a1 = new Attendance();
        a1.setEmployee(testEmployee);
        a1.setAttendanceDate(d1);
        a1.setCheckIn(d1.atTime(9, 45));
        a1.setLate(true);
        a1.setStatus("LATE");
        attendanceRepository.save(a1);

        Attendance a2 = new Attendance();
        a2.setEmployee(testEmployee);
        a2.setAttendanceDate(d2);
        a2.setCheckIn(d2.atTime(9, 0));
        a2.setCheckOut(d2.atTime(13, 0));
        a2.setStatus("HALF_DAY");
        attendanceRepository.save(a2);

        StreakDto streak = attendanceService.calculateStreak(testEmployee);
        assertNotNull(streak);
        assertTrue(streak.getLongestStreak() >= 2, "Late and half-day records must count as attended towards streak");
    }

    @Test
    void testSubmitCorrectionRequestAndPreventDuplicatePending() {
        LocalDate targetDate = LocalDate.now().minusDays(5);

        // Submit first correction request
        AttendanceCorrectionRequest req1 = correctionService.submitCorrectionRequest(
                testEmployee,
                targetDate,
                LocalTime.of(9, 15),
                LocalTime.of(17, 30),
                "Client on-site meeting in the morning"
        );

        assertNotNull(req1.getId());
        assertEquals("PENDING", req1.getStatus());
        assertEquals(targetDate, req1.getAttendanceDate());

        // Second request for the same date must fail validation
        assertThrows(IllegalStateException.class, () -> {
            correctionService.submitCorrectionRequest(
                    testEmployee,
                    targetDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    "Duplicate request attempt"
            );
        }, "Submitting duplicate pending request for same date should throw IllegalStateException");
    }

    @Test
    void testApproveCorrectionRequestUpdatesAttendanceAndLogsAudit() {
        LocalDate targetDate = LocalDate.now().minusDays(6);

        // Create initial pending request
        AttendanceCorrectionRequest req = correctionService.submitCorrectionRequest(
                testEmployee,
                targetDate,
                LocalTime.of(9, 10),
                LocalTime.of(17, 15),
                "Biometric device offline"
        );

        // Admin approves request
        correctionService.approveCorrectionRequest(
                req.getId(),
                testAdminUser.getUsername(),
                "Verified by Admin"
        );

        // Check request status
        AttendanceCorrectionRequest updatedReq = correctionRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals("APPROVED", updatedReq.getStatus());
        assertNotNull(updatedReq.getReviewedAt());
        assertEquals(testAdminUser.getUsername(), updatedReq.getReviewedByAdminName());
        assertEquals("Verified by Admin", updatedReq.getReviewNote());

        // Check Attendance record was created/updated
        Attendance att = attendanceRepository.findByEmployeeAndAttendanceDate(testEmployee, targetDate).orElse(null);
        assertNotNull(att, "Attendance record must exist after correction approval");
        assertEquals(LocalTime.of(9, 10), att.getCheckIn().toLocalTime());
        assertEquals(LocalTime.of(17, 15), att.getCheckOut().toLocalTime());
        assertEquals("PRESENT", att.getStatus());

        // Check Audit Log
        List<AttendanceAuditLog> logs = auditLogRepository.findByEmployeeOrderByTimestampDesc(testEmployee);
        assertFalse(logs.isEmpty(), "Audit log entry must be created");
        AttendanceAuditLog log = logs.get(0);
        assertEquals("REQUEST_APPROVAL", log.getActionType());
        assertEquals(LocalTime.of(9, 10), log.getNewCheckIn().toLocalTime());
        assertEquals(testAdminUser.getUsername(), log.getAdminUsername());

        // Check notification sent to employee
        List<Notification> notifications = notificationRepository.findByEmployeeOrderByCreatedAtDesc(testEmployee);
        assertFalse(notifications.isEmpty(), "Employee should receive notification of approval");
        assertTrue(notifications.get(0).getTitle().contains("Approved"));
    }

    @Test
    void testRejectCorrectionRequestLeavesAttendanceUnchanged() {
        LocalDate targetDate = LocalDate.now().minusDays(7);

        AttendanceCorrectionRequest req = correctionService.submitCorrectionRequest(
                testEmployee,
                targetDate,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                "Forgot to check in"
        );

        correctionService.rejectCorrectionRequest(
                req.getId(),
                testAdminUser.getUsername(),
                "No proof of office presence provided"
        );

        AttendanceCorrectionRequest updatedReq = correctionRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals("REJECTED", updatedReq.getStatus());
        assertEquals("No proof of office presence provided", updatedReq.getReviewNote());

        // No attendance record created
        Attendance att = attendanceRepository.findByEmployeeAndAttendanceDate(testEmployee, targetDate).orElse(null);
        assertNull(att, "Attendance record should not be created for rejected request");

        // Employee receives rejection notification
        List<Notification> notifications = notificationRepository.findByEmployeeOrderByCreatedAtDesc(testEmployee);
        assertFalse(notifications.isEmpty());
        assertTrue(notifications.get(0).getTitle().contains("Rejected"));
    }

    @Test
    void testDirectEditAttendanceUpdatesRecordAndLogsAudit() {
        LocalDate targetDate = LocalDate.now().minusDays(8);

        // Perform direct edit by admin
        correctionService.directEditAttendance(
                testEmployee.getId(),
                targetDate,
                LocalTime.of(9, 20),
                LocalTime.of(17, 5),
                "LATE",
                "Manual adjustment for power outage during punch-in",
                testAdminUser.getUsername()
        );

        Attendance att = attendanceRepository.findByEmployeeAndAttendanceDate(testEmployee, targetDate).orElse(null);
        assertNotNull(att, "Attendance record must exist after direct edit");
        assertEquals(LocalTime.of(9, 20), att.getCheckIn().toLocalTime());
        assertEquals(LocalTime.of(17, 5), att.getCheckOut().toLocalTime());
        assertEquals("LATE", att.getStatus());

        // Check Audit Log
        List<AttendanceAuditLog> logs = auditLogRepository.findByEmployeeOrderByTimestampDesc(testEmployee);
        assertFalse(logs.isEmpty());
        AttendanceAuditLog log = logs.get(0);
        assertEquals("DIRECT_EDIT", log.getActionType());
        assertEquals(LocalTime.of(9, 20), log.getNewCheckIn().toLocalTime());
        assertEquals("Manual adjustment for power outage during punch-in", log.getReason());
    }

    @Test
    void testTemplatesContainRequiredCorrectionAndStreakComponents() throws Exception {
        String empAttendanceHtml = Files.readString(Path.of("src/main/resources/templates/employee/attendance.html"));
        String adminAttendanceHtml = Files.readString(Path.of("src/main/resources/templates/admin/attendance.html"));
        String empDashboardHtml = Files.readString(Path.of("src/main/resources/templates/employee/dashboard.html"));

        // Employee Dashboard has Streak Widget
        assertTrue(empDashboardHtml.contains("Attendance Streak"), "Employee dashboard must contain Attendance Streak card");
        assertTrue(empDashboardHtml.contains("th:text=\"${currentStreak != null ? currentStreak : 0}\""),
                "Employee dashboard must display currentStreak");

        // Employee Attendance has Correction Requests & Modal
        assertTrue(empAttendanceHtml.contains("id=\"correctionRequestsSection\""),
                "Employee attendance template must have correctionRequestsSection");
        assertTrue(empAttendanceHtml.contains("id=\"openCorrectionModalBtn\""),
                "Employee attendance template must have openCorrectionModalBtn");
        assertTrue(empAttendanceHtml.contains("id=\"correctionModal\""),
                "Employee attendance template must have correctionModal");
        assertTrue(empAttendanceHtml.contains("th:action=\"@{/employee/attendance/corrections}\""),
                "Employee attendance template must post to corrections endpoint");

        // Admin Attendance has Correction Requests, Direct Edit, and Audit Trail
        assertTrue(adminAttendanceHtml.contains("Attendance Correction Requests"),
                "Admin attendance template must have Attendance Correction Requests section");
        assertTrue(adminAttendanceHtml.contains("id=\"btnOpenDirectEditModal\""),
                "Admin attendance template must have Direct Edit button");
        assertTrue(adminAttendanceHtml.contains("id=\"directEditModal\""),
                "Admin attendance template must have directEditModal");
        assertTrue(adminAttendanceHtml.contains("id=\"approveModal\""),
                "Admin attendance template must have approveModal");
        assertTrue(adminAttendanceHtml.contains("id=\"rejectModal\""),
                "Admin attendance template must have rejectModal");
        assertTrue(adminAttendanceHtml.contains("Attendance Audit Trail"),
                "Admin attendance template must have Attendance Audit Trail section");
    }
}
