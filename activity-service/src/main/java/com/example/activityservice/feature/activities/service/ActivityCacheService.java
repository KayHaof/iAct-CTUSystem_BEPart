package com.example.activityservice.feature.activities.service;

import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.dto.RecommendationResponse;
import com.example.dto.PageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityCacheService {

    private static final String FOR_REGISTRATION_KEY_PREFIX = "activities:for-registration:";
    private static final String SEARCH_KEY_PREFIX = "activities:search:";
    private static final String RECOMMENDATION_KEY_PREFIX = "activities:recommendations:";
    private static final long LIST_TTL_SECONDS = 60;
    private static final long RECOMMENDATION_TTL_SECONDS = 120;
    private static final int MAX_CACHEABLE_SEARCH_PAGE = 5;
    private static final int MAX_CACHEABLE_SEARCH_SIZE = 50;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<PageDTO<ActivityResponse>> getForRegistration(
            Long semesterId, Long studentDepartmentId, Pageable pageable) {
        return read(pageKey(FOR_REGISTRATION_KEY_PREFIX, semesterId, studentDepartmentId, pageable),
                new TypeReference<>() {});
    }

    public void putForRegistration(
            Long semesterId, Long studentDepartmentId, Pageable pageable, PageDTO<ActivityResponse> response) {
        write(pageKey(FOR_REGISTRATION_KEY_PREFIX, semesterId, studentDepartmentId, pageable), response,
                LIST_TTL_SECONDS);
    }

    public Optional<PageDTO<ActivityResponse>> getSearch(
            String keyword, Long departmentId, String startDate, String endDate,
            List<Long> categoryIds, String category, String status, Long studentDepartmentId, Pageable pageable) {
        if (!isSearchCacheable(keyword, pageable)) {
            return Optional.empty();
        }
        return read(searchKey(keyword, departmentId, startDate, endDate, categoryIds, category, status,
                studentDepartmentId, pageable), new TypeReference<>() {});
    }

    public void putSearch(
            String keyword, Long departmentId, String startDate, String endDate,
            List<Long> categoryIds, String category, String status, Long studentDepartmentId, Pageable pageable,
            PageDTO<ActivityResponse> response) {
        if (!isSearchCacheable(keyword, pageable)) {
            return;
        }
        write(searchKey(keyword, departmentId, startDate, endDate, categoryIds, category, status,
                studentDepartmentId, pageable), response, LIST_TTL_SECONDS);
    }

    public Optional<RecommendationResponse> getRecommendations(Long studentId, Long semesterId, int limit) {
        return read(recommendationKey(studentId, semesterId, limit), RecommendationResponse.class);
    }

    public void putRecommendations(Long studentId, Long semesterId, int limit, RecommendationResponse response) {
        write(recommendationKey(studentId, semesterId, limit), response, RECOMMENDATION_TTL_SECONDS);
    }

    public void evictActivityListCaches() {
        evictByPrefix(FOR_REGISTRATION_KEY_PREFIX);
        evictByPrefix(SEARCH_KEY_PREFIX);
        evictByPrefix(RECOMMENDATION_KEY_PREFIX);
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }
            log.debug("Activity cache hit for key {}", key);
            return Optional.of(objectMapper.readValue(String.valueOf(cached), type));
        } catch (Exception e) {
            log.warn("Redis read failed for activity cache key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> read(String key, TypeReference<T> type) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }
            log.debug("Activity cache hit for key {}", key);
            return Optional.of(objectMapper.readValue(String.valueOf(cached), type));
        } catch (Exception e) {
            log.warn("Redis read failed for activity cache key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void write(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write failed for activity cache key {}: {}", key, e.getMessage());
        }
    }

    private void evictByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} activity cache keys with prefix {}", keys.size(), prefix);
            }
        } catch (Exception e) {
            log.warn("Failed to evict activity cache prefix {}: {}", prefix, e.getMessage());
        }
    }

    private String pageKey(String prefix, Long semesterId, Long studentDepartmentId, Pageable pageable) {
        return prefix
                + token(semesterId)
                + ":dept:" + token(studentDepartmentId)
                + ":page:" + pageable.getPageNumber()
                + ":size:" + pageable.getPageSize()
                + ":sort:" + sortToken(pageable);
    }

    private String searchKey(
            String keyword, Long departmentId, String startDate, String endDate,
            List<Long> categoryIds, String category, String status, Long studentDepartmentId, Pageable pageable) {
        String canonical = "keyword=" + normalize(keyword)
                + "|departmentId=" + token(departmentId)
                + "|startDate=" + token(startDate)
                + "|endDate=" + token(endDate)
                + "|categoryIds=" + categoryIdsToken(categoryIds)
                + "|category=" + normalize(category)
                + "|status=" + normalize(status)
                + "|studentDepartmentId=" + token(studentDepartmentId)
                + "|page=" + pageable.getPageNumber()
                + "|size=" + pageable.getPageSize()
                + "|sort=" + sortToken(pageable);
        return SEARCH_KEY_PREFIX + sha256(canonical);
    }

    private String recommendationKey(Long studentId, Long semesterId, int limit) {
        return RECOMMENDATION_KEY_PREFIX + token(studentId) + ":" + token(semesterId) + ":" + limit;
    }

    private boolean isSearchCacheable(String keyword, Pageable pageable) {
        String normalizedKeyword = normalize(keyword);
        boolean keywordCacheable = normalizedKeyword.isEmpty() || normalizedKeyword.length() >= 2;
        return keywordCacheable
                && pageable.getPageNumber() <= MAX_CACHEABLE_SEARCH_PAGE
                && pageable.getPageSize() <= MAX_CACHEABLE_SEARCH_SIZE;
    }

    private String sortToken(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return "unsorted";
        }
        return pageable.getSort().stream()
                .map(order -> order.getProperty() + "_" + order.getDirection().name())
                .collect(Collectors.joining(","));
    }

    private String categoryIdsToken(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return "none";
        }
        return categoryIds.stream()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String token(Object value) {
        return value == null ? "none" : String.valueOf(value);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
