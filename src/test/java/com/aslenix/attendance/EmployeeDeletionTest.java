package com.aslenix.attendance;

import com.aslenix.attendance.controller.EmployeeController;
import com.aslenix.attendance.entity.*;
import com.aslenix.attendance.repository.*;
import com.aslenix.attendance.service.EmployeeDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeeDeletionTest {

    @Autowired
    private EmployeeDeletionService employeeDeletionService;

    @Autowired
    private EmployeeController employeeController;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private TaskAssignmentHistoryRepository taskAssignmentHistoryRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Department department;

    @BeforeEach
    void setUp() {
        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering");
            d.setActive(true);
            return departmentRepository.save(d);
        });
    }

    private Employee createEmployee(String username, String code, String email) {
        User user = new User(username, passwordEncoder.encode("Secret123"), Role.EMPLOYEE);
        user = userRepository.save(user);

        Employee emp = new Employee();
        emp.setFirstName("First_" + username);
        emp.setLastName("Last_" + username);
        emp.setEmail(email);
        emp.setEmployeeCode(code);
        emp.setJoiningDate(LocalDate.now());
        emp.setPosition("Developer");
        emp.setEnabled(true);
        emp.setDepartment(department);
        emp.setUser(user);
        return employeeRepository.save(emp);
    }

    @Test
    void testDeleteEmployeeWithSharedTasksAndSubtasks() {
        Employee empA = createEmployee("del_user_a", "ASL-DEL-01", "del_a@test.com");
        Employee empB = createEmployee("del_user_b", "ASL-DEL-02", "del_b@test.com");
        Long userAId = empA.getUser().getId();
        Long empAId = empA.getId();

        // 1. Create a task assigned to both empA and empB, with empA as primary
        Task task = new Task();
        task.setTitle("Migration Project Task");
        task.setTaskCode("TASK-DEL-99");
        task.setEmployee(empA);
        task.setAssignees(new HashSet<>(Set.of(empA, empB)));
        task.setTeamLeads(new HashSet<>(Set.of(empB)));
        task.setWeeksRequired(2);
        task.setStatus("IN_PROGRESS");
        task.setProgress(25);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task = taskRepository.save(task);

        // 2. Create subtask assigned to empA
        TaskAssignment subtask = new TaskAssignment();
        subtask.setTask(task);
        subtask.setWeekNumber(1);
        subtask.setSubtaskNumber(1);
        subtask.setTitle("Design architecture");
        subtask.setAssignee(empA);
        subtask.setStatus("IN_PROGRESS");
        subtask.setProgress(50);
        subtask.setCreatedAt(LocalDateTime.now());
        subtask.setUpdatedAt(LocalDateTime.now());
        subtask = taskAssignmentRepository.save(subtask);

        // 3. Create subtask history updated by empA
        TaskAssignmentHistory history = new TaskAssignmentHistory(
                subtask, 0, 50, "Initial mock progress", empA);
        history = taskAssignmentHistoryRepository.save(history);

        // 4. Create attendance for empA
        Attendance attendance = new Attendance();
        attendance.setEmployee(empA);
        attendance.setAttendanceDate(LocalDate.now());
        attendance.setCheckIn(LocalDateTime.now());
        attendance.setStatus("PRESENT");
        attendanceRepository.save(attendance);

        // 5. Create leave request for empA
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(empA);
        leave.setLeaveType("SICK");
        leave.setDuration("FULL_DAY");
        leave.setTotalDays(1.0);
        leave.setStartDate(LocalDate.now());
        leave.setEndDate(LocalDate.now());
        leave.setStatus("PENDING");
        leave.setReason("Doctor visit");
        leave.setCreatedAt(LocalDateTime.now());
        leaveRequestRepository.save(leave);

        // 6. Create notification for empA
        Notification notif = new Notification();
        notif.setEmployee(empA);
        notif.setTitle("Welcome");
        notif.setMessage("Welcome to Aslenix");
        notif.setType("INFO");
        notif.setRead(false);
        notif.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notif);

        // Execute deletion via controller
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = employeeController.deleteEmployee(empAId, redirectAttributes);

        assertEquals("redirect:/admin/employees", redirect);
        assertNotNull(redirectAttributes.getFlashAttributes().get("successMessage"),
                "Deletion should produce a successMessage flash attribute");

        // Verify Employee A is removed
        assertFalse(employeeRepository.findById(empAId).isPresent(), "Employee A must be deleted");
        assertFalse(userRepository.findById(userAId).isPresent(), "User A must be deleted");

        // Verify Employee B is intact
        assertTrue(employeeRepository.findById(empB.getId()).isPresent(), "Employee B must still exist");

        // Verify Task is preserved, primary employee was promoted to remaining assignee (empB)
        Task remainingTask = taskRepository.findById(task.getId()).orElseThrow();
        assertNotNull(remainingTask);
        assertEquals(empB.getId(), remainingTask.getEmployee().getId(), "Primary assignee should be promoted to remaining assignee empB");

        // Verify subtask is preserved, assignee set to null
        TaskAssignment remainingSubtask = taskAssignmentRepository.findById(subtask.getId()).orElseThrow();
        assertNull(remainingSubtask.getAssignee(), "Subtask assignee should be set to null");

        // Verify history record is preserved, updated_by set to null
        TaskAssignmentHistory remainingHistory = taskAssignmentHistoryRepository.findById(history.getId()).orElseThrow();
        assertNull(remainingHistory.getUpdatedBy(), "History updatedBy should be set to null");
    }

    @Test
    void testDeleteSoleAssigneeEmployeeWithTask() {
        Employee empC = createEmployee("del_user_c", "ASL-DEL-03", "del_c@test.com");
        Long empCId = empC.getId();

        // Task with empC as sole assignee
        Task task = new Task();
        task.setTitle("Sole Assignee Task");
        task.setTaskCode("TASK-DEL-SOLE");
        task.setEmployee(empC);
        task.setAssignees(new HashSet<>(Set.of(empC)));
        task.setWeeksRequired(1);
        task.setStatus("TODO");
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task = taskRepository.save(task);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = employeeController.deleteEmployee(empCId, redirectAttributes);

        assertEquals("redirect:/admin/employees", redirect);
        assertNotNull(redirectAttributes.getFlashAttributes().get("successMessage"));

        assertFalse(employeeRepository.findById(empCId).isPresent(), "Employee C must be deleted");

        Task remainingTask = taskRepository.findById(task.getId()).orElseThrow();
        // Since empC was sole assignee, employee should be null (detached)
        assertNull(remainingTask.getEmployee(), "Sole assignee task employee should be null or detached");
    }

    @Test
    void testDeleteNonExistentEmployeeReturnsError() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = employeeController.deleteEmployee(999999L, redirectAttributes);

        assertEquals("redirect:/admin/employees", redirect);
        assertNotNull(redirectAttributes.getFlashAttributes().get("errorMessage"),
                "Deleting nonexistent employee should return errorMessage");
    }
}
