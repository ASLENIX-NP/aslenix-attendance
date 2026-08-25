package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.TaskService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String tasks(
            Authentication authentication,
            Model model) {

        String username =
                authentication.getName();

        Employee employee =
                employeeRepository
                        .findByUserUsername(username)
                        .orElse(null);

        if (employee == null) {

            return "redirect:/login?employeeNotFound=true";
        }

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "tasks",
                taskService.getEmployeeTasks(employee)
        );

        return "employee/tasks";
    }

    // ============================================================
    // COMPLETE TASK
    // ============================================================

    @PostMapping("/{id}/complete")
    public String completeTask(
            @PathVariable Long id,
            @RequestParam(required = false) String completionNote,
            Authentication authentication) {

        String username =
                authentication.getName();

        Employee employee =
                employeeRepository
                        .findByUserUsername(username)
                        .orElse(null);

        if (employee == null) {

            return "redirect:/login?employeeNotFound=true";
        }

        taskService.completeTask(
                id,
                employee,
                completionNote
        );

        return "redirect:/employee/tasks";
    }
}