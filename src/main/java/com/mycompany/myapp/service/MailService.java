package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Service for sending emails asynchronously.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";
    private static final String ORDER = "order";
    private static final String BASE_URL = "baseUrl";

    private final JHipsterProperties jHipsterProperties;
    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;
    private final SpringTemplateEngine templateEngine;

    public MailService(
        JHipsterProperties jHipsterProperties,
        JavaMailSender javaMailSender,
        MessageSource messageSource,
        SpringTemplateEngine templateEngine
    ) {
        this.jHipsterProperties = jHipsterProperties;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        LOG.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            isMultipart,
            isHtml,
            to,
            subject,
            content
        );

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(jHipsterProperties.getMail().getFrom());
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            LOG.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            LOG.debug("❌ [MAIL_SERVICE] Email doesn't exist for user '{}'", user.getEmail());
            return;
        }
        LOG.debug(
            "📧 [MAIL_SERVICE] Preparing email from template '{}' with titleKey '{}' for user '{}'",
            templateName,
            titleKey,
            user.getEmail()
        );

        Locale locale = Locale.forLanguageTag(user.getLangKey());
        LOG.debug("📧 [MAIL_SERVICE] Using locale: {}", locale);

        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());

        LOG.debug("📧 [MAIL_SERVICE] Processing template '{}'", templateName);
        String content = templateEngine.process(templateName, context);
        LOG.debug("📧 [MAIL_SERVICE] Template processed, content length: {} chars", content.length());

        String subject = messageSource.getMessage(titleKey, null, locale);
        LOG.debug("📧 [MAIL_SERVICE] Email subject: '{}'", subject);

        sendEmailSync(user.getEmail(), subject, content, false, true);
        LOG.info("✅ [MAIL_SERVICE] Email sent from template '{}' to '{}'", templateName, user.getEmail());
    }

    @Async
    public void sendOrderStatusUpdateEmail(Order order, String titleKey, String templateName) {
        if (order.getCustomerEmail() == null) {
            LOG.debug("Email doesn't exist for order '{}'", order.getId());
            return;
        }
        Locale locale = Locale.forLanguageTag("vi"); // Default to Vietnamese
        Context context = new Context(locale);
        context.setVariable(ORDER, order);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process("mail/" + templateName, context);
        String subject = messageSource.getMessage(titleKey, new Object[] {}, locale);
        subject = subject.replace("{0}", order.getOrderCode());
        sendEmailSync(order.getCustomerEmail(), subject, content, false, true);
    }

    @Async
    public void sendActivationEmail(User user) {
        LOG.debug("Sending activation email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        LOG.debug("📧 [MAIL_SERVICE] Sending creation/welcome email to '{}'", user.getEmail());
        LOG.debug(
            "📧 [MAIL_SERVICE] User details - FirstName: {}, LastName: {}, LangKey: {}",
            user.getFirstName(),
            user.getLastName(),
            user.getLangKey()
        );

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            LOG.error("❌ [MAIL_SERVICE] Cannot send email - user email is null or empty");
            return;
        }

        try {
            sendEmailFromTemplate(user, "mail/creationEmail", "email.creation.title");
            LOG.info("✅ [MAIL_SERVICE] Creation/welcome email sent successfully to '{}'", user.getEmail());
        } catch (Exception e) {
            LOG.error("❌ [MAIL_SERVICE] Failed to send creation email to '{}'", user.getEmail(), e);
            throw e;
        }
    }

    @Async
    public void sendPasswordResetMail(User user) {
        LOG.debug("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/passwordResetEmail", "email.reset.title");
    }
}
