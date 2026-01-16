package com.mycompany.myapp.service.messaging;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.MailService;
import com.mycompany.myapp.service.dto.OrderDTO;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Service for consuming messages from RabbitMQ queues.
 */
@Service
public class MessageConsumerService {

    private final Logger log = LoggerFactory.getLogger(MessageConsumerService.class);
    private final MailService mailService;

    public MessageConsumerService(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * Listen to order queue and process order events
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderCreatedEvent(OrderDTO order) {
        try {
            log.info("📦 Received order event from RabbitMQ: Order Date = {}", order.getOrderDate());

            // Process order (e.g., update inventory, create invoice, etc.)
            processOrder(order);

            log.info("✅ Order processed successfully: Order Date = {}", order.getOrderDate());
        } catch (Exception e) {
            log.error("❌ Error processing order event", e);
            throw new RuntimeException("Failed to process order: " + order.getOrderDate(), e);
        }
    }

    /**
     * Listen to email queue and send emails
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmailNotification(Map<String, String> emailData) {
        try {
            String to = emailData.get("to");
            String subject = emailData.get("subject");
            String body = emailData.get("body");

            log.info("📧 Received email notification from RabbitMQ: to = {}, subject = {}", to, subject);

            // Send email using MailService
            mailService.sendEmail(to, subject, body, false, false);

            log.info("✅ Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Error sending email", e);
            throw new RuntimeException("Failed to send email to: " + emailData.get("to"), e);
        }
    }

    /**
     * Listen to notification queue and send push notifications
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handlePushNotification(Map<String, String> notificationData) {
        try {
            String userId = notificationData.get("userId");
            String title = notificationData.get("title");
            String message = notificationData.get("message");

            log.info("🔔 Received push notification from RabbitMQ: userId = {}, title = {}", userId, title);

            // Send push notification (integrate with FCM, WebSocket, etc.)
            sendPushNotification(userId, title, message);

            log.info("✅ Push notification sent successfully to user: {}", userId);
        } catch (Exception e) {
            log.error("❌ Error sending push notification", e);
            throw new RuntimeException("Failed to send notification to user: " + notificationData.get("userId"), e);
        }
    }

    /**
     * Listen to user registration queue
     */
    @RabbitListener(queues = RabbitMQConfig.USER_QUEUE)
    public void handleUserRegistration(Map<String, String> userData) {
        try {
            String email = userData.get("email");
            String firstName = userData.get("firstName");

            log.info("👤 Received user registration event from RabbitMQ: email = {}", email);

            // Send welcome email
            String subject = "Chào mừng bạn đến với PcNo.1!";
            String body = String.format("Xin chào %s,\n\nChúc mừng bạn đã đăng ký thành công!\n\nTrân trọng,\nPcNo.1 Team", firstName);
            mailService.sendEmail(email, subject, body, false, false);

            log.info("✅ Welcome email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("❌ Error processing user registration", e);
            throw new RuntimeException("Failed to process user registration: " + userData.get("email"), e);
        }
    }

    /**
     * Listen to Dead Letter Queue (DLQ) for failed messages
     */
    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void handleDeadLetterQueue(Object message) {
        log.error("💀 Received message from Dead Letter Queue: {}", message);
        // TODO: Implement error handling, logging, alerting, or retry logic
    }

    // ===================================================================
    // Helper Methods
    // ===================================================================

    private void processOrder(OrderDTO order) {
        // TODO: Implement order processing logic
        // - Update inventory
        // - Create invoice
        // - Notify warehouse
        // - etc.
        log.info("Processing order at: {}", order.getOrderDate());
    }

    private void sendPushNotification(String userId, String title, String message) {
        // TODO: Implement push notification logic
        // - Use Firebase Cloud Messaging (FCM)
        // - Use WebSocket for real-time notifications
        // - Store notification in database
        log.info("Sending push notification to user {}: {} - {}", userId, title, message);
    }
}
