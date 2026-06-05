package com.example.userservice.feature.preference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceResponse {
    private Long id;
    private Long userId;
    private Map<String, Integer> categoryRatings;  // categoryId -> rating (1-5)
    private Map<String, Boolean> categoryEnabled;  // categoryId -> enabled/disabled
    private Long[] preferredCategoryIds;
    private NotificationSettings notificationSettings;
    private String[] excludedCategories;
    private Boolean aiRecommendationEnabled;
}
