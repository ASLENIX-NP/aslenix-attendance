package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByEmployeeOrderByCreatedAtDesc(
            Employee employee
    );

    List<Task> findAllByOrderByCreatedAtDesc();

    long countByEmployee(Employee employee);

    long countByEmployeeAndStatus(
            Employee employee,
            String status
    );
}