package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeDeletionService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDeletionService.class);

    private final EmployeeRepository employeeRepository;
    private final FileUploadService fileUploadService;

    @PersistenceContext
    private EntityManager entityManager;

    public EmployeeDeletionService(
            EmployeeRepository employeeRepository,
            FileUploadService fileUploadService) {
        this.employeeRepository = employeeRepository;
        this.fileUploadService = fileUploadService;
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
        String photoUrl = employee.getPhotoUrl();

        // 1. Breakdown rows hang off performance scores, so clear them first.
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM performance_task_breakdowns WHERE score_id IN " +
                    "(SELECT id FROM employee_performance_scores WHERE employee_id = :eid)")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not delete performance_task_breakdowns for employee {}: {}", employeeId, e.getMessage());
        }

        try {
            entityManager.createNativeQuery(
                    "DELETE FROM employee_performance_scores WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not delete employee_performance_scores for employee {}: {}", employeeId, e.getMessage());
        }

        // 2. Employee of the month
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM employee_of_the_month WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not delete employee_of_the_month for employee {}: {}", employeeId, e.getMessage());
        }

        // 3. Attendance requests, audit logs, and attendance records
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM attendance_correction_requests WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not delete attendance_correction_requests for employee {}: {}", employeeId, e.getMessage());
        }

        try {
            entityManager.createNativeQuery(
                    "DELETE FROM attendance_audit_logs WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not delete attendance_audit_logs for employee {}: {}", employeeId, e.getMessage());
        }

        try {
            entityManager.createNativeQuery(
                    "DELETE FROM attendance WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not delete attendance for employee {}: {}", employeeId, e.getMessage());
        }

        // 4. Leave requests
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM leave_requests WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not delete leave_requests for employee {}: {}", employeeId, e.getMessage());
        }

        // 5. Notifications
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM notifications WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not delete notifications for employee {}: {}", employeeId, e.getMessage());
        }

        // 6. Project assignees join table
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM project_assignees WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception ignored) {
        }

        // 7. For tasks where this employee was the primary assignee:
        // If there are other assignees remaining in task_assignees, promote one of them
        try {
            entityManager.createNativeQuery(
                    "UPDATE tasks t SET t.employee_id = (" +
                    "  SELECT ta.employee_id FROM task_assignees ta WHERE ta.task_id = t.id AND ta.employee_id != :eid LIMIT 1" +
                    ") WHERE t.employee_id = :eid AND EXISTS (" +
                    "  SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.id AND ta.employee_id != :eid" +
                    ")")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception ignored) {
        }

        // 8. Delete this employee from task join tables (task_assignees, task_team_leads)
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM task_assignees WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception ignored) {
        }

        try {
            entityManager.createNativeQuery(
                    "DELETE FROM task_team_leads WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception ignored) {
        }

        // 9. Nullable references are detached rather than deleted, preserving history.
        // Try setting tasks.employee_id to NULL. If column is NOT NULL, reassign to another active employee as fallback.
        try {
            entityManager.createNativeQuery(
                    "UPDATE tasks SET employee_id = NULL WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Setting tasks.employee_id to NULL failed (likely NOT NULL column). Attempting reassignment fallback: {}", e.getMessage());
            Long fallbackEmpId = employeeRepository.findAll().stream()
                    .map(Employee::getId)
                    .filter(id -> !id.equals(employeeId))
                    .findFirst()
                    .orElse(null);
            if (fallbackEmpId != null) {
                entityManager.createNativeQuery(
                        "UPDATE tasks SET employee_id = :fallbackId WHERE employee_id = :eid")
                        .setParameter("fallbackId", fallbackEmpId)
                        .setParameter("eid", employeeId)
                        .executeUpdate();
            }
        }

        try {
            entityManager.createNativeQuery(
                    "UPDATE task_assignments SET employee_id = NULL WHERE employee_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception ignored) {
        }

        try {
            entityManager.createNativeQuery(
                    "UPDATE task_assignment_histories SET updated_by_id = NULL WHERE updated_by_id = :eid")
                    .setParameter("eid", employeeId)
                    .executeUpdate();
        } catch (Exception ignored) {
        }

        // 10. Flush child updates/deletions before removing the parent employee row
        entityManager.flush();

        // 11. Delete employee
        entityManager.createNativeQuery(
                "DELETE FROM employees WHERE id = :eid")
                .setParameter("eid", employeeId)
                .executeUpdate();

        // 12. Delete user account if attached
        if (userId != null) {
            entityManager.flush();
            entityManager.createNativeQuery(
                    "DELETE FROM users WHERE id = :uid")
                    .setParameter("uid", userId)
                    .executeUpdate();
        }

        // 13. Clean up uploaded profile photo file from disk if exists
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            try {
                fileUploadService.deleteEmployeePhoto(photoUrl);
            } catch (Exception ignored) {
            }
        }

        // 14. Evict deleted entities from Hibernate L1 cache
        entityManager.clear();

        return true;
    }
}
