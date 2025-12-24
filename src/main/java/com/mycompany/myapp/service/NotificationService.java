package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.analytics.Notification; // SỬA IMPORT
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.analytics.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional("analyticsTransactionManager")
public class NotificationService {

    private final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository repository, SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public void notifyUserOrderSuccess(Long userId, Long orderId) {
        Notification n = new Notification();
        n.setTitle("Đặt hàng thành công");
        n.setMessage("Đơn hàng #" + orderId + " đã được tạo thành công");
        n.setType("ORDER_SUCCESS");
        n.setReceiverType("USER");
        n.setReceiverId(userId);
        n.setLink("/orders/" + orderId);
        n.setRelatedEntityType("ORDER");
        n.setRelatedEntityId(orderId);

        repository.save(n);

        // Determine the user identifier used as Principal name in WebSocket (we use email lowercased)
        String userDestination = String.valueOf(userId);
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getEmail() != null) {
                userDestination = user.getEmail().toLowerCase();
            }
        } catch (Exception ex) {
            log.warn("Unable to resolve user email for userId {}: {}. Falling back to id-based destination.", userId, ex.toString());
        }

        // Send to the specific user destination (must match Principal.getName())
        messagingTemplate.convertAndSendToUser(userDestination, "/queue/notifications", n);
    }

    public void notifyAdminNewOrder(Long orderId, String customerName) {
        log.warn("Calling adapted notifyAdminNewOrder method. The 'customerName' parameter is ignored.");
        notifyAdminNewOrder(orderId);
    }

    public void notifyAdminNewOrder(Long orderId) {
        Notification n = new Notification();
        n.setTitle("Đơn hàng mới");
        n.setMessage("Có đơn hàng mới #" + orderId);
        n.setType("NEW_ORDER");
        n.setReceiverType("ADMIN");
        n.setLink("/admin/orders/" + orderId);
        n.setRelatedEntityType("ORDER");
        n.setRelatedEntityId(orderId);

        repository.save(n);

        messagingTemplate.convertAndSend("/topic/admin/notifications", n);
    }

    public void notifyOrderStatusChange(String userEmail, Long orderId, String newStatus) {
        log.warn(
            "Placeholder method 'notifyOrderStatusChange' was called for user {}, orderId {}, status {}. No notification was sent.",
            userEmail,
            orderId,
            newStatus
        );
    }
}
