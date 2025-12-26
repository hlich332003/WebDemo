package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.analytics.ChatConversation;
import com.mycompany.myapp.domain.analytics.ChatMessage;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.analytics.ChatConversationRepository;
import com.mycompany.myapp.repository.analytics.ChatMessageRepository;
import com.mycompany.myapp.service.dto.ChatResponseDTO;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional("analyticsTransactionManager")
public class ChatService {

    private final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository; // From default DB

    public ChatService(
        ChatConversationRepository conversationRepository,
        ChatMessageRepository messageRepository,
        SimpMessagingTemplate messagingTemplate,
        UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public ChatConversation startConversation(String userIdentifier) {
        Optional<ChatConversation> existing = conversationRepository.findByUserIdentifierAndStatus(userIdentifier, "OPEN");
        if (existing.isPresent()) {
            return existing.get();
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setUserIdentifier(userIdentifier);
        conversation.setStatus("OPEN");
        conversation.setCreatedAt(Instant.now());
        conversation.setLastMessageAt(Instant.now());
        return conversationRepository.save(conversation);
    }

    public Optional<ChatConversation> findOpenConversation(String userIdentifier) {
        return conversationRepository.findByUserIdentifierAndStatus(userIdentifier, "OPEN");
    }

    /**
     * Create a new conversation for the given userIdentifier regardless of existing OPEN ones.
     * This is used when the frontend user explicitly requests a new session ("Tạo phiên chat mới").
     */
    public ChatConversation createNewConversation(String userIdentifier) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserIdentifier(userIdentifier);
        conversation.setStatus("OPEN");
        conversation.setCreatedAt(Instant.now());
        conversation.setLastMessageAt(Instant.now());
        return conversationRepository.save(conversation);
    }

    public void handleUserMessage(String content, String userIdentifier) {
        ChatConversation conversation = startConversation(userIdentifier);
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        ChatMessage message = saveMessage(conversation, "USER", userIdentifier, content);

        ChatResponseDTO response = toResponseDTO(message);

        // Broadcast to standardized topic so both user and admin can subscribe to same topic
        String topic = "/topic/chat/conversations/" + conversation.getId();
        messagingTemplate.convertAndSend(topic, response);

        // Also send to user's personal queue for direct delivery if using convertAndSendToUser on some clients
        messagingTemplate.convertAndSendToUser(conversation.getUserIdentifier(), "/queue/chat", response);
    }

    public void handleCskhReply(Long conversationId, String content, String cskhIdentifier) {
        ChatConversation conversation = conversationRepository
            .findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        conversation.setLastMessageAt(Instant.now());
        if (conversation.getAssignedAdminId() == null) {
            // Assuming cskhIdentifier is the email, find the user id
            userRepository.findOneByEmailIgnoreCase(cskhIdentifier).ifPresent(user -> conversation.setAssignedAdminId(user.getId()));
        }
        conversationRepository.save(conversation);

        ChatMessage message = saveMessage(conversation, "CSKH", cskhIdentifier, content);

        ChatResponseDTO response = toResponseDTO(message);

        // Broadcast to standardized topic
        String topic = "/topic/chat/conversations/" + conversation.getId();
        messagingTemplate.convertAndSend(topic, response);

        // Also notify all CSKH for sync (optional)
        messagingTemplate.convertAndSend("/topic/cskh/chat", response);
    }

    public void closeConversation(Long conversationId, String closedBy) {
        conversationRepository
            .findById(conversationId)
            .ifPresent(conversation -> {
                conversation.setStatus("CLOSED");
                conversation.setClosedAt(Instant.now());
                // closedBy field not present in entity; could be stored elsewhere or logged
                conversationRepository.save(conversation);

                ChatResponseDTO response = new ChatResponseDTO(
                    conversationId,
                    "SYSTEM",
                    "System",
                    "Phiên chat đã kết thúc",
                    Instant.now(),
                    "SESSION_ENDED"
                );
                // Notify via standardized topic and per-user queue
                String topic = "/topic/chat/conversations/" + conversationId;
                messagingTemplate.convertAndSend(topic, response);
                messagingTemplate.convertAndSendToUser(conversation.getUserIdentifier(), "/queue/chat", response);
            });
    }

    /**
     * Mark messages sent by admin/CSKH in a conversation as read (used when user opens a conversation)
     */
    @Transactional
    public void markAdminMessagesAsRead(Long conversationId) {
        List<ChatMessage> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        boolean changed = false;
        for (ChatMessage m : msgs) {
            if (m != null && "CSKH".equalsIgnoreCase(m.getSenderType()) && !m.isRead()) {
                m.setRead(true);
                messageRepository.save(m);
                changed = true;
            }
        }
        if (changed) {
            log.debug("Marked admin messages as read for conversationId={}", conversationId);
        }
    }

    private ChatMessage saveMessage(ChatConversation conversation, String senderType, String senderIdentifier, String content) {
        // ensure conversation has updated lastMessageAt
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setSenderIdentifier(senderIdentifier);
        message.setContent(content);
        message.setCreatedAt(Instant.now());
        return messageRepository.save(message);
    }

    private ChatResponseDTO toResponseDTO(ChatMessage message) {
        return new ChatResponseDTO(
            message.getConversation().getId(),
            message.getSenderType(),
            message.getSenderIdentifier(),
            message.getContent(),
            message.getCreatedAt(),
            "MESSAGE"
        );
    }

    @Transactional(readOnly = true)
    public List<ChatConversation> getActiveConversations() {
        return conversationRepository.findByStatusIn(List.of("OPEN", "IN_PROGRESS"));
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForConversation(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
