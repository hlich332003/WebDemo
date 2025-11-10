package com.mycompany.myapp.service;

import com.mycompany.myapp.repository.DashboardStatsRepository;
import com.mycompany.myapp.service.dto.DashboardStatsDTO;
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
        return dashboardStatsRepository.getDashboardStats();
    }
}
