package com.mycompany.myapp.service;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.dto.UserRegistrationEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {

    private final Logger log = LoggerFactory.getLogger(MessageProducer.class);
    private final RabbitTemplate rabbitTemplate;

    public MessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrderCreatedEvent(OrderEventDTO event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.APP_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, event);
            log.info("Sent order created event for order: {}", event.getOrderCode());
        } catch (Exception e) {
            log.error("Failed to send order created event: {}", e.getMessage());
        }
    }

    public void sendUserRegisteredEvent(UserRegistrationEventDTO event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.APP_EXCHANGE, RabbitMQConfig.USER_REGISTERED_KEY, event);
            log.info("Sent user registered event for user: {}", event.getLogin());
        } catch (Exception e) {
            log.error("Failed to send user registered event: {}", e.getMessage());
        }
    }
}
