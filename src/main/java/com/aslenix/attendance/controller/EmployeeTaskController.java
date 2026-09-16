package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.entity.TaskAssignment;
import com.aslenix.attendance.entity.TaskComment;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.TaskService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

            return ResponseEntity.ok(buildTaskJsonMap(task));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
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

            Task task = taskService.updateProgress(id, employee, progress);

            response.put("success", true);
            response.put("message", "Progress saved.");
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
            response.put("message", "Unable to save task progress.");
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/{id}/submit-review")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitForReview(
            @PathVariable Long id,
            @RequestParam(required = false) String completionNote,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Employee employee = getCurrentEmployee(authentication);
            if (employee == null) {
                response.put("success", false);
                response.put("message", "Employee not found.");
                return ResponseEntity.status(401).body(response);
            }

            Task task = taskService.submitForReview(id, employee, completionNote);
            response.put("success", true);
            response.put("message", "Task submitted for review successfully.");
            response.put("status", task.getStatus());
            response.put("progress", task.getProgress());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage() != null ? e.getMessage() : "Failed to submit for review.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============================================================
    // DRAG & DROP MOVE
    // ============================================================

    @PostMapping("/{id}/move")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> moveTask(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            Employee employee = getCurrentEmployee(authentication);
            if (employee == null) {
                response.put("success", false);
                response.put("message", "Employee not found.");
                return ResponseEntity.status(401).body(response);
            }

            Task task = taskService.moveTask(id, employee, status);

            response.put("success", true);
            response.put("message", "Task moved successfully.");
            response.put("status", task.getStatus());
            response.put("progress", task.getProgress());

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
            response.put("message", "Unable to save task movement.");
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================================
    // WORK ASSIGNMENTS (Sub-tasks status update by employee)
    // ============================================================

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
            TaskAssignment assignment = taskService.updateAssignmentStatus(assignmentId, status);
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Assignment status updated.",
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/assignments/add")
    @ResponseBody
    public ResponseEntity<?> addAssignment(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String weight,
            @RequestParam(required = false) String note,
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

            TaskAssignment assignment = taskService.addAssignment(id, title, employee, "TODO", weight, note);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Work assignment added.",
                    "assignmentId", assignment.getId()
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

    private Map<String, Object> buildTaskJsonMap(Task task) {
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
                Map<String, Object> aMap = new HashMap<>();
                aMap.put("id", a.getId());
                aMap.put("title", a.getTitle());
                aMap.put("status", a.getStatus());
                aMap.put("weight", a.getWeight());
                aMap.put("note", a.getNote());
                if (a.getAssignee() != null) {
                    aMap.put("assigneeId", a.getAssignee().getId());
                    String f = a.getAssignee().getFirstName() != null ? a.getAssignee().getFirstName() : "";
                    String l = a.getAssignee().getLastName() != null ? a.getAssignee().getLastName() : "";
                    aMap.put("assigneeName", (f + " " + l).trim());
                }
                assignmentsList.add(aMap);

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
}