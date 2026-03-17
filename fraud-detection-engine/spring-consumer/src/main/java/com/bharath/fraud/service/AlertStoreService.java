package com.bharath.fraud.service;

import com.bharath.fraud.model.FraudAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertStoreService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ALERT_KEY = "fraud:alerts";
    private static final String STATS_KEY  = "fraud:stats";
    private static final long   MAX_ALERTS = 10_000;

    public void store(FraudAlert alert) {
        try {
            redisTemplate.opsForList().leftPush(ALERT_KEY, alert);
            redisTemplate.opsForList().trim(ALERT_KEY, 0, MAX_ALERTS - 1);
            redisTemplate.expire(ALERT_KEY, 24, TimeUnit.HOURS);
            incrementStats(alert.getRiskLevel());
        } catch (Exception e) {
            log.error("Failed to store alert: {}", e.getMessage());
        }
    }

    public List<FraudAlert> getAlerts(int page, int size, String riskLevel) {
        long start = (long) page * size;
        long end   = start + size - 1;
        List<Object> raw = redisTemplate.opsForList().range(ALERT_KEY, start, end);
        if (raw == null) return Collections.emptyList();

        List<FraudAlert> alerts = raw.stream()
                .filter(o -> o instanceof FraudAlert)
                .map(o -> (FraudAlert) o)
                .collect(Collectors.toList());

        if (riskLevel != null && !riskLevel.isBlank()) {
            alerts = alerts.stream()
                    .filter(a -> riskLevel.equalsIgnoreCase(a.getRiskLevel()))
                    .collect(Collectors.toList());
        }
        return alerts;
    }

    public AlertStats getStats() {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(STATS_KEY);
        return AlertStats.builder()
                .totalAlerts(getLong(raw, "total"))
                .criticalCount(getLong(raw, "CRITICAL"))
                .highCount(getLong(raw, "HIGH"))
                .mediumCount(getLong(raw, "MEDIUM"))
                .lowCount(getLong(raw, "LOW"))
                .build();
    }

    private void incrementStats(String riskLevel) {
        redisTemplate.opsForHash().increment(STATS_KEY, "total", 1);
        redisTemplate.opsForHash().increment(STATS_KEY, riskLevel, 1);
        redisTemplate.expire(STATS_KEY, 24, TimeUnit.HOURS);
    }

    private long getLong(Map<Object, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    @lombok.Builder
    @lombok.Data
    public static class AlertStats {
        private long totalAlerts;
        private long criticalCount;
        private long highCount;
        private long mediumCount;
        private long lowCount;
    }
}
