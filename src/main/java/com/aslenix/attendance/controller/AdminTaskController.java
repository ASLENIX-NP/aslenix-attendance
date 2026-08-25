package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.TaskService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

        model.addAttribute(
                "tasks",
                taskService.getAllTasks()
        );

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
    // APPROVE
    // ============================================================

    @PostMapping("/{id}/approve")
    public String approveTask(
            @PathVariable Long id,
            @RequestParam(required = false) String reviewNote) {

        taskService.approveTask(
                id,
                reviewNote
        );

        return "redirect:/admin/tasks";
    }

    // ============================================================
    // REJECT
    // ============================================================

    @PostMapping("/{id}/reject")
    public String rejectTask(
            @PathVariable Long id,
            @RequestParam(required = false) String reviewNote) {

        taskService.rejectTask(
                id,
                reviewNote
        );

        return "redirect:/admin/tasks";
    }

    // ============================================================
    // DELETE
    // ============================================================

    @PostMapping("/{id}/delete")
    public String deleteTask(
            @PathVariable Long id) {

        taskService.deleteTask(id);

        return "redirect:/admin/tasks";
    }
}