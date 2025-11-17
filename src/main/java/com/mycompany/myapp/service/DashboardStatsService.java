package com.mycompany.myapp.service;

import com.mycompany.myapp.repository.DashboardStatsRepository;
import com.mycompany.myapp.service.dto.DashboardStatsDTO;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardStatsService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardStatsService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public DashboardStatsDTO getDashboardStats() {
        BigDecimal totalRevenue = dashboardStatsRepository.getTotalRevenue();
        Long totalOrders = dashboardStatsRepository.getTotalOrders();
        // We will remove the customer logic, but for now, let's revert to the original single query
        Long totalCustomers = dashboardStatsRepository.getTotalCustomers();
        Long totalProducts = dashboardStatsRepository.getTotalProducts();

        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue.doubleValue() : 0.0);
        stats.setTotalOrders(totalOrders != null ? totalOrders : 0L);
        stats.setTotalCustomers(totalCustomers != null ? totalCustomers : 0L);
        stats.setTotalProducts(totalProducts != null ? totalProducts : 0L);

        return stats;
    }
}
