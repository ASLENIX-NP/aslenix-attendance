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

        List<Task> tasks = taskService.getEmployeeTasks(employee);

        long myTasksCount = tasks.size();
        long inProgressCount = tasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long completedCount = tasks.stream().filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus())).count();
        long reviewCount = tasks.stream().filter(t -> "READY_FOR_REVIEW".equalsIgnoreCase(t.getStatus())).count();
        double avgProgress = myTasksCount > 0
                ? tasks.stream().mapToInt(t -> t.getProgress() != null ? t.getProgress() : 0).average().orElse(0)
                : 0;

        model.addAttribute("activePage", "tasks");
        model.addAttribute("employee", employee);
        model.addAttribute("tasks", tasks);
        model.addAttribute("myTasksCount", myTasksCount);
        model.addAttribute("inProgressTasksCount", inProgressCount);
        model.addAttribute("completedTasksCount", completedCount);
        model.addAttribute("reviewTasksCount", reviewCount);
        model.addAttribute("avgProgress", Math.round(avgProgress));

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
            return ResponseEntity.ok(buildTaskJsonMap(task, employee));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
        }
    }

    // ============================================================
    // UPDATE SUBTASK PROGRESS (MANUAL WITH MANDATORY NOTE)
    // ============================================================

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
            if (progressNote == null || progressNote.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "A description/update note is mandatory when changing subtask progress."
                ));
            }

            TaskAssignment assignment = taskService.updateAssignmentProgress(assignmentId, progress, progressNote, employee);
            Task refreshedTask = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Subtask progress updated.",
                    "assignment", buildAssignmentMap(assignment, employee),
                    "taskProgress", refreshedTask.getProgress(),
                    "taskStatus", refreshedTask.getStatus()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update subtask progress."
            ));
        }
    }

    // ============================================================
    // SUBMIT SUBTASK FOR REVIEW (EMPLOYEE -> ADMIN)
    // ============================================================

    @PostMapping("/{id}/assignments/{assignmentId}/submit-review")
    @ResponseBody
    public ResponseEntity<?> submitAssignmentForReview(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String completionNote,
            Authentication authentication) {

        Employee employee = getCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            String submitNote = (completionNote != null && !completionNote.trim().isEmpty()) ? completionNote : note;
            if (submitNote == null || submitNote.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "A description/note is mandatory when submitting work for review."
                ));
            }

            TaskAssignment assignment = taskService.submitAssignmentForReview(assignmentId, submitNote, employee);
            Task refreshedTask = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Subtask submitted for review! Admin will review your work.",
                    "assignment", buildAssignmentMap(assignment, employee),
                    "taskProgress", refreshedTask.getProgress(),
                    "taskStatus", refreshedTask.getStatus()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to submit subtask for review."
            ));
        }
    }

    // ============================================================
    // SUBTASK AUDIT HISTORY
    // ============================================================

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
                String author = "System";
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

    // ============================================================
    // REJECT MANUAL TASK PROGRESS UPDATE
    // ============================================================

    @PostMapping("/{id}/progress")
    @ResponseBody
    public ResponseEntity<?> updateProgress(
            @PathVariable Long id,
            @RequestParam Integer progress,
            Authentication authentication) {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Task total progress cannot be manually changed. It is automatically calculated from its subtasks."
        ));
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
    // JSON MAPPING HELPERS
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
        map.put("weeksRequired", task.getWeeksRequired());
        map.put("progress", task.getProgress() != null ? task.getProgress() : 0);
        map.put("locked", task.isLocked());
        map.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);

        // Team Leads
        List<Map<String, Object>> teamLeadsList = new ArrayList<>();
        if (task.getTeamLeads() != null) {
            for (Employee emp : task.getTeamLeads()) {
                if (emp != null) {
                    teamLeadsList.add(buildEmployeeSummaryMap(emp));
                }
            }
        }
        map.put("teamLeads", teamLeadsList);

        // Assignees
        List<Map<String, Object>> assigneesList = new ArrayList<>();
        if (task.getAssignees() != null) {
            for (Employee emp : task.getAssignees()) {
                if (emp != null) {
                    assigneesList.add(buildEmployeeSummaryMap(emp));
                }
            }
        }
        if (assigneesList.isEmpty() && task.getEmployee() != null) {
            assigneesList.add(buildEmployeeSummaryMap(task.getEmployee()));
        }
        map.put("assignees", assigneesList);

        // Subtasks
        List<Map<String, Object>> assignmentsList = new ArrayList<>();
        int approvedCount = 0;
        int reviewCount = 0;
        int inProgressCount = 0;

        if (task.getAssignments() != null) {
            List<TaskAssignment> sortedAssignments = new ArrayList<>(task.getAssignments());
            sortedAssignments.sort(Comparator.comparing(a -> a.getSubtaskNumber() != null ? a.getSubtaskNumber() : 0));
            for (TaskAssignment a : sortedAssignments) {
                assignmentsList.add(buildAssignmentMap(a, currentEmployee));
                String st = a.getStatus() != null ? a.getStatus().toUpperCase() : "TODO";
                if ("APPROVED".equals(st) || "COMPLETED".equals(st)) approvedCount++;
                else if ("READY_FOR_REVIEW".equals(st)) reviewCount++;
                else if ("IN_PROGRESS".equals(st)) inProgressCount++;
            }
        }
        map.put("assignments", assignmentsList);
        map.put("totalSubtasks", assignmentsList.size());
        map.put("approvedSubtasks", approvedCount);
        map.put("reviewSubtasks", reviewCount);
        map.put("inProgressSubtasks", inProgressCount);

        // Comments
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

    private Map<String, Object> buildEmployeeSummaryMap(Employee emp) {
        Map<String, Object> empMap = new HashMap<>();
        empMap.put("id", emp.getId());
        String first = emp.getFirstName() != null ? emp.getFirstName() : "";
        String last = emp.getLastName() != null ? emp.getLastName() : "";
        empMap.put("name", (first + " " + last).trim());
        empMap.put("code", emp.getEmployeeCode());
        String initials = (first.isEmpty() ? "" : first.substring(0, 1)) +
                (last.isEmpty() ? "" : last.substring(0, 1));
        empMap.put("initials", initials.toUpperCase());
        empMap.put("photoUrl", emp.getPhotoUrl());
        return empMap;
    }

    private Map<String, Object> buildAssignmentMap(TaskAssignment a, Employee currentEmployee) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("weekNumber", a.getWeekNumber());
        map.put("subtaskNumber", a.getSubtaskNumber());
        map.put("title", a.getTitle());
        map.put("description", a.getDescription());
        map.put("status", a.getStatus());
        map.put("weight", a.getWeight());
        map.put("progress", a.getProgress() != null ? a.getProgress() : 0);
        map.put("declineReason", a.getDeclineReason());
        map.put("locked", a.isLocked());
        map.put("approvedAt", a.getApprovedAt() != null ? a.getApprovedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : null);

        boolean isAssignee = a.getAssignee() != null && currentEmployee != null && a.getAssignee().getId().equals(currentEmployee.getId());

        map.put("isAssignee", isAssignee);
        map.put("canEdit", !a.isLocked() && isAssignee);

        if (a.getAssignee() != null) {
            map.put("assignee", buildEmployeeSummaryMap(a.getAssignee()));
        } else {
            map.put("assignee", null);
        }

        return map;
    }
}