package com.mycompany.myapp.web.websocket;

import com.mycompany.myapp.web.websocket.dto.ChatMessageDTO;
import java.security.Principal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller("websocketChatController") // Đặt tên bean rõ ràng để tránh xung đột
public class ChatController {

    private final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final SimpMessageSendingOperations messagingTemplate;

    public ChatController(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessageDTO chatMessage, Principal principal) {
        if (principal == null) {
            log.warn("Anonymous user attempted to send message");
            return;
        }

        String userEmail = principal.getName();
        log.debug("Received message from {}: {}", userEmail, chatMessage.getContent());

        // 1. Ở đây bạn nên gọi Service để lưu tin nhắn vào DB
        // chatService.saveMessage(userEmail, chatMessage.getContent());

        // 2. Phản hồi lại cho chính User đó (để UI cập nhật, hoặc xác nhận đã gửi)
        // Lưu ý: UI hiện tại đang tự push vào mảng messages nên có thể không cần bước này ngay
        // Nhưng đúng chuẩn là Server confirm rồi UI mới hiện.

        ChatMessageDTO response = new ChatMessageDTO();
        response.setContent(chatMessage.getContent());
        response.setSenderType("USER");
        response.setTimestamp(Instant.now());

        // Gửi lại vào queue riêng của user
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat", response);
        // 3. Gửi cho Admin/CSKH (nếu cần)
        // messagingTemplate.convertAndSend("/topic/cskh/chat", response);
    }

    @MessageMapping("/chat.reply")
    public void reply(@Payload ChatMessageDTO chatMessage, Principal principal) {
        // Logic cho Admin reply user
        // Cần có conversationId để biết reply cho ai
        log.debug("Admin {} reply: {}", principal.getName(), chatMessage.getContent());
    }
}
