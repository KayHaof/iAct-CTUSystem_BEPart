package com.example.activityservice.feature.dashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class StatsCacheService {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    private final AtomicLong cachedDepartmentCount = new AtomicLong(0);
    private final AtomicLong cachedMajorCount = new AtomicLong(0);
    private volatile long lastUpdateTime = 0;

    public StatsCacheService(
            RestTemplate restTemplate,
            @Value("${app.services.user-service-url:http://localhost:8081}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @PostConstruct
    public void init() {
        fetchAndCacheStats();
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void fetchAndCacheStats() {
        try {
            long deptCount = fetchDepartmentCount();
            cachedDepartmentCount.set(deptCount);

            long majorCount = fetchMajorCount();
            cachedMajorCount.set(majorCount);

            lastUpdateTime = System.currentTimeMillis();
            log.info("Dashboard stats da duoc cap nhat - Departments: {}, Majors: {}",
                    cachedDepartmentCount.get(), cachedMajorCount.get());

        } catch (Exception e) {
            log.error("Loi khi fetch dashboard stats: {}", e.getMessage());
        }
    }

    private long fetchDepartmentCount() {
        try {
            String url = userServiceUrl + "/api/v1/departments/count";
            var response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data != null) {
                    return ((Number) data).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Khong the lay so luong departments tu user-service: {}", e.getMessage());
        }
        return cachedDepartmentCount.get();
    }

    private long fetchMajorCount() {
        try {
            String url = userServiceUrl + "/api/v1/majors/count";
            var response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data != null) {
                    return ((Number) data).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Khong the lay so luong majors tu user-service: {}", e.getMessage());
        }
        return cachedMajorCount.get();
    }

    public long getCachedDepartmentCount() {
        return cachedDepartmentCount.get();
    }

    public long getCachedMajorCount() {
        return cachedMajorCount.get();
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void forceRefresh() {
        fetchAndCacheStats();
    }
}
