package com.mycompany.myapp.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Main Exchange (for compatibility with existing MessageProducer)
    public static final String APP_EXCHANGE = "app_exchange";

    // Queue names
    public static final String ORDER_QUEUE = "order_queue";
    public static final String EMAIL_QUEUE = "email_queue";
    public static final String NOTIFICATION_QUEUE = "notification_queue";
    public static final String USER_QUEUE = "user_queue";
    public static final String ORDER_EMAIL_QUEUE = "order_email_queue";
    public static final String USER_REGISTRATION_QUEUE = "user_registration_queue";

    // Exchange names
    public static final String ORDER_EXCHANGE = "order_exchange";
    public static final String EMAIL_EXCHANGE = "email_exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification_exchange";

    // Routing keys (for compatibility with existing MessageProducer)
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String USER_REGISTERED_KEY = "user.registered";

    // Routing keys
    public static final String ORDER_ROUTING_KEY = "order.created";
    public static final String EMAIL_ROUTING_KEY = "email.send";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";
    public static final String USER_ROUTING_KEY = "user.registered";

    // Dead Letter Queue
    public static final String DLQ_QUEUE = "dlq_queue";
    public static final String DLQ_EXCHANGE = "dlq_exchange";
    public static final String DLQ_ROUTING_KEY = "dlq.routing";

    // ===================================================================
    // Order Queue Configuration
    // ===================================================================

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    // ===================================================================
    // Email Queue Configuration
    // ===================================================================

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailQueue).to(emailExchange).with(EMAIL_ROUTING_KEY);
    }

    // ===================================================================
    // Notification Queue Configuration
    // ===================================================================

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    // ===================================================================
    // Main App Exchange (for backward compatibility)
    // ===================================================================

    @Bean
    public TopicExchange appExchange() {
        return new TopicExchange(APP_EXCHANGE);
    }

    @Bean
    public Queue userQueue() {
        return QueueBuilder.durable(USER_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    public Binding orderToAppExchangeBinding(Queue orderQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(orderQueue).to(appExchange).with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding userToAppExchangeBinding(Queue userQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(userQueue).to(appExchange).with(USER_REGISTERED_KEY);
    }

    // Email-specific queues (for backward compatibility with EmailService/EmailConsumer)
    @Bean
    public Queue orderEmailQueue() {
        return QueueBuilder.durable(ORDER_EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    public Queue userRegistrationQueue() {
        return QueueBuilder.durable(USER_REGISTRATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
            .build();
    }

    // ===================================================================
    // Dead Letter Queue Configuration
    // ===================================================================

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    // ===================================================================
    // Message Converter (JSON)
    // ===================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}

