package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.TaskService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/admin/tasks")
public class AdminTaskController {

    private final TaskService taskService;
    private final EmployeeRepository employeeRepository;


    public AdminTaskController(
            TaskService taskService,
            EmployeeRepository employeeRepository) {

        this.taskService = taskService;
        this.employeeRepository = employeeRepository;
    }


    // ============================================================
    // TASK PAGE
    // ============================================================

    @GetMapping
    public String tasks(Model model) {

        /*
         * Tell the shared admin sidebar which page is active.
         */
        model.addAttribute(
                "activePage",
                "tasks"
        );


        /*
         * Load all tasks.
         */
        model.addAttribute(
                "tasks",
                taskService.getAllTasks()
        );


        /*
         * Load employees for the task creation form.
         */
        model.addAttribute(
                "employees",
                employeeRepository.findAll()
        );


        return "admin/tasks";
    }


    // ============================================================
    // CREATE TASK
    // ============================================================

    @PostMapping("/create")
    public String createTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam Long employeeId,
            @RequestParam(defaultValue = "MEDIUM") String priority,
            @RequestParam(required = false) LocalDate dueDate) {

        Employee employee =
                employeeRepository
                        .findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found."
                                )
                        );


        taskService.createTask(
                title,
                description,
                employee,
                priority,
                dueDate
        );


        return "redirect:/admin/tasks";
    }


    // ============================================================
    // ADMIN DRAG & DROP TASK STATUS
    // ============================================================
    //
    // This endpoint is used by the admin Kanban board.
    //
    // Allowed statuses:
    //
    // TODO
    // IN_PROGRESS
    // READY_FOR_REVIEW
    // APPROVED
    //
    // Approved tasks themselves remain locked and cannot be moved
    // back to another stage.
    //
    // ============================================================

    @PostMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        try {

            taskService.moveTaskByAdmin(
                    id,
                    status
            );


            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Task status updated successfully.",
                            "status", status.trim().toUpperCase()
                    )
            );

        } catch (IllegalArgumentException |
                 IllegalStateException |
                 SecurityException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }


    // ============================================================
    // APPROVE TASK
    // ============================================================

    @PostMapping("/{id}/approve")
    public String approveTask(
            @PathVariable Long id,
            @RequestParam(required = false) String reviewComment) {

        taskService.approveTask(
                id,
                reviewComment
        );


        return "redirect:/admin/tasks";
    }


    // ============================================================
    // REJECT TASK
    // ============================================================

    @PostMapping("/{id}/reject")
    public String rejectTask(
            @PathVariable Long id,
            @RequestParam(required = false) String reviewComment) {

        taskService.rejectTask(
                id,
                reviewComment
        );


        return "redirect:/admin/tasks";
    }


    // ============================================================
    // DELETE TASK
    // ============================================================

    @PostMapping("/{id}/delete")
    public String deleteTask(
            @PathVariable Long id) {

        taskService.deleteTask(id);


        return "redirect:/admin/tasks";
    }

}