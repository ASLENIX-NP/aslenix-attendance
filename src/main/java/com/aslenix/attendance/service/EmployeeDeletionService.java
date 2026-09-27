package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeDeletionService {

    private final EmployeeRepository employeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EmployeeDeletionService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Permanently removes an employee and every row that references it.
     * Deletions run in FK-dependency order inside a single transaction so a
     * failure anywhere rolls the whole operation back.
     */
    @Transactional
    public boolean deleteEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return false;
        }

        Long userId = employee.getUser() != null ? employee.getUser().getId() : null;

        // Breakdown rows hang off performance scores, so clear them first.
        entityManager.createNativeQuery(
                "DELETE FROM performance_task_breakdowns WHERE score_id IN " +
                "(SELECT id FROM employee_performance_scores WHERE employee_id = :eid)")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM employee_performance_scores WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM employee_of_the_month WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM attendance_correction_requests WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM attendance_audit_logs WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM attendance WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM leave_requests WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM notifications WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        // Many-to-many join tables between tasks and assignees / team leads.
        entityManager.createNativeQuery(
                "DELETE FROM task_assignees WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        try {
            entityManager.createNativeQuery(
                    "DELETE FROM task_team_leads WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception ignored) {
        }

        // Nullable references are detached rather than deleted, preserving history.
        entityManager.createNativeQuery(
                "UPDATE tasks SET employee_id = NULL WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE task_assignments SET employee_id = NULL WHERE employee_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE task_assignment_histories SET updated_by_id = NULL WHERE updated_by_id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        // Flush the child deletions before removing the parent row.
        entityManager.flush();

        entityManager.createNativeQuery(
                "DELETE FROM employees WHERE id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        if (userId != null) {
            entityManager.flush();
            entityManager.createNativeQuery(
                    "DELETE FROM users WHERE id = :uid")
                    .setParameter("uid", userId)
                    .executeUpdate();
        }

        return true;
    }
}
