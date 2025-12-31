package com.mycompany.myapp.repository.analytics;

import com.mycompany.myapp.domain.analytics.SupportTicket;
import com.mycompany.myapp.domain.enumeration.TicketStatus;
import com.mycompany.myapp.domain.enumeration.TicketType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserEmailOrderByCreatedDateDesc(String userEmail);

    List<SupportTicket> findByStatusOrderByCreatedDateDesc(TicketStatus status);

    @Query("SELECT t FROM SupportTicket t ORDER BY t.lastModifiedDate DESC")
    List<SupportTicket> findAllOrderByLastModifiedDateDesc();

    @Query("SELECT t FROM SupportTicket t WHERE t.type = :type ORDER BY t.lastModifiedDate DESC")
    List<SupportTicket> findByTypeOrderByLastModifiedDateDesc(@Param("type") TicketType type);

    Optional<SupportTicket> findFirstByUserEmailAndStatusInOrderByCreatedDateDesc(String userEmail, List<TicketStatus> statuses);

    List<SupportTicket> findByTypeAndStatusInAndLastModifiedDateBefore(
        TicketType type,
        List<TicketStatus> statuses,
        Instant lastModifiedDate
    );

    @Query(
        "SELECT t FROM SupportTicket t WHERE t.userEmail = :userEmail AND t.type = 'CHAT' AND t.status = 'OPEN' ORDER BY t.createdDate DESC"
    )
    Optional<SupportTicket> findActiveConversationByUserEmail(@Param("userEmail") String userEmail);
}
