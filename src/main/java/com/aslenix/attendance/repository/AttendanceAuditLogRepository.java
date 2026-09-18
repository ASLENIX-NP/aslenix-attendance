package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.AttendanceAuditLog;
import com.aslenix.attendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceAuditLogRepository
        extends JpaRepository<AttendanceAuditLog, Long> {

    List<AttendanceAuditLog> findAllByOrderByTimestampDesc();

    List<AttendanceAuditLog> findTop50ByOrderByTimestampDesc();

    List<AttendanceAuditLog> findByEmployeeOrderByTimestampDesc(Employee employee);
}
