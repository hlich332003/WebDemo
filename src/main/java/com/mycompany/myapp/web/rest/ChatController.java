package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.enumeration.TicketType;
import com.mycompany.myapp.repository.analytics.SupportTicketRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.ChatService;
import com.mycompany.myapp.service.SupportTicketService;
import com.mycompany.myapp.service.dto.SupportTicketDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller cho Chat (CSKH Dashboard) & WebSocket Controller.
 */
@RestController("restChatController")
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final SupportTicketService supportTicketService;
    private final SupportTicketRepository ticketRepository;
    private final ChatService chatService;

    public ChatController(SupportTicketService supportTicketService, SupportTicketRepository ticketRepository, ChatService chatService) {
        this.supportTicketService = supportTicketService;
        this.ticketRepository = ticketRepository;
        this.chatService = chatService;
    }

    // === WEBSOCKET HANDLERS ===

    @MessageMapping("/chat.send")
    public void send(@Payload Map<String, Object> payload, Authentication authentication) {
        try {
            // Parse payload
            if (payload.get("conversationId") == null) {
                log.warn("❌ Message dropped: conversationId is null in payload");
                return;
            }

            Long conversationId = Long.valueOf(payload.get("conversationId").toString());
            String content = (String) payload.get("content");

            if (content == null || content.trim().isEmpty()) {
                log.warn("❌ Message dropped: content is empty");
                return;
            }

            // Get sender identifier
            String senderIdentifier;
            boolean isFromAdmin = false;

            if (authentication == null) {
                // Guest user - get identifier from the ticket's userEmail
                var ticket = ticketRepository.findById(conversationId);
                if (ticket.isPresent()) {
                    senderIdentifier = ticket.get().getUserEmail();
                    log.info("📨 [WEBSOCKET] Guest message received | Conversation: #{} | Sender: {}", conversationId, senderIdentifier);
                } else {
                    log.warn("❌ Message dropped: Ticket #{} not found", conversationId);
                    return;
                }
            } else {
                senderIdentifier = authentication.getName();
                isFromAdmin = authentication
                    .getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals(AuthoritiesConstants.ADMIN) || authority.equals(AuthoritiesConstants.CSKH));
                log.info(
                    "📨 [WEBSOCKET] Authenticated message | Conversation: #{} | Sender: {} (isAdmin: {})",
                    conversationId,
                    senderIdentifier,
                    isFromAdmin
                );
            }

            log.info("📝 Message content: {}", content.length() > 50 ? content.substring(0, 50) + "..." : content);

            chatService.sendChatMessage(conversationId, senderIdentifier, content, isFromAdmin);
        } catch (Exception e) {
            log.error("❌ Error handling WebSocket message", e);
            log.error("Payload: {}", payload);
        }
    }

    // === REST ENDPOINTS (Admin/CSKH) ===

    /**
     * Get current user's active CHAT conversation (OPEN only).
     * Returns 204 if no active conversation found.
     */
    @GetMapping("/conversations/my-active")
    public ResponseEntity<Map<String, Object>> getMyActiveConversation(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String userEmail = authentication.getName();
        log.info("🔍 Checking active conversation for user: {}", userEmail);

        // Find OPEN CHAT conversation for this user
        var activeConversation = ticketRepository.findActiveConversationByUserEmail(userEmail);

        if (activeConversation.isEmpty()) {
            log.info("❌ No active conversation found for user: {}", userEmail);
            return ResponseEntity.noContent().build();
        }

        var ticket = activeConversation.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", ticket.getId());
        response.put("status", ticket.getStatus().toString());
        response.put("userEmail", ticket.getUserEmail());
        response.put("createdDate", ticket.getCreatedDate());

        log.info("✅ Found active conversation #{} for user: {}", ticket.getId(), userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CSKH')")
    public ResponseEntity<List<Map<String, Object>>> getActiveConversations(@RequestParam(required = false) TicketType type) {
        List<SupportTicketDTO> tickets;
        if (type != null) {
            tickets = supportTicketService.getAllActiveTicketsByType(type);
        } else {
            tickets = supportTicketService.getAllActiveTickets();
        }

        List<Map<String, Object>> conversations = tickets
            .stream()
            .map(ticket -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ticket.getId());
                map.put("userIdentifier", ticket.getUserEmail());
                map.put("status", ticket.getStatus().toString());
                map.put("lastMessageAt", ticket.getLastModifiedDate() != null ? ticket.getLastModifiedDate() : ticket.getCreatedDate());
                map.put("unreadCount", ticket.getUnreadCount());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CSKH')")
    public ResponseEntity<List<Map<String, Object>>> getConversationMessages(@PathVariable Long id) {
        List<Map<String, Object>> messages = supportTicketService
            .getTicketMessages(id)
            .stream()
            .map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("conversationId", msg.getTicketId());
                map.put("senderType", msg.getIsFromAdmin() ? "CSKH" : "USER");
                map.put("isFromAdmin", msg.getIsFromAdmin()); // Pass this field
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
        chatService.closeChatConversation(id);
        return ResponseEntity.ok().build();
    }
}
