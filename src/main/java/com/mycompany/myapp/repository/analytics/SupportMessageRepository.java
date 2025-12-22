package com.mycompany.myapp.repository.analytics;

import com.mycompany.myapp.domain.analytics.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    long countByTicketIdAndIsReadFalseAndIsFromAdminTrue(Long ticketId);

    @Modifying
    @Query("UPDATE SupportMessage sm SET sm.isRead = true WHERE sm.ticket.id = :ticketId AND sm.isRead = false AND sm.isFromAdmin = false")
    void markMessagesAsRead(@Param("ticketId") Long ticketId);
}
