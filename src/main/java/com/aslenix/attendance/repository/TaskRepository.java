package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository
        extends JpaRepository<Task, Long> {

    // ============================================================
    // ALL TASKS
    // ============================================================

    List<Task> findAllByOrderByCreatedAtDesc();

    // ============================================================
    // EMPLOYEE TASKS
    // ============================================================

    List<Task> findByEmployeeOrderByCreatedAtDesc(
            Employee employee
    );

    // ============================================================
    // EMPLOYEE TASK COUNT
    // ============================================================

    long countByEmployee(
            Employee employee
    );

    // ============================================================
    // EMPLOYEE TASK COUNT BY STATUS
    // ============================================================

    long countByEmployeeAndStatus(
            Employee employee,
            String status
    );

    // ============================================================
    // ALL TASK COUNT BY STATUS
    // ============================================================

    long countByStatus(
            String status
    );
}