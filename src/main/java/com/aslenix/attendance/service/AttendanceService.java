package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.AttendanceRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final OfficeSettingsRepository officeSettingsRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            OfficeSettingsRepository officeSettingsRepository) {

        this.attendanceRepository = attendanceRepository;
        this.officeSettingsRepository = officeSettingsRepository;
    }

    // ============================================================
    // GET ALL ATTENDANCE RECORDS FOR EMPLOYEE
    // ============================================================

    public List<Attendance> getEmployeeAttendance(Employee employee) {

        return attendanceRepository
                .findAll()
                .stream()
                .filter(a ->
                        a.getEmployee() != null
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

        LocalDate today = LocalDate.now();

        return attendanceRepository
                .findByEmployeeAndAttendanceDate(
                        employee,
                        today
                )
                .orElse(null);
    }

    // ============================================================
    // MONTHLY ATTENDANCE
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

                    LocalDate date =
                            a.getAttendanceDate();

                    return !date.isBefore(firstDay)
                            && !date.isAfter(lastDay);
                })
                .toList();
    }

    // ============================================================
    // PRESENT COUNT
    // ============================================================

    public long getPresentCount(Employee employee) {

        return getCurrentMonthAttendance(employee)
                .stream()
                .filter(a ->
                        "PRESENT".equals(a.getStatus()))
                .count();
    }

    // ============================================================
    // LATE COUNT
    // ============================================================

    public long getLateCount(Employee employee) {

        return getCurrentMonthAttendance(employee)
                .stream()
                .filter(Attendance::isLate)
                .count();
    }

    // ============================================================
    // LEAVE COUNT
    // ============================================================

    public long getLeaveCount(Employee employee) {

        /*
         * Leave functionality has not been connected yet.
         *
         * This will remain 0 until the Leave module
         * is connected.
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

        for (LocalDate date = firstDay;
             !date.isAfter(today);
             date = date.plusDays(1)) {

            if (isWorkingDay(
                    settings,
                    date.getDayOfWeek())) {

                count++;
            }
        }

        return count;
    }

    // ============================================================
    // ABSENT COUNT
    // ============================================================

    public long getAbsentCount(Employee employee) {

        long workingDays =
                getWorkingDaysThisMonth();

        long attendedDays =
                getCurrentMonthAttendance(employee)
                        .stream()
                        .filter(a ->
                                a.getStatus() != null
                                        && !a.getStatus()
                                        .equalsIgnoreCase("ABSENT"))
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
    // TOTAL ATTENDANCE THIS MONTH
    // ============================================================

    public long getAttendanceThisMonth(Employee employee) {

        return getCurrentMonthAttendance(employee)
                .size();
    }

    // ============================================================
    // TODAY STATUS
    // ============================================================

    public String getTodayStatus(Employee employee) {

        Attendance attendance =
                getTodayAttendance(employee);

        if (attendance == null) {
            return null;
        }

        if (attendance.getCheckOut() == null) {
            return "WORKING";
        }

        if (attendance.isLate()) {
            return "LATE";
        }

        return "PRESENT";
    }

    // ============================================================
    // TODAY CHECK-IN TIME
    // ============================================================

    public String getTodayCheckInTime(Employee employee) {

        Attendance attendance =
                getTodayAttendance(employee);

        if (attendance == null ||
                attendance.getCheckIn() == null) {

            return null;
        }

        return formatTime(
                attendance.getCheckIn()
        );
    }

    // ============================================================
    // TODAY CHECK-OUT TIME
    // ============================================================

    public String getTodayCheckOutTime(Employee employee) {

        Attendance attendance =
                getTodayAttendance(employee);

        if (attendance == null ||
                attendance.getCheckOut() == null) {

            return null;
        }

        return formatTime(
                attendance.getCheckOut()
        );
    }

    // ============================================================
    // TODAY WORKING HOURS
    // ============================================================

    public String getTodayWorkingHours(Employee employee) {

        Attendance attendance =
                getTodayAttendance(employee);

        if (attendance == null ||
                attendance.getCheckIn() == null) {

            return "0h 00m";
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

        return formatDuration(duration);
    }

    // ============================================================
    // TOTAL WORKING HOURS
    // ============================================================

    public String getTotalWorkingHours(Employee employee) {

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
                    Math.max(duration.toMinutes(), 0);
        }

        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;

        return hours + "h "
                + String.format(
                        "%02d",
                        minutes
                )
                + "m";
    }

    // ============================================================
    // CHECK WORKING DAY
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
    // FORMAT TIME
    // ============================================================

    private String formatTime(
            LocalDateTime dateTime) {

        return dateTime.toLocalTime()
                .format(
                        java.time.format.DateTimeFormatter
                                .ofPattern("hh:mm a")
                );
    }

    // ============================================================
    // FORMAT DURATION
    // ============================================================

    private String formatDuration(
            Duration duration) {

        long totalMinutes =
                Math.max(duration.toMinutes(), 0);

        long hours =
                totalMinutes / 60;

        long minutes =
                totalMinutes % 60;

        return hours + "h "
                + String.format(
                        "%02d",
                        minutes
                )
                + "m";
    }
}