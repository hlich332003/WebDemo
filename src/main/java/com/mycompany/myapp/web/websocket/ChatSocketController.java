package com.mycompany.myapp.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.SupportTicketService;
import com.mycompany.myapp.service.dto.SupportMessageDTO;
import com.mycompany.myapp.service.dto.SupportTicketDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatSocketController.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final SupportTicketService supportTicketService;
    private final ObjectMapper objectMapper;

    public ChatSocketController(
        SimpMessageSendingOperations messagingTemplate,
        SupportTicketService supportTicketService,
        ObjectMapper objectMapper
    ) {
        this.messagingTemplate = messagingTemplate;
        this.supportTicketService = supportTicketService;
        this.objectMapper = objectMapper;
    }

    @MessageMapping("/topic/chat/admin")
    public void sendMessageToAdmin(@Payload String message, Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated user attempted to send message");
            return;
        }

        String userEmail = principal.getName();
        log.debug("User {} sending message to admin: {}", userEmail, message);

        try {
            // Validate message
            if (message == null || message.trim().isEmpty()) {
                log.warn("Empty message from user: {}", userEmail);
                return;
            }

            // Lấy hoặc tạo ticket cho user
            SupportTicketDTO ticket = supportTicketService.getOrCreateActiveTicket(userEmail);

            // Lưu tin nhắn vào database
            SupportMessageDTO savedMessage = supportTicketService.sendMessage(
                ticket.getId(),
                userEmail,
                message.trim(),
                false // không phải từ admin
            );

            // Gửi tin nhắn đến admin với đầy đủ thông tin
            Map<String, Object> messageData = Map.of(
                "id", savedMessage.getId(),
                "user", userEmail,
                "message", message.trim(),
                "ticketId", ticket.getId(),
                "timestamp", savedMessage.getCreatedAt().toString(),
                "isFromAdmin", false
            );

            String jsonMessage = objectMapper.writeValueAsString(messageData);
            messagingTemplate.convertAndSend("/topic/admin/chat", jsonMessage);

            // Gửi xác nhận cho user
            Map<String, Object> confirmation = Map.of(
                "id", savedMessage.getId(),
                "status", "sent",
                "timestamp", savedMessage.getCreatedAt().toString()
            );
            String confirmationJson = objectMapper.writeValueAsString(confirmation);
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat/confirmation", confirmationJson);

            log.info("Message saved to ticket #{} and sent to admins", ticket.getId());
        } catch (Exception e) {
            log.error("Error processing message from user {}", userEmail, e);

            // Send error to user
            try {
                Map<String, Object> error = Map.of(
                    "status", "error",
                    "message", "Không thể gửi tin nhắn. Vui lòng thử lại."
                );
                String errorJson = objectMapper.writeValueAsString(error);
                messagingTemplate.convertAndSendToUser(userEmail, "/queue/errors", errorJson);
            } catch (Exception ex) {
                log.error("Failed to send error notification", ex);
            }
        }
    }

    @MessageMapping("/chat/{userId}")
    public void sendMessageToUser(@Payload String message, @DestinationVariable String userId, Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated admin attempted to send message");
            return;
        }

        String adminEmail = principal.getName();
        log.debug("Admin {} sending message to user {}: {}", adminEmail, userId, message);

        try {
            // Validate input
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("Invalid userId from admin: {}", adminEmail);
                return;
            }

            // Parse message JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            String messageText = (String) messageData.get("message");

            if (messageText == null || messageText.trim().isEmpty()) {
                log.warn("Empty message from admin {} to user {}", adminEmail, userId);
                return;
            }

            // Lấy ticket của user
            SupportTicketDTO ticket = supportTicketService.getOrCreateActiveTicket(userId);

            // Lưu tin nhắn vào database
            SupportMessageDTO savedMessage = supportTicketService.sendMessage(
                ticket.getId(),
                adminEmail,
                messageText.trim(),
                true // từ admin
            );

            // Gửi tin nhắn đến user
            Map<String, Object> responseData = Map.of(
                "id", savedMessage.getId(),
                "message", messageText.trim(),
                "timestamp", savedMessage.getCreatedAt().toString(),
                "isFromAdmin", true,
                "senderEmail", adminEmail,
                "ticketId", ticket.getId()
            );

            String jsonResponse = objectMapper.writeValueAsString(responseData);
            messagingTemplate.convertAndSendToUser(userId, "/queue/chat", jsonResponse);

            // Gửi xác nhận cho admin
            Map<String, Object> confirmation = Map.of(
                "id", savedMessage.getId(),
                "status", "delivered",
                "userId", userId,
                "timestamp", savedMessage.getCreatedAt().toString()
            );
            String confirmationJson = objectMapper.writeValueAsString(confirmation);
            messagingTemplate.convertAndSendToUser(adminEmail, "/queue/chat/confirmation", confirmationJson);

            log.info("Message saved to ticket #{} and sent to user {}", ticket.getId(), userId);
        } catch (Exception e) {
            log.error("Error processing message from admin {} to user {}", adminEmail, userId, e);

            // Send error to admin
            try {
                Map<String, Object> error = Map.of(
                    "status", "error",
                    "userId", userId != null ? userId : "unknown",
                    "message", "Không thể gửi tin nhắn. Vui lòng thử lại."
                );
                String errorJson = objectMapper.writeValueAsString(error);
                messagingTemplate.convertAndSendToUser(adminEmail, "/queue/errors", errorJson);
            } catch (Exception ex) {
                log.error("Failed to send error notification to admin", ex);
            }
        }
    }
}
