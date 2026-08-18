package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    // ============================================================
    // FIND BY EMPLOYEE CODE
    // ============================================================

    Optional<Employee> findByEmployeeCode(
            String employeeCode
    );

    // ============================================================
    // CHECK EMPLOYEE CODE
    // ============================================================

    boolean existsByEmployeeCode(
            String employeeCode
    );

    // ============================================================
    // FIND EMPLOYEE BY LOGIN USERNAME
    // ============================================================

    Optional<Employee> findByUserUsername(
            String username
    );

    // ============================================================
    // FIND EMPLOYEE BY QR TOKEN
    // ============================================================

    Optional<Employee> findByQrToken(
            String qrToken
    );

    // ============================================================
    // CHECK QR TOKEN
    // ============================================================

    boolean existsByQrToken(
            String qrToken
    );
}
