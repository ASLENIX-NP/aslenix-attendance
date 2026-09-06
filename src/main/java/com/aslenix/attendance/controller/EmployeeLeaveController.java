package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.LeaveRequest;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.LeaveRequestService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/employee")
public class EmployeeLeaveController {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestService leaveRequestService;

    public EmployeeLeaveController(
            EmployeeRepository employeeRepository,
            LeaveRequestService leaveRequestService) {

        this.employeeRepository = employeeRepository;
        this.leaveRequestService = leaveRequestService;
    }

    // ============================================================
    // EMPLOYEE LEAVE PAGE
    // ============================================================

    @GetMapping("/leave")
    public String leavePage(
            Authentication authentication,
            Model model) {

        Employee employee = getLoggedInEmployee(authentication);

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "leaveRequests",
                leaveRequestService.getEmployeeRequests(employee)
        );

        model.addAttribute(
                "approvedLeaves",
                leaveRequestService.countApprovedLeaves(employee)
        );

        model.addAttribute(
                "pendingLeaves",
                leaveRequestService.countPendingLeaves(employee)
        );

        model.addAttribute(
                "rejectedLeaves",
                leaveRequestService.countRejectedLeaves(employee)
        );

        model.addAttribute(
                "onLeaveToday",
                leaveRequestService.isEmployeeOnLeaveToday(employee)
        );

        model.addAttribute(
                "today",
                LocalDate.now()
        );

        return "employee/leave";
    }

    // ============================================================
    // SUBMIT LEAVE REQUEST
    // ============================================================

    @PostMapping("/leave")
    public String submitLeave(
            Authentication authentication,

            @RequestParam("leaveType")
            String leaveType,

            @RequestParam("duration")
            String duration,

            @RequestParam(
                    value = "halfDaySession",
                    required = false
            )
            String halfDaySession,

            @RequestParam("startDate")
            LocalDate startDate,

            @RequestParam(
                    value = "endDate",
                    required = false
            )
            LocalDate endDate,

            @RequestParam(
                    value = "reason",
                    required = false
            )
            String reason) {

        Employee employee =
                getLoggedInEmployee(authentication);

        leaveRequestService.createLeaveRequest(
                employee,
                leaveType,
                duration,
                halfDaySession,
                startDate,
                endDate,
                reason
        );

        return "redirect:/employee/leave?success";
    }

    // ============================================================
    // DELETE PENDING LEAVE REQUEST
    // ============================================================

    @PostMapping("/leave/delete/{id}")
    public String deleteLeave(
            Authentication authentication,
            @PathVariable Long id) {

        Employee employee =
                getLoggedInEmployee(authentication);

        LeaveRequest request =
                leaveRequestService.getRequest(id);

        // --------------------------------------------------------
        // SECURITY CHECK
        // --------------------------------------------------------

        if (request.getEmployee() == null
                || request.getEmployee().getId() == null
                || !request.getEmployee()
                        .getId()
                        .equals(employee.getId())) {

            throw new IllegalStateException(
                    "You are not allowed to delete this leave request."
            );
        }

        // --------------------------------------------------------
        // ONLY PENDING REQUESTS CAN BE DELETED
        // --------------------------------------------------------

        if (!"PENDING".equals(request.getStatus())) {

            throw new IllegalStateException(
                    "Only pending leave requests can be deleted."
            );
        }

        leaveRequestService.deleteLeave(id);

        return "redirect:/employee/leave?deleted";
    }

    // ============================================================
    // FIND LOGGED-IN EMPLOYEE
    // ============================================================

    private Employee getLoggedInEmployee(
            Authentication authentication) {

        if (authentication == null) {

            throw new IllegalStateException(
                    "User is not authenticated."
            );
        }

        String username =
                authentication.getName();

        return employeeRepository
                .findByUserUsername(username)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Employee account not found for username: "
                                        + username
                        )
                );
    }
}