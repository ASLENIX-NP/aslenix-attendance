package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.LeaveRequest;
import com.aslenix.attendance.service.LeaveRequestService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/leaves")
public class AdminLeaveController {

    private final LeaveRequestService leaveRequestService;

    public AdminLeaveController(
            LeaveRequestService leaveRequestService) {

        this.leaveRequestService = leaveRequestService;
    }

    // ============================================================
    // ADMIN LEAVE MANAGEMENT PAGE
    // ============================================================

    @GetMapping
    public String leaves(Model model) {

        List<LeaveRequest> leaveRequests =
                leaveRequestService.getAllRequests();

        // --------------------------------------------------------
        // Leave requests
        // --------------------------------------------------------

        model.addAttribute(
                "leaveRequests",
                leaveRequests
        );

        // --------------------------------------------------------
        // Statistics
        // --------------------------------------------------------

        model.addAttribute(
                "pendingCount",
                leaveRequestService.countAllPendingLeaves()
        );

        model.addAttribute(
                "approvedCount",
                leaveRequestService.countAllApprovedLeaves()
        );

        model.addAttribute(
                "rejectedCount",
                leaveRequestService.countAllRejectedLeaves()
        );

        return "admin/leave";
    }

    // ============================================================
    // APPROVE LEAVE
    // ============================================================

    @PostMapping("/{id}/approve")
    public String approveLeave(
            @PathVariable Long id,
            @RequestParam(
                    value = "reviewNote",
                    required = false
            ) String reviewNote) {

        leaveRequestService.approveLeave(
                id,
                reviewNote
        );

        return "redirect:/admin/leaves";
    }

    // ============================================================
    // REJECT LEAVE
    // ============================================================

    @PostMapping("/{id}/reject")
    public String rejectLeave(
            @PathVariable Long id,
            @RequestParam(
                    value = "reviewNote",
                    required = false
            ) String reviewNote) {

        leaveRequestService.rejectLeave(
                id,
                reviewNote
        );

        return "redirect:/admin/leaves";
    }

    // ============================================================
    // DELETE LEAVE
    // ============================================================

    @PostMapping("/{id}/delete")
    public String deleteLeave(
            @PathVariable Long id) {

        leaveRequestService.deleteLeave(id);

        return "redirect:/admin/leaves";
    }
}