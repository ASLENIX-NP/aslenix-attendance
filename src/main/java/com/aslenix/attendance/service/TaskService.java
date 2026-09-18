package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.entity.TaskAssignment;
import com.aslenix.attendance.entity.TaskComment;
import com.aslenix.attendance.repository.TaskAssignmentRepository;
import com.aslenix.attendance.repository.TaskCommentRepository;
import com.aslenix.attendance.repository.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskCommentRepository taskCommentRepository;

    public TaskService(TaskRepository taskRepository,
                       TaskAssignmentRepository taskAssignmentRepository,
                       TaskCommentRepository taskCommentRepository) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskCommentRepository = taskCommentRepository;
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
    // GET TASK
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
    // CREATE TASK (Single Employee - Backwards Compatible)
    // ============================================================

    public Task createTask(
            String title,
            String description,
            Employee employee,
            String priority,
            LocalDate dueDate
    ) {
        List<Employee> assignees = employee != null ? List.of(employee) : List.of();
        return createTask(title, description, assignees, priority, "MEDIUM", dueDate, null, null, null, null);
    }

    public Task createTask(
            String title,
            String description,
            Employee employee,
            String priority,
            LocalDate dueDate,
            LocalDate deadline
    ) {
        List<Employee> assignees = employee != null ? List.of(employee) : List.of();
        return createTask(title, description, assignees, priority, "MEDIUM", dueDate, deadline, null, null, null);
    }

    // ============================================================
    // CREATE TASK (Multi-Assignee & Full Metadata)
    // ============================================================

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
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title is required.");
        }

        Task task = new Task();
        task.setTaskCode(generateTaskCode());
        task.setTitle(title.trim());
        task.setDescription(description == null ? null : description.trim());

        if (assignees != null && !assignees.isEmpty()) {
            task.setAssignees(new HashSet<>(assignees));
            task.setEmployee(assignees.get(0));
        }

        task.setPriority(normalizePriority(priority));
        task.setComplexity(normalizeComplexity(complexity));

        int p = progress != null ? Math.max(0, Math.min(100, progress)) : 0;
        task.setProgress(p);

        String initialStatus = "TODO";
        if (status != null && !status.trim().isEmpty()) {
            String norm = status.trim().toUpperCase();
            if ("UNDER_REVIEW".equals(norm)) norm = "READY_FOR_REVIEW";
            if ("COMPLETED".equals(norm)) norm = "APPROVED";
            initialStatus = norm;
        } else {
            if (p >= 100) initialStatus = "READY_FOR_REVIEW";
            else if (p > 0) initialStatus = "IN_PROGRESS";
        }
        task.setStatus(initialStatus);
        if ("APPROVED".equals(initialStatus)) {
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            task.setApprovedAt(LocalDateTime.now());
        }

        task.setDueDate(dueDate);
        task.setDeadline(deadline);
        String cleanBs = (deadlineBs != null && !deadlineBs.trim().isEmpty() && !deadlineBs.contains("undefined")) ? deadlineBs.trim() : null;
        task.setDeadlineBs(cleanBs);
        task.setDeadlineTime(deadlineTime != null && !deadlineTime.trim().isEmpty() ? deadlineTime.trim() : null);
        task.setTags(tags != null && !tags.trim().isEmpty() ? tags.trim() : null);

        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        return taskRepository.save(task);
    }

    public Task createTask(
            String title,
            String description,
            List<Employee> assignees,
            String priority,
            String complexity,
            LocalDate dueDate,
            LocalDate deadline,
            String deadlineBs,
            String deadlineTime,
            String tags
    ) {
        return createTask(title, description, assignees, priority, complexity, "TODO", 0, dueDate, deadline, deadlineBs, deadlineTime, tags);
    }

    // ============================================================
    // UPDATE TASK (Multi-Assignee & Full Metadata)
    // ============================================================

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

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title is required.");
        }

        task.setTitle(title.trim());
        task.setDescription(description == null ? null : description.trim());

        if (assignees != null) {
            task.getAssignees().clear();
            task.getAssignees().addAll(assignees);
            task.setEmployee(assignees.isEmpty() ? null : assignees.get(0));
        }

        if (status != null && !status.trim().isEmpty()) {
            String normStatus = status.trim().toUpperCase();
            if (normStatus.equals("UNDER_REVIEW")) normStatus = "READY_FOR_REVIEW";
            if (normStatus.equals("COMPLETED")) normStatus = "APPROVED";
            task.setStatus(normStatus);
            if ("APPROVED".equals(normStatus)) {
                task.setProgress(100);
                if (task.getCompletedAt() == null) task.setCompletedAt(LocalDateTime.now());
                if (task.getApprovedAt() == null) task.setApprovedAt(LocalDateTime.now());
            }
        }

        task.setPriority(normalizePriority(priority));
        task.setComplexity(normalizeComplexity(complexity));

        if (progress != null) {
            int p = Math.max(0, Math.min(100, progress));
            task.setProgress(p);
            if (!"APPROVED".equals(task.getStatus())) {
                if (p >= 100) {
                    task.setStatus("READY_FOR_REVIEW");
                    if (task.getCompletedAt() == null) task.setCompletedAt(LocalDateTime.now());
                } else if (p == 0) {
                    task.setStatus("TODO");
                } else if ("TODO".equals(task.getStatus())) {
                    task.setStatus("IN_PROGRESS");
                }
            }
        }

        task.setDueDate(dueDate);
        task.setDeadline(deadline);
        String cleanBs = (deadlineBs != null && !deadlineBs.trim().isEmpty() && !deadlineBs.contains("undefined")) ? deadlineBs.trim() : null;
        task.setDeadlineBs(cleanBs);
        task.setDeadlineTime(deadlineTime != null && !deadlineTime.trim().isEmpty() ? deadlineTime.trim() : null);
        task.setTags(tags != null && !tags.trim().isEmpty() ? tags.trim() : null);

        task.setUpdatedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    // Legacy overload for single employee
    public Task updateTask(
            Long id,
            String title,
            String description,
            Employee employee,
            String priority,
            LocalDate dueDate,
            LocalDate deadline
    ) {
        List<Employee> assignees = employee != null ? List.of(employee) : List.of();
        Task task = getTask(id);
        return updateTask(id, title, description, assignees, task.getStatus(), priority, task.getComplexity(),
                task.getProgress(), dueDate, deadline, task.getDeadlineBs(), task.getDeadlineTime(), task.getTags());
    }

    // ============================================================
    // UPDATE TASK (Employee Self-Edit)
    // ============================================================

    public Task updateTaskByEmployee(
            Long id,
            Employee employee,
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
        verifyEmployeeOwnership(task, employee);

        // Security check: Title, description, complexity, priority, and deadlines
        // are restricted to Administrator edits only. Existing values are preserved.

        if (status != null && !status.trim().isEmpty()) {
            String normStatus = status.trim().toUpperCase();
            if (normStatus.equals("UNDER_REVIEW")) normStatus = "READY_FOR_REVIEW";
            if (!"APPROVED".equals(normStatus) && !"COMPLETED".equals(normStatus)) {
                task.setStatus(normStatus);
            }
        }

        if (progress != null) {
            int p = Math.max(0, Math.min(100, progress));
            task.setProgress(p);
            if (p >= 100 && !"APPROVED".equals(task.getStatus())) {
                task.setStatus("READY_FOR_REVIEW");
                if (task.getCompletedAt() == null) task.setCompletedAt(LocalDateTime.now());
            } else if (p == 0 && !"APPROVED".equals(task.getStatus())) {
                task.setStatus("TODO");
            } else if (p > 0 && !"APPROVED".equals(task.getStatus()) && !"READY_FOR_REVIEW".equals(task.getStatus())) {
                task.setStatus("IN_PROGRESS");
            }
        }

        task.setUpdatedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    // ============================================================
    // WORK ASSIGNMENTS (Sub-tasks)
    // ============================================================

    public TaskAssignment addAssignment(Long taskId, String title, Employee assignee, String status, String weight, String note) {
        Task task = getTask(taskId);
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment title is required.");
        }

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setTitle(title.trim());
        assignment.setAssignee(assignee);
        assignment.setStatus(status != null && !status.trim().isEmpty() ? status.trim().toUpperCase() : "TODO");
        assignment.setWeight(weight != null && !weight.trim().isEmpty() ? weight.trim().toUpperCase() : "MEDIUM");
        assignment.setNote(note != null ? note.trim() : null);

        TaskAssignment saved = taskAssignmentRepository.save(assignment);
        task.getAssignments().add(saved);

        recalculateTaskProgressFromAssignments(task);
        taskRepository.save(task);

        return saved;
    }

    public TaskAssignment updateAssignmentStatus(Long assignmentId, String status) {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.setStatus(status != null ? status.trim().toUpperCase() : "TODO");
        TaskAssignment saved = taskAssignmentRepository.save(assignment);

        if (assignment.getTask() != null) {
            recalculateTaskProgressFromAssignments(assignment.getTask());
            taskRepository.save(assignment.getTask());
        }

        return saved;
    }

    public void deleteAssignment(Long taskId, Long assignmentId) {
        Task task = getTask(taskId);
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        task.removeAssignment(assignment);
        taskAssignmentRepository.delete(assignment);

        recalculateTaskProgressFromAssignments(task);
        taskRepository.save(task);
    }

    public void recalculateTaskProgressFromAssignments(Task task) {
        if (task == null || task.getAssignments() == null || task.getAssignments().isEmpty()) {
            return;
        }

        long total = task.getAssignments().size();
        long completed = task.getAssignments().stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()) || "VERIFIED".equalsIgnoreCase(a.getStatus()))
                .count();

        int progress = (int) Math.round(((double) completed / total) * 100);
        task.setProgress(progress);

        if (progress >= 100 && !"APPROVED".equals(task.getStatus())) {
            task.setStatus("READY_FOR_REVIEW");
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        } else if (progress > 0 && "TODO".equals(task.getStatus())) {
            task.setStatus("IN_PROGRESS");
        }
    }

    // ============================================================
    // DISCUSSION COMMENTS
    // ============================================================

    public TaskComment addComment(Long taskId, String authorName, String authorRole, String content) {
        Task task = getTask(taskId);
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty.");
        }

        TaskComment comment = new TaskComment(task, authorName != null ? authorName.trim() : "Anonymous",
                authorRole != null ? authorRole.trim() : "EMPLOYEE", content.trim());

        TaskComment saved = taskCommentRepository.save(comment);
        task.getComments().add(saved);
        taskRepository.save(task);
        return saved;
    }

    // ============================================================
    // START TASK
    // ============================================================

    public Task startTask(Long id, Employee employee) {
        Task task = getTask(id);
        verifyEmployeeOwnership(task, employee);

        if ("APPROVED".equals(task.getStatus())) {
            throw new IllegalStateException("Approved tasks cannot be started again.");
        }

        task.setStatus("IN_PROGRESS");
        if (task.getProgress() == null || task.getProgress() < 1) {
            task.setProgress(1);
        }
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ============================================================
    // UPDATE PROGRESS
    // ============================================================

    public Task updateProgress(Long id, Employee employee, Integer progress) {
        Task task = getTask(id);
        verifyEmployeeOwnership(task, employee);

        if ("APPROVED".equals(task.getStatus())) {
            throw new IllegalStateException("Approved tasks cannot be modified.");
        }

        if (progress == null || progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100.");
        }

        task.setProgress(progress);
        if (progress == 0) {
            task.setStatus("TODO");
        } else if (progress >= 100) {
            task.setProgress(100);
            task.setStatus("READY_FOR_REVIEW");
            if (task.getCompletedAt() == null) task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setStatus("IN_PROGRESS");
        }

        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ============================================================
    // DRAG & DROP MOVE (EMPLOYEE)
    // ============================================================

    public Task moveTask(Long id, Employee employee, String newStatus) {
        Task task = getTask(id);
        verifyEmployeeOwnership(task, employee);

        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Task status is required.");
        }

        String status = newStatus.trim().toUpperCase();
        if (status.equals("UNDER_REVIEW")) status = "READY_FOR_REVIEW";

        if ("APPROVED".equals(task.getStatus())) {
            throw new IllegalStateException("Approved tasks cannot be moved.");
        }
        if ("APPROVED".equals(status)) {
            throw new SecurityException("Employees cannot approve tasks.");
        }

        if (!status.equals("TODO") && !status.equals("IN_PROGRESS") && !status.equals("READY_FOR_REVIEW")) {
            throw new IllegalArgumentException("Invalid task status.");
        }

        if ("TODO".equals(status)) {
            task.setStatus("TODO");
            task.setProgress(0);
            task.setCompletedAt(null);
        } else if ("IN_PROGRESS".equals(status)) {
            task.setStatus("IN_PROGRESS");
            if (task.getProgress() == null || task.getProgress() <= 0 || task.getProgress() >= 100) {
                task.setProgress(1);
            }
            task.setCompletedAt(null);
        } else if ("READY_FOR_REVIEW".equals(status)) {
            task.setProgress(100);
            task.setStatus("READY_FOR_REVIEW");
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        }

        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ============================================================
    // DRAG & DROP MOVE (ADMIN)
    // ============================================================

    public Task moveTaskByAdmin(Long id, String newStatus) {
        Task task = getTask(id);

        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Task status is required.");
        }

        String status = newStatus.trim().toUpperCase();
        if (status.equals("UNDER_REVIEW")) status = "READY_FOR_REVIEW";
        if (status.equals("COMPLETED")) status = "APPROVED";

        if ("APPROVED".equals(task.getStatus()) && !"APPROVED".equals(status)) {
            // allow admin to move out of approved if desired or retain lock
            // Admin can unlock task if moved to another column
        }

        if (!status.equals("TODO") && !status.equals("IN_PROGRESS") && !status.equals("READY_FOR_REVIEW") && !status.equals("APPROVED")) {
            throw new IllegalArgumentException("Invalid task status: " + status);
        }

        if ("TODO".equals(status)) {
            task.setStatus("TODO");
            task.setProgress(0);
            task.setCompletedAt(null);
        } else if ("IN_PROGRESS".equals(status)) {
            task.setStatus("IN_PROGRESS");
            if (task.getProgress() == null || task.getProgress() <= 0 || task.getProgress() >= 100) {
                task.setProgress(1);
            }
            task.setCompletedAt(null);
        } else if ("READY_FOR_REVIEW".equals(status)) {
            task.setStatus("READY_FOR_REVIEW");
            task.setProgress(100);
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        } else if ("APPROVED".equals(status)) {
            task.setStatus("APPROVED");
            task.setProgress(100);
            LocalDateTime now = LocalDateTime.now();
            task.setReviewedAt(now);
            task.setApprovedAt(now);
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(now);
            }
        }

        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ============================================================
    // SUBMIT FOR REVIEW
    // ============================================================

    public Task submitForReview(Long id, Employee employee, String completionNote) {
        Task task = getTask(id);
        verifyEmployeeOwnership(task, employee);

        if ("APPROVED".equals(task.getStatus())) {
            throw new IllegalStateException("This task has already been approved.");
        }

        task.setProgress(100);
        task.setStatus("READY_FOR_REVIEW");
        task.setCompletionNote(completionNote == null ? null : completionNote.trim());
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ============================================================
    // APPROVE TASK
    // ============================================================

    public Task approveTask(Long id, String reviewNote) {
        Task task = getTask(id);
        if ("APPROVED".equals(task.getStatus())) {
            return task;
        }

        task.setStatus("APPROVED");
        task.setProgress(100);
        task.setReviewNote(reviewNote == null ? null : reviewNote.trim());
        task.setReviewComment(reviewNote == null ? null : reviewNote.trim());
        LocalDateTime now = LocalDateTime.now();
        task.setReviewedAt(now);
        task.setApprovedAt(now);
        task.setUpdatedAt(now);
        if (task.getCompletedAt() == null) {
            task.setCompletedAt(now);
        }
        return taskRepository.save(task);
    }

    // ============================================================
    // REJECT TASK
    // ============================================================

    public Task rejectTask(Long id, String reviewNote) {
        Task task = getTask(id);
        if ("APPROVED".equals(task.getStatus())) {
            throw new IllegalStateException("An approved task cannot be rejected.");
        }

        task.setStatus("IN_PROGRESS");
        if (task.getProgress() == null || task.getProgress() >= 100) {
            task.setProgress(90);
        }

        task.setReviewNote(reviewNote == null ? null : reviewNote.trim());
        task.setReviewComment(reviewNote == null ? null : reviewNote.trim());
        task.setReviewedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ============================================================
    // DELETE TASK
    // ============================================================

    public void deleteTask(Long id) {
        Task task = getTask(id);
        taskRepository.delete(task);
    }

    // ============================================================
    // COUNTS
    // ============================================================

    @Transactional(readOnly = true)
    public long countEmployeeTasks(Employee employee) {
        if (employee == null) return 0;
        return taskRepository.countTasksForEmployee(employee);
    }

    @Transactional(readOnly = true)
    public long countEmployeeTasksByStatus(Employee employee, String status) {
        if (employee == null || status == null) return 0;
        return taskRepository.countTasksForEmployeeAndStatus(employee, status.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public long countTasksByStatus(String status) {
        if (status == null) return 0;
        return taskRepository.countByStatus(status.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public long countTodoTasks() {
        return countTasksByStatus("TODO");
    }

    @Transactional(readOnly = true)
    public long countInProgressTasks() {
        return countTasksByStatus("IN_PROGRESS");
    }

    @Transactional(readOnly = true)
    public long countReadyForReviewTasks() {
        return countTasksByStatus("READY_FOR_REVIEW");
    }

    @Transactional(readOnly = true)
    public long countApprovedTasks() {
        return countTasksByStatus("APPROVED");
    }

    // ============================================================
    // VERIFY EMPLOYEE OWNERSHIP
    // ============================================================

    private void verifyEmployeeOwnership(Task task, Employee employee) {
        if (employee == null || employee.getId() == null) {
            throw new IllegalArgumentException("Employee is required.");
        }

        boolean isPrimary = task.getEmployee() != null && employee.getId().equals(task.getEmployee().getId());
        boolean isAssignee = task.getAssignees() != null && task.getAssignees().stream()
                .anyMatch(a -> a != null && employee.getId().equals(a.getId()));

        if (!isPrimary && !isAssignee) {
            throw new SecurityException("You are not authorized to modify this task.");
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private String normalizePriority(String priority) {
        if (priority == null || priority.trim().isEmpty()) {
            return "MEDIUM";
        }
        String normalized = priority.trim().toUpperCase();
        if (!normalized.equals("LOW") && !normalized.equals("MEDIUM") && !normalized.equals("HIGH") && !normalized.equals("URGENT")) {
            return "MEDIUM";
        }
        return normalized;
    }

    private String normalizeComplexity(String complexity) {
        if (complexity == null || complexity.trim().isEmpty()) {
            return "MEDIUM";
        }
        String normalized = complexity.trim().toUpperCase();
        if (!normalized.equals("SMALL") && !normalized.equals("MEDIUM") && !normalized.equals("LARGE") && !normalized.equals("EPIC")) {
            return "MEDIUM";
        }
        return normalized;
    }

    private String generateTaskCode() {
        String code;
        do {
            code = "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            final String generatedCode = code;
            if (taskRepository.findAll().stream().noneMatch(task -> generatedCode.equals(task.getTaskCode()))) {
                return generatedCode;
            }
        } while (true);
    }
}