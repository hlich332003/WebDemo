package com.mycompany.myapp.repository.analytics;

import com.mycompany.myapp.domain.analytics.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository cho Notification - Kết nối đến analytics_db
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Tìm tất cả thông báo của một user, sắp xếp theo thời gian mới nhất
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Tìm thông báo của user với phân trang
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Đếm số thông báo chưa đọc của user
     */
    long countByUserIdAndIsReadFalse(String userId);

    /**
     * Tìm thông báo chưa đọc của user
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    /**
     * Tìm thông báo theo role (cho admin)
     */
    @Query("SELECT n FROM Notification n WHERE n.targetRole = :role ORDER BY n.createdAt DESC")
    List<Notification> findByTargetRoleOrderByCreatedAtDesc(@Param("role") String role);

    /**
     * Tìm thông báo theo role với phân trang
     */
    @Query("SELECT n FROM Notification n WHERE n.targetRole = :role ORDER BY n.createdAt DESC")
    Page<Notification> findByTargetRoleOrderByCreatedAtDesc(@Param("role") String role, Pageable pageable);

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") String userId);

    /**
     * Đánh dấu thông báo cụ thể là đã đọc
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    int markAsReadById(@Param("id") Long id);

    /**
     * Xóa thông báo cũ hơn một khoảng thời gian
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") Instant cutoffDate);
}
