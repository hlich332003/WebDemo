package com.mycompany.myapp.service;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.dto.UserRegistrationEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class EmailConsumer {

    private final Logger log = LoggerFactory.getLogger(EmailConsumer.class);
    private final MailService mailService;

    public EmailConsumer(MailService mailService) {
        this.mailService = mailService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_EMAIL_QUEUE)
    public void handleOrderCreated(OrderEventDTO event) {
        log.info("Received order created event for order: {}", event.getOrderCode());
        try {
            sendOrderConfirmationEmail(event);
            log.info("Successfully sent order confirmation email to: {}", event.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.USER_REGISTRATION_QUEUE)
    public void handleUserRegistered(UserRegistrationEventDTO event) {
        log.info("Received user registered event for user: {}", event.getLogin());
        try {
            sendWelcomeEmail(event);
            log.info("Successfully sent welcome email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }
    }

    private void sendOrderConfirmationEmail(OrderEventDTO event) {
        String subject = "Xác nhận đơn hàng " + event.getOrderCode();
        String content = String.format(
            	"""
            	Kính gửi %s,

            	Cảm ơn bạn đã đặt hàng tại WebDemo!

            	Thông tin đơn hàng:
            	- Mã đơn hàng: %s
            	- Tổng tiền: %,.0f VNĐ
            	- Trạng thái: Đang xử lý

            	Chúng tôi sẽ liên hệ với bạn sớm nhất!

            	Trân trọng,
            	WebDemo Team
            	""",
            	event.getCustomerName(),
            	event.getOrderCode(),
            	event.getTotalAmount()
        );

        log.info("Sending order confirmation email to: {}", event.getCustomerEmail());
        log.info("Email content:\n{}", content);
        mailService.sendEmail(event.getCustomerEmail(), subject, content, false, false);
    }

    private void sendWelcomeEmail(UserRegistrationEventDTO event) {
        String subject = "Tạo tài khoản WebDemo thành công";
        String fullName =
            (event.getFirstName() != null ? event.getFirstName() : "") + " " + (event.getLastName() != null ? event.getLastName() : "");

        String content = String.format(
            """
            Xin chào %s,

            Cảm ơn bạn đã đăng ký tài khoản tại WebDemo!

            Thông tin tài khoản:
            - Tên đăng nhập: %s
            - Email: %s

            Tài khoản của bạn đã được kích hoạt tự động. Bạn có thể đăng nhập tại: http://localhost:8080/login

            Trân trọng,
            WebDemo Team
            """,
            fullName.trim().isEmpty() ? event.getLogin() : fullName,
            event.getLogin(),
            event.getEmail()
        );

        log.info("Sending welcome email to: {}", event.getEmail());
        log.info("Email content:\n{}", content);
        mailService.sendEmail(event.getEmail(), subject, content, false, false);
    }
}
