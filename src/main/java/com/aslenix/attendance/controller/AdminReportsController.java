package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.repository.AttendanceRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportsController {

    private final AttendanceRepository attendanceRepository;

    public AdminReportsController(
            AttendanceRepository attendanceRepository) {

        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping
    public String reports(Model model) {

        LocalDate today = LocalDate.now();

        // Get all attendance records
        List<Attendance> attendanceRecords =
                attendanceRepository.findAll();

        // Total attendance records
        long totalRecords =
                attendanceRecords.size();

        // Present
        long presentCount =
                attendanceRecords.stream()
                        .filter(a ->
                                "PRESENT".equals(a.getStatus()))
                        .count();

        // Late
        long lateCount =
                attendanceRecords.stream()
                        .filter(Attendance::isLate)
                        .count();

        // Early leave
        long earlyLeaveCount =
                attendanceRecords.stream()
                        .filter(Attendance::isEarlyLeave)
                        .count();

        // Half day
        long halfDayCount =
                attendanceRecords.stream()
                        .filter(Attendance::isHalfDay)
                        .count();

        // Today's attendance records
        long todayCount =
                attendanceRecords.stream()
                        .filter(a ->
                                today.equals(
                                        a.getAttendanceDate()
                                ))
                        .count();

        // Send data to Thymeleaf
        model.addAttribute(
                "attendanceRecords",
                attendanceRecords
        );

        model.addAttribute(
                "totalRecords",
                totalRecords
        );

        model.addAttribute(
                "presentCount",
                presentCount
        );

        model.addAttribute(
                "lateCount",
                lateCount
        );

        model.addAttribute(
                "earlyLeaveCount",
                earlyLeaveCount
        );

        model.addAttribute(
                "halfDayCount",
                halfDayCount
        );

        model.addAttribute(
                "todayCount",
                todayCount
        );

        return "admin/reports";
    }
}