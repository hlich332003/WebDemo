package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.analytics.ChatConversation;
import com.mycompany.myapp.domain.analytics.ChatMessage;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.analytics.ChatConversationRepository;
import com.mycompany.myapp.repository.analytics.ChatMessageRepository;
import com.mycompany.myapp.security.SecurityUtils;
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

    public void handleUserMessage(String content, String userIdentifier) {
        ChatConversation conversation = startConversation(userIdentifier);
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        ChatMessage message = saveMessage(conversation, "USER", userIdentifier, content);

        ChatResponseDTO response = toResponseDTO(message);

        // Echo back to user
        messagingTemplate.convertAndSendToUser(userIdentifier, "/queue/chat", response);
        // Send to CSKH - publish to a topic only CSKH subscribe to
        messagingTemplate.convertAndSend("/topic/cskh/chat", response);
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

        // Send to user
        messagingTemplate.convertAndSendToUser(conversation.getUserIdentifier(), "/queue/chat", response);
        // Send to other CSKHs for sync
        messagingTemplate.convertAndSend("/topic/cskh/chat", response);
    }

    public void closeConversation(Long conversationId) {
        conversationRepository
            .findById(conversationId)
            .ifPresent(conversation -> {
                conversation.setStatus("CLOSED");
                conversationRepository.save(conversation);

                ChatResponseDTO response = new ChatResponseDTO(
                    conversationId,
                    "SYSTEM",
                    "System",
                    "Phiên chat đã kết thúc",
                    Instant.now(),
                    "SESSION_ENDED"
                );
                messagingTemplate.convertAndSendToUser(conversation.getUserIdentifier(), "/queue/chat", response);
            });
    }

    private ChatMessage saveMessage(ChatConversation conversation, String senderType, String senderIdentifier, String content) {
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
