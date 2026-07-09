package com.example.activityservice.feature.points.service;

import com.example.activityservice.feature.points.dto.CategoryPointResponse;
import com.example.activityservice.feature.points.dto.PointDetailsResponse;
import com.example.activityservice.feature.points.dto.PointSummaryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointCacheService {

    private static final String SUMMARY_KEY_PREFIX = "points:summary:";
    private static final String DETAILS_KEY_PREFIX = "points:details:";
    private static final String CATEGORIES_KEY_PREFIX = "points:categories:";
    private static final long STUDENT_POINT_TTL_MINUTES = 2;
    private static final long CATEGORY_RULE_TTL_MINUTES = 10;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<PointSummaryResponse> getSummary(Long studentId, Long semesterId) {
        return read(summaryKey(studentId, semesterId), PointSummaryResponse.class);
    }

    public void putSummary(Long studentId, Long semesterId, PointSummaryResponse response) {
        write(summaryKey(studentId, semesterId), response, STUDENT_POINT_TTL_MINUTES);
    }

    public Optional<PointDetailsResponse> getDetails(Long studentId, Long semesterId) {
        return read(detailsKey(studentId, semesterId), PointDetailsResponse.class);
    }

    public void putDetails(Long studentId, Long semesterId, PointDetailsResponse response) {
        write(detailsKey(studentId, semesterId), response, STUDENT_POINT_TTL_MINUTES);
    }

    public Optional<List<CategoryPointResponse>> getCategories(Long semesterId) {
        return read(categoriesKey(semesterId), new TypeReference<>() {});
    }

    public void putCategories(Long semesterId, List<CategoryPointResponse> response) {
        write(categoriesKey(semesterId), response, CATEGORY_RULE_TTL_MINUTES);
    }

    public void evictStudentPointCaches(Long studentId, Long semesterId) {
        if (studentId == null || semesterId == null) {
            return;
        }

        try {
            redisTemplate.delete(List.of(summaryKey(studentId, semesterId), detailsKey(studentId, semesterId)));
        } catch (Exception e) {
            log.warn("Failed to evict point caches for student {} semester {}: {}",
                    studentId, semesterId, e.getMessage());
        }
    }

    public void evictSemesterPointCaches(Long semesterId) {
        if (semesterId == null) {
            return;
        }

        try {
            Set<String> summaryKeys = redisTemplate.keys(SUMMARY_KEY_PREFIX + "*:" + semesterId);
            Set<String> detailsKeys = redisTemplate.keys(DETAILS_KEY_PREFIX + "*:" + semesterId);
            if (summaryKeys != null && !summaryKeys.isEmpty()) {
                redisTemplate.delete(summaryKeys);
            }
            if (detailsKeys != null && !detailsKeys.isEmpty()) {
                redisTemplate.delete(detailsKeys);
            }
            redisTemplate.delete(categoriesKey(semesterId));
        } catch (Exception e) {
            log.warn("Failed to evict point caches for semester {}: {}", semesterId, e.getMessage());
        }
    }

    public void evictCategoryRuleCaches() {
        try {
            Set<String> categoryKeys = redisTemplate.keys(CATEGORIES_KEY_PREFIX + "*");
            if (categoryKeys != null && !categoryKeys.isEmpty()) {
                redisTemplate.delete(categoryKeys);
            }
        } catch (Exception e) {
            log.warn("Failed to evict point category rule caches: {}", e.getMessage());
        }
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(String.valueOf(cached), type));
        } catch (Exception e) {
            log.warn("Redis read failed for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> read(String key, TypeReference<T> type) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(String.valueOf(cached), type));
        } catch (Exception e) {
            log.warn("Redis read failed for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void write(String key, Object value, long ttlMinutes) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(value),
                    ttlMinutes,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("Redis write failed for key {}: {}", key, e.getMessage());
        }
    }

    private String summaryKey(Long studentId, Long semesterId) {
        return SUMMARY_KEY_PREFIX + studentId + ":" + semesterId;
    }

    private String detailsKey(Long studentId, Long semesterId) {
        return DETAILS_KEY_PREFIX + studentId + ":" + semesterId;
    }

    private String categoriesKey(Long semesterId) {
        return CATEGORIES_KEY_PREFIX + semesterId;
    }
}
