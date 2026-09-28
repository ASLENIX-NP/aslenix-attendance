package com.aslenix.attendance.controller;

import com.aslenix.attendance.dto.SubtaskInputDto;
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
        List<Task> allTasks = taskService.getAllTasks();
        model.addAttribute("activePage", "tasks");
        model.addAttribute("tasks", allTasks);
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("pendingLeaveCount", leaveRequestRepository.countByStatus("PENDING"));

        long totalCount = allTasks.size();
        long inProgressCount = allTasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long completedCount = allTasks.stream().filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus())).count();
        long reviewCount = allTasks.stream().filter(t -> "READY_FOR_REVIEW".equalsIgnoreCase(t.getStatus())).count();
        double avgProgress = totalCount > 0
                ? allTasks.stream().mapToInt(t -> t.getProgress() != null ? t.getProgress() : 0).average().orElse(0)
                : 0;

        model.addAttribute("totalTasksCount", totalCount);
        model.addAttribute("inProgressTasksCount", inProgressCount);
        model.addAttribute("completedTasksCount", completedCount);
        model.addAttribute("reviewTasksCount", reviewCount);
        model.addAttribute("avgProgress", Math.round(avgProgress));

        return "admin/tasks";
    }

    // ============================================================
    // CREATE TASK (WEEKLY SUBTASK STRUCTURE)
    // ============================================================

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "MEDIUM") String complexity,
            @RequestParam(defaultValue = "MEDIUM") String priority,
            @RequestParam(defaultValue = "1") Integer weeksRequired,
            @RequestParam(required = false) List<Long> teamLeadIds,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) String subtasksJson,
            @RequestParam(required = false) List<Long> subtaskIds,
            @RequestParam(required = false) List<Integer> subtaskWeekNumbers,
            @RequestParam(required = false) List<String> subtaskTitles,
            @RequestParam(required = false) List<String> subtaskDescriptions,
            @RequestParam(required = false) List<String> subtaskAssigneeIds) {

        try {
            int weeks = (weeksRequired != null && weeksRequired > 0) ? weeksRequired : 1;
            List<Employee> teamLeads = (teamLeadIds != null && !teamLeadIds.isEmpty())
                    ? employeeRepository.findAllById(teamLeadIds) : List.of();
            List<Employee> assignees = (employeeIds != null && !employeeIds.isEmpty())
                    ? employeeRepository.findAllById(employeeIds) : List.of();

            List<SubtaskInputDto> subtasks = parseSubtasks(subtasksJson, subtaskIds, subtaskWeekNumbers, subtaskTitles, subtaskDescriptions, subtaskAssigneeIds, weeks);

            taskService.createTask(
                    title,
                    description,
                    complexity,
                    priority,
                    weeks,
                    teamLeads,
                    assignees,
                    subtasks
            );

            return ResponseEntity.ok(Map.of("success", true, "message", "Task and weekly subtasks created successfully."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to create task."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An unexpected error occurred: " + e.getMessage()
            ));
        }
    }

    // ============================================================
    // UPDATE TASK (WEEKLY SUBTASK STRUCTURE)
    // ============================================================

    @PostMapping("/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateTask(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "MEDIUM") String complexity,
            @RequestParam(defaultValue = "MEDIUM") String priority,
            @RequestParam(defaultValue = "1") Integer weeksRequired,
            @RequestParam(required = false) List<Long> teamLeadIds,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) String subtasksJson,
            @RequestParam(required = false) List<Long> subtaskIds,
            @RequestParam(required = false) List<Integer> subtaskWeekNumbers,
            @RequestParam(required = false) List<String> subtaskTitles,
            @RequestParam(required = false) List<String> subtaskDescriptions,
            @RequestParam(required = false) List<String> subtaskAssigneeIds) {

        try {
            int weeks = (weeksRequired != null && weeksRequired > 0) ? weeksRequired : 1;
            List<Employee> teamLeads = teamLeadIds != null ? employeeRepository.findAllById(teamLeadIds) : null;
            List<Employee> assignees = employeeIds != null ? employeeRepository.findAllById(employeeIds) : null;

            List<SubtaskInputDto> subtasks = parseSubtasks(subtasksJson, subtaskIds, subtaskWeekNumbers, subtaskTitles, subtaskDescriptions, subtaskAssigneeIds, weeks);

            Task task = taskService.updateTask(
                    id,
                    title,
                    description,
                    complexity,
                    priority,
                    weeks,
                    teamLeads,
                    assignees,
                    subtasks
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task updated successfully.",
                    "task", buildTaskJsonMap(task)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update task."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An unexpected error occurred: " + e.getMessage()
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
            @RequestParam Integer progress) {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Task total progress cannot be manually changed. It is automatically calculated from its subtasks."
        ));
    }

    // ============================================================
    // ADMIN APPROVE SUBTASK
    // ============================================================

    @PostMapping("/{id}/assignments/{assignmentId}/approve")
    @ResponseBody
    public ResponseEntity<?> approveAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            Authentication authentication) {
        try {
            Employee adminEmployee = getCurrentEmployee(authentication);
            TaskAssignment assignment = taskService.approveAssignment(assignmentId, adminEmployee);
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Subtask approved and locked.",
                    "assignment", buildAssignmentMap(assignment),
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to approve subtask."
            ));
        }
    }

    // ============================================================
    // ADMIN DECLINE SUBTASK (MANDATORY REASON)
    // ============================================================

    @PostMapping("/{id}/assignments/{assignmentId}/decline")
    @ResponseBody
    public ResponseEntity<?> declineAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @RequestParam String declineReason,
            Authentication authentication) {
        try {
            if (declineReason == null || declineReason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "A decline reason is mandatory when declining a subtask."
                ));
            }

            Employee adminEmployee = getCurrentEmployee(authentication);
            TaskAssignment assignment = taskService.declineAssignment(assignmentId, declineReason, adminEmployee);
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Subtask declined. Employee has been notified with the reason.",
                    "assignment", buildAssignmentMap(assignment),
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to decline subtask."
            ));
        }
    }

    // ============================================================
    // SUBTASK PROGRESS UPDATE (MANUAL WITH MANDATORY NOTE)
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

        try {
            Employee currentEmployee = getCurrentEmployee(authentication);
            String progressNote = (updateNote != null && !updateNote.trim().isEmpty()) ? updateNote : note;

            if (progressNote == null || progressNote.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "A description/update note is mandatory when changing progress."
                ));
            }

            TaskAssignment assignment = taskService.updateAssignmentProgress(assignmentId, progress, progressNote, currentEmployee);
            Task task = taskService.getTask(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Subtask progress updated.",
                    "assignment", buildAssignmentMap(assignment),
                    "taskProgress", task.getProgress(),
                    "taskStatus", task.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update subtask progress."
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

    // ============================================================
    // GET TASK DATA (FOR MODALS)
    // ============================================================

    @GetMapping("/{id}/data")
    @ResponseBody
    public ResponseEntity<?> getTaskData(@PathVariable Long id) {
        try {
            Task task = taskService.getTask(id);
            return ResponseEntity.ok(buildTaskJsonMap(task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Task not found."
            ));
        }
    }

    // ============================================================
    // DELETE TASK
    // ============================================================

    @PostMapping("/{id}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Task deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to delete task."
            ));
        }
    }

    // ============================================================
    // COMMENTS
    // ============================================================

    @PostMapping("/{id}/comments/add")
    @ResponseBody
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestParam String comment,
            Authentication authentication) {
        try {
            Employee author = getCurrentEmployee(authentication);
            TaskComment c = taskService.addComment(id, author, comment);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Comment added.",
                    "comment", buildCommentMap(c)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to add comment."
            ));
        }
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private List<SubtaskInputDto> parseSubtasks(
            String subtasksJson,
            List<Long> subtaskIds,
            List<Integer> subtaskWeekNumbers,
            List<String> subtaskTitles,
            List<String> subtaskDescriptions,
            List<String> subtaskAssigneeIds,
            int weeksRequired) {

        List<SubtaskInputDto> result = new ArrayList<>();

        // 1. Try parsing JSON if provided
        if (subtasksJson != null && !subtasksJson.trim().isEmpty() && subtasksJson.startsWith("[")) {
            try {
                String inner = subtasksJson.trim();
                if (inner.startsWith("[")) inner = inner.substring(1);
                if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
                String[] objects = inner.split("\\},\\s*\\{");
                int seq = 1;
                for (String obj : objects) {
                    String clean = obj.replace("{", "").replace("}", "");
                    Long id = null;
                    Integer weekNumber = 1;
                    String desc = "";
                    String title = "";
                    Long assigneeId = null;

                    for (String pair : clean.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
                        String[] kv = pair.split(":", 2);
                        if (kv.length == 2) {
                            String key = kv[0].replace("\"", "").trim();
                            String val = kv[1].replace("\"", "").trim();
                            if ("id".equalsIgnoreCase(key) && !val.isEmpty() && !"null".equalsIgnoreCase(val)) {
                                try { id = Long.parseLong(val); } catch (Exception ignored) {}
                            } else if ("weekNumber".equalsIgnoreCase(key) && !val.isEmpty() && !"null".equalsIgnoreCase(val)) {
                                try { weekNumber = Integer.parseInt(val); } catch (Exception ignored) {}
                            } else if ("description".equalsIgnoreCase(key)) {
                                desc = val;
                            } else if ("title".equalsIgnoreCase(key) && !val.isEmpty()) {
                                title = val;
                            } else if ("assigneeId".equalsIgnoreCase(key) && !val.isEmpty() && !"null".equalsIgnoreCase(val)) {
                                try { assigneeId = Long.parseLong(val); } catch (Exception ignored) {}
                            }
                        }
                    }

                    if (title.isEmpty()) {
                        title = "Week " + weekNumber + " Subtask " + seq;
                    }

                    result.add(new SubtaskInputDto(id, weekNumber, title, desc, assigneeId));
                    seq++;
                }
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Fallback to list parameters
        if (subtaskDescriptions != null && !subtaskDescriptions.isEmpty()) {
            for (int i = 0; i < subtaskDescriptions.size(); i++) {
                Long id = (subtaskIds != null && i < subtaskIds.size()) ? subtaskIds.get(i) : null;
                int weekNum = (subtaskWeekNumbers != null && i < subtaskWeekNumbers.size() && subtaskWeekNumbers.get(i) != null && subtaskWeekNumbers.get(i) > 0)
                        ? subtaskWeekNumbers.get(i)
                        : (i + 1);
                String title = (subtaskTitles != null && i < subtaskTitles.size() && subtaskTitles.get(i) != null && !subtaskTitles.get(i).trim().isEmpty())
                        ? subtaskTitles.get(i).trim() : ("Week " + weekNum + " Subtask " + (i + 1));
                String desc = subtaskDescriptions.get(i);
                Long assigneeId = null;
                if (subtaskAssigneeIds != null && i < subtaskAssigneeIds.size()) {
                    String rawId = subtaskAssigneeIds.get(i);
                    if (rawId != null && !rawId.trim().isEmpty() && !"null".equalsIgnoreCase(rawId.trim())) {
                        try {
                            assigneeId = Long.parseLong(rawId.trim());
                        } catch (NumberFormatException ignored) {}
                    }
                }
                result.add(new SubtaskInputDto(id, weekNum, title, desc, assigneeId));
            }
        }

        return result;
    }

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
                assignmentsList.add(buildAssignmentMap(a));
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
                commentsList.add(buildCommentMap(c));
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

    private Map<String, Object> buildAssignmentMap(TaskAssignment a) {
        Map<String, Object> map = new HashMap<>();
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

        if (a.getAssignee() != null) {
            map.put("assignee", buildEmployeeSummaryMap(a.getAssignee()));
        } else {
            map.put("assignee", null);
        }
        return map;
    }

    private Map<String, Object> buildCommentMap(TaskComment c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("content", c.getContent());
        map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "");
        map.put("authorName", c.getAuthorName() != null ? c.getAuthorName() : "System");
        map.put("authorRole", c.getAuthorRole() != null ? c.getAuthorRole() : "EMPLOYEE");
        return map;
    }
}