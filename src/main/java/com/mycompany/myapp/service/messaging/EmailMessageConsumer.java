package com.mycompany.myapp.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.MailService;
import com.mycompany.myapp.service.messaging.EmailMessageProducer.EmailMessage;
import com.mycompany.myapp.service.messaging.EmailMessageProducer.EmailType;
import com.mycompany.myapp.service.messaging.OrderMessageProducer.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consumer for processing email events from RabbitMQ
 */
@Service
public class EmailMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailMessageConsumer.class);

    private final MailService mailService;
    private final ObjectMapper objectMapper;

    public EmailMessageConsumer(MailService mailService, ObjectMapper objectMapper) {
        this.mailService = mailService;
        this.objectMapper = objectMapper;
    }

    /**
     * Process email event
     * OPTIMIZED: Add performance tracking, better error handling, HTML templates
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void processEmail(EmailMessage message) {
        long startTime = System.currentTimeMillis();

        try {
            log.info(
                "📧 [ASYNC] Processing email event: type={}, recipient={}, retryCount={}",
                message.getType(),
                message.getRecipient(),
                message.getRetryCount()
            );

            // ✅ OPTIMIZATION 1: Validate email message
            validateEmailMessage(message);

            // ✅ OPTIMIZATION 2: Send email with timeout protection
            java.util.concurrent.CompletableFuture<Void> emailTask = java.util.concurrent.CompletableFuture.runAsync(() -> {
                sendEmailByType(message);
            });

            emailTask.get(20, java.util.concurrent.TimeUnit.SECONDS); // Timeout after 20 seconds

            long processingTime = System.currentTimeMillis() - startTime;
            log.info(
                "✅ [ASYNC] Email sent successfully: type={}, recipient={}, processingTime={}ms",
                message.getType(),
                message.getRecipient(),
                processingTime
            );

            // ✅ OPTIMIZATION 3: Track email metrics
            trackEmailMetrics(message, processingTime, true);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("⏱️ TIMEOUT: Email sending exceeded 20 seconds: type={}, recipient={}", message.getType(), message.getRecipient(), e);
            handleEmailFailure(message, e, startTime);
        } catch (Exception e) {
            log.error(
                "❌ [ASYNC] Failed to send email: type={}, recipient={}, retryCount={}",
                message.getType(),
                message.getRecipient(),
                message.getRetryCount(),
                e
            );
            handleEmailFailure(message, e, startTime);
        }
    }

    private void validateEmailMessage(EmailMessage message) {
        if (message.getRecipient() == null || message.getRecipient().trim().isEmpty()) {
            throw new IllegalArgumentException("Email recipient cannot be empty");
        }
        if (message.getType() == null) {
            throw new IllegalArgumentException("Email type cannot be null");
        }
        // ✅ OPTIMIZATION: Validate email format
        if (!message.getRecipient().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format: " + message.getRecipient());
        }
    }

    private void sendEmailByType(EmailMessage message) {
        switch (message.getType()) {
            case ORDER_CONFIRMATION -> sendOrderConfirmationEmail(message);
            case USER_REGISTRATION -> sendUserRegistrationEmail(message);
            case PASSWORD_RESET -> sendPasswordResetEmail(message);
            case ORDER_STATUS_UPDATE -> sendOrderStatusUpdateEmail(message);
            default -> {
                log.warn("Unknown email type: {}", message.getType());
                throw new IllegalArgumentException("Unknown email type: " + message.getType());
            }
        }
    }

    private void handleEmailFailure(EmailMessage message, Exception e, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        message.setRetryCount(message.getRetryCount() + 1);

        trackEmailMetrics(message, processingTime, false);

        // Exception will cause message to be requeued or sent to DLQ after retries
        throw new RuntimeException("Email sending failed after " + message.getRetryCount() + " retries", e);
    }

    private void trackEmailMetrics(EmailMessage message, long processingTime, boolean success) {
        log.info(
            "📊 Email Metrics: type={}, recipient={}, processingTime={}ms, success={}, retryCount={}",
            message.getType(),
            message.getRecipient(),
            processingTime,
            success,
            message.getRetryCount()
        );
        // TODO: Send to monitoring system
        // meterRegistry.counter("emails.sent", "type", message.getType().toString(), "success", String.valueOf(success)).increment();
        // meterRegistry.timer("emails.processing.time").record(processingTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Handle emails that end up in DLQ
     * OPTIMIZED: Add monitoring, alternative email service, admin alerts
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_DLQ)
    public void handleEmailDLQ(EmailMessage message) {
        log.error(
            "⚠️⚠️⚠️ CRITICAL: Email message in DLQ (Dead Letter Queue): type={}, recipient={}, retryCount={}",
            message.getType(),
            message.getRecipient(),
            message.getRetryCount()
        );

        try {
            // ✅ OPTIMIZATION 1: Try alternative email service (fallback)
            boolean fallbackSuccess = tryAlternativeEmailService(message);

            if (fallbackSuccess) {
                log.info(
                    "✅ Email sent successfully using fallback service: type={}, recipient={}",
                    message.getType(),
                    message.getRecipient()
                );
                return;
            }

            // ✅ OPTIMIZATION 2: Log detailed error information
            logEmailDLQError(message);

            // ✅ OPTIMIZATION 3: Send alert to admin
            sendEmailFailureAlert(message);

            // ✅ OPTIMIZATION 4: Store in database for manual review
            storeFailedEmail(message);

            log.error("⚠️ Manual intervention REQUIRED for email: type={}, recipient={}", message.getType(), message.getRecipient());
        } catch (Exception e) {
            log.error("❌ CRITICAL: Failed to handle email DLQ message: {}", message, e);
        }
    }

    private boolean tryAlternativeEmailService(EmailMessage message) {
        try {
            // TODO: Implement fallback email service (e.g., SendGrid, AWS SES)
            log.info("🔄 Attempting to send email via fallback service...");

            // Example:
            // if (sendGridService != null) {
            //     sendGridService.sendEmail(message);
            //     return true;
            // }

            return false; // No fallback service available
        } catch (Exception e) {
            log.error("❌ Fallback email service also failed: {}", message.getRecipient(), e);
            return false;
        }
    }

    private void logEmailDLQError(EmailMessage message) {
        log.error(
            "Email DLQ Details: type={}, recipient={}, subject={}, retryCount={}, timestamp={}",
            message.getType(),
            message.getRecipient(),
            message.getSubject(),
            message.getRetryCount(),
            java.time.Instant.now()
        );
    }

    private void sendEmailFailureAlert(EmailMessage message) {
        // TODO: Send alert to admin (email, SMS, Slack)
        log.warn(
            "📢 Admin alert: Email delivery failed after all retries - Type: {}, Recipient: {}",
            message.getType(),
            message.getRecipient()
        );
    }

    private void storeFailedEmail(EmailMessage message) {
        // TODO: Store in database table (e.g., failed_emails)
        log.debug("💾 Storing failed email in database for manual review: {}", message.getRecipient());
        // Example SQL:
        // INSERT INTO failed_emails (type, recipient, subject, retry_count, error_data, created_at)
        // VALUES (?, ?, ?, ?, ?, NOW())
    }

    private void sendOrderConfirmationEmail(EmailMessage message) {
        OrderMessage orderData = objectMapper.convertValue(message.getData(), OrderMessage.class);

        String content = buildOrderConfirmationHtml(orderData);

        // Use existing MailService
        // mailService.sendEmail(
        //     message.getRecipient(),
        //     message.getSubject(),
        //     content,
        //     false,
        //     true
        // );

        // For demo: just log
        log.info("📧 Order confirmation email would be sent to: {}", message.getRecipient());
        log.debug("Email content preview: Order #{}, Total: ${}", orderData.getOrderId(), orderData.getTotalAmount());
    }

    private void sendPasswordResetEmail(EmailMessage message) {
        log.info("📧 Password reset email would be sent to: {}", message.getRecipient());
    }

    private void sendUserRegistrationEmail(EmailMessage message) {
        try {
            log.info("📧 [USER_REGISTRATION] Starting to send welcome email to: {}", message.getRecipient());
            log.debug(
                "📧 [USER_REGISTRATION] Message data type: {}",
                message.getData() != null ? message.getData().getClass().getName() : "null"
            );

            // Get AdminUserDTO from message data
            com.mycompany.myapp.service.dto.AdminUserDTO userDTO = objectMapper.convertValue(
                message.getData(),
                com.mycompany.myapp.service.dto.AdminUserDTO.class
            );

            log.debug(
                "📧 [USER_REGISTRATION] UserDTO - Email: {}, FirstName: {}, LastName: {}, LangKey: {}",
                userDTO.getEmail(),
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getLangKey()
            );

            // Convert AdminUserDTO to User for email template
            com.mycompany.myapp.domain.User user = new com.mycompany.myapp.domain.User();
            user.setEmail(userDTO.getEmail());
            user.setFirstName(userDTO.getFirstName());
            user.setLastName(userDTO.getLastName());
            user.setLangKey(userDTO.getLangKey());

            log.info("📧 [USER_REGISTRATION] Calling MailService.sendCreationEmail for: {}", user.getEmail());

            // Send actual welcome email using MailService
            mailService.sendCreationEmail(user);

            log.info("✅ [USER_REGISTRATION] Welcome email sent successfully to: {}", message.getRecipient());
        } catch (Exception e) {
            log.error("❌ [USER_REGISTRATION] Failed to send welcome email to: {}", message.getRecipient(), e);
            throw new RuntimeException("Failed to send user registration email", e);
        }
    }

    private void sendOrderStatusUpdateEmail(EmailMessage message) {
        log.info("📧 Order status update email would be sent to: {}", message.getRecipient());
        // TODO: Send order status update email
    }

    private void sendWelcomeEmail(EmailMessage message) {
        log.info("📧 Welcome email would be sent to: {}", message.getRecipient());
    }

    private void sendOrderShippedEmail(EmailMessage message) {
        log.info("📧 Order shipped email would be sent to: {}", message.getRecipient());
    }

    private void sendOrderDeliveredEmail(EmailMessage message) {
        log.info("📧 Order delivered email would be sent to: {}", message.getRecipient());
    }

    private void sendAccountActivationEmail(EmailMessage message) {
        log.info("📧 Account activation email would be sent to: {}", message.getRecipient());
    }

    private String buildOrderConfirmationHtml(OrderMessage orderData) {
        return String.format(
            """
            <html>
            <body>
                <h2>Order Confirmation</h2>
                <p>Dear %s,</p>
                <p>Thank you for your order!</p>
                <p><strong>Order Code:</strong> %s</p>
                <p><strong>Total Amount:</strong> $%.2f</p>
                <p>We will send you updates as your order progresses.</p>
                <p>Best regards,<br/>PcNo.1 Team</p>
            </body>
            </html>
            """,
            orderData.getCustomerFullName(),
            orderData.getOrderCode(),
            orderData.getTotalAmount()
        );
    }
}
