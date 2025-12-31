package com.mycompany.myapp.repository.analytics;

import com.mycompany.myapp.domain.analytics.SupportMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /**
     * ✅ Load latest N messages (first page load)
     * Query: SELECT TOP N * FROM support_message WHERE ticket_id = ? ORDER BY id DESC
     */
    List<SupportMessage> findTop30ByTicketIdOrderByIdDesc(Long ticketId);

    /**
     * ✅ Load older messages before a specific message ID (pagination/scroll-up)
     * Query: SELECT TOP N * FROM support_message WHERE ticket_id = ? AND id < ? ORDER BY id DESC
     */
    List<SupportMessage> findTop30ByTicketIdAndIdLessThanOrderByIdDesc(Long ticketId, Long beforeId);

    // Corrected: Count unread messages FROM USER for the admin dashboard
    long countByTicketIdAndIsReadFalseAndIsFromAdminFalse(Long ticketId);

    @Modifying
    @Query("UPDATE SupportMessage sm SET sm.isRead = true WHERE sm.ticket.id = :ticketId AND sm.isRead = false AND sm.isFromAdmin = false")
    void markAsReadByAdmin(@Param("ticketId") Long ticketId);
}
