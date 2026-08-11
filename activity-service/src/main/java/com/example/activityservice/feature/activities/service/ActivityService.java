package com.example.activityservice.feature.activities.service;

import com.example.activityservice.feature.activities.dto.*;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface ActivityService {
    ActivityResponse createActivity(ActivityRequest request);

    ActivityResponse getActivityById(Long id);

    ActivityResponse getMyCreatedActivity(Long id);

    ActivityTimeLocationResponse getActivityTimesAndLocation(Long id);

    PageDTO<ActivityResponse> getAllActivities(
            String keyword,
            String level,
            String status,
            Long departmentId,
            boolean adminApprovalOnly,
            Pageable pageable);

    PageDTO<ActivityResponse> getDepartmentApprovalActivities(String keyword, String status, Long classId, Pageable pageable);

    ActivityStatsResponse getDepartmentApprovalStats(String keyword, Long classId);

    PageDTO<ActivityResponse> getMyCreatedActivities(Pageable pageable);

    ActivityResponse updateActivity(Long id, ActivityRequest request);

    void deleteActivity(Long id);

    void approveActivity(Long id);
    void rejectActivity(Long id, String reason);
    void cancelActivity(Long id, String reason);
    void requestAdminSupport(Long id, String reason);

    String getQrCodeForActivity(Long activityId);

    ActivityScheduleQrCodeResponse getQrCodeForSchedule(Long activityId, Long scheduleId);

    List<ActivityScheduleQrCodeResponse> getQrCodesForActivity(Long activityId);

    ActivityStatsResponse getActivityStats();

    PageDTO<ActivityResponse> searchActivities(
            String keyword, Long departmentId, String startDate, String endDate,
            List<Long> categoryIds, String category, String status, Pageable pageable);

    RecommendationResponse getRecommendations(Long studentId, int limit, Jwt jwt);

    PageDTO<ActivityResponse> getActivitiesForRegistration(Long semesterId, Pageable pageable);

    DepartmentStatsResponse getDepartmentStatistics(Long departmentId, Long semesterId);

    SystemStatsResponse getSystemStatistics(Long semesterId);

    String generateDescription(String prompt);
}
