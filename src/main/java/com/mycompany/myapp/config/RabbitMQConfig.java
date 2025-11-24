package com.mycompany.myapp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String ORDER_EMAIL_QUEUE = "order.email.queue";
    public static final String USER_REGISTRATION_QUEUE = "user.registration.queue";

    // Dead Letter Queue names
    public static final String ORDER_EMAIL_DLQ = "order.email.queue.dlq";
    public static final String USER_REGISTRATION_DLQ = "user.registration.queue.dlq";

    // Exchange names
    public static final String APP_EXCHANGE = "app.exchange";
    public static final String DLQ_EXCHANGE = "dlq.exchange";

    // Routing keys
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String USER_REGISTERED_KEY = "user.registered";

    /**
     * Main Order Email Queue với Dead Letter Queue config
     * Khi message fail sau tất cả retry attempts, sẽ được gửi vào DLQ
     */
    @Bean
    public Queue orderEmailQueue() {
        return QueueBuilder.durable(ORDER_EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ORDER_EMAIL_DLQ)
            .build();
    }

    /**
     * Dead Letter Queue cho Order Email
     * Lưu trữ các message bị fail để admin review sau
     */
    @Bean
    public Queue orderEmailDLQ() {
        return new Queue(ORDER_EMAIL_DLQ, true);
    }

    /**
     * Main User Registration Queue với Dead Letter Queue config
     */
    @Bean
    public Queue userRegistrationQueue() {
        return QueueBuilder.durable(USER_REGISTRATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", USER_REGISTRATION_DLQ)
            .build();
    }

    /**
     * Dead Letter Queue cho User Registration
     */
    @Bean
    public Queue userRegistrationDLQ() {
        return new Queue(USER_REGISTRATION_DLQ, true);
    }

    @Bean
    public TopicExchange appExchange() {
        return new TopicExchange(APP_EXCHANGE);
    }

    /**
     * Dead Letter Exchange
     * Nhận các message failed từ main queues
     */
    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Binding orderEmailBinding() {
        return BindingBuilder.bind(orderEmailQueue()).to(appExchange()).with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding userRegistrationBinding() {
        return BindingBuilder.bind(userRegistrationQueue()).to(appExchange()).with(USER_REGISTERED_KEY);
    }

    /**
     * Binding cho Dead Letter Queues
     */
    @Bean
    public Binding orderEmailDLQBinding() {
        return BindingBuilder.bind(orderEmailDLQ()).to(dlqExchange()).with(ORDER_EMAIL_DLQ);
    }

    @Bean
    public Binding userRegistrationDLQBinding() {
        return BindingBuilder.bind(userRegistrationDLQ()).to(dlqExchange()).with(USER_REGISTRATION_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * Message Recoverer để republish failed messages vào DLQ
     * Khi consumer throw exception, message sẽ được gửi vào DLQ thay vì lost
     */
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLQ_EXCHANGE, ORDER_EMAIL_DLQ);
    }
}
