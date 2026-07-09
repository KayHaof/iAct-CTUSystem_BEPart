package com.example.activityservice.feature.complaints.controller;

import com.example.activityservice.feature.complaints.dto.ComplaintEligibleActivityResponse;
import com.example.activityservice.feature.complaints.dto.ComplaintRequest;
import com.example.activityservice.feature.complaints.dto.ComplaintResponse;
import com.example.activityservice.feature.complaints.service.ComplaintService;
import com.example.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {
    private final ComplaintService complaintService;

    @GetMapping("/my-eligible-activities")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<ComplaintEligibleActivityResponse>> getMyEligibleActivities(
            @RequestParam(required = false) Long semesterId) {
        return ApiResponse.success(complaintService.getMyEligibleActivities(semesterId));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ComplaintResponse> submitComplaint(@RequestBody @Valid ComplaintRequest request) {
        return ApiResponse.success(complaintService.submitComplaint(request), "Gui khieu nai thanh cong");
    }
}
