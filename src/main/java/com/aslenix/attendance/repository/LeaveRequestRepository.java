package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeOrderByCreatedAtDesc(
            Employee employee
    );

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    long countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String status,
            LocalDate date1,
            LocalDate date2
    );

    boolean existsByEmployeeAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Employee employee,
            String status,
            LocalDate startDate,
            LocalDate endDate
    );

    long countByStatus(String status);

    long countByEmployeeAndStatus(
            Employee employee,
            String status
    );
}