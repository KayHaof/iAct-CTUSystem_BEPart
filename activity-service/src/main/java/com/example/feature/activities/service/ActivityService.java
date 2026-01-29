package com.example.feature.activities.service;

import com.example.feature.activities.dto.ActivityApprovalRequest;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;

import java.util.List;

public interface ActivityService {
    ActivityResponse createActivity(ActivityRequest request);
    ActivityResponse getActivityById(Long id);
    List<ActivityResponse> getAllActivities();
    ActivityResponse updateActivity(Long id, ActivityRequest request);
    void deleteActivity(Long id);
    ActivityResponse approveActivity(Long id, ActivityApprovalRequest request);
}