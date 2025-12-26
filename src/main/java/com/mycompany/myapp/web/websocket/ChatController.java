package com.mycompany.myapp.web.websocket;

import com.mycompany.myapp.service.ChatService;
import com.mycompany.myapp.web.websocket.dto.ChatMessageDTO;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller("websocketChatController") // Đặt tên bean rõ ràng để tránh xung đột
public class ChatController {

    private final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessageDTO chatMessage, Principal principal) {
        if (principal == null) {
            log.warn("❌ ChatController.send() - Principal is NULL! Message NOT sent");
            return;
        }

        String userIdentifier = principal.getName();
        log.info("✅ ChatController.send() - Received message from USER: {} | Content: {}", userIdentifier, chatMessage.getContent());

        // Delegate to service which will persist and broadcast to standardized topic
        try {
            chatService.handleUserMessage(chatMessage.getContent(), userIdentifier);
        } catch (Exception e) {
            log.error("[CHAT_SEND_FAILED] Error while handling user message: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.reply")
    public void reply(@Payload ChatMessageDTO chatMessage, Principal principal) {
        if (principal == null) {
            log.warn("❌ ChatController.reply() - Principal is NULL! Reply NOT processed");
            return;
        }
        // Admin reply must include conversationId
        if (chatMessage.getConversationId() == null) {
            log.warn("❌ ChatController.reply() - Missing conversationId");
            return;
        }

        String cskhIdentifier = principal.getName();
        log.debug("Admin {} reply to conversation {}: {}", cskhIdentifier, chatMessage.getConversationId(), chatMessage.getContent());

        try {
            chatService.handleCskhReply(chatMessage.getConversationId(), chatMessage.getContent(), cskhIdentifier);
        } catch (Exception e) {
            log.error("[CHAT_REPLY_FAILED] Error while handling cskh reply: {}", e.getMessage(), e);
        }
    }
}
