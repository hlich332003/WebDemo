package com.mycompany.myapp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String ORDER_EMAIL_QUEUE = "order.email.queue";
    public static final String USER_REGISTRATION_QUEUE = "user.registration.queue";

    // Exchange name
    public static final String APP_EXCHANGE = "app.exchange";

    // Routing keys
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String USER_REGISTERED_KEY = "user.registered";

    @Bean
    public Queue orderEmailQueue() {
        return new Queue(ORDER_EMAIL_QUEUE, true);
    }

    @Bean
    public Queue userRegistrationQueue() {
        return new Queue(USER_REGISTRATION_QUEUE, true);
    }

    @Bean
    public TopicExchange appExchange() {
        return new TopicExchange(APP_EXCHANGE);
    }

    @Bean
    public Binding orderEmailBinding() {
        return BindingBuilder.bind(orderEmailQueue()).to(appExchange()).with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding userRegistrationBinding() {
        return BindingBuilder.bind(userRegistrationQueue()).to(appExchange()).with(USER_REGISTERED_KEY);
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
}
