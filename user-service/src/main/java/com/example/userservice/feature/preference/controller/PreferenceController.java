package com.example.userservice.feature.preference.controller;

import com.example.dto.ApiResponse;
import com.example.userservice.feature.preference.dto.PreferenceRequest;
import com.example.userservice.feature.preference.dto.PreferenceResponse;
import com.example.userservice.feature.preference.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student-preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    /**
     * UC10: Lay cau hinh uu tien goi y cua sinh vien
     */
    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PreferenceResponse> getMyPreferences(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ApiResponse.success(preferenceService.getPreferences(userId));
    }

    /**
     * UC10: Cap nhat cau hinh uu tien goi y
     */
    @PutMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PreferenceResponse> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PreferenceRequest request) {
        Long userId = extractUserId(jwt);
        return ApiResponse.success(preferenceService.updatePreferences(userId, request));
    }

    /**
     * UC10: Khoi phuc cau hinh mac dinh
     */
    @PostMapping("/reset")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PreferenceResponse> resetPreferences(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ApiResponse.success(preferenceService.resetToDefault(userId));
    }

    private Long extractUserId(Jwt jwt) {
        String subject = jwt.getSubject();
        return Long.parseLong(subject);
    }
}
