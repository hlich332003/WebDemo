package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Order;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardStatsRepository extends JpaRepository<Order, Long> {
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal getTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o")
    Long getTotalOrders();

    @Query("SELECT COUNT(DISTINCT o.customer.id) FROM Order o WHERE o.customer IS NOT NULL")
    Long getTotalCustomers();

    @Query("SELECT COUNT(p) FROM Product p")
    Long getTotalProducts();
}
