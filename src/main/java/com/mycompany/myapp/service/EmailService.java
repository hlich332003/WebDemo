package com.mycompany.myapp.service;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.dto.UserRegistrationEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tech.jhipster.config.JHipsterProperties;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Service
public class EmailService {

    private final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final JHipsterProperties jHipsterProperties;
    private final MailProperties mailProperties;

    public EmailService(JavaMailSender mailSender, JHipsterProperties jHipsterProperties, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.jHipsterProperties = jHipsterProperties;
        this.mailProperties = mailProperties;
    }

    private String getFromAddress() {
        String from = jHipsterProperties.getMail().getFrom();
        if (from == null || from.isBlank()) {
            return "webDemo@localhost";
        }
        return from;
    }

    // Returns true when SMTP username or password is missing/blank. Named to avoid callers negating the result.
    private boolean isSmtpCredentialsMissing() {
        String user = mailProperties.getUsername();
        String pass = mailProperties.getPassword();
        return user == null || user.isBlank() || pass == null || pass.isBlank();
    }

    private void fallbackWriteEmail(SimpleMailMessage message) {
        // Append email to a temp log file so developer can inspect it when SMTP is not configured.
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path out = Paths.get(tmpDir, "webdemo-emails.log");
        StringBuilder sb = new StringBuilder();
        sb.append("--- EMAIL [").append(Instant.now()).append("] ---\n");

        // Null-safe values for logging
        String from = message.getFrom();
        if (from == null || from.isBlank()) {
            from = getFromAddress();
        }
        sb.append("From: ").append(from).append("\n");

        String[] tos = message.getTo();
        String toJoined;
        if (tos == null || tos.length == 0) {
            toJoined = "(no recipients)";
        } else {
            toJoined = String.join(",", tos);
        }
        sb.append("To: ").append(toJoined).append("\n");

        String subject = message.getSubject();
        if (subject == null) {
            subject = "(no subject)";
        }
        sb.append("Subject: ").append(subject).append("\n");

        String body = message.getText();
        if (body == null) {
            body = "(no body)";
        }
        sb.append("Body:\n").append(body).append("\n\n");

        try {
            Path parent = out.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (FileWriter fw = new FileWriter(out.toFile(), true)) {
                fw.write(sb.toString());
            }
            log.warn("SMTP not configured or authentication failed - wrote email to {}", out.toAbsolutePath());
        } catch (IOException ioe) {
            log.error("Failed to write fallback email to {}: {}", out.toAbsolutePath(), ioe.getMessage(), ioe);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_EMAIL_QUEUE)
    public void handleOrderCreatedEvent(OrderEventDTO event) {
        log.info("Received order created event for order: {} and email: {}", event.getOrderCode(), event.getCustomerEmail());
        log.info("Preparing confirmation email for order {} to {}", event.getOrderCode(), event.getCustomerEmail());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromAddress()); // Sử dụng email từ JHipster properties
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

        try {
            if (isSmtpCredentialsMissing()) {
                log.warn("No SMTP username/password configured (SPRING_MAIL_USERNAME/SPRING_MAIL_PASSWORD). If you want real emails in dev, run MailHog and set SPRING_MAIL_HOST/PORT accordingly or set the SMTP credentials.");
            }
            mailSender.send(message);
            log.info("Confirmation email sent for order {}", event.getOrderCode());
        } catch (MailAuthenticationException ae) {
            log.warn("SMTP authentication failed: {}. Falling back to writing email to a local file.", ae.getMessage());
            fallbackWriteEmail(message);
        } catch (Exception e) {
            log.error("Failed to send confirmation email for order {}: {}", event.getOrderCode(), e.getMessage(), e);
            // fallback to file for visibility in dev
            fallbackWriteEmail(message);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.USER_REGISTRATION_QUEUE)
    public void handleUserRegisteredEvent(UserRegistrationEventDTO event) {
        log.info("Received user registered event for login: {} email: {}", event.getLogin(), event.getEmail());
        log.info("Preparing welcome email to user {}", event.getLogin());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromAddress()); // Sử dụng email từ JHipster properties
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

        try {
            if (isSmtpCredentialsMissing()) {
                log.warn("No SMTP username/password configured (SPRING_MAIL_USERNAME/SPRING_MAIL_PASSWORD). If you want real emails in dev, run MailHog and set SPRING_MAIL_HOST/PORT accordingly or set the SMTP credentials.");
            }
            mailSender.send(message);
            log.info("Welcome email sent for user {}", event.getLogin());
        } catch (MailAuthenticationException ae) {
            log.warn("SMTP authentication failed: {}. Falling back to writing email to a local file.", ae.getMessage());
            fallbackWriteEmail(message);
        } catch (Exception e) {
            log.error("Failed to send welcome email for user {}: {}", event.getLogin(), e.getMessage(), e);
            // fallback to file for visibility in dev
            fallbackWriteEmail(message);
        }
    }
}
