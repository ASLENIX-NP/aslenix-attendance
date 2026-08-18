package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final OfficeSettingsRepository officeSettingsRepository;

    public AttendanceController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            OfficeSettingsRepository officeSettingsRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.officeSettingsRepository = officeSettingsRepository;
    }

    // ============================================================
    // CHECK IN
    // ============================================================

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(
            @RequestBody Map<String, Double> location,
            Authentication authentication) {

        try {

            // ----------------------------------------------------
            // Get logged-in username
            // ----------------------------------------------------

            String username = authentication.getName();

            Employee employee = employeeRepository
                    .findByUserUsername(username)
                    .orElse(null);

            if (employee == null) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Employee account not found."
                        )
                );
            }

            // ----------------------------------------------------
            // Get GPS coordinates sent by browser
            // ----------------------------------------------------

            Double latitude = location.get("latitude");
            Double longitude = location.get("longitude");

            if (latitude == null || longitude == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "GPS location was not provided."
                        )
                );
            }

            // ----------------------------------------------------
            // Get office settings
            // ----------------------------------------------------

            OfficeSettings settings =
                    officeSettingsRepository.findFirstByOrderByIdAsc()
                            .orElse(null);

            if (settings == null) {

                return ResponseEntity.internalServerError().body(
                        Map.of(
                                "success", false,
                                "message", "Office settings have not been configured."
                        )
                );
            }

            // ----------------------------------------------------
            // Check working day
            // ----------------------------------------------------

            LocalDate today = LocalDate.now();

            if (!isWorkingDay(settings, today.getDayOfWeek())) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Today is not a working day."
                        )
                );
            }

            // ----------------------------------------------------
            // Check whether employee already checked in
            // ----------------------------------------------------

            if (attendanceRepository
                    .findByEmployeeAndAttendanceDate(employee, today)
                    .isPresent()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "You have already checked in today."
                        )
                );
            }

            // ----------------------------------------------------
            // Calculate distance from office
            // ----------------------------------------------------

            double distance = calculateDistance(
                    latitude,
                    longitude,
                    settings.getLatitude(),
                    settings.getLongitude()
            );

            // ----------------------------------------------------
            // Check GPS radius
            // ----------------------------------------------------

            if (distance > settings.getAllowedRadiusMeters()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "You are outside the office attendance area.",
                                "distance",
                                Math.round(distance * 100.0) / 100.0,
                                "allowedRadius",
                                settings.getAllowedRadiusMeters()
                        )
                );
            }

            // ----------------------------------------------------
            // Create attendance record
            // ----------------------------------------------------

            LocalDateTime now = LocalDateTime.now();

            Attendance attendance = new Attendance();

            attendance.setEmployee(employee);
            attendance.setAttendanceDate(today);
            attendance.setCheckIn(now);

            attendance.setCheckInLatitude(latitude);
            attendance.setCheckInLongitude(longitude);
            attendance.setCheckInDistanceMeters(distance);

            // ----------------------------------------------------
            // Determine late status
            // ----------------------------------------------------

            LocalTime lateTime =
                    settings.getWorkStartTime()
                            .plusMinutes(settings.getLateGraceMinutes());

            boolean late = now.toLocalTime().isAfter(lateTime);

            attendance.setLate(late);

            if (late) {
                attendance.setStatus("LATE");
            } else {
                attendance.setStatus("PRESENT");
            }

            attendance.setEarlyLeave(false);
            attendance.setHalfDay(false);

            attendanceRepository.save(attendance);

            // ----------------------------------------------------
            // Successful response
            // ----------------------------------------------------

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", late
                                    ? "Check-in successful. You are late."
                                    : "Check-in successful.",
                            "checkInTime", now.toString(),
                            "distance",
                            Math.round(distance * 100.0) / 100.0,
                            "status",
                            attendance.getStatus()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message", "Unable to process check-in."
                    )
            );
        }
    }

    // ============================================================
    // CHECK OUT
    // ============================================================

    @PostMapping("/check-out")
    public ResponseEntity<?> checkOut(
            Authentication authentication) {

        try {

            // ----------------------------------------------------
            // Get logged-in employee
            // ----------------------------------------------------

            String username = authentication.getName();

            Employee employee = employeeRepository
                    .findByUserUsername(username)
                    .orElse(null);

            if (employee == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Employee account not found."
                        )
                );
            }

            // ----------------------------------------------------
            // Find today's attendance
            // ----------------------------------------------------

            LocalDate today = LocalDate.now();

            Attendance attendance =
                    attendanceRepository
                            .findByEmployeeAndAttendanceDate(
                                    employee,
                                    today
                            )
                            .orElse(null);

            if (attendance == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "You have not checked in today."
                        )
                );
            }

            // ----------------------------------------------------
            // Prevent duplicate checkout
            // ----------------------------------------------------

            if (attendance.getCheckOut() != null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "You have already checked out today."
                        )
                );
            }

            // ----------------------------------------------------
            // Save checkout
            // ----------------------------------------------------

            LocalDateTime now = LocalDateTime.now();

            attendance.setCheckOut(now);

            // ----------------------------------------------------
            // Determine early leave
            // ----------------------------------------------------

            OfficeSettings settings =
                    officeSettingsRepository
                            .findFirstByOrderByIdAsc()
                            .orElse(null);

            if (settings != null) {

                boolean early =
                        now.toLocalTime()
                                .isBefore(settings.getWorkEndTime());

                attendance.setEarlyLeave(early);
            }

            attendanceRepository.save(attendance);

            // ----------------------------------------------------
            // Successful response
            // ----------------------------------------------------

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Check-out successful.",
                            "checkOutTime", now.toString()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message", "Unable to process check-out."
                    )
            );
        }
    }

    // ============================================================
    // TODAY'S ATTENDANCE
    // ============================================================

    @GetMapping("/today")
    public ResponseEntity<?> today(
            Authentication authentication) {

        try {

            String username = authentication.getName();

            Employee employee = employeeRepository
                    .findByUserUsername(username)
                    .orElse(null);

            if (employee == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Employee account not found."
                        )
                );
            }

            LocalDate today = LocalDate.now();

            Attendance attendance =
                    attendanceRepository
                            .findByEmployeeAndAttendanceDate(
                                    employee,
                                    today
                            )
                            .orElse(null);

            if (attendance == null) {

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "checkedIn", false,
                                "checkedOut", false
                        )
                );
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "checkedIn", attendance.getCheckIn() != null,
                            "checkedOut", attendance.getCheckOut() != null,
                            "checkIn",
                            attendance.getCheckIn() != null
                                    ? attendance.getCheckIn().toString()
                                    : "",
                            "checkOut",
                            attendance.getCheckOut() != null
                                    ? attendance.getCheckOut().toString()
                                    : "",
                            "status",
                            attendance.getStatus(),
                            "late",
                            attendance.isLate(),
                            "earlyLeave",
                            attendance.isEarlyLeave(),
                            "distance",
                            attendance.getCheckInDistanceMeters() != null
                                    ? attendance.getCheckInDistanceMeters()
                                    : 0
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message", "Unable to load today's attendance."
                    )
            );
        }
    }

    // ============================================================
    // WORKING DAY CHECK
    // ============================================================

    private boolean isWorkingDay(
            OfficeSettings settings,
            DayOfWeek day) {

        return switch (day) {

            case SUNDAY -> settings.isSunday();

            case MONDAY -> settings.isMonday();

            case TUESDAY -> settings.isTuesday();

            case WEDNESDAY -> settings.isWednesday();

            case THURSDAY -> settings.isThursday();

            case FRIDAY -> settings.isFriday();

            case SATURDAY -> settings.isSaturday();
        };
    }

    // ============================================================
    // DISTANCE CALCULATION
    // ============================================================

    private double calculateDistance(
            double employeeLatitude,
            double employeeLongitude,
            double officeLatitude,
            double officeLongitude) {

        final double EARTH_RADIUS_METERS = 6371000;

        double lat1 = Math.toRadians(employeeLatitude);
        double lat2 = Math.toRadians(officeLatitude);

        double deltaLat =
                Math.toRadians(
                        officeLatitude - employeeLatitude
                );

        double deltaLon =
                Math.toRadians(
                        officeLongitude - employeeLongitude
                );

        double a =
                Math.sin(deltaLat / 2)
                        * Math.sin(deltaLat / 2)
                        +
                        Math.cos(lat1)
                                * Math.cos(lat2)
                                * Math.sin(deltaLon / 2)
                                * Math.sin(deltaLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return EARTH_RADIUS_METERS * c;
    }
}