package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.DashboardStatsService;
import com.mycompany.myapp.service.dto.DashboardStatsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class DashboardStatsResource {

    private final Logger log = LoggerFactory.getLogger(DashboardStatsResource.class);

    private final DashboardStatsService dashboardStatsService;

    public DashboardStatsResource(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = dashboardStatsService;
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        log.debug("REST request to get dashboard stats");
        DashboardStatsDTO stats = dashboardStatsService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
