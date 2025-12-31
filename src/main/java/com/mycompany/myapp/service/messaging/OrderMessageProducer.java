package com.mycompany.myapp.service.messaging;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.domain.Order;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing order events to RabbitMQ
 */
@Service
public class OrderMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish order created event
     */
    public void publishOrderCreated(Order order) {
        try {
            log.info("Publishing order created event for order with total: {}", order.getTotalAmount());

            // Wrap Order in OrderMessage
            OrderMessage orderMessage = new OrderMessage(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerEmail(),
                order.getCustomerFullName(),
                order.getTotalAmount()
            );

            // Send to order processing queue
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY, orderMessage);

            // Also send to email queue
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EMAIL_QUEUE, orderMessage);

            log.info("Order created event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish order created event", e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }

    // ===== INNER CLASS - Message Wrapper =====

    public static class OrderMessage {

        private Long orderId;
        private String orderCode;
        private String customerEmail;
        private String customerFullName;
        private BigDecimal totalAmount;

        public OrderMessage() {}

        public OrderMessage(Long orderId, String orderCode, String customerEmail, String customerFullName, BigDecimal totalAmount) {
            this.orderId = orderId;
            this.orderCode = orderCode;
            this.customerEmail = customerEmail;
            this.customerFullName = customerFullName;
            this.totalAmount = totalAmount;
        }

        // Getters and Setters
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public String getOrderCode() {
            return orderCode;
        }

        public void setOrderCode(String orderCode) {
            this.orderCode = orderCode;
        }

        public String getCustomerEmail() {
            return customerEmail;
        }

        public void setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
        }

        public String getCustomerFullName() {
            return customerFullName;
        }

        public void setCustomerFullName(String customerFullName) {
            this.customerFullName = customerFullName;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }
    }
}
