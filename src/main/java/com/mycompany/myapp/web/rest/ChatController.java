package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.SupportTicketService;
import com.mycompany.myapp.service.dto.SupportMessageDTO;
import com.mycompany.myapp.service.dto.SupportTicketDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller cho Chat (CSKH Dashboard).
 * Đã được refactor để sử dụng SupportTicketService thay vì ChatService cũ.
 * Giúp đồng bộ dữ liệu giữa User (Ticket) và Admin (Chat).
 */
@RestController("restChatController") // Đặt tên bean rõ ràng để tránh xung đột
@RequestMapping("/api/chat")
public class ChatController {

    private final SupportTicketService supportTicketService;

    public ChatController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    // REST endpoints for CSKH dashboard

    @GetMapping("/conversations")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CSKH')")
    public ResponseEntity<List<Map<String, Object>>> getActiveConversations() {
        // Map SupportTicketDTO sang format JSON mà Frontend Admin (CustomerSupportComponent) mong đợi
        List<Map<String, Object>> conversations = supportTicketService
            .getAllActiveTickets()
            .stream()
            .map(ticket -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ticket.getId());
                map.put("userIdentifier", ticket.getUserEmail());
                map.put("status", ticket.getStatus().toString());
                map.put("lastMessageAt", ticket.getLastModifiedDate() != null ? ticket.getLastModifiedDate() : ticket.getCreatedDate());
                // unreadCount là long (primitive) nên không cần check null
                map.put("unreadCount", ticket.getUnreadCount());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CSKH')")
    public ResponseEntity<List<Map<String, Object>>> getConversationMessages(@PathVariable Long id) {
        // Map SupportMessageDTO sang format JSON cũ
        List<Map<String, Object>> messages = supportTicketService
            .getTicketMessages(id)
            .stream()
            .map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("conversationId", msg.getTicketId());
                map.put("senderType", msg.getIsFromAdmin() ? "CSKH" : "USER");
                map.put("senderIdentifier", msg.getSenderEmail());
                map.put("content", msg.getMessage());
                map.put("createdAt", msg.getCreatedAt());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(messages);
    }

    @PostMapping("/conversations/{id}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CSKH')")
    public ResponseEntity<Void> closeConversationFromAdmin(@PathVariable Long id) {
        supportTicketService.closeTicket(id);
        return ResponseEntity.ok().build();
    }
}
