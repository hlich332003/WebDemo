package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.analytics.SupportMessage;
import com.mycompany.myapp.domain.analytics.SupportTicket;
import com.mycompany.myapp.domain.enumeration.TicketStatus;
import com.mycompany.myapp.repository.analytics.SupportMessageRepository;
import com.mycompany.myapp.repository.analytics.SupportTicketRepository;
import com.mycompany.myapp.service.dto.SupportMessageDTO;
import com.mycompany.myapp.service.dto.SupportTicketDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional("analyticsTransactionManager")
public class SupportTicketService {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketService.class);

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final NotificationService notificationService;

    public SupportTicketService(
        SupportTicketRepository ticketRepository,
        SupportMessageRepository messageRepository,
        SimpMessageSendingOperations messagingTemplate,
        NotificationService notificationService
    ) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "activeTickets", key = "#userEmail", unless = "#result == null")
    public SupportTicketDTO getCurrentActiveTicket(String userEmail) {
        log.debug("Getting current active ticket for user: {}", userEmail);

        Optional<SupportTicket> existingTicket = ticketRepository
            .findFirstByUserEmailAndStatusInOrderByCreatedDateDesc(
                userEmail,
                Arrays.asList(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_CUSTOMER)
            );

        return existingTicket.map(this::convertToDTO).orElse(null);
    }

    @CacheEvict(value = "activeTickets", key = "#userEmail")
    public SupportTicketDTO createTicket(String userEmail, String title, String description) {
        log.debug("Creating new ticket for user: {}", userEmail);

        // Tạo ticket mới
        SupportTicket newTicket = new SupportTicket();
        newTicket.setUserEmail(userEmail);
        newTicket.setTitle(title);
        newTicket.setStatus(TicketStatus.OPEN);

        SupportTicket savedTicket = ticketRepository.save(newTicket);
        log.info("✅ Created new support ticket #{} for user: {}", savedTicket.getId(), userEmail);

        // Lưu tin nhắn đầu tiên (mô tả vấn đề)
        if (description != null && !description.trim().isEmpty()) {
            SupportMessage message = new SupportMessage();
            message.setTicket(savedTicket);
            message.setSenderEmail(userEmail);
            message.setMessage(description);
            message.setIsFromAdmin(false);
            message.setCreatedAt(Instant.now());
            messageRepository.save(message);
        }

        // Gửi thông báo đến admin qua WebSocket
        notificationService.notifyAdminNewSupportTicket(savedTicket.getId(), savedTicket.getUserEmail());

        return convertToDTO(savedTicket);
    }

    public SupportTicketDTO getOrCreateActiveTicket(String userEmail) {
        log.debug("Getting or creating active ticket for user: {}", userEmail);

        // Query directly to avoid cache self-invocation issue
        Optional<SupportTicket> existingTicket = ticketRepository
            .findFirstByUserEmailAndStatusInOrderByCreatedDateDesc(
                userEmail,
                Arrays.asList(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_CUSTOMER)
            );

        if (existingTicket.isPresent()) {
            return convertToDTO(existingTicket.get());
        }

        return createTicket(userEmail, "Yêu cầu hỗ trợ từ " + userEmail, null);
    }

    @CacheEvict(value = "activeTickets", allEntries = true)
    public SupportMessageDTO sendMessage(Long ticketId, String senderEmail, String message, boolean isFromAdmin) {
        log.debug("Sending message for ticket #{} from {}", ticketId, senderEmail);

        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu hỗ trợ: " + ticketId));

        // Cập nhật trạng thái ticket
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

        // Gửi thông báo real-time
        notifyNewMessage(savedMessage, ticket);

        return convertToDTO(savedMessage);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "allActiveTickets")
    public List<SupportTicketDTO> getAllActiveTickets() {
        log.debug("Getting all active support tickets");
        return ticketRepository.findActiveTickets().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getUserTickets(String userEmail) {
        log.debug("Getting tickets for user: {}", userEmail);
        return ticketRepository.findByUserEmailOrderByCreatedDateDesc(userEmail).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ticketMessages", key = "#ticketId")
    public List<SupportMessageDTO> getTicketMessages(Long ticketId) {
        log.debug("Getting messages for ticket #{}", ticketId);
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @CacheEvict(value = {"activeTickets", "allActiveTickets", "ticketMessages"}, allEntries = true)
    public void closeTicket(Long ticketId) {
        log.debug("Closing ticket #{}", ticketId);
        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(Instant.now());
        ticket.setLastModifiedDate(Instant.now());
        ticketRepository.save(ticket);

        // Gửi thông báo kết thúc phiên cho khách hàng
        messagingTemplate.convertAndSendToUser(
            ticket.getUserEmail(),
            "/queue/chat",
            Map.of("type", "SESSION_ENDED", "message", "Phiên hỗ trợ đã kết thúc.")
        );

        log.info("✅ Closed ticket #{}", ticketId);
    }

    @CacheEvict(value = {"activeTickets", "allActiveTickets"}, allEntries = true)
    public void assignTicket(Long ticketId, String adminEmail) {
        log.debug("Assigning ticket #{} to {}", ticketId, adminEmail);
        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        ticket.setAssignedTo(adminEmail);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setLastModifiedDate(Instant.now());
        ticketRepository.save(ticket);

        log.info("✅ Assigned ticket #{} to {}", ticketId, adminEmail);
    }

    public void markMessagesAsRead(Long ticketId) {
        log.debug("Marking messages as read for ticket #{}", ticketId);
        messageRepository.markMessagesAsRead(ticketId);
    }

    private void notifyNewMessage(SupportMessage message, SupportTicket ticket) {
        try {
            SupportMessageDTO dto = convertToDTO(message);

            // Gửi đến admin nếu tin nhắn từ user
            if (!message.getIsFromAdmin()) {
                messagingTemplate.convertAndSend("/topic/admin/messages", dto);
                log.info("📢 Sent message notification to admins for ticket #{}", ticket.getId());
            } else {
                // Gửi đến user nếu tin nhắn từ admin
                messagingTemplate.convertAndSendToUser(
                    ticket.getUserEmail(),
                    "/queue/messages",
                    dto
                );
                log.info("📢 Sent message notification to user {} for ticket #{}",
                    ticket.getUserEmail(), ticket.getId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send message notification", e);
        }
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

        // Đếm số tin nhắn chưa đọc
        long unreadCount = messageRepository.countByTicketIdAndIsReadFalseAndIsFromAdminTrue(ticket.getId());
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
