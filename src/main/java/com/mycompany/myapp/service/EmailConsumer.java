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
        // mailService.sendEmail(event.getCustomerEmail(), subject, content, false, false);
    }

    private void sendWelcomeEmail(UserRegistrationEventDTO event) {
        String subject = "Xác thực tài khoản WebDemo";
        String fullName =
            (event.getFirstName() != null ? event.getFirstName() : "") + " " + (event.getLastName() != null ? event.getLastName() : "");

        // Lấy activation key từ event (cần thêm vào DTO)
        String activationLink = "http://localhost:8080/api/activate?key=" + event.getActivationKey();

        String content = String.format(
            """
            Xin chào %s,

            Cảm ơn bạn đã đăng ký tài khoản tại WebDemo!

            Thông tin tài khoản:
            - Tên đăng nhập: %s
            - Email: %s

            Để kích hoạt tài khoản và bắt đầu mua sắm, vui lòng click vào link bên dưới:

            %s

            Link này sẽ hết hạn sau 24 giờ.

            Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.

            Trân trọng,
            WebDemo Team
            """,
            fullName.trim().isEmpty() ? event.getLogin() : fullName,
            event.getLogin(),
            event.getEmail(),
            activationLink
        );

        log.info("Sending activation email to: {}", event.getEmail());
        log.info("Activation link: {}", activationLink);
        log.info("Email content:\n{}", content);
        // mailService.sendEmail(event.getEmail(), subject, content, false, false);
    }
}
