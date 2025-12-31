package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.analytics.SupportMessage;
import com.mycompany.myapp.domain.analytics.SupportTicket;
import com.mycompany.myapp.domain.enumeration.TicketStatus;
import com.mycompany.myapp.domain.enumeration.TicketType;
import com.mycompany.myapp.repository.analytics.SupportMessageRepository;
import com.mycompany.myapp.repository.analytics.SupportTicketRepository;
import com.mycompany.myapp.service.dto.SupportMessageDTO;
import com.mycompany.myapp.service.dto.SupportTicketDTO;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional("analyticsTransactionManager")
public class SupportTicketService {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketService.class);

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final NotificationService notificationService;

    public SupportTicketService(
        SupportTicketRepository ticketRepository,
        SupportMessageRepository messageRepository,
        NotificationService notificationService
    ) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Optional<SupportTicketDTO> findById(Long id) {
        return ticketRepository.findById(id).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "activeTickets", key = "#userEmail", unless = "#result == null")
    public SupportTicketDTO getCurrentActiveTicket(String userEmail) {
        log.debug("Getting current active ticket for user: {}", userEmail);
        Optional<SupportTicket> existingTicket = ticketRepository.findFirstByUserEmailAndStatusInOrderByCreatedDateDesc(
            userEmail,
            Arrays.asList(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_CUSTOMER)
        );
        return existingTicket.map(this::convertToDTO).orElse(null);
    }

    @CacheEvict(value = "activeTickets", key = "#userEmail")
    public SupportTicketDTO createTicket(String userEmail, String title, String description, TicketType type) {
        log.debug("Creating new TICKET for user: {}", userEmail);

        SupportTicket newTicket = new SupportTicket();
        newTicket.setUserEmail(userEmail);
        newTicket.setTitle(title);
        newTicket.setStatus(TicketStatus.OPEN);
        newTicket.setType(type);

        SupportTicket savedTicket = ticketRepository.save(newTicket);
        log.info("✅ Created new support ticket #{} for user: {}", savedTicket.getId(), userEmail);

        // Gửi thông báo cho admin về ticket mới
        notificationService.notifyAdminNewSupportTicket(savedTicket.getId(), userEmail, title != null ? title : "Không có tiêu đề");

        if (description != null && !description.trim().isEmpty()) {
            SupportMessage message = new SupportMessage();
            message.setTicket(savedTicket);
            message.setSenderEmail(userEmail);
            message.setMessage(description);
            message.setIsFromAdmin(false);
            message.setCreatedAt(Instant.now());
            messageRepository.save(message);
        }

        return convertToDTO(savedTicket);
    }

    @CacheEvict(value = "activeTickets", allEntries = true)
    public SupportMessageDTO addMessageToTicket(Long ticketId, String senderEmail, String message, boolean isFromAdmin) {
        log.debug("Adding message to ticket #{} from {}", ticketId, senderEmail);

        SupportTicket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        // Update status logic for Ticket flow
        if (ticket.getStatus() == TicketStatus.OPEN && isFromAdmin) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        } else if (ticket.getStatus() == TicketStatus.WAITING_FOR_CUSTOMER && !isFromAdmin) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setLastModifiedDate(Instant.now());

        SupportMessage supportMessage = new SupportMessage();
        supportMessage.setTicket(ticket);
        supportMessage.setSenderEmail(senderEmail);
        supportMessage.setMessage(message);
        supportMessage.setIsFromAdmin(isFromAdmin);
        supportMessage.setCreatedAt(Instant.now());

        SupportMessage savedMessage = messageRepository.save(supportMessage);
        ticketRepository.save(ticket);

        // Gửi thông báo cho user khi admin reply
        if (isFromAdmin && ticket.getUserEmail() != null) {
            String messagePreview = message.length() > 100 ? message.substring(0, 100) + "..." : message;
            notificationService.notifyUserTicketReply(ticket.getUserEmail(), ticketId, senderEmail, messagePreview);
        }
        // Gửi thông báo cho admin khi user reply
        else if (!isFromAdmin) {
            String messagePreview = message.length() > 100 ? message.substring(0, 100) + "..." : message;
            notificationService.notifyAdminTicketReply(ticketId, senderEmail, messagePreview);
        }

        return convertToDTO(savedMessage);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "allActiveTickets")
    public List<SupportTicketDTO> getAllActiveTickets() {
        return ticketRepository.findAllOrderByLastModifiedDateDesc().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllActiveTicketsByType(TicketType type) {
        return ticketRepository.findByTypeOrderByLastModifiedDateDesc(type).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getUserTickets(String userEmail) {
        return ticketRepository
            .findByUserEmailOrderByCreatedDateDesc(userEmail)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ticketMessages", key = "#ticketId")
    public List<SupportMessageDTO> getTicketMessages(Long ticketId) {
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * ✅ Load messages with pagination support (for scroll-up-to-load-more)
     * Query: SELECT * FROM support_message WHERE ticket_id = ? AND (beforeId IS NULL OR id < beforeId) ORDER BY id DESC LIMIT ?
     * Note: Client should reverse the result to display oldest first
     *
     * @param ticketId Conversation ID
     * @param beforeId Load messages before this ID (null = load latest)
     * @param limit Maximum number of messages
     * @return List of messages in descending order (newest first) - client should reverse
     */
    @Transactional(readOnly = true)
    public List<SupportMessageDTO> getTicketMessages(Long ticketId, Long beforeId, int limit) {
        List<SupportMessage> messages;

        if (beforeId == null) {
            // Load latest messages (first page)
            messages = messageRepository.findTop30ByTicketIdOrderByIdDesc(ticketId);
        } else {
            // Load older messages (pagination)
            messages = messageRepository.findTop30ByTicketIdAndIdLessThanOrderByIdDesc(ticketId, beforeId);
        }

        // Note: Messages are in DESC order from DB, client should reverse them for display
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @CacheEvict(value = { "activeTickets", "allActiveTickets", "ticketMessages" }, allEntries = true)
    public void closeTicket(Long ticketId) {
        log.debug("Closing ticket #{}", ticketId);
        SupportTicket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(Instant.now());
        ticket.setLastModifiedDate(Instant.now());
        ticketRepository.save(ticket);
    }

    @CacheEvict(value = { "activeTickets", "allActiveTickets" }, allEntries = true)
    public void assignTicket(Long ticketId, String adminEmail) {
        SupportTicket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setAssignedTo(adminEmail);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setLastModifiedDate(Instant.now());
        ticketRepository.save(ticket);
    }

    public void markMessagesAsRead(Long ticketId) {
        messageRepository.markAsReadByAdmin(ticketId);
    }

    private SupportTicketDTO convertToDTO(SupportTicket ticket) {
        SupportTicketDTO dto = new SupportTicketDTO();
        dto.setId(ticket.getId());
        dto.setUserEmail(ticket.getUserEmail());
        dto.setTitle(ticket.getTitle());
        dto.setStatus(ticket.getStatus());
        dto.setAssignedTo(ticket.getAssignedTo());
        dto.setCreatedDate(ticket.getCreatedDate());
        dto.setLastModifiedDate(ticket.getLastModifiedDate());
        dto.setClosedAt(ticket.getClosedAt());
        // Use the corrected method to count unread messages from user
        long unreadCount = messageRepository.countByTicketIdAndIsReadFalseAndIsFromAdminFalse(ticket.getId());
        dto.setUnreadCount(unreadCount);
        return dto;
    }

    private SupportMessageDTO convertToDTO(SupportMessage message) {
        SupportMessageDTO dto = new SupportMessageDTO();
        dto.setId(message.getId());
        dto.setTicketId(message.getTicket().getId());
        dto.setSenderEmail(message.getSenderEmail());
        dto.setMessage(message.getMessage());
        dto.setIsFromAdmin(message.getIsFromAdmin());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setIsRead(message.getIsRead());
        return dto;
    }
}
