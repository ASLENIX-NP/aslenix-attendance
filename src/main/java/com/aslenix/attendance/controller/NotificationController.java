package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Notification;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class NotificationController {

    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    public NotificationController(
            EmployeeRepository employeeRepository,
            NotificationService notificationService
    ) {
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
    }

    // ============================================================
    // HELPER: CHECK IF ADMIN
    // ============================================================

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(auth.getAuthority()) || "ADMIN".equals(auth.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // HELPER: GET CURRENT EMPLOYEE
    // ============================================================

    private Employee getCurrentEmployee(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        return employeeRepository
                .findByUserUsername(authentication.getName())
                .orElse(null);
    }

    // ============================================================
    // GET ALL NOTIFICATIONS
    // Supports both /employee/notifications and /api/notifications
    // ============================================================

    @GetMapping({"/employee/notifications", "/api/notifications"})
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        List<Notification> notifications;

        if (isAdmin(authentication)) {
            notifications = notificationService.getAdminNotifications();
        } else {
            Employee employee = getCurrentEmployee(authentication);
            if (employee == null) {
                return ResponseEntity.ok(List.of());
            }
            notifications = notificationService.getEmployeeNotifications(employee);
        }

        List<NotificationResponse> response = notifications.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET UNREAD COUNT
    // Supports /employee/notifications/unread-count,
    // /api/notifications/unread-count, and /api/notifications/count
    // ============================================================

    @GetMapping({
            "/employee/notifications/unread-count",
            "/api/notifications/unread-count",
            "/api/notifications/count"
    })
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }

        long count;

        if (isAdmin(authentication)) {
            count = notificationService.getAdminUnreadCount();
        } else {
            Employee employee = getCurrentEmployee(authentication);
            if (employee == null) {
                return ResponseEntity.ok(Map.of("count", 0L));
            }
            count = notificationService.getUnreadCount(employee);
        }

        return ResponseEntity.ok(Map.of("count", count));
    }

    // ============================================================
    // MARK ONE AS READ
    // ============================================================

    @PostMapping({"/employee/notifications/{id}/read", "/api/notifications/{id}/read"})
    public ResponseEntity<Map<String, Boolean>> markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        boolean updated;

        if (isAdmin(authentication)) {
            updated = notificationService.markAdminAsRead(id);
        } else {
            Employee employee = getCurrentEmployee(authentication);
            if (employee == null) {
                return ResponseEntity.notFound().build();
            }
            updated = notificationService.markAsRead(id, employee);
        }

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ============================================================
    // MARK ALL AS READ
    // ============================================================

    @PostMapping({"/employee/notifications/read-all", "/api/notifications/read-all"})
    public ResponseEntity<Map<String, Boolean>> markAllAsRead(
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        if (isAdmin(authentication)) {
            notificationService.markAllAdminAsRead();
        } else {
            Employee employee = getCurrentEmployee(authentication);
            if (employee != null) {
                notificationService.markAllAsRead(employee);
            }
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ============================================================
    // CONVERT ENTITY -> JSON RESPONSE
    // ============================================================

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt(),
                formatTime(notification.getCreatedAt())
        );
    }

    // ============================================================
    // TIME FORMAT
    // ============================================================

    private String formatTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }

        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "Just now";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        long days = hours / 24;
        if (days < 7) {
            return days + (days == 1 ? " day ago" : " days ago");
        }

        return createdAt.toLocalDate().toString();
    }

    // ============================================================
    // RESPONSE DTO
    // ============================================================

    public record NotificationResponse(
            Long id,
            String title,
            String message,
            String type,
            boolean read,
            LocalDateTime createdAt,
            String time
    ) {
    }
}