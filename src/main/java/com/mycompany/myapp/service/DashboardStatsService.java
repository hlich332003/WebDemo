package com.mycompany.myapp.service;

import com.mycompany.myapp.repository.DashboardStatsRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.dto.DashboardStatsDTO;
import java.math.BigDecimal;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardStatsService {

    private final DashboardStatsRepository dashboardStatsRepository;
    private final UserRepository userRepository;

    public DashboardStatsService(DashboardStatsRepository dashboardStatsRepository, UserRepository userRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get dashboard statistics
     * ✅ OPTIMIZED: Cached for 2 minutes to reduce database load
     * Dashboard is frequently accessed but doesn't need real-time data
     */
    @Cacheable(value = "dashboardStats", key = "'stats'")
    public DashboardStatsDTO getDashboardStats() {
        BigDecimal totalRevenue = dashboardStatsRepository.getTotalRevenue();
        Long totalOrders = dashboardStatsRepository.getTotalOrders();
        Long totalCustomers = userRepository.countByAuthority_Name(AuthoritiesConstants.USER);
        Long totalProducts = dashboardStatsRepository.getTotalProducts();

        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.setTotalOrders(totalOrders != null ? totalOrders : 0L);
        stats.setTotalCustomers(totalCustomers != null ? totalCustomers : 0L);
        stats.setTotalProducts(totalProducts != null ? totalProducts : 0L);

        return stats;
    }
}
