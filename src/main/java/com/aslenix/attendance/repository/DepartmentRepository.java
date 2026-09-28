package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    Optional<Department> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Department> findByActiveTrueOrderByIdAsc();

    @Query("SELECT d FROM Department d WHERE d.active = true AND LOWER(d.name) NOT IN ('full stack', 'hr', 'fullstack', 'full-stack') ORDER BY d.id ASC")
    List<Department> findAvailableDepartments();
}