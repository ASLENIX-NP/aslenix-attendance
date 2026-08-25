package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import com.aslenix.attendance.service.AttendanceService;

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
    private final AttendanceService attendanceService;

    public AttendanceController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            OfficeSettingsRepository officeSettingsRepository,
            AttendanceService attendanceService) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.officeSettingsRepository = officeSettingsRepository;
        this.attendanceService = attendanceService;
    }

    // ============================================================
    // CHECK IN
    // ============================================================

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(
            @RequestBody Map<String, Double> location,
            Authentication authentication) {

        try {

            String username =
                    authentication.getName();

            Employee employee =
                    employeeRepository
                            .findByUserUsername(username)
                            .orElse(null);

            if (employee == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Employee account not found."
                        )
                );
            }

            Double latitude =
                    location.get("latitude");

            Double longitude =
                    location.get("longitude");

            if (latitude == null
                    || longitude == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "GPS location was not provided."
                        )
                );
            }

            Attendance attendance =
                    attendanceService.checkIn(
                            employee,
                            latitude,
                            longitude
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            attendance.isLate()
                                    ? "Check-in successful. You are late."
                                    : "Check-in successful.",
                            "checkInTime",
                            attendance.getCheckIn().toString(),
                            "status",
                            attendance.getStatus(),
                            "late",
                            attendance.isLate()
                    )
            );

        } catch (IllegalStateException
                 | IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Unable to process check-in."
                    )
            );
        }
    }

    // ============================================================
    // CHECK OUT
    // ============================================================

    @PostMapping("/check-out")
    public ResponseEntity<?> checkOut(
            @RequestBody(required = false)
            Map<String, Double> location,
            Authentication authentication) {

        try {

            String username =
                    authentication.getName();

            Employee employee =
                    employeeRepository
                            .findByUserUsername(username)
                            .orElse(null);

            if (employee == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Employee account not found."
                        )
                );
            }

            /*
             * If scanner sends GPS coordinates,
             * use them.
             */
            double latitude = 0;
            double longitude = 0;

            if (location != null) {

                if (location.get("latitude") != null) {
                    latitude =
                            location.get("latitude");
                }

                if (location.get("longitude") != null) {
                    longitude =
                            location.get("longitude");
                }
            }

            /*
             * If no GPS was provided, use the
             * normal service validation only when
             * coordinates are available.
             */
            if (latitude == 0 && longitude == 0) {

                Attendance attendance =
                        attendanceRepository
                                .findByEmployeeAndAttendanceDate(
                                        employee,
                                        LocalDate.now()
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

                if (attendance.getCheckOut() != null) {

                    return ResponseEntity.badRequest().body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "You have already checked out today."
                            )
                    );

                }

                LocalDateTime now =
                        LocalDateTime.now();

                attendance.setCheckOut(now);

                OfficeSettings settings =
                        officeSettingsRepository
                                .findFirstByOrderByIdAsc()
                                .orElse(null);

                if (settings != null) {

                    boolean early =
                            settings.getWorkEndTime() != null
                                    && now.toLocalTime()
                                    .isBefore(
                                            settings.getWorkEndTime()
                                    );

                    attendance.setEarlyLeave(early);
                }

                attendanceRepository.save(attendance);

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "message",
                                "Check-out successful.",
                                "checkOutTime",
                                now.toString()
                        )
                );
            }

            Attendance attendance =
                    attendanceService.checkOut(
                            employee,
                            latitude,
                            longitude
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Check-out successful.",
                            "checkOutTime",
                            attendance
                                    .getCheckOut()
                                    .toString()
                    )
            );

        } catch (IllegalStateException
                 | IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Unable to process check-out."
                    )
            );
        }
    }

    // ============================================================
    // TODAY
    // ============================================================

    @GetMapping("/today")
    public ResponseEntity<?> today(
            Authentication authentication) {

        try {

            String username =
                    authentication.getName();

            Employee employee =
                    employeeRepository
                            .findByUserUsername(username)
                            .orElse(null);

            if (employee == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Employee account not found."
                        )
                );
            }

            Attendance attendance =
                    attendanceService
                            .getTodayAttendance(employee);

            if (attendance == null) {

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "checkedIn", false,
                                "checkedOut", false,
                                "status",
                                "NOT CHECKED IN",
                                "checkIn",
                                "",
                                "checkOut",
                                "",
                                "workingHours",
                                "0h 00m"
                        )
                );
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,

                            "checkedIn",
                            attendance.getCheckIn() != null,

                            "checkedOut",
                            attendance.getCheckOut() != null,

                            "checkIn",
                            attendance.getCheckIn() != null
                                    ? attendance.getCheckIn().toString()
                                    : "",

                            "checkOut",
                            attendance.getCheckOut() != null
                                    ? attendance.getCheckOut().toString()
                                    : "",

                            "status",
                            attendanceService
                                    .getTodayStatus(employee),

                            "late",
                            attendance.isLate(),

                            "earlyLeave",
                            attendance.isEarlyLeave(),

                            "workingHours",
                            attendanceService
                                    .getTodayWorkingHours(employee)
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Unable to load today's attendance."
                    )
            );
        }
    }

    // ============================================================
    // DASHBOARD DATA
    // ============================================================

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(
            Authentication authentication) {

        try {

            String username =
                    authentication.getName();

            Employee employee =
                    employeeRepository
                            .findByUserUsername(username)
                            .orElse(null);

            if (employee == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Employee account not found."
                        )
                );
            }

            long attendanceThisMonth =
                    attendanceService
                            .getAttendanceThisMonth(employee);

            long presentCount =
                    attendanceService
                            .getPresentCount(employee);

            long absentCount =
                    attendanceService
                            .getAbsentCount(employee);

            long lateCount =
                    attendanceService
                            .getLateCount(employee);

            long leaveCount =
                    attendanceService
                            .getLeaveCount(employee);

            String todayStatus =
                    attendanceService
                            .getTodayStatus(employee);

            String checkInTime =
                    attendanceService
                            .getTodayCheckInTime(employee);

            String checkOutTime =
                    attendanceService
                            .getTodayCheckOutTime(employee);

            String workingHours =
                    attendanceService
                            .getTodayWorkingHours(employee);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,

                            "attendanceThisMonth",
                            attendanceThisMonth,

                            "presentCount",
                            presentCount,

                            "absentCount",
                            absentCount,

                            "lateCount",
                            lateCount,

                            "leaveCount",
                            leaveCount,

                            "todayStatus",
                            todayStatus,

                            "checkInTime",
                            checkInTime,

                            "checkOutTime",
                            checkOutTime,

                            "workingHours",
                            workingHours
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Unable to load dashboard data."
                    )
            );
        }
    }

    // ============================================================
    // WORKING DAY
    // ============================================================

    private boolean isWorkingDay(
            OfficeSettings settings,
            DayOfWeek day) {

        return switch (day) {

            case SUNDAY ->
                    settings.isSunday();

            case MONDAY ->
                    settings.isMonday();

            case TUESDAY ->
                    settings.isTuesday();

            case WEDNESDAY ->
                    settings.isWednesday();

            case THURSDAY ->
                    settings.isThursday();

            case FRIDAY ->
                    settings.isFriday();

            case SATURDAY ->
                    settings.isSaturday();
        };
    }

    // ============================================================
    // DISTANCE
    // ============================================================

    private double calculateDistance(
            double employeeLatitude,
            double employeeLongitude,
            double officeLatitude,
            double officeLongitude) {

        final double EARTH_RADIUS_METERS =
                6371000;

        double lat1 =
                Math.toRadians(employeeLatitude);

        double lat2 =
                Math.toRadians(officeLatitude);

        double deltaLat =
                Math.toRadians(
                        officeLatitude
                                - employeeLatitude
                );

        double deltaLon =
                Math.toRadians(
                        officeLongitude
                                - employeeLongitude
                );

        double a =
                Math.sin(deltaLat / 2)
                        * Math.sin(deltaLat / 2)
                        +
                        Math.cos(lat1)
                                * Math.cos(lat2)
                                *
                                Math.sin(deltaLon / 2)
                                * Math.sin(deltaLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return EARTH_RADIUS_METERS * c;
    }
}