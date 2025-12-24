package com.mycompany.myapp.repository.analytics;

import com.mycompany.myapp.domain.analytics.ChatConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findByUserIdentifierAndStatus(String userIdentifier, String status);

    List<ChatConversation> findByStatusIn(List<String> statuses);
}
