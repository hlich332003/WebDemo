package com.mycompany.myapp.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * RabbitMQ Configuration with Dead Letter Queue (DLQ) support
 */
@Configuration
public class RabbitMQConfig {

    // ==================== QUEUE NAMES ====================
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_DLQ = "order.dlq";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_ROUTING_KEY = "order.created";
    public static final String ORDER_DLQ_ROUTING_KEY = "order.dlq";

    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_DLQ = "email.dlq";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.send";
    public static final String EMAIL_DLQ_ROUTING_KEY = "email.dlq";

    // ==================== ADDITIONAL QUEUES ====================
    public static final String ORDER_EMAIL_QUEUE = "order.email.queue";
    public static final String USER_REGISTRATION_QUEUE = "user.registration.queue";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String USER_QUEUE = "user.queue";
    public static final String DLQ_QUEUE = "dlq.queue"; // General DLQ for all failed messages

    // ==================== LEGACY/BACKWARD COMPATIBILITY ====================
    public static final String APP_EXCHANGE = ORDER_EXCHANGE; // Alias for backward compatibility
    public static final String ORDER_CREATED_KEY = ORDER_ROUTING_KEY; // Alias
    public static final String USER_REGISTERED_KEY = "user.registered"; // For user registration events
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange"; // For notifications
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send"; // For notifications

    // ==================== MESSAGE CONVERTER ====================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());

        // Retry configuration
        RetryTemplate retryTemplate = new RetryTemplate();

        // Retry 3 times
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Exponential backoff: 2s, 4s, 8s
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        rabbitTemplate.setRetryTemplate(retryTemplate);

        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }

    // ==================== ORDER QUEUE WITH DLQ ====================

    /**
     * Main order queue with DLQ configuration
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
            .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
            .withArgument("x-message-ttl", 300000) // 5 minutes TTL
            .build();
    }

    /**
     * Dead Letter Queue for failed order messages
     */
    @Bean
    public Queue orderDLQ() {
        return QueueBuilder.durable(ORDER_DLQ)
            .withArgument("x-message-ttl", 86400000) // 24 hours in DLQ
            .build();
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding orderQueueBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding orderDLQBinding() {
        return BindingBuilder.bind(orderDLQ()).to(orderExchange()).with(ORDER_DLQ_ROUTING_KEY);
    }

    // ==================== EMAIL QUEUE WITH DLQ ====================

    /**
     * Main email queue with DLQ configuration
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", EMAIL_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
            .withArgument("x-message-ttl", 600000) // 10 minutes TTL
            .build();
    }

    /**
     * Dead Letter Queue for failed email messages
     */
    @Bean
    public Queue emailDLQ() {
        return QueueBuilder.durable(EMAIL_DLQ)
            .withArgument("x-message-ttl", 86400000) // 24 hours in DLQ
            .build();
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Binding emailQueueBinding() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding emailDLQBinding() {
        return BindingBuilder.bind(emailDLQ()).to(emailExchange()).with(EMAIL_DLQ_ROUTING_KEY);
    }

    // ==================== ADDITIONAL QUEUES ====================

    /**
     * Queue for order confirmation emails
     */
    @Bean
    public Queue orderEmailQueue() {
        return QueueBuilder.durable(ORDER_EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", EMAIL_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
            .withArgument("x-message-ttl", 600000) // 10 minutes TTL
            .build();
    }

    /**
     * Queue for user registration emails
     */
    @Bean
    public Queue userRegistrationQueue() {
        return QueueBuilder.durable(USER_REGISTRATION_QUEUE)
            .withArgument("x-dead-letter-exchange", EMAIL_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
            .withArgument("x-message-ttl", 600000) // 10 minutes TTL
            .build();
    }

    /**
     * Queue for notifications
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArgument("x-message-ttl", 300000) // 5 minutes TTL
            .build();
    }

    /**
     * Queue for user-related events
     */
    @Bean
    public Queue userQueue() {
        return QueueBuilder.durable(USER_QUEUE)
            .withArgument("x-message-ttl", 300000) // 5 minutes TTL
            .build();
    }

    /**
     * General Dead Letter Queue for all failed messages
     */
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
            .withArgument("x-message-ttl", 86400000) // 24 hours in DLQ
            .build();
    }
}
