package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.AttendanceCorrectionRequest;
import com.aslenix.attendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceCorrectionRequestRepository
        extends JpaRepository<AttendanceCorrectionRequest, Long> {

    List<AttendanceCorrectionRequest> findByEmployeeOrderByCreatedAtDesc(Employee employee);

    List<AttendanceCorrectionRequest> findAllByOrderByCreatedAtDesc();

    List<AttendanceCorrectionRequest> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByEmployeeAndAttendanceDateAndStatus(
            Employee employee,
            LocalDate attendanceDate,
            String status
    );

    long countByStatus(String status);
}
