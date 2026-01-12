package com.mycompany.myapp.service.messaging;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.OrderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing user registration email events to RabbitMQ
 */
@Service
public class EmailMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(EmailMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public EmailMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish user registration email event
     */
    public void publishUserRegistrationEmail(AdminUserDTO user) {
        try {
            log.info("🚀 [PRODUCER] Publishing user registration email event for: {}", user.getEmail());

            // Send directly to USER_REGISTRATION_QUEUE like Order flow
            rabbitTemplate.convertAndSend(RabbitMQConfig.USER_REGISTRATION_QUEUE, user);

            log.info("✅ [PRODUCER] User registration email event published successfully to RabbitMQ");
        } catch (Exception e) {
            log.error("❌ [PRODUCER] Failed to publish user registration email event", e);
            throw new RuntimeException("Failed to publish email event", e);
        }
    }

    /**
     * Publish order confirmation email event
     */
    public void publishOrderEmail(com.mycompany.myapp.domain.Order order) {
        try {
            log.info("Publishing order confirmation email for order");

            EmailMessage emailMessage = new EmailMessage(
                order.getCustomerEmail(),
                "Order Confirmation",
                EmailType.ORDER_CONFIRMATION,
                order
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, emailMessage);

            log.info("Order confirmation email event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish order confirmation email event", e);
            throw new RuntimeException("Failed to publish email event", e);
        }
    }

    // ===== INNER CLASSES - Message Wrappers =====

    public enum EmailType {
        ORDER_CONFIRMATION,
        USER_REGISTRATION,
        PASSWORD_RESET,
        ORDER_STATUS_UPDATE,
    }

    public static class EmailMessage {

        private String recipient;
        private String subject;
        private EmailType type;
        private Object data;
        private int retryCount = 0;

        public EmailMessage() {}

        public EmailMessage(String recipient, String subject, EmailType type, Object data) {
            this.recipient = recipient;
            this.subject = subject;
            this.type = type;
            this.data = data;
        }

        // Getters and Setters
        public String getRecipient() {
            return recipient;
        }

        public void setRecipient(String recipient) {
            this.recipient = recipient;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public EmailType getType() {
            return type;
        }

        public void setType(EmailType type) {
            this.type = type;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }
    }
}
