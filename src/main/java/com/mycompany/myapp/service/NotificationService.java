package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.analytics.NotificationEntity;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.analytics.NotificationRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserRepository userRepository,
        SimpMessageSendingOperations messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ==========================================
    // USER NOTIFICATIONS
    // ==========================================

    /**
     * Thông báo khi người dùng đặt đơn thành công
     */
    public void notifyUserOrderSuccess(Long userId, Long orderId, String orderCode) {
        userRepository
            .findById(userId)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("ORDER_SUCCESS");
                notification.setTitle("Đặt hàng thành công");
                notification.setMessage("Đơn hàng " + orderCode + " của bạn đã được tạo thành công!");
                notification.setLink("/account/my-orders/" + orderId);
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo khi đơn hàng được xác nhận
     */
    public void notifyUserOrderConfirmed(String userEmail, Long orderId, String orderCode) {
        userRepository
            .findOneByEmailIgnoreCase(userEmail)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("ORDER_CONFIRMED");
                notification.setTitle("Đơn hàng đã được xác nhận");
                notification.setMessage("Đơn hàng " + orderCode + " của bạn đã được xác nhận và đang được xử lý!");
                notification.setLink("/account/my-orders/" + orderId);
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo khi đơn hàng đang được giao
     */
    public void notifyUserOrderShipped(String userEmail, Long orderId, String orderCode) {
        userRepository
            .findOneByEmailIgnoreCase(userEmail)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("ORDER_SHIPPED");
                notification.setTitle("Đơn hàng đang được giao");
                notification.setMessage("Đơn hàng " + orderCode + " của bạn đang trên đường giao đến!");
                notification.setLink("/account/my-orders/" + orderId);
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo khi giao hàng thành công
     */
    public void notifyUserOrderDelivered(String userEmail, Long orderId, String orderCode) {
        userRepository
            .findOneByEmailIgnoreCase(userEmail)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("ORDER_DELIVERED");
                notification.setTitle("Giao hàng thành công");
                notification.setMessage("Đơn hàng " + orderCode + " đã được giao thành công! Cảm ơn bạn đã mua hàng.");
                notification.setLink("/account/my-orders/" + orderId);
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo thay đổi trạng thái đơn hàng (general)
     */
    public void notifyOrderStatusChange(String userEmail, Long orderId, String status, String orderCode) {
        userRepository
            .findOneByEmailIgnoreCase(userEmail)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("ORDER_STATUS_CHANGE");
                notification.setTitle("Cập nhật trạng thái đơn hàng");
                notification.setMessage("Đơn hàng " + orderCode + " của bạn đã được cập nhật thành: " + status);
                notification.setLink("/account/my-orders/" + orderId);
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo khi sản phẩm trong wishlist còn ít (dưới 5)
     */
    public void notifyUserWishlistLowStock(Long userId, Long productId, String productName, int stockQuantity) {
        userRepository
            .findById(userId)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("WISHLIST_LOW_STOCK");
                notification.setTitle("Sản phẩm yêu thích sắp hết hàng");
                notification.setMessage(
                    "Sản phẩm \"" + productName + "\" trong danh sách yêu thích của bạn chỉ còn " + stockQuantity + " sản phẩm!"
                );
                notification.setLink("/product/" + productId);
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo khi có khuyến mãi mới
     */
    public void notifyUserNewPromotion(Long userId, String promotionTitle, String promotionDetails) {
        userRepository
            .findById(userId)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("NEW_PROMOTION");
                notification.setTitle("Khuyến mãi mới: " + promotionTitle);
                notification.setMessage(promotionDetails);
                notification.setLink("/products");
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo broadcast khuyến mãi cho tất cả user
     */
    public void notifyAllUsersPromotion(String promotionTitle, String promotionDetails) {
        List<User> users = userRepository.findAllByActivatedIsTrue();
        for (User user : users) {
            NotificationEntity notification = new NotificationEntity();
            notification.setReceiverId(user.getId());
            notification.setReceiverType("USER");
            notification.setType("NEW_PROMOTION");
            notification.setTitle("Khuyến mãi mới: " + promotionTitle);
            notification.setMessage(promotionDetails);
            notification.setLink("/products");
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
        }
    }

    /**
     * Thông báo khi sản phẩm wishlist có giá giảm
     */
    public void notifyUserWishlistPriceDown(Long userId, Long productId, String productName, double oldPrice, double newPrice) {
        userRepository
            .findById(userId)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("WISHLIST_PRICE_DOWN");
                notification.setTitle("Giá giảm cho sản phẩm yêu thích");
                notification.setMessage(
                    String.format("Sản phẩm \"%s\" đã giảm giá từ %,.0fđ xuống %,.0fđ!", productName, oldPrice, newPrice)
                );
                notification.setLink("/product/" + productId);
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo user khi admin reply trong ticket support
     */
    public void notifyUserTicketReply(String userEmail, Long ticketId, String adminName, String messagePreview) {
        userRepository
            .findOneByEmailIgnoreCase(userEmail)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("TICKET_REPLY");
                notification.setTitle("Admin đã trả lời Ticket #" + ticketId);
                notification.setMessage(adminName + " đã phản hồi: " + messagePreview);
                notification.setLink("/");
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    /**
     * Thông báo user khi ticket được đóng
     */
    public void notifyUserTicketClosed(String userEmail, Long ticketId, String reason) {
        userRepository
            .findOneByEmailIgnoreCase(userEmail)
            .ifPresent(user -> {
                NotificationEntity notification = new NotificationEntity();
                notification.setReceiverId(user.getId());
                notification.setReceiverType("USER");
                notification.setType("TICKET_CLOSED");
                notification.setTitle("Ticket #" + ticketId + " đã được đóng");
                notification.setMessage("Yêu cầu hỗ trợ của bạn đã được giải quyết. " + (reason != null ? "Lý do: " + reason : ""));
                notification.setLink("/");
                notificationRepository.save(notification);
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", notification);
            });
    }

    // ==========================================
    // ADMIN NOTIFICATIONS
    // ==========================================

    /**
     * Thông báo admin khi có đơn hàng mới
     */
    public void notifyAdminNewOrder(Long orderId, String orderNumber, String customerName) {
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");
        for (User admin : admins) {
            NotificationEntity notification = new NotificationEntity();
            notification.setReceiverId(admin.getId());
            notification.setReceiverType("ADMIN");
            notification.setType("NEW_ORDER");
            notification.setTitle("Đơn hàng mới");
            notification.setMessage("Có đơn hàng mới " + orderNumber + " từ khách hàng " + customerName);
            notification.setLink("/admin/order-management?orderId=" + orderId);
            notification.setRelatedEntityType("ORDER");
            notification.setRelatedEntityId(orderId);
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(admin.getEmail(), "/queue/notifications", notification);
        }
    }

    public void notifyAdminNewOrder(Long orderId, String orderNumber) {
        notifyAdminNewOrder(orderId, orderNumber, "Khách hàng");
    }

    /**
     * Thông báo admin khi sản phẩm sắp hết hàng (dưới 10)
     */
    public void notifyAdminLowStock(Long productId, String productName, int stockQuantity) {
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");
        for (User admin : admins) {
            NotificationEntity notification = new NotificationEntity();
            notification.setReceiverId(admin.getId());
            notification.setReceiverType("ADMIN");
            notification.setType("LOW_STOCK");
            notification.setTitle("Sản phẩm sắp hết hàng");
            notification.setMessage("Sản phẩm \"" + productName + "\" chỉ còn " + stockQuantity + " sản phẩm trong kho!");
            notification.setLink("/admin/product-management?productId=" + productId);
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(admin.getEmail(), "/queue/notifications", notification);
        }
    }

    /**
     * Thông báo admin khi có chat support mới cần xử lý
     */
    public void notifyAdminNewChatSupport(Long conversationId, String userIdentifier, long unreadCount) {
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");
        List<User> cskhs = userRepository.findAllByAuthority("ROLE_CSKH");

        // Thông báo cho cả Admin và CSKH
        admins.addAll(cskhs);

        // Broadcast to all admin users for real-time updates
        Map<String, Object> broadcastMessage = new HashMap<>();
        broadcastMessage.put("type", "NEW_CHAT");
        broadcastMessage.put("conversationId", conversationId);
        broadcastMessage.put("userIdentifier", userIdentifier);
        broadcastMessage.put("unreadCount", unreadCount);
        broadcastMessage.put("timestamp", Instant.now().toString());
        messagingTemplate.convertAndSend("/topic/admin/chat-notifications", broadcastMessage);

        for (User admin : admins) {
            NotificationEntity notification = new NotificationEntity();
            notification.setReceiverId(admin.getId());
            notification.setReceiverType("ADMIN");
            notification.setType("NEW_CHAT");
            notification.setTitle("Chat hỗ trợ mới");
            notification.setMessage("Có yêu cầu chat trực tiếp mới từ " + userIdentifier + " (" + unreadCount + " tin nhắn chưa đọc)");
            notification.setLink("/admin/customer-support?conversationId=" + conversationId);
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(admin.getEmail(), "/queue/notifications", notification);
        }
    }

    /**
     * Thông báo admin khi có chat support mới (compatibility method)
     */
    public void notifyAdminNewChatSupport(Long conversationId, String userIdentifier) {
        notifyAdminNewChatSupport(conversationId, userIdentifier, 1);
    }

    /**
     * Thông báo admin khi có ticket support mới (hỗ trợ offline qua email/form)
     */
    public void notifyAdminNewSupportTicket(Long ticketId, String userEmail, String title) {
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");
        List<User> cskhs = userRepository.findAllByAuthority("ROLE_CSKH");

        admins.addAll(cskhs);

        // Broadcast to all admin users for real-time updates
        Map<String, Object> broadcastMessage = new HashMap<>();
        broadcastMessage.put("type", "NEW_TICKET");
        broadcastMessage.put("ticketId", ticketId);
        broadcastMessage.put("userEmail", userEmail);
        broadcastMessage.put("title", title);
        broadcastMessage.put("timestamp", Instant.now().toString());
        messagingTemplate.convertAndSend("/topic/admin/ticket-notifications", broadcastMessage);

        for (User admin : admins) {
            NotificationEntity notification = new NotificationEntity();
            notification.setReceiverId(admin.getId());
            notification.setReceiverType("ADMIN");
            notification.setType("NEW_TICKET");
            notification.setTitle("Yêu cầu hỗ trợ mới (Ticket)");
            notification.setMessage("Ticket #" + ticketId + " từ " + userEmail + ": " + title);
            notification.setLink("/admin/customer-support?conversationId=" + ticketId);
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(admin.getEmail(), "/queue/notifications", notification);
        }
    }

    /**
     * Thông báo admin khi có reply mới trong ticket
     */
    public void notifyAdminTicketReply(Long ticketId, String userEmail, String messagePreview) {
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");
        List<User> cskhs = userRepository.findAllByAuthority("ROLE_CSKH");

        admins.addAll(cskhs);

        // Broadcast to all admin users for real-time updates
        Map<String, Object> broadcastMessage = new HashMap<>();
        broadcastMessage.put("type", "TICKET_REPLY");
        broadcastMessage.put("ticketId", ticketId);
        broadcastMessage.put("userEmail", userEmail);
        broadcastMessage.put("messagePreview", messagePreview);
        broadcastMessage.put("timestamp", Instant.now().toString());
        messagingTemplate.convertAndSend("/topic/admin/ticket-notifications", broadcastMessage);

        for (User admin : admins) {
            NotificationEntity notification = new NotificationEntity();
            notification.setReceiverId(admin.getId());
            notification.setReceiverType("ADMIN");
            notification.setType("TICKET_REPLY");
            notification.setTitle("Phản hồi mới trong Ticket #" + ticketId);
            notification.setMessage("Khách hàng " + userEmail + " đã trả lời: " + messagePreview);
            notification.setLink("/admin/customer-support?conversationId=" + ticketId);
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(admin.getEmail(), "/queue/notifications", notification);
        }
    }

    /**
     * Thông báo admin khi sản phẩm hết hàng hoàn toàn
     */
    public void notifyAdminOutOfStock(Long productId, String productName) {
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");
        for (User admin : admins) {
            NotificationEntity notification = new NotificationEntity();
            notification.setReceiverId(admin.getId());
            notification.setReceiverType("ADMIN");
            notification.setType("OUT_OF_STOCK");
            notification.setTitle("Sản phẩm đã hết hàng");
            notification.setMessage("Sản phẩm \"" + productName + "\" đã hết hàng hoàn toàn!");
            notification.setLink("/admin/product-management?productId=" + productId);
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(admin.getEmail(), "/queue/notifications", notification);
        }
    }

    /**
     * Thông báo admin khi có đơn hàng bị hủy
     */
    public void notifyAdminOrderCancelled(Long orderId, String customerName, String reason, String orderCode) {
        List<User> admins = userRepository.findAllByAuthority("ROLE_ADMIN");
        for (User admin : admins) {
            NotificationEntity notification = new NotificationEntity();
            notification.setReceiverId(admin.getId());
            notification.setReceiverType("ADMIN");
            notification.setType("ORDER_CANCELLED");
            notification.setTitle("Đơn hàng bị hủy");
            notification.setMessage("Đơn hàng " + orderCode + " từ " + customerName + " đã bị hủy. Lý do: " + reason);
            notification.setLink("/admin/order-management?orderId=" + orderId);
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(admin.getEmail(), "/queue/notifications", notification);
        }
    }
}
