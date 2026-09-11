package com.aslenix.attendance.repository;

import com.aslenix.attendance.entity.Notification;
import com.aslenix.attendance.entity.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // ============================================================
    // ALL NOTIFICATIONS FOR EMPLOYEE
    // ============================================================

    List<Notification> findByEmployeeOrderByCreatedAtDesc(
            Employee employee
    );

    // ============================================================
    // UNREAD NOTIFICATIONS
    // ============================================================

    List<Notification> findByEmployeeAndReadFalseOrderByCreatedAtDesc(
            Employee employee
    );

    // ============================================================
    // UNREAD COUNT
    // ============================================================

    long countByEmployeeAndReadFalse(
            Employee employee
    );

    // ============================================================
    // FIND NOTIFICATION BELONGING TO EMPLOYEE
    // ============================================================

    Optional<Notification> findByIdAndEmployee(
            Long id,
            Employee employee
    );
}