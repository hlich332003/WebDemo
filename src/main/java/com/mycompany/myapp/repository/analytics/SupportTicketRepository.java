package com.mycompany.myapp.repository.analytics;

import com.mycompany.myapp.domain.analytics.SupportTicket;
import com.mycompany.myapp.domain.enumeration.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByUserEmailOrderByCreatedDateDesc(String userEmail);

    List<SupportTicket> findByStatusOrderByCreatedDateDesc(TicketStatus status);

    @Query("SELECT t FROM SupportTicket t WHERE t.status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER') ORDER BY t.createdDate DESC")
    List<SupportTicket> findActiveTickets();

    Optional<SupportTicket> findFirstByUserEmailAndStatusInOrderByCreatedDateDesc(
        String userEmail,
        List<TicketStatus> statuses
    );
}


