package com.example.userservice.feature.preference.service;

import com.example.userservice.feature.preference.dto.PreferenceRequest;
import com.example.userservice.feature.preference.dto.PreferenceResponse;

public interface PreferenceService {
    PreferenceResponse getPreferences(Long userId);
    PreferenceResponse updatePreferences(Long userId, PreferenceRequest request);
    PreferenceResponse resetToDefault(Long userId);
}
