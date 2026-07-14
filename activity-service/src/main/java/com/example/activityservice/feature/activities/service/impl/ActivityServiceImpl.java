package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityRequest;
import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.dto.ActivityStatsResponse;
import com.example.activityservice.feature.activities.dto.ActivityTimeLocationResponse;
import com.example.activityservice.feature.activities.dto.DepartmentStatsResponse;
import com.example.activityservice.feature.activities.dto.RecommendationResponse;
import com.example.activityservice.feature.activities.dto.SystemStatsResponse;
import com.example.activityservice.feature.activities.service.ActivityService;
import com.example.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityCommandOperations commandOperations;
    private final ActivityQueryOperations queryOperations;
    private final StudentActivityOperations studentOperations;
    private final AdminActivityOperations adminOperations;
    private final ActivityContentGenerationService contentGenerationService;

    @Override
    public ActivityResponse createActivity(ActivityRequest request) {
        return commandOperations.createActivity(request);
    }

    @Override
    public ActivityResponse getActivityById(Long id) {
        return queryOperations.getActivityById(id);
    }

    @Override
    public ActivityResponse getMyCreatedActivity(Long id) {
        return queryOperations.getMyCreatedActivity(id);
    }

    @Override
    public ActivityTimeLocationResponse getActivityTimesAndLocation(Long id) {
        return queryOperations.getActivityTimesAndLocation(id);
    }

    @Override
    public PageDTO<ActivityResponse> getAllActivities(String keyword, String level, String status, Long departmentId,
            Pageable pageable) {
        return queryOperations.getAllActivities(keyword, level, status, departmentId, pageable);
    }

    @Override
    public PageDTO<ActivityResponse> getMyCreatedActivities(Pageable pageable) {
        return queryOperations.getMyCreatedActivities(pageable);
    }

    @Override
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        return commandOperations.updateActivity(id, request);
    }

    @Override
    public void deleteActivity(Long id) {
        commandOperations.deleteActivity(id);
    }

    @Override
    public void approveActivity(Long id) {
        adminOperations.approveActivity(id);
    }

    @Override
    public void rejectActivity(Long id, String reason) {
        adminOperations.rejectActivity(id, reason);
    }

    @Override
    public void cancelActivity(Long id, String reason) {
        adminOperations.cancelActivity(id, reason);
    }

    @Override
    public String getQrCodeForActivity(Long activityId) {
        return commandOperations.getQrCodeForActivity(activityId);
    }

    @Override
    public ActivityStatsResponse getActivityStats() {
        return adminOperations.getActivityStats();
    }

    @Override
    public PageDTO<ActivityResponse> searchActivities(
            String keyword, Long departmentId, String startDate, String endDate,
            List<Long> categoryIds, String category, String status, Pageable pageable) {
        return studentOperations.searchActivities(keyword, departmentId, startDate, endDate,
                categoryIds, category, status, pageable);
    }

    @Override
    public RecommendationResponse getRecommendations(Long studentId, int limit, Jwt jwt) {
        return studentOperations.getRecommendations(studentId, limit, jwt);
    }

    @Override
    public PageDTO<ActivityResponse> getActivitiesForRegistration(Long semesterId, Pageable pageable) {
        return studentOperations.getActivitiesForRegistration(semesterId, pageable);
    }

    @Override
    public DepartmentStatsResponse getDepartmentStatistics(Long departmentId, Long semesterId) {
        return adminOperations.getDepartmentStatistics(departmentId, semesterId);
    }

    @Override
    public SystemStatsResponse getSystemStatistics(Long semesterId) {
        return adminOperations.getSystemStatistics(semesterId);
    }

    @Override
    public String generateDescription(String prompt) {
        return contentGenerationService.generateDescription(prompt);
    }
}
