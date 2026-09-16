package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // ============================================================
    // ALL TASKS
    // ============================================================

    List<Task> findAllByOrderByCreatedAtDesc();

    // ============================================================
    // EMPLOYEE TASKS (Single & Multi-Assignee)
    // ============================================================

    List<Task> findByEmployeeOrderByCreatedAtDesc(Employee employee);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.assignees a WHERE t.employee = :employee OR a = :employee ORDER BY t.createdAt DESC")
    List<Task> findTasksForEmployee(@Param("employee") Employee employee);

    // ============================================================
    // EMPLOYEE TASK COUNT
    // ============================================================

    long countByEmployee(Employee employee);

    @Query("SELECT COUNT(DISTINCT t) FROM Task t LEFT JOIN t.assignees a WHERE t.employee = :employee OR a = :employee")
    long countTasksForEmployee(@Param("employee") Employee employee);

    // ============================================================
    // EMPLOYEE TASK COUNT BY STATUS
    // ============================================================

    long countByEmployeeAndStatus(Employee employee, String status);

    @Query("SELECT COUNT(DISTINCT t) FROM Task t LEFT JOIN t.assignees a WHERE (t.employee = :employee OR a = :employee) AND UPPER(t.status) = UPPER(:status)")
    long countTasksForEmployeeAndStatus(@Param("employee") Employee employee, @Param("status") String status);

    // ============================================================
    // ALL TASK COUNT BY STATUS
    // ============================================================

    long countByStatus(String status);
}