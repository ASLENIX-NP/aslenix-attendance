package com.aslenix.attendance.service;

import com.aslenix.attendance.dto.SubtaskInputDto;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.entity.TaskAssignment;
import com.aslenix.attendance.entity.TaskAssignmentHistory;
import com.aslenix.attendance.entity.TaskComment;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.TaskAssignmentHistoryRepository;
import com.aslenix.attendance.repository.TaskAssignmentRepository;
import com.aslenix.attendance.repository.TaskCommentRepository;
import com.aslenix.attendance.repository.TaskRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskAssignmentHistoryRepository taskAssignmentHistoryRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;

    public TaskService(TaskRepository taskRepository,
                       TaskAssignmentRepository taskAssignmentRepository,
                       TaskAssignmentHistoryRepository taskAssignmentHistoryRepository,
                       TaskCommentRepository taskCommentRepository,
                       NotificationService notificationService,
                       EmployeeRepository employeeRepository) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskAssignmentHistoryRepository = taskAssignmentHistoryRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.notificationService = notificationService;
        this.employeeRepository = employeeRepository;
    }

    // ============================================================
    // GET ALL TASKS
    // ============================================================

    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return taskRepository.findAllByOrderByCreatedAtDesc();
    }

    // ============================================================
    // GET EMPLOYEE TASKS
    // ============================================================

    @Transactional(readOnly = true)
    public List<Task> getEmployeeTasks(Employee employee) {
        if (employee == null) {
            return List.of();
        }
        return taskRepository.findTasksForEmployee(employee);
    }

    // ============================================================
    // GET TASK BY ID
    // ============================================================

    @Transactional(readOnly = true)
    public Task getTask(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Task ID is required.");
        }
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
    }

    // ============================================================
    // CREATE TASK (WEEKLY SUBTASK STRUCTURE)
    // ============================================================

    public Task createTask(
            String title,
            String description,
            String complexity,
            String priority,
            Integer weeksRequired,
            List<Employee> teamLeads,
            List<Employee> assignees,
            List<SubtaskInputDto> subtasks
    ) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title is required.");
        }

        int weeks = (weeksRequired != null && weeksRequired > 0) ? weeksRequired : 1;

        if (subtasks == null || subtasks.isEmpty()) {
            throw new IllegalArgumentException("At least one subtask is required.");
        }

        int maxWeek = subtasks.stream()
                .mapToInt(s -> s.getWeekNumber() != null && s.getWeekNumber() > 0 ? s.getWeekNumber() : 1)
                .max()
                .orElse(1);
        weeks = Math.max(weeks, maxWeek);

        // Validate that admin provided actual details for every subtask
        for (int i = 0; i < subtasks.size(); i++) {
            SubtaskInputDto st = subtasks.get(i);
            int weekNum = (st.getWeekNumber() != null && st.getWeekNumber() > 0) ? st.getWeekNumber() : 1;
            String stTitle = (st.getTitle() != null && !st.getTitle().trim().isEmpty())
                    ? st.getTitle().trim()
                    : ("Week " + weekNum + " Subtask " + (i + 1));
            if (st.getDescription() == null || st.getDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Work details/description is mandatory for " + stTitle + " (Week " + weekNum + "). Please describe the required work.");
            }
        }

        Task task = new Task();
        task.setTaskCode(generateTaskCode());
        task.setTitle(title.trim());
        task.setDescription(description != null && !description.trim().isEmpty() ? description.trim() : null);
        task.setComplexity(normalizeComplexity(complexity));
        task.setPriority(normalizePriority(priority));
        task.setWeeksRequired(weeks);
        task.setStatus("TODO");
        task.setProgress(0);
        task.setLocked(false);

        if (teamLeads != null && !teamLeads.isEmpty()) {
            task.setTeamLeads(new HashSet<>(teamLeads));
        }

        if (assignees != null && !assignees.isEmpty()) {
            task.setAssignees(new HashSet<>(assignees));
            task.setEmployee(assignees.get(0));
        } else if (teamLeads != null && !teamLeads.isEmpty()) {
            task.setEmployee(teamLeads.get(0));
        } else {
            List<Employee> allEmployees = employeeRepository.findAll();
            if (!allEmployees.isEmpty()) {
                task.setEmployee(allEmployees.get(0));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        Task savedTask = taskRepository.save(task);

        // Create the subtasks
        for (int i = 0; i < subtasks.size(); i++) {
            SubtaskInputDto dto = subtasks.get(i);
            int weekNum = (dto.getWeekNumber() != null && dto.getWeekNumber() > 0) ? dto.getWeekNumber() : 1;
            String stTitle = (dto.getTitle() != null && !dto.getTitle().trim().isEmpty())
                    ? dto.getTitle().trim()
                    : ("Week " + weekNum + " Subtask " + (i + 1));

            Employee subAssignee = null;
            if (dto.getAssigneeId() != null) {
                subAssignee = employeeRepository.findById(dto.getAssigneeId()).orElse(null);
            }
            if (subAssignee == null && assignees != null && !assignees.isEmpty()) {
                subAssignee = assignees.get(i % assignees.size());
            }

            TaskAssignment assignment = new TaskAssignment();
            assignment.setTask(savedTask);
            assignment.setWeekNumber(weekNum);
            assignment.setSubtaskNumber(i + 1);
            assignment.setTitle(stTitle);
            assignment.setDescription(dto.getDescription().trim());
            assignment.setAssignee(subAssignee);
            assignment.setStatus("TODO");
            assignment.setProgress(0);
            assignment.setWeight(savedTask.getComplexity());
            assignment.setLocked(false);

            TaskAssignment savedAssignment = taskAssignmentRepository.save(assignment);
            savedTask.getAssignments().add(savedAssignment);

            // Notify assigned employee
            if (subAssignee != null) {
                try {
                    notificationService.createNotification(
                            subAssignee,
                            "New Assignment: " + stTitle,
                            "You have been assigned to " + stTitle + " (" + savedTask.getTitle() + "): " + dto.getDescription().trim(),
                            "TASK_ASSIGNED"
                    );
                } catch (Exception ignored) {
                }
            }
        }

        recalculateTaskProgressFromAssignments(savedTask);
        return taskRepository.save(savedTask);
    }

    // ============================================================
    // UPDATE TASK (WEEKLY SUBTASK STRUCTURE)
    // ============================================================

    public Task updateTask(
            Long id,
            String title,
            String description,
            String complexity,
            String priority,
            Integer weeksRequired,
            List<Employee> teamLeads,
            List<Employee> assignees,
            List<SubtaskInputDto> subtasks
    ) {
        Task task = getTask(id);

        if (task.isLocked()) {
            throw new IllegalStateException("Task is approved and locked. Modifications are not allowed.");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title is required.");
        }

        int weeks = (weeksRequired != null && weeksRequired > 0) ? weeksRequired : task.getWeeksRequired();

        if (subtasks != null && !subtasks.isEmpty()) {
            int maxWeek = subtasks.stream()
                    .mapToInt(s -> s.getWeekNumber() != null && s.getWeekNumber() > 0 ? s.getWeekNumber() : 1)
                    .max()
                    .orElse(1);
            weeks = Math.max(weeks, maxWeek);

            for (int i = 0; i < subtasks.size(); i++) {
                SubtaskInputDto st = subtasks.get(i);
                int weekNum = (st.getWeekNumber() != null && st.getWeekNumber() > 0) ? st.getWeekNumber() : 1;
                String stTitle = (st.getTitle() != null && !st.getTitle().trim().isEmpty())
                        ? st.getTitle().trim()
                        : ("Week " + weekNum + " Subtask " + (i + 1));
                if (st.getDescription() == null || st.getDescription().trim().isEmpty()) {
                    throw new IllegalArgumentException("Work details/description is mandatory for " + stTitle + " (Week " + weekNum + ").");
                }
            }
        }

        task.setTitle(title.trim());
        task.setDescription(description != null && !description.trim().isEmpty() ? description.trim() : null);
        task.setComplexity(normalizeComplexity(complexity));
        task.setPriority(normalizePriority(priority));
        task.setWeeksRequired(weeks);

        if (teamLeads != null) {
            task.getTeamLeads().clear();
            task.getTeamLeads().addAll(teamLeads);
        }

        if (assignees != null) {
            task.getAssignees().clear();
            task.getAssignees().addAll(assignees);
            if (!assignees.isEmpty()) {
                task.setEmployee(assignees.get(0));
            }
        }

        // Synchronize subtasks if provided
        if (subtasks != null && !subtasks.isEmpty()) {
            Set<Long> incomingIds = new HashSet<>();
            for (SubtaskInputDto dto : subtasks) {
                if (dto.getId() != null) {
                    incomingIds.add(dto.getId());
                }
            }

            // Remove assignments deleted by admin (unless approved)
            List<TaskAssignment> toRemove = new ArrayList<>();
            if (!incomingIds.isEmpty()) {
                for (TaskAssignment existing : task.getAssignments()) {
                    if (!incomingIds.contains(existing.getId())) {
                        if (!existing.isLocked() && !"APPROVED".equalsIgnoreCase(existing.getStatus())) {
                            toRemove.add(existing);
                        }
                    }
                }
                for (TaskAssignment rem : toRemove) {
                    task.removeAssignment(rem);
                    taskAssignmentRepository.delete(rem);
                }
            }

            // Process each subtask
            for (int i = 0; i < subtasks.size(); i++) {
                SubtaskInputDto dto = subtasks.get(i);
                int weekNum = (dto.getWeekNumber() != null && dto.getWeekNumber() > 0) ? dto.getWeekNumber() : 1;
                String stTitle = (dto.getTitle() != null && !dto.getTitle().trim().isEmpty())
                        ? dto.getTitle().trim()
                        : ("Week " + weekNum + " Subtask " + (i + 1));

                Employee subAssignee = null;
                if (dto.getAssigneeId() != null) {
                    subAssignee = employeeRepository.findById(dto.getAssigneeId()).orElse(null);
                }

                TaskAssignment existing = null;
                if (dto.getId() != null) {
                    existing = task.getAssignments().stream()
                            .filter(a -> Objects.equals(a.getId(), dto.getId()))
                            .findFirst()
                            .orElse(null);
                } else if (incomingIds.isEmpty() && i < task.getAssignments().size()) {
                    existing = task.getAssignments().get(i);
                }

                if (existing != null) {
                    existing.setWeekNumber(weekNum);
                    existing.setSubtaskNumber(i + 1);
                    existing.setTitle(stTitle);
                    existing.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
                    existing.setAssignee(subAssignee);
                    existing.setWeight(task.getComplexity());
                    taskAssignmentRepository.save(existing);
                } else {
                    TaskAssignment newAssignment = new TaskAssignment();
                    newAssignment.setTask(task);
                    newAssignment.setWeekNumber(weekNum);
                    newAssignment.setSubtaskNumber(i + 1);
                    newAssignment.setTitle(stTitle);
                    newAssignment.setDescription(dto.getDescription().trim());
                    newAssignment.setAssignee(subAssignee);
                    newAssignment.setStatus("TODO");
                    newAssignment.setProgress(0);
                    newAssignment.setWeight(task.getComplexity());
                    newAssignment.setLocked(false);
                    TaskAssignment saved = taskAssignmentRepository.save(newAssignment);
                    task.getAssignments().add(saved);
                }
            }
        }

        recalculateTaskProgressFromAssignments(task);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ============================================================
    // BACKWARDS-COMPATIBLE CREATE / UPDATE OVERLOADS
    // ============================================================

    public Task createTask(String title, String description, Employee employee, String priority, LocalDate dueDate) {
        List<Employee> assignees = employee != null ? List.of(employee) : List.of();
        List<SubtaskInputDto> subtasks = List.of(new SubtaskInputDto(1, "Week 1", description != null ? description : title, employee != null ? employee.getId() : null));
        return createTask(title, description, "MEDIUM", priority, 1, List.of(), assignees, subtasks);
    }

    public Task createTask(
            String title,
            String description,
            List<Employee> assignees,
            String priority,
            String complexity,
            String status,
            Integer progress,
            LocalDate dueDate,
            LocalDate deadline,
            String deadlineBs,
            String deadlineTime,
            String tags
    ) {
        List<SubtaskInputDto> subtasks = List.of(new SubtaskInputDto(1, "Week 1", description != null ? description : title, (assignees != null && !assignees.isEmpty()) ? assignees.get(0).getId() : null));
        return createTask(title, description, complexity, priority, 1, List.of(), assignees, subtasks);
    }

    public Task updateTask(
            Long id,
            String title,
            String description,
            List<Employee> assignees,
            String status,
            String priority,
            String complexity,
            Integer progress,
            LocalDate dueDate,
            LocalDate deadline,
            String deadlineBs,
            String deadlineTime,
            String tags
    ) {
        Task task = getTask(id);
        return updateTask(id, title, description, complexity, priority, task.getWeeksRequired(), new ArrayList<>(task.getTeamLeads()), assignees, null);
    }

    public Task updateTaskByEmployee(
            Long id,
            Employee currentEmployee,
            String title,
            String description,
            String status,
            String priority,
            String complexity,
            Integer progress,
            String deadlineBs,
            String deadlineTime,
            String tags
    ) {
        Task task = getTask(id);
        verifyEmployeeOwnership(task, currentEmployee);

        if (task.isLocked()) {
            throw new IllegalStateException("Task is approved and locked. Modifications are not allowed.");
        }

        if (title != null && !title.trim().isEmpty()) {
            task.setTitle(title.trim());
        }
        if (description != null) {
            task.setDescription(description.trim());
        }

        recalculateTaskProgressFromAssignments(task);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ============================================================
    // WORK ITEM / SUBTASK PROGRESS UPDATE (MANUAL WITH MANDATORY NOTE)
    // ============================================================

    public TaskAssignment updateAssignmentProgress(Long assignmentId, Integer newProgress, String updateNote, Employee updatedBy) {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Subtask not found: " + assignmentId));

        Task task = assignment.getTask();
        if (task != null && task.isLocked()) {
            throw new IllegalStateException("Task is approved and locked. Subtasks cannot be modified.");
        }
        if (assignment.isLocked()) {
            throw new IllegalStateException("Subtask is approved and locked. It cannot be modified.");
        }

        if (newProgress == null || newProgress < 0 || newProgress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100.");
        }

        // Mandatory description check on EVERY progress update
        if (updateNote == null || updateNote.trim().isEmpty()) {
            throw new IllegalArgumentException("A description/update note is mandatory when changing subtask progress.");
        }

        // Ownership / permission check: only the assigned employee can update progress
        if (updatedBy != null && updatedBy.getUser() != null && updatedBy.getUser().getRole() == Role.EMPLOYEE) {
            boolean isAssignee = assignment.getAssignee() != null && assignment.getAssignee().getId().equals(updatedBy.getId());
            if (!isAssignee) {
                throw new SecurityException("This subtask is not assigned to you. You can only view it, not update it.");
            }
        }

        int oldProgress = assignment.getProgress() != null ? assignment.getProgress() : 0;
        int nextProgress = newProgress;

        TaskAssignmentHistory history = new TaskAssignmentHistory(
                assignment,
                oldProgress,
                nextProgress,
                updateNote.trim(),
                updatedBy
        );
        taskAssignmentHistoryRepository.save(history);
        assignment.addHistory(history);

        assignment.setProgress(nextProgress);

        if (nextProgress == 0) {
            assignment.setStatus("TODO");
        } else if (nextProgress > 0 && ("TODO".equalsIgnoreCase(assignment.getStatus()) || "DECLINED".equalsIgnoreCase(assignment.getStatus()))) {
            assignment.setStatus("IN_PROGRESS");
        }

        TaskAssignment saved = taskAssignmentRepository.save(assignment);

        if (task != null) {
            recalculateTaskProgressFromAssignments(task);
            taskRepository.save(task);
        }

        return saved;
    }

    // ============================================================
    // SUBMIT SUBTASK FOR REVIEW (EMPLOYEE -> ADMIN)
    // ============================================================

    public TaskAssignment submitAssignmentForReview(Long assignmentId, String note, Employee updatedBy) {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Subtask not found: " + assignmentId));

        Task task = assignment.getTask();
        if (task != null && task.isLocked()) {
            throw new IllegalStateException("Task is approved and locked.");
        }
        if (assignment.isLocked()) {
            throw new IllegalStateException("Subtask is already approved and locked.");
        }

        // Ownership / permission check: only the assigned employee can submit for review
        if (updatedBy != null && updatedBy.getUser() != null && updatedBy.getUser().getRole() == Role.EMPLOYEE) {
            boolean isAssignee = assignment.getAssignee() != null && assignment.getAssignee().getId().equals(updatedBy.getId());
            if (!isAssignee) {
                throw new SecurityException("This subtask is not assigned to you. You can only view it, not update it.");
            }
        }

        if (note == null || note.trim().isEmpty()) {
            throw new IllegalArgumentException("A description/note is mandatory when submitting for review.");
        }

        if (assignment.getProgress() == null || assignment.getProgress() < 100) {
            throw new IllegalStateException("Subtask progress must be 100% before submitting for review.");
        }

        assignment.setProgress(100);
        assignment.setStatus("READY_FOR_REVIEW");

        TaskAssignmentHistory history = new TaskAssignmentHistory(
                assignment,
                assignment.getProgress(),
                100,
                "Submitted for Review: " + note.trim(),
                updatedBy
        );
        taskAssignmentHistoryRepository.save(history);
        assignment.addHistory(history);

        TaskAssignment saved = taskAssignmentRepository.save(assignment);

        // Notify Admins
        try {
            String author = updatedBy != null ? (updatedBy.getFirstName() + " " + (updatedBy.getLastName() != null ? updatedBy.getLastName() : "")).trim() : "Employee";
            notificationService.createAdminNotification(
                    updatedBy,
                    "Subtask Ready for Review",
                    author + " has submitted " + assignment.getTitle() + " (" + (task != null ? task.getTitle() : "Task") + ") for review: " + note.trim(),
                    "TASK_REVIEW"
            );
        } catch (Exception ignored) {
        }

        if (task != null) {
            recalculateTaskProgressFromAssignments(task);
            taskRepository.save(task);
        }

        return saved;
    }

    // ============================================================
    // ADMIN APPROVE SUBTASK
    // ============================================================

    public TaskAssignment approveAssignment(Long assignmentId, Employee adminUser) {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Subtask not found: " + assignmentId));

        Task task = assignment.getTask();
        assignment.setStatus("APPROVED");
        assignment.setLocked(true);
        assignment.setApprovedAt(LocalDateTime.now());
        assignment.setDeclineReason(null);
        assignment.setProgress(100);

        TaskAssignmentHistory history = new TaskAssignmentHistory(
                assignment,
                100,
                100,
                "Approved by administrator.",
                adminUser
        );
        taskAssignmentHistoryRepository.save(history);
        assignment.addHistory(history);

        TaskAssignment saved = taskAssignmentRepository.save(assignment);

        // Notify assigned employee
        if (assignment.getAssignee() != null) {
            try {
                notificationService.createNotification(
                        assignment.getAssignee(),
                        "Subtask Approved: " + assignment.getTitle(),
                        "Great work! Your subtask '" + assignment.getTitle() + "' for task '"
                                + (task != null ? task.getTitle() : "") + "' has been approved by the administrator.",
                        "TASK_APPROVED"
                );
            } catch (Exception ignored) {
            }
        }

        if (task != null) {
            recalculateTaskProgressFromAssignments(task);
            taskRepository.save(task);
        }

        return saved;
    }

    // ============================================================
    // ADMIN DECLINE SUBTASK
    // ============================================================

    public TaskAssignment declineAssignment(Long assignmentId, String declineReason, Employee adminUser) {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Subtask not found: " + assignmentId));

        if (declineReason == null || declineReason.trim().isEmpty()) {
            throw new IllegalArgumentException("A decline reason/description is strictly mandatory when declining work.");
        }

        Task task = assignment.getTask();
        assignment.setStatus("DECLINED");
        assignment.setDeclineReason(declineReason.trim());
        assignment.setLocked(false);

        TaskAssignmentHistory history = new TaskAssignmentHistory(
                assignment,
                assignment.getProgress(),
                assignment.getProgress(),
                "Declined by administrator: " + declineReason.trim(),
                adminUser
        );
        taskAssignmentHistoryRepository.save(history);
        assignment.addHistory(history);

        TaskAssignment saved = taskAssignmentRepository.save(assignment);

        // Notify assigned employee
        if (assignment.getAssignee() != null) {
            try {
                notificationService.createNotification(
                        assignment.getAssignee(),
                        "Subtask Declined: " + assignment.getTitle(),
                        "Your subtask '" + assignment.getTitle() + "' requires adjustments. Reason: " + declineReason.trim(),
                        "TASK_DECLINED"
                );
            } catch (Exception ignored) {
            }
        }

        if (task != null) {
            recalculateTaskProgressFromAssignments(task);
            taskRepository.save(task);
        }

        return saved;
    }

    // ============================================================
    // RECALCULATE TASK PROGRESS FROM SUBTASKS
    // Formula: Total Progress = round(Sum(subtask.progress) / N)
    // ============================================================

    public void recalculateTaskProgressFromAssignments(Task task) {
        if (task == null || task.getAssignments() == null || task.getAssignments().isEmpty()) {
            return;
        }

        double total = 0;
        int count = task.getAssignments().size();
        boolean allApproved = true;

        for (TaskAssignment a : task.getAssignments()) {
            if (a == null) {
                continue;
            }
            int p = a.getProgress() != null ? a.getProgress() : 0;
            total += p;
            if (!"APPROVED".equalsIgnoreCase(a.getStatus())) {
                allApproved = false;
            }
        }

        int avg = (int) Math.round(total / count);
        avg = Math.max(0, Math.min(100, avg));
        task.setProgress(avg);

        if (allApproved && count > 0) {
            task.setStatus("APPROVED");
            task.setLocked(true);
            task.setProgress(100);
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
            if (task.getApprovedAt() == null) {
                task.setApprovedAt(LocalDateTime.now());
            }
        } else if (avg >= 100 && !"APPROVED".equals(task.getStatus())) {
            task.setStatus("READY_FOR_REVIEW");
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        } else if (avg > 0 && "TODO".equals(task.getStatus())) {
            task.setStatus("IN_PROGRESS");
        }
    }

    // ============================================================
    // CANNOT MANUALLY UPDATE TOTAL TASK PROGRESS
    // ============================================================

    public Task updateProgress(Long id, Employee employee, Integer progress) {
        throw new UnsupportedOperationException("Task total progress cannot be manually changed. It is automatically calculated from its subtasks.");
    }

    // ============================================================
    // SUBTASK MANAGEMENT
    // ============================================================

    public TaskAssignment addAssignment(
            Long taskId,
            String title,
            String description,
            Employee assignee,
            LocalDate deadline,
            String deadlineBs,
            String deadlineTime,
            Integer progress,
            String status,
            String weight,
            String note
    ) {
        Task task = getTask(taskId);
        if (task.isLocked()) {
            throw new IllegalStateException("Task is approved and locked. Subtasks cannot be added.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Subtask title is required.");
        }

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setSubtaskNumber(task.getAssignments().size() + 1);
        assignment.setTitle(title.trim());
        assignment.setDescription(description != null && !description.trim().isEmpty() ? description.trim() : null);
        assignment.setAssignee(assignee);
        assignment.setWeight(weight != null && !weight.trim().isEmpty() ? weight.trim().toUpperCase() : task.getComplexity());

        int p = progress != null ? Math.max(0, Math.min(100, progress)) : 0;
        assignment.setProgress(p);
        assignment.setStatus(status != null && !status.trim().isEmpty() ? status.trim().toUpperCase() : (p >= 100 ? "READY_FOR_REVIEW" : (p > 0 ? "IN_PROGRESS" : "TODO")));

        TaskAssignment saved = taskAssignmentRepository.save(assignment);
        task.getAssignments().add(saved);
        task.setWeeksRequired(task.getAssignments().size());

        recalculateTaskProgressFromAssignments(task);
        taskRepository.save(task);

        return saved;
    }

    public TaskAssignment updateAssignment(
            Long taskId,
            Long assignmentId,
            String title,
            String description,
            Employee assignee,
            LocalDate deadline,
            String deadlineBs,
            String deadlineTime,
            String status,
            String weight,
            String note
    ) {
        Task task = getTask(taskId);
        if (task.isLocked()) {
            throw new IllegalStateException("Task is approved and locked. Subtasks cannot be modified.");
        }

        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Subtask not found: " + assignmentId));

        if (title != null && !title.trim().isEmpty()) {
            assignment.setTitle(title.trim());
        }
        if (description != null) {
            assignment.setDescription(description.trim().isEmpty() ? null : description.trim());
        }
        if (assignee != null) {
            assignment.setAssignee(assignee);
        }
        if (status != null && !status.trim().isEmpty()) {
            assignment.setStatus(status.trim().toUpperCase());
        }
        if (weight != null && !weight.trim().isEmpty()) {
            assignment.setWeight(weight.trim().toUpperCase());
        }

        TaskAssignment saved = taskAssignmentRepository.save(assignment);
        recalculateTaskProgressFromAssignments(task);
        taskRepository.save(task);
        return saved;
    }

    public TaskAssignment updateAssignmentStatus(Long assignmentId, String status) {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Subtask not found: " + assignmentId));

        if (assignment.getTask() != null && assignment.getTask().isLocked()) {
            throw new IllegalStateException("Task is approved and locked.");
        }

        String targetStatus = status != null ? status.trim().toUpperCase() : "TODO";
        assignment.setStatus(targetStatus);
        if ("APPROVED".equalsIgnoreCase(targetStatus) || "COMPLETED".equalsIgnoreCase(targetStatus)) {
            assignment.setProgress(100);
            assignment.setLocked("APPROVED".equalsIgnoreCase(targetStatus));
        }

        TaskAssignment saved = taskAssignmentRepository.save(assignment);

        if (assignment.getTask() != null) {
            recalculateTaskProgressFromAssignments(assignment.getTask());
            taskRepository.save(assignment.getTask());
        }

        return saved;
    }

    public void deleteAssignment(Long taskId, Long assignmentId) {
        Task task = getTask(taskId);
        if (task.isLocked()) {
            throw new IllegalStateException("Task is approved and locked. Subtasks cannot be deleted.");
        }

        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Subtask not found: " + assignmentId));

        task.removeAssignment(assignment);
        taskAssignmentRepository.delete(assignment);
        task.setWeeksRequired(Math.max(1, task.getAssignments().size()));

        recalculateTaskProgressFromAssignments(task);
        taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<TaskAssignmentHistory> getAssignmentHistories(Long assignmentId) {
        return taskAssignmentHistoryRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);
    }

    // ============================================================
    // DELETE TASK
    // ============================================================

    public void deleteTask(Long id) {
        Task task = getTask(id);
        taskRepository.delete(task);
    }

    // ============================================================
    // TASK REVIEW & SUBMISSION (PARENT TASK LEVEL)
    // ============================================================

    public Task submitForReview(Long id, Employee employee, String completionNote) {
        Task task = getTask(id);
        verifyEmployeeOwnership(task, employee);

        if (task.isLocked()) {
            throw new IllegalStateException("Approved and locked tasks cannot be submitted again.");
        }

        task.setStatus("READY_FOR_REVIEW");
        task.setCompletionNote(completionNote);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);

        try {
            notificationService.createAdminNotification(
                    employee,
                    "Task Submitted for Review",
                    employee.getFirstName() + " " + employee.getLastName() + " submitted task: " + task.getTitle(),
                    "TASK_REVIEW"
            );
        } catch (Exception ignored) {
        }

        return saved;
    }

    public Task reviewTask(Long id, String action, String reviewNote) {
        Task task = getTask(id);

        if (task.isLocked()) {
            throw new IllegalStateException("Task is already approved and locked.");
        }

        LocalDateTime now = LocalDateTime.now();
        task.setReviewedAt(now);
        task.setReviewNote(reviewNote);
        task.setReviewComment(reviewNote);

        if ("APPROVE".equalsIgnoreCase(action)) {
            task.setStatus("APPROVED");
            task.setProgress(100);
            task.setLocked(true);
            task.setApprovedAt(now);

            // Approve all child subtasks as well
            for (TaskAssignment a : task.getAssignments()) {
                a.setStatus("APPROVED");
                a.setProgress(100);
                a.setLocked(true);
                a.setApprovedAt(now);
                taskAssignmentRepository.save(a);
            }

            if (task.getEmployee() != null) {
                try {
                    notificationService.createNotification(
                            task.getEmployee(),
                            "Task Approved!",
                            "Your task '" + task.getTitle() + "' has been approved by admin.",
                            "TASK_APPROVED"
                    );
                } catch (Exception ignored) {
                }
            }
        } else if ("REJECT".equalsIgnoreCase(action)) {
            task.setStatus("IN_PROGRESS");
            task.setLocked(false);

            if (task.getEmployee() != null) {
                try {
                    notificationService.createNotification(
                            task.getEmployee(),
                            "Task Review: Changes Requested",
                            "Your task '" + task.getTitle() + "' requires changes. Note: " + (reviewNote != null ? reviewNote : "None"),
                            "TASK_REJECTED"
                    );
                } catch (Exception ignored) {
                }
            }
        } else {
            throw new IllegalArgumentException("Action must be APPROVE or REJECT.");
        }

        task.setUpdatedAt(now);
        return taskRepository.save(task);
    }

    // ============================================================
    // COMMENTS
    // ============================================================

    public TaskComment addComment(Long taskId, String authorName, String authorRole, String commentText) {
        Task task = getTask(taskId);
        if (commentText == null || commentText.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be empty.");
        }

        TaskComment comment = new TaskComment(
                task,
                authorName != null ? authorName : "System",
                authorRole != null ? authorRole : "EMPLOYEE",
                commentText.trim()
        );

        TaskComment saved = taskCommentRepository.save(comment);
        task.getComments().add(saved);
        return saved;
    }

    public TaskComment addComment(Long taskId, Employee author, String commentText) {
        String authorName = "Administrator";
        String role = "ADMIN";
        if (author != null) {
            authorName = (author.getFirstName() + " " + (author.getLastName() != null ? author.getLastName() : "")).trim();
            if (author.getUser() != null && author.getUser().getRole() != null) {
                role = author.getUser().getRole().name();
            }
        }
        return addComment(taskId, authorName, role, commentText);
    }

    @Transactional(readOnly = true)
    public List<TaskComment> getTaskComments(Long taskId) {
        Task task = getTask(taskId);
        return taskCommentRepository.findByTaskOrderByCreatedAtAsc(task);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private String generateTaskCode() {
        return "TSK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String normalizePriority(String priority) {
        if (priority == null) return "MEDIUM";
        String p = priority.trim().toUpperCase();
        return switch (p) {
            case "LOW", "HIGH", "URGENT" -> p;
            default -> "MEDIUM";
        };
    }

    private String normalizeComplexity(String complexity) {
        if (complexity == null) return "MEDIUM";
        String c = complexity.trim().toUpperCase();
        return switch (c) {
            case "SMALL", "LARGE", "EPIC" -> c;
            default -> "MEDIUM";
        };
    }

    private void verifyEmployeeOwnership(Task task, Employee employee) {
        if (employee == null) {
            throw new SecurityException("Employee context is missing.");
        }
        boolean isPrimary = task.getEmployee() != null && employee.getId().equals(task.getEmployee().getId());
        boolean isAssignee = task.getAssignees() != null && task.getAssignees().stream()
                .anyMatch(a -> a != null && a.getId().equals(employee.getId()));
        boolean isTeamLead = task.getTeamLeads() != null && task.getTeamLeads().stream()
                .anyMatch(tl -> tl != null && tl.getId().equals(employee.getId()));
        boolean isSubtaskAssignee = task.getAssignments() != null && task.getAssignments().stream()
                .anyMatch(a -> a.getAssignee() != null && a.getAssignee().getId().equals(employee.getId()));

        if (!isPrimary && !isAssignee && !isTeamLead && !isSubtaskAssignee) {
            throw new SecurityException("You do not have permission to modify this task.");
        }
    }
}