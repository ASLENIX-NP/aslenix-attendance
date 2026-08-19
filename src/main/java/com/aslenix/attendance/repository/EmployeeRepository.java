package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUserUsername(String username);

    Optional<Employee> findByQrToken(String qrToken);

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    List<Employee> findAllByOrderByFirstNameAsc();
}