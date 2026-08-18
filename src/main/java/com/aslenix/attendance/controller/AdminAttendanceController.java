package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.repository.AttendanceRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/attendance")
public class AdminAttendanceController {

    private final AttendanceRepository attendanceRepository;

    public AdminAttendanceController(
            AttendanceRepository attendanceRepository) {

        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping
    public String attendance(Model model) {

        List<Attendance> attendanceRecords =
                attendanceRepository.findAll();

        model.addAttribute(
                "attendanceRecords",
                attendanceRecords
        );

        return "admin/attendance";
    }
}