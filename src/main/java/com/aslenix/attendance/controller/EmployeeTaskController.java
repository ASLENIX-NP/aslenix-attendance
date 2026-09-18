package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.entity.TaskAssignment;
import com.aslenix.attendance.entity.TaskAssignmentHistory;
import com.aslenix.attendance.entity.TaskComment;
import com.aslenix.attendance.repository.EmployeeRepository;
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
@RequestMapping("/employee/tasks")
public class EmployeeTaskController {

    private final EmployeeRepository employeeRepository;
    private final TaskService taskService;

    public EmployeeTaskController(
            EmployeeRepository employeeRepository,
            TaskService taskService) {
        this.employeeRepository = employeeRepository;
        this.taskService = taskService;
    }

    // ============================================================
    // EMPLOYEE TASK PAGE
    // ============================================================

    @GetMapping
    public String tasks(Authentication authentication, Model model) {
        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return "redirect:/login?employeeNotFound=true";
        }

        model.addAttribute("employee", employee);
        model.addAttribute("tasks", taskService.getEmployeeTasks(employee));
        return "employee/tasks";
    }

    // ============================================================
    // GET TASK JSON DATA
    // ============================================================

    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<?> getTaskJson(
            @PathVariable Long id,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Task task = taskService.getTask(id);
            // Verify assigned
            boolean isAssigned = (task.getEmployee() != null && task.getEmployee().getId().equals(employee.getId())) ||
                    (task.getAssignees() != null && task.getAssignees().stream().anyMatch(a -> a.getId().equals(employee.getId())));

            if (!isAssigned) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }

            return ResponseEntity.ok(buildTaskJsonMap(task, employee));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
        }
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
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "MEDIUM") String priority,
            @RequestParam(defaultValue = "MEDIUM") String complexity,
            @RequestParam(required = false) Integer progress,
            @RequestParam(required = false) String deadlineBs,
            @RequestParam(required = false) String deadlineTime,
            @RequestParam(required = false) String tags,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Task task = taskService.updateTaskByEmployee(
                    id,
                    employee,
                    title,
                    description,
                    status,
                    priority,
                    complexity,
                    progress,
                    deadlineBs,
                    deadlineTime,
                    tags
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task updated successfully.",
                    "task", buildTaskJsonMap(task, employee)
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update task."
            ));
        }
    }

    // ============================================================
    // UPDATE PROGRESS
    // ============================================================

    @PostMapping("/{id}/progress")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProgress(
            @PathVariable Long id,
            @RequestParam Integer progress,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            Employee employee = getCurrentEmployee(authentication);
            if (employee == null) {
                response.put("success", false);
                response.put("message", "Employee not found.");
                return ResponseEntity.status(401).body(response);
            }

            if (progress == null || progress < 0 || progress > 100) {
                response.put("success", false);
                response.put("message", "Progress must be between 0 and 100.");
                return ResponseEntity.badRequest().body(response);
            }

            Task task = taskService.getTask(id);
            if (task.isLocked()) {
                response.put("success", false);
                response.put("message", "Task is approved and locked. Progress cannot be modified.");
                return ResponseEntity.badRequest().body(response);
            }

            task = taskService.updateProgress(id, employee, progress);

            response.put("success", true);
            response.put("message", "Progress updated successfully.");
            response.put("progress", task.getProgress());
            response.put("status", task.getStatus());
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An error occurred while updating progress.");
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================================
    // WORK ASSIGNMENTS (Work Items)
    // ============================================================

    @PostMapping("/{id}/assignments/add")
    @ResponseBody
    public ResponseEntity<?> addAssignment(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "MEDIUM") String weight,
            @RequestParam(required = false) LocalDate deadline,
            @RequestParam(required = false) String deadlineBs,
            @RequestParam(required = false) String deadlineTime,
            @RequestParam(required = false) String note,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Task task = taskService.getTask(id);
            if (task.isLocked()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Task is approved and locked."));
            }

            boolean isAssigned = (task.getEmployee() != null && task.getEmployee().getId().equals(employee.getId())) ||
                    (task.getAssignees() != null && task.getAssignees().stream().anyMatch(a -> a.getId().equals(employee.getId())));

            if (!isAssigned) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }

            Employee targetAssignee = employee;
            if (assigneeId != null) {
                Employee chosen = employeeRepository.findById(assigneeId).orElse(null);
                if (chosen != null) {
                    targetAssignee = chosen;
                }
            }

            TaskAssignment assignment = taskService.addAssignment(
                    id, title, description, targetAssignee, deadline, deadlineBs, deadlineTime, 0, "TODO", weight, note
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Work item added.",
                    "assignment", buildAssignmentMap(assignment, employee),
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
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

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Task task = taskService.getTask(id);
            if (task.isLocked()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Task is approved and locked."));
            }

            String progressNote = (updateNote != null && !updateNote.trim().isEmpty()) ? updateNote : note;
            TaskAssignment assignment = taskService.updateAssignmentProgress(assignmentId, progress, progressNote, employee);
            Task refreshedTask = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Work item progress updated.",
                    "assignment", buildAssignmentMap(assignment, employee),
                    "taskProgress", refreshedTask.getProgress(),
                    "taskStatus", refreshedTask.getStatus()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update work item progress."
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
            @RequestParam(required = false) LocalDate deadline,
            @RequestParam(required = false) String deadlineBs,
            @RequestParam(required = false) String deadlineTime,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String weight,
            @RequestParam(required = false) String note,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Task task = taskService.getTask(id);
            if (task.isLocked()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Task is approved and locked."));
            }

            // Ownership check: must be assigned to this work item
            TaskAssignment existing = task.getAssignments().stream()
                    .filter(a -> a.getId().equals(assignmentId))
                    .findFirst()
                    .orElse(null);

            if (existing == null || existing.getAssignee() == null || !existing.getAssignee().getId().equals(employee.getId())) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "You can only edit your own work items."));
            }

            TaskAssignment updated = taskService.updateAssignment(
                    id, assignmentId, title, description, employee, deadline, deadlineBs, deadlineTime, status, weight, note
            );

            Task refreshedTask = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Work item updated.",
                    "assignment", buildAssignmentMap(updated, employee),
                    "taskProgress", refreshedTask.getProgress(),
                    "taskStatus", refreshedTask.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/assignments/{assignmentId}/history")
    @ResponseBody
    public ResponseEntity<?> getAssignmentHistory(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            List<TaskAssignmentHistory> histories = taskService.getAssignmentHistories(assignmentId);
            List<Map<String, Object>> list = new ArrayList<>();
            for (TaskAssignmentHistory h : histories) {
                Map<String, Object> hMap = new HashMap<>();
                hMap.put("id", h.getId());
                hMap.put("previousProgress", h.getPreviousProgress());
                hMap.put("newProgress", h.getNewProgress());
                hMap.put("note", h.getNote());
                String author = "Team Member";
                String role = "EMPLOYEE";
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
            @RequestParam String status,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Task task = taskService.getTask(id);
            if (task.isLocked()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Task is approved and locked."));
            }

            TaskAssignment assignment = taskService.updateAssignmentStatus(assignmentId, status);
            Task refreshedTask = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Assignment status updated.",
                    "assignment", buildAssignmentMap(assignment, employee),
                    "taskProgress", refreshedTask.getProgress(),
                    "taskStatus", refreshedTask.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/assignments/{assignmentId}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Task task = taskService.getTask(id);
            if (task.isLocked()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Task is approved and locked."));
            }

            TaskAssignment existing = task.getAssignments().stream()
                    .filter(a -> a.getId().equals(assignmentId))
                    .findFirst()
                    .orElse(null);

            if (existing == null || existing.getAssignee() == null || !existing.getAssignee().getId().equals(employee.getId())) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "You can only delete your own work items."));
            }

            taskService.deleteAssignment(id, assignmentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Assignment deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ============================================================
    // DISCUSSION COMMENTS
    // ============================================================

    @PostMapping("/{id}/comments/add")
    @ResponseBody
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestParam String content,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            String authorName = (employee.getFirstName() + " " + (employee.getLastName() != null ? employee.getLastName() : "")).trim();
            TaskComment comment = taskService.addComment(id, authorName, "EMPLOYEE", content);

            Map<String, Object> cMap = new HashMap<>();
            cMap.put("id", comment.getId());
            cMap.put("authorName", comment.getAuthorName());
            cMap.put("authorRole", comment.getAuthorRole());
            cMap.put("content", comment.getContent());
            cMap.put("createdAt", comment.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Comment posted.",
                    "comment", cMap
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ============================================================
    // CURRENT EMPLOYEE
    // ============================================================

    private Employee getCurrentEmployee(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String username = authentication.getName();
        return employeeRepository.findByUserUsername(username).orElse(null);
    }

    // ============================================================
    // JSON MAPPING HELPER
    // ============================================================

    private Map<String, Object> buildTaskJsonMap(Task task, Employee currentEmployee) {
        Map<String, Object> map = new LinkedHashMap<>();
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

        List<Map<String, Object>> assignmentsList = new ArrayList<>();
        int completedCount = 0;
        int inProgressCount = 0;
        int underReviewCount = 0;
        int blockedCount = 0;
        int verifiedCount = 0;
        int waitingCount = 0;

        if (task.getAssignments() != null) {
            for (TaskAssignment a : task.getAssignments()) {
                assignmentsList.add(buildAssignmentMap(a, currentEmployee));
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

        List<Map<String, Object>> commentsList = new ArrayList<>();
        if (task.getComments() != null) {
            for (TaskComment c : task.getComments()) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("id", c.getId());
                cMap.put("authorName", c.getAuthorName());
                cMap.put("authorRole", c.getAuthorRole());
                cMap.put("content", c.getContent());
                cMap.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "");
                commentsList.add(cMap);
            }
        }
        map.put("comments", commentsList);

        return map;
    }

    private Map<String, Object> buildAssignmentMap(TaskAssignment a, Employee currentEmployee) {
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
        map.put("historiesCount", a.getHistories() != null ? a.getHistories().size() : 0);

        boolean isAssignedToCurrent = currentEmployee != null && a.getAssignee() != null &&
                currentEmployee.getId().equals(a.getAssignee().getId());
        boolean isTaskUnlocked = a.getTask() == null || !a.getTask().isLocked();
        map.put("canEdit", isAssignedToCurrent && isTaskUnlocked);

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
}