package com.aslenix.attendance.controller;

import com.aslenix.attendance.dto.QrAttendanceRequest;
import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class QRVerificationController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final OfficeSettingsRepository officeSettingsRepository;

    public QRVerificationController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            OfficeSettingsRepository officeSettingsRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.officeSettingsRepository = officeSettingsRepository;
    }

    // ============================================================
    // QR CHECK-IN / CHECK-OUT
    // ============================================================

    @PostMapping("/qr")
    public ResponseEntity<?> qrAttendance(
            @RequestBody QrAttendanceRequest request) {

        // ========================================================
        // 1. VALIDATE REQUEST
        // ========================================================

        if (request == null) {
            return error("Invalid attendance request.");
        }

        // ========================================================
        // 2. VALIDATE QR TOKEN
        // ========================================================

        if (request.getQrToken() == null ||
                request.getQrToken().isBlank()) {

            return error("Invalid QR code.");
        }

        String qrToken =
                request.getQrToken().trim();

        // ========================================================
        // 3. FIND EMPLOYEE BY QR TOKEN
        // ========================================================

        Employee employee =
                employeeRepository
                        .findByQrToken(qrToken)
                        .orElse(null);

        if (employee == null) {

            return error(
                    "This QR code is not registered."
            );
        }

        // ========================================================
        // 4. CHECK EMPLOYEE ACCOUNT
        // ========================================================

        if (!employee.isEnabled()) {

            return error(
                    "This employee account is disabled."
            );
        }

        // ========================================================
        // 5. VALIDATE LOCATION
        // ========================================================

        double latitude =
                request.getLatitude();

        double longitude =
                request.getLongitude();

        if (Double.isNaN(latitude) ||
                Double.isNaN(longitude) ||
                Double.isInfinite(latitude) ||
                Double.isInfinite(longitude)) {

            return error(
                    "Unable to determine your location."
            );
        }

        // ========================================================
        // 6. GET OFFICE SETTINGS
        // ========================================================

        OfficeSettings settings =
                officeSettingsRepository
                        .findFirstByOrderByIdAsc()
                        .orElse(null);

        if (settings == null) {

            return error(
                    "Office location has not been configured."
            );
        }

        // ========================================================
        // 7. CALCULATE DISTANCE
        // ========================================================

        double distance =
                calculateDistance(
                        latitude,
                        longitude,
                        settings.getLatitude(),
                        settings.getLongitude()
                );

        // ========================================================
        // 8. GET ALLOWED OFFICE RADIUS
        // ========================================================

        double allowedRadius =
        settings.getAttendanceRadius();
        // ========================================================
        // 9. CHECK OFFICE RADIUS
        // ========================================================

        if (allowedRadius <= 0) {

            return error(
                    "Office attendance radius has not been configured."
            );
        }

        if (distance > allowedRadius) {

            return error(
                    "You are outside the office attendance area. "
                            + "You are approximately "
                            + Math.round(distance)
                            + " meters away. "
                            + "Allowed radius is "
                            + Math.round(allowedRadius)
                            + " meters."
            );
        }

        // ========================================================
        // 10. GET TODAY'S ATTENDANCE
        // ========================================================

        LocalDate today =
                LocalDate.now();

        Attendance attendance =
                attendanceRepository
                        .findByEmployeeAndAttendanceDate(
                                employee,
                                today
                        )
                        .orElse(null);

        // ========================================================
        // 11. CHECK-IN
        // ========================================================

        if (attendance == null) {

            attendance =
                    new Attendance();

            attendance.setEmployee(employee);

            attendance.setAttendanceDate(today);

            attendance.setCheckIn(
                    LocalDateTime.now()
            );

            attendance.setStatus("PRESENT");

            attendanceRepository.save(
                    attendance
            );

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "success",
                    true
            );

            response.put(
                    "action",
                    "CHECK-IN"
            );

            response.put(
                    "message",
                    "Check-in successful."
            );

            response.put(
                    "employeeName",
                    employee.getFirstName()
                            + " "
                            + employee.getLastName()
            );

            response.put(
                    "employeeCode",
                    employee.getEmployeeCode()
            );

            response.put(
                    "time",
                    formatTime(
                            attendance.getCheckIn()
                    )
            );

            response.put(
                    "distance",
                    Math.round(distance)
            );

            response.put(
                    "allowedRadius",
                    Math.round(allowedRadius)
            );

            return ResponseEntity.ok(
                    response
            );
        }

        // ========================================================
        // 12. ALREADY CHECKED OUT
        // ========================================================

        if (attendance.getCheckOut() != null) {

            return error(
                    "You have already checked out today."
            );
        }

        // ========================================================
        // 13. CHECK-OUT
        // ========================================================

        attendance.setCheckOut(
                LocalDateTime.now()
        );

        attendanceRepository.save(
                attendance
        );

        // ========================================================
        // 14. CALCULATE WORKING HOURS
        // ========================================================

        Duration duration =
                Duration.between(
                        attendance.getCheckIn(),
                        attendance.getCheckOut()
                );

        long totalMinutes =
                Math.max(
                        duration.toMinutes(),
                        0
                );

        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;

        // ========================================================
        // 15. CHECK-OUT RESPONSE
        // ========================================================

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "action",
                "CHECK-OUT"
        );

        response.put(
                "message",
                "Check-out successful."
        );

        response.put(
                "employeeName",
                employee.getFirstName()
                        + " "
                        + employee.getLastName()
        );

        response.put(
                "employeeCode",
                employee.getEmployeeCode()
        );

        response.put(
                "time",
                formatTime(
                        attendance.getCheckOut()
                )
        );

        response.put(
                "workingHours",
                hours
                        + "h "
                        + String.format(
                                "%02d",
                                minutes
                        )
                        + "m"
        );

        response.put(
                "distance",
                Math.round(distance)
        );

        response.put(
                "allowedRadius",
                Math.round(allowedRadius)
        );

        return ResponseEntity.ok(
                response
        );
    }

    // ============================================================
    // DISTANCE CALCULATION - HAVERSINE
    // ============================================================

    private double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final double earthRadius =
                6371000.0;

        double dLat =
                Math.toRadians(
                        lat2 - lat1
                );

        double dLon =
                Math.toRadians(
                        lon2 - lon1
                );

        double a =
                Math.sin(dLat / 2)
                        * Math.sin(dLat / 2)
                        +
                        Math.cos(
                                Math.toRadians(lat1)
                        )
                        *
                        Math.cos(
                                Math.toRadians(lat2)
                        )
                        *
                        Math.sin(dLon / 2)
                        *
                        Math.sin(dLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return earthRadius * c;
    }

    // ============================================================
    // FORMAT TIME
    // ============================================================

    private String formatTime(
            LocalDateTime time) {

        return time.format(
                DateTimeFormatter.ofPattern(
                        "hh:mm a"
                )
        );
    }

    // ============================================================
    // ERROR RESPONSE
    // ============================================================

    private ResponseEntity<Map<String, Object>> error(
            String message) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                false
        );

        response.put(
                "message",
                message
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }
}

