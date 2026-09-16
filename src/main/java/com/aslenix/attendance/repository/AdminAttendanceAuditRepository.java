package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.AdminAttendanceAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAttendanceAuditRepository
        extends JpaRepository<AdminAttendanceAudit, Long> {

    List<AdminAttendanceAudit> findTop30ByOrderByTimestampDesc();
}
