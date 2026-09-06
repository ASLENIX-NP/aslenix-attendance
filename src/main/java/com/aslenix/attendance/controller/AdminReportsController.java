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

        /*
         * =========================================================
         * GET ALL ATTENDANCE RECORDS
         * =========================================================
         */
        List<Attendance> attendanceRecords =
                attendanceRepository.findAll();

        /*
         * =========================================================
         * TOTAL RECORDS
         * =========================================================
         */
        long totalRecords =
                attendanceRecords.size();

        /*
         * =========================================================
         * PRESENT
         *
         * A normal present record has status PRESENT.
         * =========================================================
         */
        long presentCount =
                attendanceRecords.stream()
                        .filter(a ->
                                "PRESENT".equalsIgnoreCase(
                                        a.getStatus()
                                ))
                        .count();

        /*
         * =========================================================
         * LATE
         * =========================================================
         */
        long lateCount =
                attendanceRecords.stream()
                        .filter(Attendance::isLate)
                        .count();

        /*
         * =========================================================
         * EARLY LEAVE
         * =========================================================
         */
        long earlyLeaveCount =
                attendanceRecords.stream()
                        .filter(Attendance::isEarlyLeave)
                        .count();

        /*
         * =========================================================
         * HALF DAY
         * =========================================================
         */
        long halfDayCount =
                attendanceRecords.stream()
                        .filter(Attendance::isHalfDay)
                        .count();

        /*
         * =========================================================
         * TODAY'S RECORDS
         * =========================================================
         */
        long todayCount =
                attendanceRecords.stream()
                        .filter(a ->
                                a.getAttendanceDate() != null
                                        && today.equals(
                                        a.getAttendanceDate()
                                ))
                        .count();

        /*
         * =========================================================
         * SEND DATA TO THYMELEAF
         * =========================================================
         */
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