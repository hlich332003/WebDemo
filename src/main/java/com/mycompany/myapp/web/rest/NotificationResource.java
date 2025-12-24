package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.analytics.Notification; // SỬA IMPORT
import com.mycompany.myapp.repository.analytics.NotificationRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationResource {

    private final NotificationRepository repo;

    public NotificationResource(NotificationRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public List<Notification> getUserNotifications(@PathVariable Long userId) {
        // TODO: Add security check to ensure the authenticated user can only access their own notifications
        return repo.findByReceiverTypeAndReceiverIdOrderByCreatedAtDesc("USER", userId);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Notification> getAdminNotifications() {
        return repo.findByReceiverTypeOrderByCreatedAtDesc("ADMIN");
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public void markRead(@PathVariable Long id) {
        // TODO: Add security check to ensure the authenticated user can only mark their own notifications as read
        repo
            .findById(id)
            .ifPresent(n -> {
                n.setIsRead(true);
                repo.save(n);
            });
    }
}
