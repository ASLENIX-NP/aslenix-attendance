package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Notification;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee/notifications")
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
    // GET CURRENT EMPLOYEE
    // ============================================================

    private Employee getCurrentEmployee(
            Authentication authentication
    ) {

        if (authentication == null ||
                authentication.getName() == null) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        return employeeRepository
                .findByUserUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Employee account not found"
                        )
                );
    }

    // ============================================================
    // GET ALL NOTIFICATIONS
    // ============================================================

    @GetMapping
    public ResponseEntity<List<NotificationResponse>>
    getNotifications(
            Authentication authentication
    ) {

        Employee employee =
                getCurrentEmployee(authentication);

        List<Notification> notifications =
                notificationService
                        .getEmployeeNotifications(employee);

        List<NotificationResponse> response =
                notifications.stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET UNREAD COUNT
    // ============================================================

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>>
    getUnreadCount(
            Authentication authentication
    ) {

        Employee employee =
                getCurrentEmployee(authentication);

        long count =
                notificationService
                        .getUnreadCount(employee);

        return ResponseEntity.ok(
                Map.of("count", count)
        );
    }

    // ============================================================
    // MARK ONE AS READ
    // ============================================================

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Boolean>>
    markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Employee employee =
                getCurrentEmployee(authentication);

        boolean updated =
                notificationService
                        .markAsRead(id, employee);

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                Map.of("success", true)
        );
    }

    // ============================================================
    // MARK ALL AS READ
    // ============================================================

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Boolean>>
    markAllAsRead(
            Authentication authentication
    ) {

        Employee employee =
                getCurrentEmployee(authentication);

        notificationService
                .markAllAsRead(employee);

        return ResponseEntity.ok(
                Map.of("success", true)
        );
    }

    // ============================================================
    // CONVERT ENTITY → JSON RESPONSE
    // ============================================================

    private NotificationResponse toResponse(
            Notification notification
    ) {

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

    private String formatTime(
            LocalDateTime createdAt
    ) {

        if (createdAt == null) {
            return "";
        }

        Duration duration =
                Duration.between(
                        createdAt,
                        LocalDateTime.now()
                );

        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "Just now";
        }

        long minutes = seconds / 60;

        if (minutes < 60) {
            return minutes + (minutes == 1
                    ? " minute ago"
                    : " minutes ago");
        }

        long hours = minutes / 60;

        if (hours < 24) {
            return hours + (hours == 1
                    ? " hour ago"
                    : " hours ago");
        }

        long days = hours / 24;

        if (days < 7) {
            return days + (days == 1
                    ? " day ago"
                    : " days ago");
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