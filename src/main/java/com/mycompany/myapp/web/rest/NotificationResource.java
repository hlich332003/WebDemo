package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.analytics.Notification;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller để quản lý Thông báo
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationResource {

    private final Logger log = LoggerFactory.getLogger(NotificationResource.class);
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationResource(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/notifications : Lấy danh sách thông báo của user hiện tại
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getUserNotifications() {
        log.debug("REST request to get notifications for current user");

        // Lấy email của user hiện tại để query
        String userEmail = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneWithAuthoritiesByEmailOrPhone)
            .map(User::getEmail)
            .orElseThrow(() -> new RuntimeException("User not found or email is missing"));

        List<Notification> notifications = notificationService.getUserNotifications(userEmail);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/unread-count : Đếm số thông báo chưa đọc
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadCount() {
        log.debug("REST request to count unread notifications");

        String userEmail = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneWithAuthoritiesByEmailOrPhone)
            .map(User::getEmail)
            .orElseThrow(() -> new RuntimeException("User not found or email is missing"));

        long count = notificationService.countUnreadNotifications(userEmail);
        return ResponseEntity.ok(count);
    }

    /**
     * PUT /api/notifications/{id}/read : Đánh dấu một thông báo là đã đọc
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        log.debug("REST request to mark notification as read: {}", id);
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/notifications/mark-all-read : Đánh dấu tất cả thông báo là đã đọc
     */
    @PutMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Integer> markAllAsRead() {
        log.debug("REST request to mark all notifications as read");

        String userEmail = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneWithAuthoritiesByEmailOrPhone)
            .map(User::getEmail)
            .orElseThrow(() -> new RuntimeException("User not found or email is missing"));

        int count = notificationService.markAllAsRead(userEmail);
        return ResponseEntity.ok(count);
    }
}
