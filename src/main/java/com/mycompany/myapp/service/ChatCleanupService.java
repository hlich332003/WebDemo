package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.analytics.SupportTicket;
import com.mycompany.myapp.domain.enumeration.TicketStatus;
import com.mycompany.myapp.domain.enumeration.TicketType;
import com.mycompany.myapp.repository.analytics.SupportTicketRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatCleanupService {

    private final Logger log = LoggerFactory.getLogger(ChatCleanupService.class);

    private final SupportTicketRepository ticketRepository;
    private final ChatService chatService;
    private static final long IDLE_TIMEOUT_MINUTES = 20;

    public ChatCleanupService(SupportTicketRepository ticketRepository, ChatService chatService) {
        this.ticketRepository = ticketRepository;
        this.chatService = chatService;
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional("analyticsTransactionManager")
    public void closeIdleChats() {
        log.info("Running scheduled job to close idle chats...");
        Instant idleThreshold = Instant.now().minus(IDLE_TIMEOUT_MINUTES, ChronoUnit.MINUTES);

        List<TicketStatus> activeStatuses = Arrays.asList(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_CUSTOMER);

        List<SupportTicket> idleTickets = ticketRepository.findByTypeAndStatusInAndLastModifiedDateBefore(
            TicketType.CHAT,
            activeStatuses,
            idleThreshold
        );

        if (idleTickets.isEmpty()) {
            log.info("No idle chats to close.");
            return;
        }

        log.warn("Found {} idle chats to close.", idleTickets.size());
        for (SupportTicket ticket : idleTickets) {
            try {
                log.info("Closing idle chat ticket #{}", ticket.getId());
                chatService.closeChatConversation(ticket.getId());
            } catch (Exception e) {
                log.error("Error closing idle chat ticket #{}: {}", ticket.getId(), e.getMessage());
            }
        }
    }
}
