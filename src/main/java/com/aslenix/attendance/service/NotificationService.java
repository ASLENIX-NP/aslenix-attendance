package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Notification;
import com.aslenix.attendance.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
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
    // CREATE EMPLOYEE NOTIFICATION
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

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @jakarta.annotation.PostConstruct
    public void initSchemaCompatibility() {
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("ALTER TABLE notifications MODIFY employee_id BIGINT NULL");
            } catch (Exception ignored) {
                // Table might already allow null or DB might not be MySQL
            }
        }
    }

    // ============================================================
    // CREATE ADMIN NOTIFICATION
    // ============================================================

    public Notification createAdminNotification(
            Employee employee,
            String title,
            String message,
            String type
    ) {

        Notification notification = new Notification();

        notification.setEmployee(employee);
        notification.setTargetRole("ADMIN");
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    public Notification createAdminNotification(
            String title,
            String message,
            String type
    ) {
        return createAdminNotification(null, title, message, type);
    }

    // ============================================================
    // GET ALL NOTIFICATIONS (EMPLOYEE)
    // ============================================================

    @Transactional(readOnly = true)
    public List<Notification> getEmployeeNotifications(
            Employee employee
    ) {

        return notificationRepository
                .findByEmployeeOrderByCreatedAtDesc(employee);
    }

    // ============================================================
    // GET UNREAD NOTIFICATIONS (EMPLOYEE)
    // ============================================================

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(
            Employee employee
    ) {

        return notificationRepository
                .findByEmployeeAndReadFalseOrderByCreatedAtDesc(employee);
    }

    // ============================================================
    // GET UNREAD COUNT (EMPLOYEE)
    // ============================================================

    @Transactional(readOnly = true)
    public long getUnreadCount(
            Employee employee
    ) {

        return notificationRepository
                .countByEmployeeAndReadFalse(employee);
    }

    // ============================================================
    // MARK ONE AS READ (EMPLOYEE)
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
    // MARK ALL AS READ (EMPLOYEE)
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
    // DELETE NOTIFICATION (EMPLOYEE)
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

    // ============================================================
    // ADMIN NOTIFICATIONS API
    // ============================================================

    @Transactional(readOnly = true)
    public List<Notification> getAdminNotifications() {
        return notificationRepository.findByTargetRoleOrderByCreatedAtDesc("ADMIN");
    }

    @Transactional(readOnly = true)
    public List<Notification> getAdminUnreadNotifications() {
        return notificationRepository.findByTargetRoleAndReadFalseOrderByCreatedAtDesc("ADMIN");
    }

    @Transactional(readOnly = true)
    public long getAdminUnreadCount() {
        return notificationRepository.countByTargetRoleAndReadFalse("ADMIN");
    }

    public boolean markAdminAsRead(Long notificationId) {
        return notificationRepository
                .findByIdAndTargetRole(notificationId, "ADMIN")
                .map(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                    return true;
                })
                .orElse(false);
    }

    public void markAllAdminAsRead() {
        List<Notification> notifications =
                notificationRepository.findByTargetRoleAndReadFalseOrderByCreatedAtDesc("ADMIN");
        for (Notification notification : notifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }
}