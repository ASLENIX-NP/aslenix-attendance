package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.repository.AttendanceRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminAttendanceController {

    private final AttendanceRepository attendanceRepository;

    public AdminAttendanceController(
            AttendanceRepository attendanceRepository
    ) {
        this.attendanceRepository = attendanceRepository;
    }

    // ============================================================
    // ADMIN ATTENDANCE PAGE
    // ============================================================

    @GetMapping("/attendance")
    public String attendance(Model model) {

        List<Attendance> attendanceRecords =
                attendanceRepository.findAll();

        // Newest attendance date first.
        // If two records have the same date,
        // newest check-in appears first.
        attendanceRecords.sort(
                Comparator
                        .comparing(
                                Attendance::getAttendanceDate,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                        .thenComparing(
                                Attendance::getCheckIn,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
        );

        model.addAttribute(
                "attendanceRecords",
                attendanceRecords
        );

        return "admin/attendance";
    }
}