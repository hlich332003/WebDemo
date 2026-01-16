package com.mycompany.myapp.service;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.messaging.OrderMessageProducer.OrderMessage;
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
    public void handleOrderCreatedEvent(OrderMessage orderMessage) {
        log.info(
            "Received order created event for email: OrderID={}, Email={}",
            orderMessage.getOrderId(),
            orderMessage.getCustomerEmail()
        );

        try {
            String customerEmail = orderMessage.getCustomerEmail();
            String customerName = orderMessage.getCustomerFullName() != null ? orderMessage.getCustomerFullName() : "Khách hàng";

            // Validate email address
            if (customerEmail == null || customerEmail.isEmpty() || customerEmail.contains("example.com")) {
                log.warn("Invalid or placeholder email address: {}. Skipping email send.", customerEmail);
                return;
            }

            log.info("Sending confirmation email for order {} to {}", orderMessage.getOrderCode(), customerEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(customerEmail);
            message.setSubject("Xác nhận đơn hàng #" + orderMessage.getOrderCode());
            message.setText(
                "Kính gửi " +
                customerName +
                ",\n\n" +
                "Cảm ơn bạn đã đặt hàng tại cửa hàng của chúng tôi!\n" +
                "Mã đơn hàng: " +
                orderMessage.getOrderCode() +
                "\n" +
                "Tổng tiền: " +
                orderMessage.getTotalAmount() +
                " VND\n" +
                "Chúng tôi sẽ xử lý đơn hàng của bạn sớm nhất có thể.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ PcNo.1"
            );
            mailSender.send(message);
            log.info("✅ Confirmation email sent successfully to {}", customerEmail);
        } catch (Exception e) {
            log.error("❌ Failed to send confirmation email: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.USER_REGISTRATION_QUEUE)
    public void handleUserRegisteredEvent(AdminUserDTO user) {
        log.info("Received user registered event for email: {}", user);
        log.info("Sending welcome email to user {}", user.getEmail());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Chào mừng bạn đến với PcNo.1!");
            message.setText(
                "Kính gửi " +
                user.getFirstName() +
                ",\n\n" +
                "Chào mừng bạn đã đăng ký tài khoản tại PcNo.1. Chúc bạn có trải nghiệm mua sắm tuyệt vời!\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ PcNo.1"
            );
            mailSender.send(message);
            log.info("Welcome email sent for user {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email for user {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
