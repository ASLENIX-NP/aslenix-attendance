package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.entity.WeeklyWorkingSchedule;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import com.aslenix.attendance.repository.WeeklyWorkingScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final OfficeSettingsRepository officeSettingsRepository;
    private final WeeklyWorkingScheduleRepository weeklyWorkingScheduleRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            OfficeSettingsRepository officeSettingsRepository,
            WeeklyWorkingScheduleRepository weeklyWorkingScheduleRepository) {

        this.attendanceRepository = attendanceRepository;
        this.officeSettingsRepository = officeSettingsRepository;
        this.weeklyWorkingScheduleRepository = weeklyWorkingScheduleRepository;
    }

    // ============================================================
    // ALL EMPLOYEE ATTENDANCE
    // ============================================================

    public List<Attendance> getEmployeeAttendance(Employee employee) {

        return attendanceRepository
                .findAll()
                .stream()
                .filter(a ->
                        a.getEmployee() != null
                                && a.getEmployee().getId() != null
                                && employee != null
                                && employee.getId() != null
                                && a.getEmployee().getId()
                                .equals(employee.getId()))
                .sorted((a, b) ->
                        b.getAttendanceDate()
                                .compareTo(a.getAttendanceDate()))
                .toList();
    }

    // ============================================================
    // TODAY'S ATTENDANCE
    // ============================================================

    public Attendance getTodayAttendance(Employee employee) {

        if (employee == null) {
            return null;
        }

        return attendanceRepository
                .findByEmployeeAndAttendanceDate(
                        employee,
                        LocalDate.now()
                )
                .orElse(null);
    }

    // ============================================================
    // CURRENT MONTH ATTENDANCE
    // ============================================================

    public List<Attendance> getCurrentMonthAttendance(
            Employee employee) {

        LocalDate today = LocalDate.now();

        LocalDate firstDay =
                today.withDayOfMonth(1);

        LocalDate lastDay =
                today.withDayOfMonth(
                        today.lengthOfMonth()
                );

        return getEmployeeAttendance(employee)
                .stream()
                .filter(a -> {

                    if (a.getAttendanceDate() == null) {
                        return false;
                    }

                    LocalDate date =
                            a.getAttendanceDate();

                    return !date.isBefore(firstDay)
                            && !date.isAfter(lastDay);
                })
                .toList();
    }

    // ============================================================
    // ATTENDANCE THIS MONTH
    // ============================================================

    public long getAttendanceThisMonth(
            Employee employee) {

        return getCurrentMonthAttendance(employee)
                .size();
    }

    // ============================================================
    // PRESENT
    // ============================================================

    public long getPresentCount(
            Employee employee) {

        return getCurrentMonthAttendance(employee)
                .stream()
                .filter(a ->
                        a.getStatus() != null
                                && a.getStatus()
                                .equalsIgnoreCase("PRESENT"))
                .count();
    }

    // ============================================================
    // LATE
    // ============================================================

    public long getLateCount(
            Employee employee) {

        return getCurrentMonthAttendance(employee)
                .stream()
                .filter(a ->
                        a.isLate()
                                || (
                                a.getStatus() != null
                                        && a.getStatus()
                                        .equalsIgnoreCase("LATE")
                        ))
                .count();
    }

    // ============================================================
    // LEAVE
    // ============================================================

    public long getLeaveCount(
            Employee employee) {

        /*
         * Leave functionality can be connected here later.
         */
        return 0;
    }

    // ============================================================
    // WORKING DAYS THIS MONTH
    // ============================================================

    public long getWorkingDaysThisMonth() {

        OfficeSettings settings =
                officeSettingsRepository
                        .findFirstByOrderByIdAsc()
                        .orElse(null);

        if (settings == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();

        LocalDate firstDay =
                today.withDayOfMonth(1);

        long count = 0;

        for (
                LocalDate date = firstDay;
                !date.isAfter(today);
                date = date.plusDays(1)
        ) {

            if (isDateWorkingDay(
                    date,
                    settings)) {

                count++;
            }
        }

        return count;
    }

    // ============================================================
    // ABSENT
    // ============================================================

    public long getAbsentCount(
            Employee employee) {

        long workingDays =
                getWorkingDaysThisMonth();

        long attendedDays =
                getCurrentMonthAttendance(employee)
                        .stream()
                        .filter(a ->
                                a.getStatus() != null
                                        && (
                                        a.getStatus()
                                                .equalsIgnoreCase("PRESENT")
                                                ||
                                        a.getStatus()
                                                .equalsIgnoreCase("LATE")
                                ))
                        .count();

        long leaveDays =
                getLeaveCount(employee);

        long absent =
                workingDays
                        - attendedDays
                        - leaveDays;

        return Math.max(absent, 0);
    }

    // ============================================================
    // TODAY STATUS
    // ============================================================

    public String getTodayStatus(
            Employee employee) {

        Attendance attendance =
                getTodayAttendance(employee);

        if (attendance == null) {
            return "NOT CHECKED IN";
        }

        if (attendance.getCheckIn() == null) {
            return "NOT CHECKED IN";
        }

        if (attendance.getCheckOut() == null) {

            if (attendance.isLate()) {
                return "LATE / WORKING";
            }

            return "WORKING";
        }

        if (attendance.isLate()) {
            return "LATE";
        }

        return "PRESENT";
    }

    // ============================================================
    // TODAY CHECK-IN
    // ============================================================

    public String getTodayCheckInTime(
            Employee employee) {

        Attendance attendance =
                getTodayAttendance(employee);

        if (attendance == null
                || attendance.getCheckIn() == null) {

            return "--:--";
        }

        return formatTime(
                attendance.getCheckIn()
        );
    }

    // ============================================================
    // TODAY CHECK-OUT
    // ============================================================

    public String getTodayCheckOutTime(
            Employee employee) {

        Attendance attendance =
                getTodayAttendance(employee);

        if (attendance == null
                || attendance.getCheckOut() == null) {

            return "--:--";
        }

        return formatTime(
                attendance.getCheckOut()
        );
    }

    // ============================================================
    // TODAY WORKING HOURS
    // ============================================================

    public String getTodayWorkingHours(
            Employee employee) {

        Attendance attendance =
                getTodayAttendance(employee);

        if (attendance == null
                || attendance.getCheckIn() == null) {

            return "0h 00m";
        }

        LocalDateTime checkIn =
                attendance.getCheckIn();

        LocalDateTime checkOut =
                attendance.getCheckOut();

        /*
         * Employee is still working.
         * Calculate working time until now.
         */
        if (checkOut == null) {
            checkOut = LocalDateTime.now();
        }

        Duration duration =
                Duration.between(
                        checkIn,
                        checkOut
                );

        return formatDuration(duration);
    }

    // ============================================================
    // TOTAL WORKING HOURS
    // ============================================================

    public String getTotalWorkingHours(
            Employee employee) {

        List<Attendance> records =
                getCurrentMonthAttendance(employee);

        long totalMinutes = 0;

        for (Attendance attendance : records) {

            if (attendance.getCheckIn() == null) {
                continue;
            }

            LocalDateTime checkIn =
                    attendance.getCheckIn();

            LocalDateTime checkOut =
                    attendance.getCheckOut();

            if (checkOut == null) {
                checkOut = LocalDateTime.now();
            }

            Duration duration =
                    Duration.between(
                            checkIn,
                            checkOut
                    );

            totalMinutes +=
                    Math.max(
                            duration.toMinutes(),
                            0
                    );
        }

        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;

        return hours
                + "h "
                + String.format(
                        "%02d",
                        minutes
                )
                + "m";
    }

    // ============================================================
    // CHECK IN
    // ============================================================

    public Attendance checkIn(
            Employee employee,
            double latitude,
            double longitude) {

        if (employee == null) {

            throw new IllegalArgumentException(
                    "Employee not found."
            );
        }

        if (!employee.isEnabled()) {

            throw new IllegalStateException(
                    "Employee account is disabled."
            );
        }

        OfficeSettings settings =
                getOfficeSettings();

        LocalDate today =
                LocalDate.now();

        // --------------------------------------------------------
        // 2:00 PM CUTOFF FOR MANUAL CHECK-IN
        // --------------------------------------------------------
        if (LocalTime.now().isAfter(LocalTime.of(14, 0))) {
            throw new IllegalStateException(
                    "Manual check-in is not allowed after 02:00 PM. Please contact your administrator."
            );
        }

        // --------------------------------------------------------
        // WORKING DAY
        // --------------------------------------------------------

        if (!isDateWorkingDay(
                today,
                settings)) {

            throw new IllegalStateException(
                    "Today is not an operational working day."
            );
        }

        // --------------------------------------------------------
        // CHECK EXISTING RECORD
        // --------------------------------------------------------

        Attendance existing =
                attendanceRepository
                        .findByEmployeeAndAttendanceDate(
                                employee,
                                today
                        )
                        .orElse(null);

        if (existing != null
                && existing.getCheckIn() != null) {

            throw new IllegalStateException(
                    "You have already checked in today."
            );
        }

        // --------------------------------------------------------
        // VALIDATE GPS
        // --------------------------------------------------------

        double distance =
                calculateDistanceMeters(
                        latitude,
                        longitude,
                        settings.getLatitude(),
                        settings.getLongitude()
                );

        if (distance >
                settings.getAllowedRadiusMeters()) {

            throw new IllegalStateException(
                    "You are outside the office attendance radius. "
                            + "Distance: "
                            + Math.round(distance)
                            + " meters. Allowed: "
                            + Math.round(
                            settings.getAllowedRadiusMeters())
                            + " meters."
            );
        }

        // --------------------------------------------------------
        // CREATE / REUSE ATTENDANCE
        // --------------------------------------------------------

        Attendance attendance =
                existing != null
                        ? existing
                        : new Attendance();

        attendance.setEmployee(employee);

        attendance.setAttendanceDate(today);

        LocalDateTime now =
                LocalDateTime.now();

        attendance.setCheckIn(now);

        // --------------------------------------------------------
        // LATE CALCULATION
        // --------------------------------------------------------

        LocalTime workStart =
                settings.getWorkStartTime();

        int graceMinutes =
                settings.getLateGraceMinutes();

        LocalTime allowedStart =
                workStart.plusMinutes(
                        graceMinutes
                );

        boolean late =
                now.toLocalTime()
                        .isAfter(allowedStart);

        attendance.setLate(late);

        attendance.setStatus(
                late
                        ? "LATE"
                        : "PRESENT"
        );

        attendance.setEarlyLeave(false);

        attendance.setHalfDay(false);

        // --------------------------------------------------------
        // LOCATION DATA
        // --------------------------------------------------------

        attendance.setCheckInLatitude(
                latitude
        );

        attendance.setCheckInLongitude(
                longitude
        );

        attendance.setCheckInDistanceMeters(
                distance
        );

        return attendanceRepository.save(
                attendance
        );
    }

    // ============================================================
    // CHECK OUT
    // ============================================================

    public Attendance checkOut(
            Employee employee,
            double latitude,
            double longitude) {

        if (employee == null) {

            throw new IllegalArgumentException(
                    "Employee not found."
            );
        }

        if (!employee.isEnabled()) {

            throw new IllegalStateException(
                    "Employee account is disabled."
            );
        }

        OfficeSettings settings =
                getOfficeSettings();

        // --------------------------------------------------------
        // VALIDATE LOCATION
        // --------------------------------------------------------

        double distance =
                calculateDistanceMeters(
                        latitude,
                        longitude,
                        settings.getLatitude(),
                        settings.getLongitude()
                );

        if (distance >
                settings.getAllowedRadiusMeters()) {

            throw new IllegalStateException(
                    "You are outside the office attendance radius. "
                            + "Distance: "
                            + Math.round(distance)
                            + " meters. Allowed: "
                            + Math.round(
                            settings.getAllowedRadiusMeters())
                            + " meters."
            );
        }

        // --------------------------------------------------------
        // TODAY'S RECORD
        // --------------------------------------------------------

        LocalDate today =
                LocalDate.now();

        Attendance attendance =
                attendanceRepository
                        .findByEmployeeAndAttendanceDate(
                                employee,
                                today
                        )
                        .orElse(null);

        if (attendance == null) {

            throw new IllegalStateException(
                    "You have not checked in today."
            );
        }

        // --------------------------------------------------------
        // CHECK-IN REQUIRED
        // --------------------------------------------------------

        if (attendance.getCheckIn() == null) {

            throw new IllegalStateException(
                    "You have not checked in today."
            );
        }

        // --------------------------------------------------------
        // ALREADY CHECKED OUT
        // --------------------------------------------------------

        if (attendance.getCheckOut() != null) {

            throw new IllegalStateException(
                    "You have already checked out today."
            );
        }

        // --------------------------------------------------------
        // SAVE CHECK-OUT
        // --------------------------------------------------------

        LocalDateTime now =
                LocalDateTime.now();

        attendance.setCheckOut(now);

        // --------------------------------------------------------
        // EARLY LEAVE
        // --------------------------------------------------------

        boolean early =
                settings.getWorkEndTime() != null
                        && now.toLocalTime()
                        .isBefore(
                                settings.getWorkEndTime()
                        );

        attendance.setEarlyLeave(
                early
        );

        return attendanceRepository.save(
                attendance
        );
    }

    // ============================================================
    // OFFICE SETTINGS
    // ============================================================

    public OfficeSettings getOfficeSettings() {

        return officeSettingsRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Office settings have not been configured."
                        )
                );
    }

    // ============================================================
    // DISTANCE CALCULATION
    // ============================================================

    public double calculateDistanceMeters(
            double employeeLatitude,
            double employeeLongitude,
            double officeLatitude,
            double officeLongitude) {

        final double EARTH_RADIUS_METERS =
                6_371_000.0;

        double lat1 =
                Math.toRadians(
                        employeeLatitude
                );

        double lat2 =
                Math.toRadians(
                        officeLatitude
                );

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

    // ============================================================
    // WORKING DAY (DATE-AWARE WITH SCHEDULE OVERRIDE)
    // ============================================================

    public boolean isDateWorkingDay(
            LocalDate date,
            OfficeSettings settings) {

        if (date == null) {
            return false;
        }

        if (weeklyWorkingScheduleRepository != null) {
            LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            Optional<WeeklyWorkingSchedule> schedule =
                    weeklyWorkingScheduleRepository.findByWeekStartAndWorkDate(weekStart, date);
            if (schedule.isPresent()) {
                return schedule.get().isWorkingDay();
            }
        }

        if (settings != null) {
            return isWorkingDay(settings, date.getDayOfWeek());
        }

        return false;
    }

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
    // FORMAT TIME
    // ============================================================

    private String formatTime(
            LocalDateTime dateTime) {

        return dateTime
                .toLocalTime()
                .format(
                        DateTimeFormatter.ofPattern(
                                "hh:mm a"
                        )
                );
    }

    // ============================================================
    // FORMAT DURATION
    // ============================================================

    private String formatDuration(
            Duration duration) {

        long totalMinutes =
                Math.max(
                        duration.toMinutes(),
                        0
                );

        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;

        return hours
                + "h "
                + String.format(
                        "%02d",
                        minutes
                )
                + "m";
    }
}