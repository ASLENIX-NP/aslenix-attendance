package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.TaskService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/employee/tasks")
public class EmployeeTaskController {

    private final EmployeeRepository employeeRepository;
    private final TaskService taskService;

    public EmployeeTaskController(
            EmployeeRepository employeeRepository,
            TaskService taskService) {

        this.employeeRepository =
                employeeRepository;

        this.taskService =
                taskService;
    }


    // ============================================================
    // EMPLOYEE TASK PAGE
    // ============================================================

    @GetMapping
    public String tasks(
            Authentication authentication,
            Model model) {

        Employee employee =
                getCurrentEmployee(authentication);

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
    // UPDATE PROGRESS
    // ============================================================
    //
    // FIXED: this used to return a view name / redirect
    // ("redirect:/employee/tasks"), which sends back an HTML
    // redirect response instead of JSON.
    //
    // tasks.html calls this via fetch() and then does
    // `await response.json()` — parsing HTML as JSON throws,
    // which is why the UI showed "Unable to save task progress"
    // even though the database write itself was succeeding.
    //
    // Now mirrors the /move endpoint: @ResponseBody + JSON,
    // with the same try/catch + status-code pattern.
    // ============================================================

    @PostMapping("/{id}/progress")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProgress(
            @PathVariable Long id,
            @RequestParam Integer progress,
            Authentication authentication) {

        Map<String, Object> response =
                new HashMap<>();

        try {

            // ----------------------------------------------------
            // GET CURRENT EMPLOYEE
            // ----------------------------------------------------

            Employee employee =
                    getCurrentEmployee(authentication);

            if (employee == null) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Employee not found."
                );

                return ResponseEntity
                        .status(401)
                        .body(response);
            }


            // ----------------------------------------------------
            // VALIDATE RANGE
            // ----------------------------------------------------

            if (progress == null
                    || progress < 0
                    || progress > 100) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Progress must be between 0 and 100."
                );

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }


            // ----------------------------------------------------
            // SAVE PROGRESS
            // ----------------------------------------------------
            //
            // NOTE: this assumes taskService.updateProgress(...)
            // returns the updated Task, mirroring how
            // taskService.moveTask(...) works below. If your
            // TaskService method currently returns void, change it
            // to `return taskRepository.save(task);` at the end,
            // or tell me and I'll adjust this controller to match.

            Task task =
                    taskService.updateProgress(
                            id,
                            employee,
                            progress
                    );


            // ----------------------------------------------------
            // SUCCESS RESPONSE
            // ----------------------------------------------------

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Progress saved."
            );

            response.put(
                    "progress",
                    task.getProgress()
            );

            response.put(
                    "status",
                    task.getStatus()
            );

            return ResponseEntity
                    .ok(response);


        } catch (SecurityException e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    e.getMessage()
            );

            return ResponseEntity
                    .status(403)
                    .body(response);


        } catch (
                IllegalArgumentException |
                IllegalStateException e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    e.getMessage()
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);


        } catch (Exception e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Unable to save task progress."
            );

            return ResponseEntity
                    .status(500)
                    .body(response);
        }
    }


    // ============================================================
    // SUBMIT FOR REVIEW
    // ============================================================

    @PostMapping("/{id}/submit-review")
    public String submitForReview(
            @PathVariable Long id,
            @RequestParam(required = false)
            String completionNote,
            Authentication authentication) {

        Employee employee =
                getCurrentEmployee(authentication);

        if (employee == null) {

            return "redirect:/login?employeeNotFound=true";
        }

        taskService.submitForReview(
                id,
                employee,
                completionNote
        );

        return "redirect:/employee/tasks";
    }


    // ============================================================
    // DRAG & DROP MOVE
    // ============================================================
    //
    // This endpoint is required by tasks.html.
    //
    // JavaScript sends:
    //
    // POST /employee/tasks/{id}/move
    //
    // status=TODO
    // status=IN_PROGRESS
    // status=READY_FOR_REVIEW
    //
    // APPROVED is never allowed for employees.
    //
    // IMPORTANT: taskService.moveTask() must only ever change
    // task.status. It must NOT set task.progress to 0/1/20/100
    // etc. based on the target status — progress is only ever
    // written by updateProgress() above, from the employee's
    // manual slider input.
    // ============================================================

    @PostMapping("/{id}/move")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> moveTask(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {

        Map<String, Object> response =
                new HashMap<>();


        try {

            // ----------------------------------------------------
            // GET CURRENT EMPLOYEE
            // ----------------------------------------------------

            Employee employee =
                    getCurrentEmployee(authentication);


            if (employee == null) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Employee not found."
                );

                return ResponseEntity
                        .status(401)
                        .body(response);
            }


            // ----------------------------------------------------
            // MOVE TASK
            // ----------------------------------------------------

            Task task =
                    taskService.moveTask(
                            id,
                            employee,
                            status
                    );


            // ----------------------------------------------------
            // SUCCESS RESPONSE
            // ----------------------------------------------------

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Task moved successfully."
            );

            response.put(
                    "status",
                    task.getStatus()
            );

            response.put(
                    "progress",
                    task.getProgress()
            );


            return ResponseEntity
                    .ok(response);


        } catch (SecurityException e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    e.getMessage()
            );

            return ResponseEntity
                    .status(403)
                    .body(response);


        } catch (
                IllegalArgumentException |
                IllegalStateException e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    e.getMessage()
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);


        } catch (Exception e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Unable to save task movement."
            );

            return ResponseEntity
                    .status(500)
                    .body(response);
        }
    }


    // ============================================================
    // GET CURRENT EMPLOYEE
    // ============================================================

    private Employee getCurrentEmployee(
            Authentication authentication) {

        if (authentication == null) {

            return null;
        }

        String username =
                authentication.getName();

        return employeeRepository
                .findByUserUsername(username)
                .orElse(null);
    }
}