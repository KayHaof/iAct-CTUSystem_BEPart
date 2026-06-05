package com.example.userservice.feature.preference.service.impl;

import com.example.userservice.feature.preference.dto.NotificationSettings;
import com.example.userservice.feature.preference.dto.PreferenceRequest;
import com.example.userservice.feature.preference.dto.PreferenceResponse;
import com.example.userservice.feature.preference.model.StudentPreferences;
import com.example.userservice.feature.preference.repository.StudentPreferencesRepository;
import com.example.userservice.feature.preference.service.PreferenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceServiceImpl implements PreferenceService {

    private final StudentPreferencesRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_CATEGORY_RATINGS = "{\"1\":3,\"2\":3,\"3\":3,\"4\":3,\"5\":3}";
    private static final String DEFAULT_CATEGORY_ENABLED = "{\"1\":true,\"2\":true,\"3\":true,\"4\":true,\"5\":true}";
    private static final String DEFAULT_NOTIFICATION_SETTINGS = "{\"newActivityAlert\":true,\"reminderAlert\":true,\"reminderDaysBefore\":1}";

    @Override
    @Transactional(readOnly = true)
    public PreferenceResponse getPreferences(Long userId) {
        StudentPreferences prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        return mapToResponse(prefs);
    }

    @Override
    @Transactional
    public PreferenceResponse updatePreferences(Long userId, PreferenceRequest request) {
        StudentPreferences prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        if (request.getCategoryRatings() != null) {
            prefs.setCategoryRatings(toJson(request.getCategoryRatings()));
        }
        if (request.getCategoryEnabled() != null) {
            prefs.setCategoryEnabled(toJson(request.getCategoryEnabled()));
        }
        if (request.getPreferredCategoryIds() != null) {
            prefs.setPreferredCategoryIds(toJson(request.getPreferredCategoryIds()));
        }
        if (request.getNotificationSettings() != null) {
            prefs.setNotificationSettings(toJson(request.getNotificationSettings()));
        }
        if (request.getExcludedCategories() != null) {
            prefs.setExcludedCategories(toJson(request.getExcludedCategories()));
        }
        if (request.getAiRecommendationEnabled() != null) {
            prefs.setAiRecommendationEnabled(request.getAiRecommendationEnabled());
        }

        return mapToResponse(preferenceRepository.save(prefs));
    }

    @Override
    @Transactional
    public PreferenceResponse resetToDefault(Long userId) {
        StudentPreferences prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> StudentPreferences.builder().userId(userId).build());

        prefs.setCategoryRatings(DEFAULT_CATEGORY_RATINGS);
        prefs.setCategoryEnabled(DEFAULT_CATEGORY_ENABLED);
        prefs.setPreferredCategoryIds(null);
        prefs.setNotificationSettings(DEFAULT_NOTIFICATION_SETTINGS);
        prefs.setExcludedCategories(null);
        prefs.setAiRecommendationEnabled(true);

        return mapToResponse(preferenceRepository.save(prefs));
    }

    private StudentPreferences createDefaultPreferences(Long userId) {
        StudentPreferences prefs = StudentPreferences.builder()
                .userId(userId)
                .categoryRatings(DEFAULT_CATEGORY_RATINGS)
                .categoryEnabled(DEFAULT_CATEGORY_ENABLED)
                .notificationSettings(DEFAULT_NOTIFICATION_SETTINGS)
                .aiRecommendationEnabled(true)
                .build();
        return preferenceRepository.save(prefs);
    }

    private PreferenceResponse mapToResponse(StudentPreferences prefs) {
        return PreferenceResponse.builder()
                .id(prefs.getId())
                .userId(prefs.getUserId())
                .categoryRatings(parseJson(prefs.getCategoryRatings(), new TypeReference<Map<String, Integer>>() {}))
                .categoryEnabled(parseJson(prefs.getCategoryEnabled(), new TypeReference<Map<String, Boolean>>() {}))
                .preferredCategoryIds(parseJson(prefs.getPreferredCategoryIds(), new TypeReference<Long[]>() {}))
                .notificationSettings(parseJson(prefs.getNotificationSettings(), 
                        new TypeReference<NotificationSettings>() {}))
                .excludedCategories(parseJson(prefs.getExcludedCategories(), new TypeReference<String[]>() {}))
                .aiRecommendationEnabled(prefs.getAiRecommendationEnabled())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
            return null;
        }
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON: {}", json, e);
            return null;
        }
    }
}
