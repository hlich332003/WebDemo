package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Order; // Import Order để JpaRepository có thể hoạt động
import com.mycompany.myapp.service.dto.DashboardStatsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardStatsRepository extends JpaRepository<Order, Long> { // JpaRepository cần một Entity và kiểu ID
    @Query(
        nativeQuery = true,
        value = "SELECT " +
        "   (SELECT SUM(total_amount) FROM jhi_order WHERE status = 'DELIVERED') as totalRevenue, " +
        "   (SELECT COUNT(*) FROM jhi_order) as totalOrders, " +
        "   (SELECT COUNT(DISTINCT user_id) FROM jhi_order) as totalCustomers"
    )
    DashboardStatsDTO getDashboardStats();
}
