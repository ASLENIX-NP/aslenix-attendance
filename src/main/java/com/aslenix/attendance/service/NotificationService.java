package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Notification;
import com.aslenix.attendance.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    // ============================================================
    // CREATE NOTIFICATION
    // ============================================================

    public Notification createNotification(
            Employee employee,
            String title,
            String message,
            String type
    ) {

        Notification notification = new Notification();

        notification.setEmployee(employee);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    // ============================================================
    // GET ALL NOTIFICATIONS
    // ============================================================

    @Transactional(readOnly = true)
    public List<Notification> getEmployeeNotifications(
            Employee employee
    ) {

        return notificationRepository
                .findByEmployeeOrderByCreatedAtDesc(employee);
    }

    // ============================================================
    // GET UNREAD NOTIFICATIONS
    // ============================================================

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(
            Employee employee
    ) {

        return notificationRepository
                .findByEmployeeAndReadFalseOrderByCreatedAtDesc(employee);
    }

    // ============================================================
    // GET UNREAD COUNT
    // ============================================================

    @Transactional(readOnly = true)
    public long getUnreadCount(
            Employee employee
    ) {

        return notificationRepository
                .countByEmployeeAndReadFalse(employee);
    }

    // ============================================================
    // MARK ONE AS READ
    // ============================================================

    public boolean markAsRead(
            Long notificationId,
            Employee employee
    ) {

        return notificationRepository
                .findByIdAndEmployee(notificationId, employee)
                .map(notification -> {

                    notification.setRead(true);

                    notificationRepository.save(notification);

                    return true;

                })
                .orElse(false);
    }

    // ============================================================
    // MARK ALL AS READ
    // ============================================================

    public void markAllAsRead(
            Employee employee
    ) {

        List<Notification> notifications =
                notificationRepository
                        .findByEmployeeAndReadFalseOrderByCreatedAtDesc(
                                employee
                        );

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);
    }

    // ============================================================
    // DELETE NOTIFICATION
    // ============================================================

    public boolean deleteNotification(
            Long notificationId,
            Employee employee
    ) {

        return notificationRepository
                .findByIdAndEmployee(notificationId, employee)
                .map(notification -> {

                    notificationRepository.delete(notification);

                    return true;

                })
                .orElse(false);
    }
}