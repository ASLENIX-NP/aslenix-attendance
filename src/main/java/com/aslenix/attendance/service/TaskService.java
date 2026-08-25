package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // ============================================================
    // GET ALL TASKS
    // ============================================================

    public List<Task> getAllTasks() {

        return taskRepository
                .findAllByOrderByCreatedAtDesc();
    }

    // ============================================================
    // GET EMPLOYEE TASKS
    // ============================================================

    public List<Task> getEmployeeTasks(Employee employee) {

        return taskRepository
                .findByEmployeeOrderByCreatedAtDesc(employee);
    }

    // ============================================================
    // CREATE TASK
    // ============================================================

    public Task createTask(
            String title,
            String description,
            Employee employee,
            String priority,
            java.time.LocalDate dueDate) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }

        Task task = new Task();

        task.setTitle(title.trim());
        task.setDescription(description);
        task.setEmployee(employee);
        task.setPriority(
                priority == null || priority.isBlank()
                        ? "MEDIUM"
                        : priority.toUpperCase()
        );
        task.setDueDate(dueDate);
        task.setStatus("ASSIGNED");

        return taskRepository.save(task);
    }

    // ============================================================
    // FIND TASK
    // ============================================================

    public Task getTask(Long id) {

        return taskRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found."
                        )
                );
    }

    // ============================================================
    // EMPLOYEE COMPLETES TASK
    // ============================================================

    public Task completeTask(
            Long taskId,
            Employee employee,
            String completionNote) {

        Task task = getTask(taskId);

        // Security check
        if (!task.getEmployee()
                .getId()
                .equals(employee.getId())) {

            throw new IllegalStateException(
                    "You are not allowed to update this task."
            );
        }

        if ("APPROVED".equals(task.getStatus())) {

            throw new IllegalStateException(
                    "This task has already been approved."
            );
        }

        if ("COMPLETED".equals(task.getStatus())) {

            throw new IllegalStateException(
                    "This task has already been submitted."
            );
        }

        task.setCompletionNote(completionNote);
        task.setCompletedAt(LocalDateTime.now());
        task.setStatus("COMPLETED");

        return taskRepository.save(task);
    }

    // ============================================================
    // ADMIN APPROVE
    // ============================================================

    public Task approveTask(
            Long taskId,
            String reviewNote) {

        Task task = getTask(taskId);

        if (!"COMPLETED".equals(task.getStatus())) {

            throw new IllegalStateException(
                    "Only completed tasks can be approved."
            );
        }

        task.setStatus("APPROVED");
        task.setReviewNote(reviewNote);
        task.setReviewedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    // ============================================================
    // ADMIN REJECT
    // ============================================================

    public Task rejectTask(
            Long taskId,
            String reviewNote) {

        Task task = getTask(taskId);

        if (!"COMPLETED".equals(task.getStatus())) {

            throw new IllegalStateException(
                    "Only completed tasks can be rejected."
            );
        }

        task.setStatus("REJECTED");
        task.setReviewNote(reviewNote);
        task.setReviewedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    // ============================================================
    // DELETE TASK
    // ============================================================

    public void deleteTask(Long taskId) {

        if (!taskRepository.existsById(taskId)) {

            throw new IllegalArgumentException(
                    "Task not found."
            );
        }

        taskRepository.deleteById(taskId);
    }
}