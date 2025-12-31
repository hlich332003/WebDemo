package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.analytics.SupportMessage;
import com.mycompany.myapp.domain.analytics.SupportTicket;
import com.mycompany.myapp.domain.enumeration.TicketStatus;
import com.mycompany.myapp.domain.enumeration.TicketType;
import com.mycompany.myapp.repository.analytics.SupportMessageRepository;
import com.mycompany.myapp.repository.analytics.SupportTicketRepository;
import com.mycompany.myapp.service.dto.ChatInitiationDTO;
import com.mycompany.myapp.service.dto.SupportMessageDTO;
import com.mycompany.myapp.service.dto.SupportTicketDTO;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional("analyticsTransactionManager")
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final NotificationService notificationService;
    private final ChatRedisPublisher chatRedisPublisher;

    public ChatService(
        SupportTicketRepository ticketRepository,
        SupportMessageRepository messageRepository,
        SimpMessageSendingOperations messagingTemplate,
        NotificationService notificationService,
        ChatRedisPublisher chatRedisPublisher
    ) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.chatRedisPublisher = chatRedisPublisher;
    }

    @CacheEvict(value = "activeTickets", key = "#userIdentifier")
    public ChatInitiationDTO createChatConversation(String userIdentifier) {
        log.debug("Creating new CHAT conversation for user: {}", userIdentifier);

        SupportTicket newTicket = new SupportTicket();
        newTicket.setUserEmail(userIdentifier);
        newTicket.setTitle("Hỗ trợ trực tuyến");
        newTicket.setStatus(TicketStatus.OPEN);
        newTicket.setType(TicketType.CHAT);

        SupportTicket savedTicket = ticketRepository.save(newTicket);
        log.info("✅ Created new chat conversation #{} for user: {}", savedTicket.getId(), userIdentifier);

        // Calculate initial unread count
        long unreadCount = messageRepository.countByTicketIdAndIsReadFalseAndIsFromAdminFalse(savedTicket.getId());

        // Gửi thông báo cho admin/CSKH về chat mới với unread count
        notificationService.notifyAdminNewChatSupport(savedTicket.getId(), userIdentifier, unreadCount);

        SupportMessage welcomeMessage = new SupportMessage();
        welcomeMessage.setTicket(savedTicket);
        welcomeMessage.setSenderEmail("SYSTEM");
        welcomeMessage.setMessage("Chào bạn, bạn cần hỗ trợ gì?");
        welcomeMessage.setIsFromAdmin(true);
        welcomeMessage.setCreatedAt(Instant.now());
        SupportMessage savedWelcomeMessage = messageRepository.save(welcomeMessage);

        // 🔥 Broadcast welcome message to both user and admin via WebSocket
        notifyNewMessage(savedWelcomeMessage, savedTicket);

        SupportTicketDTO ticketDTO = convertToDTO(savedTicket);
        SupportMessageDTO messageDTO = convertToDTO(savedWelcomeMessage);

        return new ChatInitiationDTO(ticketDTO, Collections.singletonList(messageDTO));
    }

    @CacheEvict(value = "activeTickets", allEntries = true)
    public SupportMessageDTO sendChatMessage(Long ticketId, String senderIdentifier, String content, boolean isFromAdmin) {
        log.debug("Sending chat message for conversation #{} from {}", ticketId, senderIdentifier);

        SupportTicket ticket = ticketRepository
            .findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Conversation not found: " + ticketId));

        // Strict closed ticket handling: Once closed, no more messages allowed (unless from admin to reopen)
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            if (!isFromAdmin) {
                // User cannot send to closed ticket - they must start a new conversation
                log.warn("⚠️ User {} attempted to send message to CLOSED conversation #{}. Message blocked.", senderIdentifier, ticketId);
                throw new RuntimeException("This conversation is closed. Please start a new conversation.");
            }
            // If admin sends message to closed ticket, reopen it
            log.info("🔓 Admin {} is reopening CLOSED conversation #{}", senderIdentifier, ticketId);
            ticket.setStatus(TicketStatus.OPEN);
        }

        ticket.setLastModifiedDate(Instant.now());

        SupportMessage supportMessage = new SupportMessage();
        supportMessage.setTicket(ticket);
        supportMessage.setSenderEmail(senderIdentifier);
        supportMessage.setMessage(content);
        supportMessage.setIsFromAdmin(isFromAdmin);
        supportMessage.setCreatedAt(Instant.now());

        SupportMessage savedMessage = messageRepository.save(supportMessage);
        ticketRepository.save(ticket);

        notifyNewMessage(savedMessage, ticket);

        return convertToDTO(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<SupportMessageDTO> getChatMessages(Long ticketId) {
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @CacheEvict(value = { "activeTickets", "allActiveTickets", "ticketMessages" }, allEntries = true)
    public void closeChatConversation(Long ticketId) {
        log.debug("Closing chat conversation #{}", ticketId);
        SupportTicket ticket = ticketRepository
            .findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Conversation not found: " + ticketId));

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(Instant.now());
        ticketRepository.save(ticket);

        // ✅ Broadcast SESSION_ENDED qua Redis → WebSocket (tất cả instances)
        Map<String, Object> payload = Map.of(
            "type",
            "SESSION_ENDED",
            "message",
            "Phiên chat đã kết thúc.",
            "conversationId",
            ticket.getId()
        );

        // Publish to Redis để broadcast tới tất cả instances
        chatRedisPublisher.publishChatMessage(ticket.getId(), payload);

        log.info("🔒 [CLOSE] Conversation #{} closed and SESSION_ENDED broadcasted", ticketId);
    }

    private void notifyNewMessage(SupportMessage message, SupportTicket ticket) {
        try {
            SupportMessageDTO dto = convertToDTO(message);

            // Calculate unread count for this conversation (messages from USER not read by ADMIN)
            long unreadCount = messageRepository.countByTicketIdAndIsReadFalseAndIsFromAdminFalse(ticket.getId());

            // ✅ FLOW CHUẨN: DB → Redis → WebSocket
            // 1. Message đã được lưu vào DB (called from sendChatMessage)
            // 2. Bây giờ publish lên Redis để broadcast tới tất cả instances

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", dto.getId());
            payload.put("conversationId", ticket.getId());
            payload.put("content", dto.getMessage());
            payload.put("message", dto.getMessage()); // Include both for compatibility
            payload.put("senderType", dto.getIsFromAdmin() ? "ADMIN" : "USER");
            payload.put("senderIdentifier", dto.getSenderEmail());
            payload.put("isFromAdmin", dto.getIsFromAdmin());
            payload.put("createdAt", dto.getCreatedAt().toString());
            payload.put("type", "MESSAGE");
            payload.put("unreadCount", unreadCount); // ✅ Include unread count for real-time update
            payload.put(
                "lastMessageAt",
                ticket.getLastModifiedDate() != null ? ticket.getLastModifiedDate().toString() : ticket.getCreatedDate().toString()
            );

            // Publish to Redis - ChatRedisSubscriber sẽ nhận và broadcast qua WebSocket
            chatRedisPublisher.publishChatMessage(ticket.getId(), payload);

            log.info(
                "✅ [DB→REDIS] Message saved & published | ConvID: {} | From: {} | Content: {} | UnreadCount: {}",
                ticket.getId(),
                dto.getSenderEmail(),
                dto.getMessage().length() > 50 ? dto.getMessage().substring(0, 50) + "..." : dto.getMessage(),
                unreadCount
            );
        } catch (Exception e) {
            log.error("❌ Failed to publish message notification", e);
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
