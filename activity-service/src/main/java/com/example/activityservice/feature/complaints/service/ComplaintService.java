package com.example.activityservice.feature.complaints.service;

import com.example.activityservice.feature.complaints.dto.ComplaintEligibleActivityResponse;
import com.example.activityservice.feature.complaints.dto.ComplaintRequest;
import com.example.activityservice.feature.complaints.dto.ComplaintResponse;
import com.example.activityservice.feature.complaints.dto.ResolveComplaintRequest;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ComplaintService {
    List<ComplaintEligibleActivityResponse> getMyEligibleActivities(Long semesterId);

    ComplaintResponse submitComplaint(ComplaintRequest request);

    PageDTO<ComplaintResponse> getComplaints(Long activityId, Integer status, Pageable pageable);

    ComplaintResponse approveComplaint(Long id, ResolveComplaintRequest request);

    ComplaintResponse rejectComplaint(Long id, ResolveComplaintRequest request);
}
