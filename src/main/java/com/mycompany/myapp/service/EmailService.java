package com.mycompany.myapp.service;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.dto.UserRegistrationEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_EMAIL_QUEUE)
    public void handleOrderCreatedEvent(OrderEventDTO event) {
        log.info("Received order created event for email: {}", event);
        log.info("Sending confirmation email for order {} to {}", event.getOrderCode(), event.getCustomerEmail());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // Sử dụng email từ file cấu hình
            message.setTo(event.getCustomerEmail());
            message.setSubject("Xác nhận đơn hàng #" + event.getOrderCode());
            message.setText(
                "Kính gửi " +
                event.getCustomerName() +
                ",\n\n" +
                "Cảm ơn bạn đã đặt hàng tại cửa hàng của chúng tôi!\n" +
                "Mã đơn hàng của bạn là: " +
                event.getOrderCode() +
                "\n" +
                "Tổng tiền: " +
                event.getTotalAmount() +
                " VND\n" +
                "Chúng tôi sẽ xử lý đơn hàng của bạn sớm nhất có thể.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ WebDemo"
            );
            mailSender.send(message);
            log.info("Confirmation email sent for order {}", event.getOrderCode());
        } catch (Exception e) {
            log.error("Failed to send confirmation email for order {}: {}", event.getOrderCode(), e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.USER_REGISTRATION_QUEUE)
    public void handleUserRegisteredEvent(UserRegistrationEventDTO event) {
        log.info("Received user registered event for email: {}", event);
        log.info("Sending welcome email to user {}", event.getLogin());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // Sử dụng email từ file cấu hình
            message.setTo(event.getEmail());
            message.setSubject("Chào mừng bạn đến với WebDemo!");
            message.setText(
                "Kính gửi " +
                event.getFirstName() +
                ",\n\n" +
                "Chào mừng bạn đã đăng ký tài khoản tại WebDemo. Chúc bạn có trải nghiệm mua sắm tuyệt vời!\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ WebDemo"
            );
            mailSender.send(message);
            log.info("Welcome email sent for user {}", event.getLogin());
        } catch (Exception e) {
            log.error("Failed to send welcome email for user {}: {}", event.getLogin(), e.getMessage());
        }
    }
}
