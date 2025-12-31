package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.enumeration.TicketType;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.ChatService;
import com.mycompany.myapp.service.SupportTicketService;
import com.mycompany.myapp.service.dto.ChatInitiationDTO;
import com.mycompany.myapp.service.dto.SupportMessageDTO;
import com.mycompany.myapp.service.dto.SupportTicketDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SupportTicketResource {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketResource.class);

    private final SupportTicketService supportTicketService;
    private final ChatService chatService;

    public SupportTicketResource(SupportTicketService supportTicketService, ChatService chatService) {
        this.supportTicketService = supportTicketService;
        this.chatService = chatService;
    }

    // === CHAT FLOW ===
    @PostMapping("/chat/conversations")
    public ResponseEntity<ChatInitiationDTO> createChatConversation(@RequestBody(required = false) Map<String, String> payload) {
        log.debug("REST request to create a new CHAT conversation");

        String userIdentifier = SecurityUtils.getCurrentUserLogin().orElse(null);

        // If not logged in, try to get contact info from payload
        if (userIdentifier == null && payload != null) {
            userIdentifier = payload.get("contact");
        }

        // Fallback to sessionId if contact is missing (though frontend should enforce contact)
        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            if (payload != null && payload.containsKey("sessionId")) {
                userIdentifier = payload.get("sessionId");
            }
        }

        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            return ResponseEntity.badRequest().header("X-error", "User identifier (email/phone) is missing").build();
        }

        ChatInitiationDTO chatInit = chatService.createChatConversation(userIdentifier);
        return ResponseEntity.ok(chatInit);
    }

    // === TICKET FLOW ===
    @PostMapping("/support/tickets")
    public ResponseEntity<SupportTicketDTO> createSupportTicket(@RequestBody Map<String, String> payload) {
        log.debug("REST request to create a new TICKET");

        String title = payload.get("title");
        String description = payload.get("description");

        if (title == null || title.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().header("X-error", "Title and description are required for tickets").build();
        }

        String userIdentifier = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (userIdentifier == null) {
            userIdentifier = payload.get("email");
            if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
                return ResponseEntity.badRequest().header("X-error", "Guest email is missing for ticket creation").build();
            }
        }

        SupportTicketDTO ticket = supportTicketService.createTicket(userIdentifier, title, description, TicketType.TICKET);
        return ResponseEntity.ok(ticket);
    }

    // === COMMON & ADMIN ENDPOINTS ===

    @GetMapping("/support/tickets/current")
    public ResponseEntity<SupportTicketDTO> getCurrentTicket() {
        log.debug("REST request to get current user's active ticket");
        Optional<String> userEmailOpt = SecurityUtils.getCurrentUserLogin();
        if (userEmailOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        SupportTicketDTO ticket = supportTicketService.getCurrentActiveTicket(userEmailOpt.get());
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ticket);
    }

    // FIX: Add endpoint to get ticket by ID (needed for chat widget reload)
    @GetMapping("/support/tickets/{id}")
    public ResponseEntity<SupportTicketDTO> getTicketById(@PathVariable Long id) {
        log.debug("REST request to get ticket #{}", id);

        Optional<SupportTicketDTO> ticketOpt = supportTicketService.findById(id);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        SupportTicketDTO ticket = ticketOpt.get();

        // Security check
        if (SecurityUtils.isAuthenticated()) {
            String currentUser = SecurityUtils.getCurrentUserLogin().orElse("");
            boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN, AuthoritiesConstants.CSKH);

            if (!isAdmin && !ticket.getUserEmail().equalsIgnoreCase(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        // For guests, we currently allow access if they know the ID (for widget reload)
        // In production, you might want to validate against a session cookie or token

        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/support/tickets")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.CSKH + "')")
    public ResponseEntity<List<SupportTicketDTO>> getAllActiveTickets() {
        log.debug("REST request to get all active support tickets");
        List<SupportTicketDTO> tickets = supportTicketService.getAllActiveTickets();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/support/tickets/my")
    public ResponseEntity<List<SupportTicketDTO>> getMyTickets() {
        log.debug("REST request to get current user's support tickets");
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));

        List<SupportTicketDTO> tickets = supportTicketService.getUserTickets(userEmail);
        return ResponseEntity.ok(tickets);
    }

    /**
     * ✅ REST API to load chat history (STANDARD FLOW - không dùng WebSocket)
     * Supports pagination with beforeId parameter for "scroll up to load more"
     *
     * @param ticketId Conversation ID
     * @param beforeId Load messages before this message ID (for pagination)
     * @param limit Maximum number of messages to return (default 30)
     * @return List of messages in ascending order (oldest first)
     */
    @GetMapping("/support/tickets/{ticketId}/messages")
    public ResponseEntity<List<SupportMessageDTO>> getTicketMessages(
        @PathVariable Long ticketId,
        @RequestParam(required = false) Long beforeId,
        @RequestParam(defaultValue = "30") int limit
    ) {
        log.info("🔍 [REST API] Loading messages for ticket #{} | beforeId: {} | limit: {}", ticketId, beforeId, limit);

        // Security check
        Optional<SupportTicketDTO> ticketOpt = supportTicketService.findById(ticketId);
        if (ticketOpt.isPresent()) {
            SupportTicketDTO ticket = ticketOpt.get();
            if (SecurityUtils.isAuthenticated()) {
                String currentUser = SecurityUtils.getCurrentUserLogin().orElse("");
                boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN, AuthoritiesConstants.CSKH);

                if (!isAdmin && !ticket.getUserEmail().equalsIgnoreCase(currentUser)) {
                    log.warn(
                        "❌ [REST API] Forbidden: User {} tried to access ticket #{} owned by {}",
                        currentUser,
                        ticketId,
                        ticket.getUserEmail()
                    );
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
        } else {
            log.warn("❌ [REST API] Ticket #{} not found", ticketId);
            return ResponseEntity.notFound().build();
        }

        List<SupportMessageDTO> messages = supportTicketService.getTicketMessages(ticketId, beforeId, limit);
        log.info("✅ [REST API] Returning {} messages for ticket #{}", messages.size(), ticketId);

        return ResponseEntity.ok(messages);
    }

    /**
     * DEPRECATED: Use WebSocket (/app/chat.send) for real-time messaging instead.
     * This endpoint is kept for backward compatibility only.
     */
    @PostMapping("/support/tickets/{ticketId}/messages")
    @Deprecated
    public ResponseEntity<SupportMessageDTO> sendMessage(@PathVariable Long ticketId, @RequestBody Map<String, String> payload) {
        log.warn("⚠️ DEPRECATED: HTTP POST for sending messages is deprecated. Use WebSocket /app/chat.send instead.");
        log.debug("REST request to send message to ticket #{}", ticketId);

        String userIdentifier = SecurityUtils.getCurrentUserLogin().orElse("GUEST");

        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean isFromAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN, AuthoritiesConstants.CSKH);

        // Use ChatService to ensure WebSocket notifications are sent
        SupportMessageDTO message = chatService.sendChatMessage(ticketId, userIdentifier, content, isFromAdmin);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/support/tickets/{ticketId}/messages/mark-as-read")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.CSKH + "')")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable Long ticketId) {
        log.debug("REST request to mark messages as read for ticket #{}", ticketId);
        supportTicketService.markMessagesAsRead(ticketId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/support/tickets/{ticketId}/close")
    public ResponseEntity<Void> closeTicket(@PathVariable Long ticketId) {
        log.debug("REST request to close ticket #{}", ticketId);
        // Use ChatService to close to ensure WebSocket notification
        chatService.closeChatConversation(ticketId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/support/tickets/{ticketId}/assign")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.CSKH + "')")
    public ResponseEntity<Void> assignTicket(@PathVariable Long ticketId, @RequestBody Map<String, String> payload) {
        log.debug("REST request to assign ticket #{}", ticketId);
        String adminEmail = payload.get("adminEmail");
        supportTicketService.assignTicket(ticketId, adminEmail);
        return ResponseEntity.ok().build();
    }
}
