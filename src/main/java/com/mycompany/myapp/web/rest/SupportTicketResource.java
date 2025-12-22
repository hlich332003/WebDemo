package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.SupportTicketService;
import com.mycompany.myapp.service.dto.SupportMessageDTO;
import com.mycompany.myapp.service.dto.SupportTicketDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportTicketResource {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketResource.class);

    private final SupportTicketService supportTicketService;

    public SupportTicketResource(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping("/tickets/current")
    public ResponseEntity<SupportTicketDTO> getCurrentTicket() {
        log.debug("REST request to get current user's active ticket");
        String userEmail = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("Người dùng chưa đăng nhập"));

        SupportTicketDTO ticket = supportTicketService.getCurrentActiveTicket(userEmail);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicketDTO> createTicket(@RequestBody Map<String, String> payload) {
        log.debug("REST request to create support ticket");
        String userEmail = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("Người dùng chưa đăng nhập"));

        String title = payload.get("title");
        String description = payload.get("description");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SupportTicketDTO ticket = supportTicketService.createTicket(userEmail, title, description);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.CSKH + "')")
    public ResponseEntity<List<SupportTicketDTO>> getAllActiveTickets() {
        log.debug("REST request to get all active support tickets");
        List<SupportTicketDTO> tickets = supportTicketService.getAllActiveTickets();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/tickets/my")
    public ResponseEntity<List<SupportTicketDTO>> getMyTickets() {
        log.debug("REST request to get current user's support tickets");
        String userEmail = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        List<SupportTicketDTO> tickets = supportTicketService.getUserTickets(userEmail);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<List<SupportMessageDTO>> getTicketMessages(@PathVariable Long ticketId) {
        log.debug("REST request to get messages for ticket #{}", ticketId);
        List<SupportMessageDTO> messages = supportTicketService.getTicketMessages(ticketId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<SupportMessageDTO> sendMessage(
        @PathVariable Long ticketId,
        @RequestBody Map<String, String> payload
    ) {
        log.debug("REST request to send message to ticket #{}", ticketId);
        String userEmail = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("Người dùng chưa đăng nhập"));

        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean isFromAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(
            AuthoritiesConstants.ADMIN,
            AuthoritiesConstants.CSKH
        );

        SupportMessageDTO message = supportTicketService.sendMessage(ticketId, userEmail, content, isFromAdmin);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/tickets/{ticketId}/messages/mark-as-read")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.CSKH + "')")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable Long ticketId) {
        log.debug("REST request to mark messages as read for ticket #{}", ticketId);
        supportTicketService.markMessagesAsRead(ticketId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tickets/{ticketId}/close")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.CSKH + "')")
    public ResponseEntity<Void> closeTicket(@PathVariable Long ticketId) {
        log.debug("REST request to close ticket #{}", ticketId);
        supportTicketService.closeTicket(ticketId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tickets/{ticketId}/assign")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.CSKH + "')")
    public ResponseEntity<Void> assignTicket(
        @PathVariable Long ticketId,
        @RequestBody Map<String, String> payload
    ) {
        log.debug("REST request to assign ticket #{}", ticketId);
        String adminEmail = payload.get("adminEmail");
        supportTicketService.assignTicket(ticketId, adminEmail);
        return ResponseEntity.ok().build();
    }
}
