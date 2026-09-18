package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.entity.TaskAssignment;
import com.aslenix.attendance.entity.TaskAssignmentHistory;
import com.aslenix.attendance.entity.TaskComment;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.LeaveRequestRepository;
import com.aslenix.attendance.service.TaskService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin/tasks")
public class AdminTaskController {

    private final TaskService taskService;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public AdminTaskController(
            TaskService taskService,
            EmployeeRepository employeeRepository,
            LeaveRequestRepository leaveRequestRepository) {
        this.taskService = taskService;
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    // ============================================================
    // TASK PAGE
    // ============================================================

    @GetMapping
    public String tasks(Model model) {
        // Trigger check for overdue work items to deliver immediate alerts
        try {
            taskService.checkAndNotifyOverdueWorkItems();
        } catch (Exception ignored) {
        }

        model.addAttribute("activePage", "tasks");
        model.addAttribute("tasks", taskService.getAllTasks());
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("pendingLeaveCount", leaveRequestRepository.countByStatus("PENDING"));
        return "admin/tasks";
    }

    // ============================================================
    // CREATE TASK (Multi-Assignee & Full Metadata)
    // ============================================================

    @PostMapping("/create")
    public String createTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "MEDIUM") String priority,
            @RequestParam(defaultValue = "MEDIUM") String complexity,
            @RequestParam(defaultValue = "TODO") String status,
            @RequestParam(required = false) Integer progress,
            @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) String deadlineBs,
            @RequestParam(required = false) String deadlineTime,
            @RequestParam(required = false) String tags) {

        List<Long> targetIds = new ArrayList<>();
        if (employeeIds != null && !employeeIds.isEmpty()) {
            targetIds.addAll(employeeIds);
        } else if (employeeId != null) {
            targetIds.add(employeeId);
        }

        List<Employee> assignees = targetIds.isEmpty() ? List.of() : employeeRepository.findAllById(targetIds);

        taskService.createTask(
                title,
                description,
                assignees,
                priority,
                complexity,
                status,
                progress,
                dueDate,
                dueDate,
                deadlineBs,
                deadlineTime,
                tags
        );

        return "redirect:/admin/tasks";
    }

    // ============================================================
    // UPDATE TASK (From Modal)
    // ============================================================

    @PostMapping("/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateTask(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "MEDIUM") String priority,
            @RequestParam(defaultValue = "MEDIUM") String complexity,
            @RequestParam(required = false) Integer progress,
            @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) String deadlineBs,
            @RequestParam(required = false) String deadlineTime,
            @RequestParam(required = false) String tags) {

        try {
            List<Employee> assignees = null;
            if (employeeIds != null) {
                assignees = employeeRepository.findAllById(employeeIds);
            }

            Task task = taskService.updateTask(
                    id,
                    title,
                    description,
                    assignees,
                    status,
                    priority,
                    complexity,
                    progress,
                    dueDate,
                    dueDate,
                    deadlineBs,
                    deadlineTime,
                    tags
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task updated successfully.",
                    "task", buildTaskJsonMap(task)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update task."
            ));
        }
    }

    // ============================================================
    // GET TASK JSON DATA
    // ============================================================

    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<?> getTaskJson(@PathVariable Long id) {
        try {
            Task task = taskService.getTask(id);
            return ResponseEntity.ok(buildTaskJsonMap(task));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Task not found."
            ));
        }
    }

    // ============================================================
    // WORK ASSIGNMENTS (Sub-tasks / Work Items)
    // ============================================================

    @PostMapping("/{id}/assignments/add")
    @ResponseBody
    public ResponseEntity<?> addAssignment(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "TODO") String status,
            @RequestParam(defaultValue = "MEDIUM") String weight,
            @RequestParam(required = false) LocalDate deadline,
            @RequestParam(required = false) String deadlineBs,
            @RequestParam(required = false) String deadlineTime,
            @RequestParam(required = false) Integer progress,
            @RequestParam(required = false) String note) {

        try {
            Employee assignee = assigneeId != null ? employeeRepository.findById(assigneeId).orElse(null) : null;
            TaskAssignment assignment = taskService.addAssignment(
                    id, title, description, assignee, deadline, deadlineBs, deadlineTime, progress, status, weight, note
            );
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Work item added successfully.",
                    "assignment", buildAssignmentMap(assignment),
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to add work item."
            ));
        }
    }

    @PostMapping("/{id}/assignments/{assignmentId}/update")
    @ResponseBody
    public ResponseEntity<?> updateAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) LocalDate deadline,
            @RequestParam(required = false) String deadlineBs,
            @RequestParam(required = false) String deadlineTime,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String weight,
            @RequestParam(required = false) String note) {

        try {
            Employee assignee = assigneeId != null ? employeeRepository.findById(assigneeId).orElse(null) : null;
            TaskAssignment assignment = taskService.updateAssignment(
                    id, assignmentId, title, description, assignee, deadline, deadlineBs, deadlineTime, status, weight, note
            );
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Work item updated.",
                    "assignment", buildAssignmentMap(assignment),
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update work item."
            ));
        }
    }

    @PostMapping("/{id}/assignments/{assignmentId}/progress")
    @ResponseBody
    public ResponseEntity<?> updateAssignmentProgress(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @RequestParam Integer progress,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String updateNote,
            Authentication authentication) {

        try {
            Employee currentEmployee = getCurrentEmployee(authentication);
            String progressNote = (updateNote != null && !updateNote.trim().isEmpty()) ? updateNote : note;
            TaskAssignment assignment = taskService.updateAssignmentProgress(assignmentId, progress, progressNote, currentEmployee);
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Work item progress updated.",
                    "assignment", buildAssignmentMap(assignment),
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update work item progress."
            ));
        }
    }

    @GetMapping("/{id}/assignments/{assignmentId}/history")
    @ResponseBody
    public ResponseEntity<?> getAssignmentHistory(
            @PathVariable Long id,
            @PathVariable Long assignmentId) {

        try {
            List<TaskAssignmentHistory> histories = taskService.getAssignmentHistories(assignmentId);
            List<Map<String, Object>> list = new ArrayList<>();
            for (TaskAssignmentHistory h : histories) {
                Map<String, Object> hMap = new HashMap<>();
                hMap.put("id", h.getId());
                hMap.put("previousProgress", h.getPreviousProgress());
                hMap.put("newProgress", h.getNewProgress());
                hMap.put("note", h.getNote());
                String author = "Administrator";
                String role = "ADMIN";
                if (h.getUpdatedBy() != null) {
                    author = (h.getUpdatedBy().getFirstName() + " " + (h.getUpdatedBy().getLastName() != null ? h.getUpdatedBy().getLastName() : "")).trim();
                    if (h.getUpdatedBy().getUser() != null && h.getUpdatedBy().getUser().getRole() != null) {
                        role = h.getUpdatedBy().getUser().getRole().name();
                    }
                }
                hMap.put("authorName", author);
                hMap.put("updatedByName", author);
                hMap.put("updatedByRole", role);
                hMap.put("createdAt", h.getCreatedAt() != null ? h.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "");
                list.add(hMap);
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to fetch work item history."
            ));
        }
    }

    @PostMapping("/{id}/assignments/{assignmentId}/status")
    @ResponseBody
    public ResponseEntity<?> updateAssignmentStatus(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @RequestParam String status) {

        try {
            TaskAssignment assignment = taskService.updateAssignmentStatus(assignmentId, status);
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Assignment status updated.",
                    "assignment", buildAssignmentMap(assignment),
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update status."
            ));
        }
    }

    @PostMapping("/{id}/assignments/{assignmentId}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId) {

        try {
            taskService.deleteAssignment(id, assignmentId);
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Assignment deleted.",
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to delete assignment."
            ));
        }
    }

    // ============================================================
    // DISCUSSION COMMENTS
    // ============================================================

    @PostMapping("/{id}/comments/add")
    @ResponseBody
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestParam String content) {

        try {
            TaskComment comment = taskService.addComment(id, "Administrator", "ADMIN", content);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Comment added.",
                    "comment", buildCommentMap(comment)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to add comment."
            ));
        }
    }

    // ============================================================
    // ADMIN DRAG & DROP TASK STATUS
    // ============================================================

    @PostMapping(value = {"/{id}/status", "/{id}/move"})
    @ResponseBody
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, Object> body) {

        try {
            String targetStatus = status;
            if ((targetStatus == null || targetStatus.trim().isEmpty()) && body != null && body.containsKey("status")) {
                targetStatus = Objects.toString(body.get("status"), null);
            }
            if (targetStatus == null || targetStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Status is required."
                ));
            }

            Task task = taskService.moveTaskByAdmin(id, targetStatus.trim());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task status updated successfully.",
                    "status", task.getStatus(),
                    "progress", task.getProgress(),
                    "locked", task.isLocked()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Error updating task status"
            ));
        }
    }

    // ============================================================
    // APPROVE & REJECT
    // ============================================================

    @PostMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveTask(
            @PathVariable Long id,
            @RequestParam(required = false) String reviewComment) {
        try {
            Task task = taskService.approveTask(id, reviewComment);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task approved and locked successfully.",
                    "status", task.getStatus(),
                    "progress", task.getProgress(),
                    "locked", task.isLocked()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to approve task."
            ));
        }
    }

    @PostMapping("/{id}/progress")
    @ResponseBody
    public ResponseEntity<?> updateProgress(
            @PathVariable Long id,
            @RequestParam Integer progress) {
        try {
            if (progress == null || progress < 0 || progress > 100) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Progress must be between 0 and 100."
                ));
            }

            Task task = taskService.getTask(id);
            if (task.isLocked()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Task is approved and locked. Progress cannot be modified."
                ));
            }

            task.setProgress(progress);
            if (progress >= 100) {
                task.setStatus("READY_FOR_REVIEW");
            } else if (progress == 0) {
                task.setStatus("TODO");
            } else {
                task.setStatus("IN_PROGRESS");
            }
            task = taskService.updateTask(id, task.getTitle(), task.getDescription(),
                    new ArrayList<>(task.getAssignees()), task.getStatus(), task.getPriority(),
                    task.getComplexity(), task.getProgress(), task.getDueDate(), task.getDeadline(),
                    task.getDeadlineBs(), task.getDeadlineTime(), task.getTags());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Progress updated successfully.",
                    "status", task.getStatus(),
                    "progress", task.getProgress()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Error updating progress."
            ));
        }
    }

    // ============================================================
    // DELETE TASK
    // ============================================================

    @PostMapping("/{id}/delete")
    public String deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
        } catch (Exception ignored) {
        }
        return "redirect:/admin/tasks";
    }

    // ============================================================
    // PRIVATE HELPERS FOR JSON BUILDERS
    // ============================================================

    private Employee getCurrentEmployee(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return employeeRepository.findByUserUsername(authentication.getName()).orElse(null);
    }

    private Map<String, Object> buildTaskJsonMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("taskCode", task.getTaskCode());
        map.put("title", task.getTitle());
        map.put("description", task.getDescription());
        map.put("status", task.getStatus());
        map.put("priority", task.getPriority());
        map.put("complexity", task.getComplexity());
        map.put("progress", task.getProgress() != null ? task.getProgress() : 0);
        map.put("deadlineBs", task.getDeadlineBs());
        map.put("deadlineTime", task.getDeadlineTime());
        map.put("tags", task.getTags());
        map.put("dueDate", task.getDueDate() != null ? task.getDueDate().toString() : null);
        map.put("completionNote", task.getCompletionNote());
        map.put("reviewNote", task.getReviewNote());
        map.put("locked", task.isLocked());
        map.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);

        // Assignees
        List<Map<String, Object>> assigneesList = new ArrayList<>();
        if (task.getAssignees() != null) {
            for (Employee emp : task.getAssignees()) {
                if (emp != null) {
                    Map<String, Object> empMap = new HashMap<>();
                    empMap.put("id", emp.getId());
                    String first = emp.getFirstName() != null ? emp.getFirstName() : "";
                    String last = emp.getLastName() != null ? emp.getLastName() : "";
                    empMap.put("name", (first + " " + last).trim());
                    empMap.put("code", emp.getEmployeeCode());
                    String initials = (first.isEmpty() ? "" : first.substring(0, 1)) +
                            (last.isEmpty() ? "" : last.substring(0, 1));
                    empMap.put("initials", initials.toUpperCase());
                    assigneesList.add(empMap);
                }
            }
        }
        if (assigneesList.isEmpty() && task.getEmployee() != null) {
            Employee emp = task.getEmployee();
            Map<String, Object> empMap = new HashMap<>();
            empMap.put("id", emp.getId());
            String first = emp.getFirstName() != null ? emp.getFirstName() : "";
            String last = emp.getLastName() != null ? emp.getLastName() : "";
            empMap.put("name", (first + " " + last).trim());
            empMap.put("code", emp.getEmployeeCode());
            String initials = (first.isEmpty() ? "" : first.substring(0, 1)) +
                    (last.isEmpty() ? "" : last.substring(0, 1));
            empMap.put("initials", initials.toUpperCase());
            assigneesList.add(empMap);
        }
        map.put("assignees", assigneesList);

        // Assignments
        List<Map<String, Object>> assignmentsList = new ArrayList<>();
        int completedCount = 0;
        int inProgressCount = 0;
        int underReviewCount = 0;
        int blockedCount = 0;
        int verifiedCount = 0;
        int waitingCount = 0;

        if (task.getAssignments() != null) {
            for (TaskAssignment a : task.getAssignments()) {
                assignmentsList.add(buildAssignmentMap(a));
                String st = a.getStatus() != null ? a.getStatus().toUpperCase() : "TODO";
                if ("COMPLETED".equals(st)) completedCount++;
                else if ("IN_PROGRESS".equals(st)) inProgressCount++;
                else if ("UNDER_REVIEW".equals(st)) underReviewCount++;
                else if ("BLOCKED".equals(st)) blockedCount++;
                else if ("VERIFIED".equals(st)) verifiedCount++;
                else if ("WAITING".equals(st)) waitingCount++;
            }
        }
        map.put("assignments", assignmentsList);

        // Sprint Summary Metrics
        int totalAssignments = assignmentsList.size();
        map.put("totalAssignments", totalAssignments);
        map.put("completedAssignments", completedCount);
        map.put("inProgressAssignments", inProgressCount);
        map.put("underReviewAssignments", underReviewCount);
        map.put("blockedAssignments", blockedCount);
        map.put("verifiedAssignments", verifiedCount);
        map.put("waitingAssignments", waitingCount);

        int weightedPct = totalAssignments > 0 ? (int) Math.round(((double) (completedCount + verifiedCount) / totalAssignments) * 100) : 0;
        map.put("weightedProgress", weightedPct);

        // Comments
        List<Map<String, Object>> commentsList = new ArrayList<>();
        if (task.getComments() != null) {
            for (TaskComment c : task.getComments()) {
                commentsList.add(buildCommentMap(c));
            }
        }
        map.put("comments", commentsList);

        return map;
    }

    private Map<String, Object> buildAssignmentMap(TaskAssignment a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("title", a.getTitle());
        map.put("description", a.getDescription() != null ? a.getDescription() : "");
        map.put("status", a.getStatus());
        map.put("weight", a.getWeight());
        map.put("note", a.getNote());
        map.put("deadline", a.getDeadline() != null ? a.getDeadline().toString() : "");
        map.put("deadlineBs", a.getDeadlineBs() != null ? a.getDeadlineBs() : "");
        map.put("deadlineTime", a.getDeadlineTime() != null ? a.getDeadlineTime() : "");
        map.put("progress", a.getProgress() != null ? a.getProgress() : 0);
        map.put("isOverdue", a.isOverdue());
        map.put("canEdit", a.getTask() == null || !a.getTask().isLocked());
        map.put("historiesCount", a.getHistories() != null ? a.getHistories().size() : 0);

        if (a.getAssignee() != null) {
            map.put("assigneeId", a.getAssignee().getId());
            String f = a.getAssignee().getFirstName() != null ? a.getAssignee().getFirstName() : "";
            String l = a.getAssignee().getLastName() != null ? a.getAssignee().getLastName() : "";
            map.put("assigneeName", (f + " " + l).trim());
        } else {
            map.put("assigneeId", null);
            map.put("assigneeName", "Unassigned");
        }
        return map;
    }

    private Map<String, Object> buildCommentMap(TaskComment c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("authorName", c.getAuthorName());
        map.put("authorRole", c.getAuthorRole());
        map.put("content", c.getContent());
        map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "");
        return map;
    }
}