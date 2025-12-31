package com.mycompany.myapp.repository.analytics;

import com.mycompany.myapp.domain.analytics.NotificationEntity;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findAllByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.receiverId = :receiverId AND n.isRead = false")
    long countByReceiverIdAndIsReadFalse(@Param("receiverId") Long receiverId);

    List<NotificationEntity> findAllByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);
}
