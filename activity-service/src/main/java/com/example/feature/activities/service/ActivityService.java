package com.example.feature.activities.service;

import com.example.dto.PageDTO;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;
import com.example.feature.activities.dto.ActivityStatsResponse;
import com.example.feature.activities.dto.ActivityTimeLocationResponse;
import org.springframework.data.domain.Pageable;

public interface ActivityService {
    ActivityResponse createActivity(ActivityRequest request);

    ActivityResponse getActivityById(Long id);

    ActivityTimeLocationResponse getActivityTimesAndLocation(Long id);

    PageDTO<ActivityResponse> getAllActivities(String keyword, String level, String status, Long departmentId, Pageable pageable);

    ActivityResponse updateActivity(Long id, ActivityRequest request);

    void deleteActivity(Long id);

    void approveActivity(Long id);
    void rejectActivity(Long id, String reason);
    void cancelActivity(Long id, String reason);

    String getQrCodeForActivity(Long activityId);

    ActivityStatsResponse getActivityStats();
}