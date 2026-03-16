package com.example.feature.activities.service;

import com.example.dto.PageDTO;
import com.example.feature.activities.dto.ActivityApprovalRequest;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;
import org.springframework.data.domain.Pageable; // Thêm import này vào

public interface ActivityService {
    ActivityResponse createActivity(ActivityRequest request);

    ActivityResponse getActivityById(Long id);

    PageDTO<ActivityResponse> getAllActivities(String keyword, String level, String status, Pageable pageable);

    ActivityResponse updateActivity(Long id, ActivityRequest request);

    void deleteActivity(Long id);

    ActivityResponse approveActivity(Long id, ActivityApprovalRequest request);

    String getQrCodeForActivity(Long activityId);
}