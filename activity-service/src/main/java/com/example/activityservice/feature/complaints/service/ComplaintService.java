package com.example.activityservice.feature.complaints.service;

import com.example.activityservice.feature.complaints.dto.ComplaintEligibleActivityResponse;
import com.example.activityservice.feature.complaints.dto.ComplaintRequest;
import com.example.activityservice.feature.complaints.dto.ComplaintResponse;

import java.util.List;

public interface ComplaintService {
    List<ComplaintEligibleActivityResponse> getMyEligibleActivities(Long semesterId);

    ComplaintResponse submitComplaint(ComplaintRequest request);
}
