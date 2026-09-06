package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.repository.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaskService {


    private final TaskRepository taskRepository;


    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public TaskService(TaskRepository taskRepository) {

        this.taskRepository =
                taskRepository;
    }


    // ============================================================
    // GET ALL TASKS
    // ============================================================

    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {

        return taskRepository
                .findAllByOrderByCreatedAtDesc();
    }


    // ============================================================
    // GET EMPLOYEE TASKS
    // ============================================================

    @Transactional(readOnly = true)
    public List<Task> getEmployeeTasks(
            Employee employee) {

        if (employee == null) {

            return List.of();
        }

        return taskRepository
                .findByEmployeeOrderByCreatedAtDesc(
                        employee
                );
    }


    // ============================================================
    // GET TASK
    // ============================================================

    @Transactional(readOnly = true)
    public Task getTask(Long id) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Task ID is required."
            );
        }

        return taskRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found with ID: " + id
                        )
                );
    }


    // ============================================================
    // CREATE TASK
    // ============================================================

    public Task createTask(
            String title,
            String description,
            Employee employee,
            String priority,
            LocalDate dueDate
    ) {

        if (title == null
                || title.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }

        if (employee == null) {

            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }

        Task task =
                new Task();

        task.setTaskCode(
                generateTaskCode()
        );

        task.setTitle(
                title.trim()
        );

        task.setDescription(
                description == null
                        ? null
                        : description.trim()
        );

        task.setEmployee(
                employee
        );

        task.setPriority(
                normalizePriority(priority)
        );

        task.setStatus(
                "TODO"
        );

        task.setProgress(
                0
        );

        task.setDueDate(
                dueDate
        );

        task.setDeadline(
                null
        );

        LocalDateTime now =
                LocalDateTime.now();

        task.setCreatedAt(
                now
        );

        task.setUpdatedAt(
                now
        );

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // CREATE TASK WITH DEADLINE
    // ============================================================

    public Task createTask(
            String title,
            String description,
            Employee employee,
            String priority,
            LocalDate dueDate,
            LocalDate deadline
    ) {

        if (title == null
                || title.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }

        if (employee == null) {

            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }

        if (dueDate != null
                && deadline != null
                && deadline.isBefore(dueDate)) {

            throw new IllegalArgumentException(
                    "Deadline cannot be before the due date."
            );
        }

        Task task =
                new Task();

        task.setTaskCode(
                generateTaskCode()
        );

        task.setTitle(
                title.trim()
        );

        task.setDescription(
                description == null
                        ? null
                        : description.trim()
        );

        task.setEmployee(
                employee
        );

        task.setPriority(
                normalizePriority(priority)
        );

        task.setStatus(
                "TODO"
        );

        task.setProgress(
                0
        );

        task.setDueDate(
                dueDate
        );

        task.setDeadline(
                deadline
        );

        LocalDateTime now =
                LocalDateTime.now();

        task.setCreatedAt(
                now
        );

        task.setUpdatedAt(
                now
        );

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // UPDATE TASK
    // ============================================================

    public Task updateTask(
            Long id,
            String title,
            String description,
            Employee employee,
            String priority,
            LocalDate dueDate,
            LocalDate deadline
    ) {

        Task task =
                getTask(id);

        if (title == null
                || title.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }

        if (employee == null) {

            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }

        if (dueDate != null
                && deadline != null
                && deadline.isBefore(dueDate)) {

            throw new IllegalArgumentException(
                    "Deadline cannot be before the due date."
            );
        }

        task.setTitle(
                title.trim()
        );

        task.setDescription(
                description == null
                        ? null
                        : description.trim()
        );

        task.setEmployee(
                employee
        );

        task.setPriority(
                normalizePriority(priority)
        );

        task.setDueDate(
                dueDate
        );

        task.setDeadline(
                deadline
        );

        task.setUpdatedAt(
                LocalDateTime.now()
        );

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // START TASK
    // ============================================================

    public Task startTask(
            Long id,
            Employee employee
    ) {

        Task task =
                getTask(id);

        verifyEmployeeOwnership(
                task,
                employee
        );

        if ("APPROVED".equals(
                task.getStatus())) {

            throw new IllegalStateException(
                    "Approved tasks cannot be started again."
            );
        }

        task.setStatus(
                "IN_PROGRESS"
        );

        if (task.getProgress() == null
                || task.getProgress() < 1) {

            task.setProgress(
                    1
            );
        }

        task.setUpdatedAt(
                LocalDateTime.now()
        );

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // UPDATE PROGRESS
    // ============================================================

    public Task updateProgress(
            Long id,
            Employee employee,
            Integer progress
    ) {

        Task task =
                getTask(id);

        verifyEmployeeOwnership(
                task,
                employee
        );

        if ("APPROVED".equals(
                task.getStatus())) {

            throw new IllegalStateException(
                    "Approved tasks cannot be modified."
            );
        }

        if (progress == null) {

            throw new IllegalArgumentException(
                    "Progress is required."
            );
        }

        if (progress < 0
                || progress > 100) {

            throw new IllegalArgumentException(
                    "Progress must be between 0 and 100."
            );
        }

        task.setProgress(
                progress
        );


        if (progress == 0) {

            task.setStatus(
                    "TODO"
            );

        } else if (progress >= 100) {

            task.setProgress(
                    100
            );

            task.setStatus(
                    "READY_FOR_REVIEW"
            );

        } else {

            task.setStatus(
                    "IN_PROGRESS"
            );
        }

        task.setUpdatedAt(
                LocalDateTime.now()
        );

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // EMPLOYEE DRAG & DROP
    // ============================================================
    //
    // Employee can move:
    //
    // TODO
    // IN_PROGRESS
    // READY_FOR_REVIEW
    //
    // Employee cannot move a task to APPROVED.
    //
    // ============================================================

    public Task moveTask(
            Long id,
            Employee employee,
            String newStatus
    ) {

        Task task =
                getTask(id);


        verifyEmployeeOwnership(
                task,
                employee
        );


        if (newStatus == null
                || newStatus.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Task status is required."
            );
        }

        String status =
                newStatus
                        .trim()
                        .toUpperCase();


        if ("APPROVED".equals(
                task.getStatus())) {

            throw new IllegalStateException(
                    "Approved tasks cannot be moved."
            );
        }


        if ("APPROVED".equals(
                status)) {

            throw new SecurityException(
                    "Employees cannot approve tasks."
            );
        }


        if (!status.equals("TODO")
                && !status.equals("IN_PROGRESS")
                && !status.equals("READY_FOR_REVIEW")) {

            throw new IllegalArgumentException(
                    "Invalid task status."
            );
        }


        if ("TODO".equals(status)) {

            task.setStatus(
                    "TODO"
            );

            task.setProgress(
                    0
            );

            task.setCompletedAt(
                    null
            );
        }


        else if ("IN_PROGRESS".equals(status)) {

            task.setStatus(
                    "IN_PROGRESS"
            );

            if (task.getProgress() == null
                    || task.getProgress() <= 0
                    || task.getProgress() >= 100) {

                task.setProgress(
                        1
                );
            }

            task.setCompletedAt(
                    null
            );
        }


        else if ("READY_FOR_REVIEW".equals(status)) {

            task.setProgress(
                    100
            );

            task.setStatus(
                    "READY_FOR_REVIEW"
            );

            if (task.getCompletedAt() == null) {

                task.setCompletedAt(
                        LocalDateTime.now()
                );
            }
        }


        task.setUpdatedAt(
                LocalDateTime.now()
        );


        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // ADMIN DRAG & DROP
    // ============================================================
    //
    // ADMIN CAN MOVE:
    //
    // TODO
    // IN_PROGRESS
    // READY_FOR_REVIEW
    // APPROVED
    //
    // IMPORTANT:
    //
    // Once a task is APPROVED it remains locked.
    //
    // Therefore:
    //
    // TODO              -> any stage
    // IN_PROGRESS       -> any stage
    // READY_FOR_REVIEW  -> any stage
    // APPROVED          -> cannot move
    //
    // ============================================================

    public Task moveTaskByAdmin(
            Long id,
            String newStatus
    ) {

        Task task =
                getTask(id);


        // --------------------------------------------------------
        // VALIDATE STATUS
        // --------------------------------------------------------

        if (newStatus == null
                || newStatus.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Task status is required."
            );
        }


        String status =
                newStatus
                        .trim()
                        .toUpperCase();


        // --------------------------------------------------------
        // APPROVED TASKS ARE LOCKED
        // --------------------------------------------------------

        if ("APPROVED".equals(
                task.getStatus())) {

            throw new IllegalStateException(
                    "Approved tasks cannot be moved."
            );
        }


        // --------------------------------------------------------
        // VALID STATUSES
        // --------------------------------------------------------

        if (!status.equals("TODO")
                && !status.equals("IN_PROGRESS")
                && !status.equals("READY_FOR_REVIEW")
                && !status.equals("APPROVED")) {

            throw new IllegalArgumentException(
                    "Invalid task status."
            );
        }


        // ========================================================
        // MOVE TO TODO
        // ========================================================

        if ("TODO".equals(status)) {

            task.setStatus(
                    "TODO"
            );

            task.setProgress(
                    0
            );

            task.setCompletedAt(
                    null
            );
        }


        // ========================================================
        // MOVE TO IN PROGRESS
        // ========================================================

        else if ("IN_PROGRESS".equals(status)) {

            task.setStatus(
                    "IN_PROGRESS"
            );


            if (task.getProgress() == null
                    || task.getProgress() <= 0
                    || task.getProgress() >= 100) {

                task.setProgress(
                        1
                );
            }


            task.setCompletedAt(
                    null
            );
        }


        // ========================================================
        // MOVE TO READY FOR REVIEW
        // ========================================================

        else if ("READY_FOR_REVIEW".equals(status)) {

            task.setStatus(
                    "READY_FOR_REVIEW"
            );

            task.setProgress(
                    100
            );


            if (task.getCompletedAt() == null) {

                task.setCompletedAt(
                        LocalDateTime.now()
                );
            }
        }


        // ========================================================
        // MOVE TO APPROVED
        // ========================================================
        //
        // Admin is allowed to approve a task through drag/drop.
        //
        // This performs the same important database updates
        // as the normal Approve button.
        //
        // ========================================================

        else if ("APPROVED".equals(status)) {

            task.setStatus(
                    "APPROVED"
            );

            task.setProgress(
                    100
            );


            LocalDateTime now =
                    LocalDateTime.now();


            task.setReviewedAt(
                    now
            );

            task.setApprovedAt(
                    now
            );


            if (task.getCompletedAt() == null) {

                task.setCompletedAt(
                        now
                );
            }
        }


        // --------------------------------------------------------
        // UPDATE TIMESTAMP
        // --------------------------------------------------------

        task.setUpdatedAt(
                LocalDateTime.now()
        );


        // --------------------------------------------------------
        // SAVE
        // --------------------------------------------------------

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // SUBMIT FOR REVIEW
    // ============================================================

    public Task submitForReview(
            Long id,
            Employee employee,
            String completionNote
    ) {

        Task task =
                getTask(id);

        verifyEmployeeOwnership(
                task,
                employee
        );

        if ("APPROVED".equals(
                task.getStatus())) {

            throw new IllegalStateException(
                    "This task has already been approved."
            );
        }

        task.setProgress(
                100
        );

        task.setStatus(
                "READY_FOR_REVIEW"
        );

        task.setCompletionNote(
                completionNote == null
                        ? null
                        : completionNote.trim()
        );

        task.setCompletedAt(
                LocalDateTime.now()
        );

        task.setUpdatedAt(
                LocalDateTime.now()
        );

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // APPROVE TASK
    // ============================================================

    public Task approveTask(
            Long id,
            String reviewNote
    ) {

        Task task =
                getTask(id);

        if ("APPROVED".equals(
                task.getStatus())) {

            return task;
        }

        task.setStatus(
                "APPROVED"
        );

        task.setProgress(
                100
        );

        task.setReviewNote(
                reviewNote == null
                        ? null
                        : reviewNote.trim()
        );

        task.setReviewComment(
                reviewNote == null
                        ? null
                        : reviewNote.trim()
        );

        LocalDateTime now =
                LocalDateTime.now();

        task.setReviewedAt(
                now
        );

        task.setApprovedAt(
                now
        );

        task.setUpdatedAt(
                now
        );

        if (task.getCompletedAt() == null) {

            task.setCompletedAt(
                    now
            );
        }

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // REJECT / SEND BACK TASK
    // ============================================================

    public Task rejectTask(
            Long id,
            String reviewNote
    ) {

        Task task =
                getTask(id);

        if ("APPROVED".equals(
                task.getStatus())) {

            throw new IllegalStateException(
                    "An approved task cannot be rejected."
            );
        }

        task.setStatus(
                "IN_PROGRESS"
        );

        if (task.getProgress() == null
                || task.getProgress() >= 100) {

            task.setProgress(
                    90
            );
        }

        task.setReviewNote(
                reviewNote == null
                        ? null
                        : reviewNote.trim()
        );

        task.setReviewComment(
                reviewNote == null
                        ? null
                        : reviewNote.trim()
        );

        task.setReviewedAt(
                LocalDateTime.now()
        );

        task.setUpdatedAt(
                LocalDateTime.now()
        );

        return taskRepository.save(
                task
        );
    }


    // ============================================================
    // DELETE TASK
    // ============================================================

    public void deleteTask(
            Long id) {

        Task task =
                getTask(id);

        taskRepository.delete(
                task
        );
    }


    // ============================================================
    // EMPLOYEE TASK COUNT
    // ============================================================

    @Transactional(readOnly = true)
    public long countEmployeeTasks(
            Employee employee) {

        if (employee == null) {

            return 0;
        }

        return taskRepository
                .countByEmployee(
                        employee
                );
    }


    // ============================================================
    // EMPLOYEE TASK COUNT BY STATUS
    // ============================================================

    @Transactional(readOnly = true)
    public long countEmployeeTasksByStatus(
            Employee employee,
            String status
    ) {

        if (employee == null
                || status == null) {

            return 0;
        }

        return taskRepository
                .countByEmployeeAndStatus(
                        employee,
                        status
                                .trim()
                                .toUpperCase()
                );
    }


    // ============================================================
    // ALL TASK COUNT BY STATUS
    // ============================================================

    @Transactional(readOnly = true)
    public long countTasksByStatus(
            String status) {

        if (status == null) {

            return 0;
        }

        return taskRepository
                .countByStatus(
                        status
                                .trim()
                                .toUpperCase()
                );
    }


    // ============================================================
    // TODO
    // ============================================================

    @Transactional(readOnly = true)
    public long countTodoTasks() {

        return countTasksByStatus(
                "TODO"
        );
    }


    // ============================================================
    // IN PROGRESS
    // ============================================================

    @Transactional(readOnly = true)
    public long countInProgressTasks() {

        return countTasksByStatus(
                "IN_PROGRESS"
        );
    }


    // ============================================================
    // READY FOR REVIEW
    // ============================================================

    @Transactional(readOnly = true)
    public long countReadyForReviewTasks() {

        return countTasksByStatus(
                "READY_FOR_REVIEW"
        );
    }


    // ============================================================
    // APPROVED
    // ============================================================

    @Transactional(readOnly = true)
    public long countApprovedTasks() {

        return countTasksByStatus(
                "APPROVED"
        );
    }


    // ============================================================
    // VERIFY EMPLOYEE OWNERSHIP
    // ============================================================

    private void verifyEmployeeOwnership(
            Task task,
            Employee employee
    ) {

        if (employee == null) {

            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }

        if (task.getEmployee() == null
                || task.getEmployee().getId() == null
                || employee.getId() == null
                || !task.getEmployee()
                        .getId()
                        .equals(
                                employee.getId()
                        )) {

            throw new SecurityException(
                    "You are not authorized to modify this task."
            );
        }
    }


    // ============================================================
    // NORMALIZE PRIORITY
    // ============================================================

    private String normalizePriority(
            String priority) {

        if (priority == null
                || priority.trim().isEmpty()) {

            return "MEDIUM";
        }

        String normalized =
                priority
                        .trim()
                        .toUpperCase();

        if (!normalized.equals("LOW")
                && !normalized.equals("MEDIUM")
                && !normalized.equals("HIGH")
                && !normalized.equals("URGENT")) {

            throw new IllegalArgumentException(
                    "Invalid task priority."
            );
        }

        return normalized;
    }


    // ============================================================
    // GENERATE TASK CODE
    // ============================================================

    private String generateTaskCode() {

        String code;

        do {

            code =
                    "TASK-" +
                    UUID.randomUUID()
                            .toString()
                            .substring(
                                    0,
                                    8
                            )
                            .toUpperCase();

            final String generatedCode =
                    code;

            if (taskRepository
                    .findAll()
                    .stream()
                    .noneMatch(task ->
                            generatedCode.equals(
                                    task.getTaskCode()
                            ))) {

                return generatedCode;
            }

        } while (true);
    }
}