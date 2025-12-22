package com.mycompany.myapp.repository.analytics;

import com.mycompany.myapp.domain.analytics.AnalyticsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnalyticsLogRepository extends JpaRepository<AnalyticsLog, Long> {
    List<AnalyticsLog> findByUserIdOrderByTimestampDesc(String userId);
    List<AnalyticsLog> findByTimestampBetween(Instant startDate, Instant endDate);
}
