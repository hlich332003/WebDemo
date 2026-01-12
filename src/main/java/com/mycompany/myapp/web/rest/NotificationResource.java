package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.analytics.NotificationEntity;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.analytics.NotificationRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class NotificationResource {

    private final Logger log = LoggerFactory.getLogger(NotificationResource.class);
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationResource(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationEntity>> getUserNotifications() {
        log.debug("REST request to get user notifications");
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new RuntimeException("User not found"));
        List<NotificationEntity> notifications = notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadCount() {
        log.debug("REST request to get unread notification count");
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new RuntimeException("User not found"));
        long count = notificationRepository.countByReceiverIdAndIsReadFalse(user.getId());
        return ResponseEntity.ok(count);
    }

    @PostMapping("/notifications/{id}/mark-as-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        log.debug("REST request to mark notification as read : {}", id);
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new RuntimeException("User not found"));

        notificationRepository
            .findById(id)
            .ifPresent(notification -> {
                // Verify the notification belongs to the current user
                if (notification.getReceiverId().equals(user.getId())) {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                }
            });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notifications/mark-all-as-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead() {
        log.debug("REST request to mark all notifications as read");
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new RuntimeException("User not found"));
        List<NotificationEntity> notifications = notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(user.getId());
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/notifications/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        log.debug("REST request to delete notification : {}", id);
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new RuntimeException("User not found"));

        notificationRepository
            .findById(id)
            .ifPresent(notification -> {
                // Verify the notification belongs to the current user
                if (notification.getReceiverId().equals(user.getId())) {
                    notificationRepository.delete(notification);
                }
            });
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/notifications/delete-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReadNotifications() {
        log.debug("REST request to delete read notifications");
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new RuntimeException("User not found"));
        List<NotificationEntity> notifications = notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(user.getId());
        List<NotificationEntity> readNotifications = notifications.stream().filter(NotificationEntity::isRead).toList();
        notificationRepository.deleteAll(readNotifications);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/notifications/delete-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAllNotifications() {
        log.debug("REST request to delete all notifications");
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new RuntimeException("User not found"));
        List<NotificationEntity> notifications = notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(user.getId());
        notificationRepository.deleteAll(notifications);
        return ResponseEntity.noContent().build();
    }
}
