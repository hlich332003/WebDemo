package com.mycompany.myapp.service.messaging;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.dto.OrderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending messages to RabbitMQ queues.
 */
@Service
public class MessageProducerService {

    private final Logger log = LoggerFactory.getLogger(MessageProducerService.class);
    private final RabbitTemplate rabbitTemplate;

    public MessageProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Send order created event to RabbitMQ
     */
    public void sendOrderCreatedEvent(OrderDTO order) {
        try {
            log.info("Sending order created event to RabbitMQ: Order Date = {}", order.getOrderDate());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                order
            );
            log.info("✅ Order event sent successfully");
        } catch (Exception e) {
            log.error("❌ Error sending order event to RabbitMQ", e);
        }
    }

    /**
     * Send email notification to RabbitMQ
     */
    public void sendEmailNotification(String to, String subject, String body) {
        try {
            Map<String, String> emailData = new HashMap<>();
            emailData.put("to", to);
            emailData.put("subject", subject);
            emailData.put("body", body);
            emailData.put("timestamp", String.valueOf(System.currentTimeMillis()));

            log.info("Sending email notification to RabbitMQ: to = {}, subject = {}", to, subject);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                emailData
            );
            log.info("✅ Email notification sent to queue successfully");
        } catch (Exception e) {
            log.error("❌ Error sending email notification to RabbitMQ", e);
        }
    }

    /**
     * Send push notification to RabbitMQ
     */
    public void sendPushNotification(String userId, String title, String message) {
        try {
            Map<String, String> notificationData = new HashMap<>();
            notificationData.put("userId", userId);
            notificationData.put("title", title);
            notificationData.put("message", message);
            notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

            log.info("Sending push notification to RabbitMQ: userId = {}, title = {}", userId, title);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                notificationData
            );
            log.info("✅ Push notification sent to queue successfully");
        } catch (Exception e) {
            log.error("❌ Error sending push notification to RabbitMQ", e);
        }
    }

    /**
     * Send generic message to any queue
     */
    public void sendMessage(String exchange, String routingKey, Object message) {
        try {
            log.info("Sending message to RabbitMQ: exchange = {}, routingKey = {}", exchange, routingKey);
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("✅ Message sent successfully");
        } catch (Exception e) {
            log.error("❌ Error sending message to RabbitMQ", e);
        }
    }
}

