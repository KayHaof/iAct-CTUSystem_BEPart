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
public class PreferenceRequest {
    private Map<String, Integer> categoryRatings;
    private Map<String, Boolean> categoryEnabled;
    private Long[] preferredCategoryIds;
    private NotificationSettingsDto notificationSettings;
    private String[] excludedCategories;
    private Boolean aiRecommendationEnabled;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class NotificationSettingsDto {
    private Boolean newActivityAlert;
    private Boolean reminderAlert;
    private Integer reminderDaysBefore;
}
