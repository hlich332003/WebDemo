package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.analytics.Notification;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.analytics.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional("analyticsTransactionManager")
public class NotificationService {

    private final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public NotificationService(
        SimpMessagingTemplate messagingTemplate,
        UserRepository userRepository,
        NotificationRepository notificationRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }


    /**
     * Gửi thông báo đơn hàng mới cho Admin
     */
    @Async
    public void notifyAdminNewOrder(Long orderId, String customerName) {
        log.debug("Sending new order notification to admins for order: {}", orderId);
        String message = "Có đơn hàng mới #" + orderId + " từ khách hàng: " + customerName;
        String link = "/admin/order-management/" + orderId + "/view";

        // Lấy danh sách admin
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");

        for (User admin : admins) {
            // Lưu thông báo vào database
            Notification notification = createAndSaveNotification(
                admin.getEmail(),
                "NEW_ORDER",
                message,
                link,
                "ROLE_ADMIN"
            );

            // Gửi qua WebSocket
            sendNotificationToUser(admin.getEmail(), notification);
        }

        // Gửi broadcast cho topic admin
        sendNotificationToTopic("/topic/notifications/admin", "NEW_ORDER", message, link);
    }

    /**
     * Gửi thông báo ticket hỗ trợ mới cho Admin/CSKH
     */
    @Async
    public void notifyAdminNewSupportTicket(Long ticketId, String userEmail) {
        log.debug("Sending new support ticket notification to admins for ticket: {}", ticketId);
        String message = "Yêu cầu hỗ trợ mới #" + ticketId + " từ: " + userEmail;
        String link = "/admin/customer-support/ticket/" + ticketId;

        // Lấy danh sách admin và CSKH
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");
        List<User> cskhUsers = userRepository.findAllByAuthority("ROLE_CSKH");

        admins.addAll(cskhUsers);

        for (User user : admins) {
            // Lưu thông báo vào database
            Notification notification = createAndSaveNotification(
                user.getEmail(),
                "NEW_SUPPORT_TICKET",
                message,
                link,
                "ROLE_ADMIN"
            );

            // Gửi qua WebSocket
            sendNotificationToUser(user.getEmail(), notification);
        }

        // Gửi broadcast cho topic admin
        sendNotificationToTopic("/topic/notifications/admin", "NEW_SUPPORT_TICKET", message, link);
    }

    /**
     * Gửi thông báo đặt hàng thành công cho User
     */
    @Async
    public void notifyUserOrderSuccess(User user, String orderCode) {
        log.debug("Sending order success notification to user: {}", user.getEmail());
        String message = "Đơn hàng #" + orderCode + " của bạn đã được đặt thành công! Cảm ơn bạn đã mua hàng.";
        String link = "/account/orders";

        // Lưu thông báo vào database
        Notification notification = createAndSaveNotification(
            user.getEmail(),
            "ORDER_SUCCESS",
            message,
            link,
            null
        );

        // Gửi qua WebSocket
        sendNotificationToUser(user.getEmail(), notification);
    }

    /**
     * Gửi thông báo sản phẩm sắp hết hàng cho User
     */
    @Async
    public void notifyUserLowStock(User user, Product product) {
        log.debug("Sending low stock notification to user: {} for product: {}", user.getEmail(), product.getName());
        String message = "Sản phẩm '" + product.getName() + "' trong danh sách yêu thích của bạn chỉ còn " + product.getQuantity() + " sản phẩm!";
        String link = "/product/" + product.getId();

        // Lưu thông báo vào database
        Notification notification = createAndSaveNotification(
            user.getEmail(),
            "LOW_STOCK",
            message,
            link,
            null
        );

        // Gửi qua WebSocket
        sendNotificationToUser(user.getEmail(), notification);
    }

    /**
     * Gửi thông báo khuyến mãi cho User
     */
    @Async
    public void notifyUserPromotion(String userEmail, String promotionMessage, String link) {
        log.debug("Sending promotion notification to user: {}", userEmail);

        // Lưu thông báo vào database
        Notification notification = createAndSaveNotification(
            userEmail,
            "PROMOTION",
            promotionMessage,
            link,
            null
        );

        // Gửi qua WebSocket
        sendNotificationToUser(userEmail, notification);
    }

    /**
     * Gửi thông báo thay đổi trạng thái đơn hàng
     */
    @Async
    public void notifyOrderStatusChange(String userEmail, Long orderId, String newStatus) {
        log.debug("Sending order status change notification to user: {}", userEmail);
        String message = "Đơn hàng #" + orderId + " của bạn đã chuyển sang trạng thái: " + newStatus;
        String link = "/account/orders";

        // Lưu thông báo vào database
        Notification notification = createAndSaveNotification(
            userEmail,
            "ORDER_STATUS_CHANGE",
            message,
            link,
            null
        );

        // Gửi qua WebSocket
        sendNotificationToUser(userEmail, notification);
    }

    /**
     * Tạo và lưu thông báo vào database
     */
    private Notification createAndSaveNotification(
        String userId,
        String type,
        String message,
        String link,
        String targetRole
    ) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setTargetRole(targetRole);
        notification.setCreatedAt(Instant.now());
        notification.setIsRead(false);

        return notificationRepository.save(notification);
    }

    /**
     * Gửi thông báo qua WebSocket đến topic chung
     */
    private void sendNotificationToTopic(String topic, String type, String message, String link) {
        Map<String, Object> payload = createPayload(null, type, message, link);
        messagingTemplate.convertAndSend(topic, payload);
        log.debug("Notification sent to topic {}: {}", topic, payload);
    }

    /**
     * Gửi thông báo qua WebSocket đến user cụ thể
     */
    private void sendNotificationToUser(String userEmail, Notification notification) {
        Map<String, Object> payload = createPayload(notification.getId(), notification.getType(),
            notification.getMessage(), notification.getLink());
        
        // Ensure email is lowercase to match Principal name usually
        String normalizedEmail = userEmail.toLowerCase();
        
        messagingTemplate.convertAndSendToUser(normalizedEmail, "/queue/notifications", payload);
        log.debug("Notification sent to user {}: {}", normalizedEmail, payload);
    }

    /**
     * Tạo payload cho WebSocket message
     */
    private Map<String, Object> createPayload(Long id, String type, String message, String link) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("type", type);
        payload.put("message", message);
        payload.put("link", link);
        payload.put("timestamp", Instant.now());
        payload.put("read", false);
        return payload;
    }

    /**
     * Lấy danh sách thông báo của user
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Đếm số thông báo chưa đọc
     */
    @Transactional(readOnly = true)
    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    /**
     * Đánh dấu một thông báo là đã đọc
     */
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsReadById(notificationId);
    }

    /**
     * Xóa thông báo cũ (chạy định kỳ)
     */
    @Async
    public void cleanupOldNotifications(int daysToKeep) {
        Instant cutoffDate = Instant.now().minusSeconds(daysToKeep * 24 * 60 * 60);
        int deleted = notificationRepository.deleteOldNotifications(cutoffDate);
        log.info("Cleaned up {} old notifications older than {} days", deleted, daysToKeep);
    }

}
