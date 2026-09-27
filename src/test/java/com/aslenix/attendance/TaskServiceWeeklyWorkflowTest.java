package com.aslenix.attendance;

import com.aslenix.attendance.dto.SubtaskInputDto;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.entity.TaskAssignment;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.TaskAssignmentHistoryRepository;
import com.aslenix.attendance.repository.TaskAssignmentRepository;
import com.aslenix.attendance.repository.TaskCommentRepository;
import com.aslenix.attendance.repository.TaskRepository;
import com.aslenix.attendance.service.NotificationService;
import com.aslenix.attendance.service.TaskService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskServiceWeeklyWorkflowTest {

    private TaskRepository taskRepository;
    private TaskAssignmentRepository taskAssignmentRepository;
    private TaskAssignmentHistoryRepository taskAssignmentHistoryRepository;
    private TaskCommentRepository taskCommentRepository;
    private EmployeeRepository employeeRepository;
    private NotificationService notificationService;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        taskAssignmentRepository = mock(TaskAssignmentRepository.class);
        taskAssignmentHistoryRepository = mock(TaskAssignmentHistoryRepository.class);
        taskCommentRepository = mock(TaskCommentRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        notificationService = mock(NotificationService.class);

        taskService = new TaskService(
                taskRepository,
                taskAssignmentRepository,
                taskAssignmentHistoryRepository,
                taskCommentRepository,
                notificationService,
                employeeRepository
        );
    }

    @Test
    void testCreateTaskWithWeeksRequiredGeneratesExactSubtasks() {
        Employee emp = new Employee();
        ReflectionTestUtils.setField(emp, "id", 10L);
        emp.setFirstName("John");

        List<SubtaskInputDto> subtasks = List.of(
                new SubtaskInputDto(1, "Week 1", "Scope 1", 10L),
                new SubtaskInputDto(2, "Week 2", "Scope 2", 10L),
                new SubtaskInputDto(3, "Week 3", "Scope 3", 10L)
        );

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(emp));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 1L);
            return t;
        });
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenAnswer(inv -> {
            TaskAssignment a = inv.getArgument(0);
            return a;
        });

        Task created = taskService.createTask(
                "Website Redesign",
                "New corporate portal",
                "LARGE",
                "HIGH",
                3,
                List.of(emp),
                List.of(emp),
                subtasks
        );

        assertNotNull(created);
        assertEquals(3, created.getWeeksRequired());
        assertEquals("HIGH", created.getPriority());
        assertEquals("LARGE", created.getComplexity());
        assertEquals(0, created.getProgress());
        assertEquals("TODO", created.getStatus());

        // Verify save of 3 subtasks
        verify(taskAssignmentRepository, times(3)).save(any(TaskAssignment.class));
    }

    @Test
    void testEmptySubtaskDescriptionThrowsException() {
        List<SubtaskInputDto> subtasks = List.of(
                new SubtaskInputDto(1, "Week 1", "", null)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                taskService.createTask(
                        "Test Task",
                        "Desc",
                        "MEDIUM",
                        "MEDIUM",
                        1,
                        List.of(),
                        List.of(),
                        subtasks
                )
        );

        assertTrue(ex.getMessage().contains("mandatory"));
    }

    @Test
    void testDirectTaskProgressEditIsRejected() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () ->
                taskService.updateProgress(1L, null, 50)
        );
        assertTrue(ex.getMessage().contains("manually"));
    }

    @Test
    void testSubtaskProgressRequiresMandatoryNote() {
        TaskAssignment assignment = new TaskAssignment();
        ReflectionTestUtils.setField(assignment, "id", 100L);
        assignment.setProgress(0);

        when(taskAssignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                taskService.updateAssignmentProgress(100L, 50, "   ", null)
        );
        assertTrue(ex.getMessage().contains("mandatory"));
    }

    @Test
    void testSubtaskProgressUpdatesParentTaskAutomaticProgress() {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", 1L);
        task.setWeeksRequired(2);
        task.setStatus("TODO");

        TaskAssignment a1 = new TaskAssignment();
        ReflectionTestUtils.setField(a1, "id", 101L);
        a1.setTask(task);
        a1.setProgress(0);

        TaskAssignment a2 = new TaskAssignment();
        ReflectionTestUtils.setField(a2, "id", 102L);
        a2.setTask(task);
        a2.setProgress(0);

        task.setAssignments(new ArrayList<>(List.of(a1, a2)));

        when(taskAssignmentRepository.findById(101L)).thenReturn(Optional.of(a1));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Update a1 to 50% with note
        taskService.updateAssignmentProgress(101L, 50, "Finished initial setup", null);

        // Average should be (50 + 0) / 2 = 25%
        assertEquals(50, a1.getProgress());
        assertEquals("IN_PROGRESS", a1.getStatus());
        assertEquals(25, task.getProgress());
        assertEquals("IN_PROGRESS", task.getStatus());
    }

    @Test
    void testSubmitForReviewRequiresHundredPercent() {
        TaskAssignment a = new TaskAssignment();
        ReflectionTestUtils.setField(a, "id", 105L);
        a.setProgress(90);

        when(taskAssignmentRepository.findById(105L)).thenReturn(Optional.of(a));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                taskService.submitAssignmentForReview(105L, "Done almost all", null)
        );
        assertTrue(ex.getMessage().contains("100%"));
    }

    @Test
    void testDeclineSubtaskRequiresMandatoryReason() {
        TaskAssignment a = new TaskAssignment();
        ReflectionTestUtils.setField(a, "id", 106L);

        when(taskAssignmentRepository.findById(106L)).thenReturn(Optional.of(a));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                taskService.declineAssignment(106L, "   ", null)
        );
        assertTrue(ex.getMessage().contains("mandatory"));
    }

    @Test
    void testApprovingAllSubtasksLocksParentTask() {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", 1L);
        task.setWeeksRequired(2);
        task.setStatus("READY_FOR_REVIEW");

        TaskAssignment a1 = new TaskAssignment();
        ReflectionTestUtils.setField(a1, "id", 101L);
        a1.setTask(task);
        a1.setProgress(100);
        a1.setStatus("APPROVED");
        a1.setLocked(true);

        TaskAssignment a2 = new TaskAssignment();
        ReflectionTestUtils.setField(a2, "id", 102L);
        a2.setTask(task);
        a2.setProgress(100);
        a2.setStatus("READY_FOR_REVIEW");

        task.setAssignments(new ArrayList<>(List.of(a1, a2)));

        when(taskAssignmentRepository.findById(102L)).thenReturn(Optional.of(a2));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.approveAssignment(102L, null);

        assertTrue(a2.isLocked());
        assertEquals("APPROVED", a2.getStatus());
        assertNotNull(a2.getApprovedAt());

        // Parent task must now be approved & locked
        assertEquals(100, task.getProgress());
        assertEquals("APPROVED", task.getStatus());
        assertTrue(task.isLocked());
    }

    @Test
    void testSubtasksHaveIndependentProgressBarsAndValues() {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", 1L);
        task.setWeeksRequired(3);
        task.setStatus("TODO");

        TaskAssignment w1 = new TaskAssignment();
        ReflectionTestUtils.setField(w1, "id", 201L);
        w1.setSubtaskNumber(1);
        w1.setTitle("Week 1");
        w1.setTask(task);
        w1.setProgress(0);

        TaskAssignment w2 = new TaskAssignment();
        ReflectionTestUtils.setField(w2, "id", 202L);
        w2.setSubtaskNumber(2);
        w2.setTitle("Week 2");
        w2.setTask(task);
        w2.setProgress(0);

        TaskAssignment w3 = new TaskAssignment();
        ReflectionTestUtils.setField(w3, "id", 203L);
        w3.setSubtaskNumber(3);
        w3.setTitle("Week 3");
        w3.setTask(task);
        w3.setProgress(0);

        task.setAssignments(new ArrayList<>(List.of(w1, w2, w3)));

        when(taskAssignmentRepository.findById(201L)).thenReturn(Optional.of(w1));
        when(taskAssignmentRepository.findById(202L)).thenReturn(Optional.of(w2));
        when(taskAssignmentRepository.findById(203L)).thenReturn(Optional.of(w3));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Update Week 1 to 25%
        taskService.updateAssignmentProgress(201L, 25, "Week 1 progress update", null);
        assertEquals(25, w1.getProgress(), "Week 1 progress must be 25%");
        assertEquals(0, w2.getProgress(), "Week 2 progress must remain independent at 0%");
        assertEquals(0, w3.getProgress(), "Week 3 progress must remain independent at 0%");
        assertEquals(8, task.getProgress(), "Total task progress is round((25+0+0)/3) = 8%");

        // 2. Update Week 2 to 75%
        taskService.updateAssignmentProgress(202L, 75, "Week 2 major milestone", null);
        assertEquals(25, w1.getProgress(), "Week 1 progress must not be affected by Week 2 and remain 25%");
        assertEquals(75, w2.getProgress(), "Week 2 progress must be 75%");
        assertEquals(0, w3.getProgress(), "Week 3 progress must remain independent at 0%");
        assertEquals(33, task.getProgress(), "Total task progress is round((25+75+0)/3) = 33%");

        // 3. Update Week 3 to 100%
        taskService.updateAssignmentProgress(203L, 100, "Week 3 complete deliverables", null);
        assertEquals(25, w1.getProgress(), "Week 1 progress must strictly remain 25%");
        assertEquals(75, w2.getProgress(), "Week 2 progress must strictly remain 75%");
        assertEquals(100, w3.getProgress(), "Week 3 progress must be 100%");
        assertEquals(67, task.getProgress(), "Total task progress is round((25+75+100)/3) = 67%");
    }

    @Test
    void testUpdateTaskPreservesSubtaskProgressIndependently() {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", 1L);
        task.setTitle("Old Title");
        task.setWeeksRequired(2);
        task.setComplexity("MEDIUM");
        task.setPriority("MEDIUM");

        TaskAssignment w1 = new TaskAssignment();
        ReflectionTestUtils.setField(w1, "id", 301L);
        w1.setSubtaskNumber(1);
        w1.setTitle("Week 1 Initial");
        w1.setDescription("Desc 1");
        w1.setProgress(30);
        w1.setStatus("IN_PROGRESS");
        w1.setTask(task);

        TaskAssignment w2 = new TaskAssignment();
        ReflectionTestUtils.setField(w2, "id", 302L);
        w2.setSubtaskNumber(2);
        w2.setTitle("Week 2 Initial");
        w2.setDescription("Desc 2");
        w2.setProgress(80);
        w2.setStatus("IN_PROGRESS");
        w2.setTask(task);

        task.setAssignments(new ArrayList<>(List.of(w1, w2)));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Update task title and descriptions
        List<SubtaskInputDto> updatedSubtasks = List.of(
                new SubtaskInputDto(1, "Week 1 Updated Title", "New Scope 1", null),
                new SubtaskInputDto(2, "Week 2 Updated Title", "New Scope 2", null)
        );

        Task result = taskService.updateTask(
                1L,
                "New Project Title",
                "New Scope",
                "MEDIUM",
                "HIGH",
                2,
                List.of(),
                List.of(),
                updatedSubtasks
        );

        assertNotNull(result);
        assertEquals("New Project Title", result.getTitle());
        // Subtask 1 retains its 30% progress
        assertEquals(30, w1.getProgress());
        assertEquals("Week 1 Updated Title", w1.getTitle());
        // Subtask 2 retains its 80% progress
        assertEquals(80, w2.getProgress());
        assertEquals("Week 2 Updated Title", w2.getTitle());
        // Total progress is round((30 + 80) / 2) = 55%
        assertEquals(55, result.getProgress());
    }

    @Test
    void testEmployeeCannotUpdateSubtaskNotAssignedToThem() {
        com.aslenix.attendance.entity.User userA = new com.aslenix.attendance.entity.User();
        userA.setRole(com.aslenix.attendance.entity.Role.EMPLOYEE);
        Employee empA = new Employee();
        ReflectionTestUtils.setField(empA, "id", 10L);
        empA.setUser(userA);

        com.aslenix.attendance.entity.User userB = new com.aslenix.attendance.entity.User();
        userB.setRole(com.aslenix.attendance.entity.Role.EMPLOYEE);
        Employee empB = new Employee();
        ReflectionTestUtils.setField(empB, "id", 20L);
        empB.setUser(userB);

        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", 1L);

        TaskAssignment assignment = new TaskAssignment();
        ReflectionTestUtils.setField(assignment, "id", 501L);
        assignment.setTask(task);
        assignment.setAssignee(empA);
        assignment.setProgress(0);

        when(taskAssignmentRepository.findById(501L)).thenReturn(Optional.of(assignment));

        // Employee B attempts to update Employee A's subtask -> must be rejected
        SecurityException ex = assertThrows(SecurityException.class, () ->
                taskService.updateAssignmentProgress(501L, 40, "Unauthorized update attempt", empB)
        );
        assertTrue(ex.getMessage().contains("not assigned to you"));
    }

    @Test
    void testEmployeeCannotSubmitForReviewSubtaskNotAssignedToThem() {
        com.aslenix.attendance.entity.User userA = new com.aslenix.attendance.entity.User();
        userA.setRole(com.aslenix.attendance.entity.Role.EMPLOYEE);
        Employee empA = new Employee();
        ReflectionTestUtils.setField(empA, "id", 10L);
        empA.setUser(userA);

        com.aslenix.attendance.entity.User userB = new com.aslenix.attendance.entity.User();
        userB.setRole(com.aslenix.attendance.entity.Role.EMPLOYEE);
        Employee empB = new Employee();
        ReflectionTestUtils.setField(empB, "id", 20L);
        empB.setUser(userB);

        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", 1L);

        TaskAssignment assignment = new TaskAssignment();
        ReflectionTestUtils.setField(assignment, "id", 502L);
        assignment.setTask(task);
        assignment.setAssignee(empA);
        assignment.setProgress(100);

        when(taskAssignmentRepository.findById(502L)).thenReturn(Optional.of(assignment));

        // Employee B attempts to submit Employee A's subtask -> must be rejected
        SecurityException ex = assertThrows(SecurityException.class, () ->
                taskService.submitAssignmentForReview(502L, "Unauthorized submit attempt", empB)
        );
        assertTrue(ex.getMessage().contains("not assigned to you"));
    }

    @Test
    void testEmployeeCanUpdateSubtaskAssignedToThem() {
        com.aslenix.attendance.entity.User userA = new com.aslenix.attendance.entity.User();
        userA.setRole(com.aslenix.attendance.entity.Role.EMPLOYEE);
        Employee empA = new Employee();
        ReflectionTestUtils.setField(empA, "id", 10L);
        empA.setUser(userA);

        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", 1L);
        task.setAssignments(new ArrayList<>());

        TaskAssignment assignment = new TaskAssignment();
        ReflectionTestUtils.setField(assignment, "id", 503L);
        assignment.setTask(task);
        assignment.setAssignee(empA);
        assignment.setProgress(0);
        task.getAssignments().add(assignment);

        when(taskAssignmentRepository.findById(503L)).thenReturn(Optional.of(assignment));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        // Employee A updates their own subtask -> must succeed
        TaskAssignment updated = taskService.updateAssignmentProgress(503L, 60, "My work progress", empA);
        assertNotNull(updated);
        assertEquals(60, updated.getProgress());
    }
}
