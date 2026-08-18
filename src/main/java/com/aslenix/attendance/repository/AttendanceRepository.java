package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Attendance;
import com.aslenix.attendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeAndAttendanceDate(
            Employee employee,
            LocalDate attendanceDate
    );

    List<Attendance> findByEmployeeOrderByAttendanceDateDesc(
            Employee employee
    );

    List<Attendance> findByEmployeeAndAttendanceDateBetween(
            Employee employee,
            LocalDate startDate,
            LocalDate endDate
    );
}