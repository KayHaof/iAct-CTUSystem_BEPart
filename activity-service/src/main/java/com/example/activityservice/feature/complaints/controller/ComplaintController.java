package com.example.activityservice.feature.complaints.controller;

import com.example.activityservice.feature.complaints.dto.ComplaintEligibleActivityResponse;
import com.example.activityservice.feature.complaints.dto.ComplaintRequest;
import com.example.activityservice.feature.complaints.dto.ComplaintResponse;
import com.example.activityservice.feature.complaints.dto.ResolveComplaintRequest;
import com.example.activityservice.feature.complaints.service.ComplaintService;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<PageDTO<ComplaintResponse>> getComplaints(
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(complaintService.getComplaints(activityId, status, pageable));
    }

    @GetMapping("/my-eligible-activities")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<ComplaintEligibleActivityResponse>> getMyEligibleActivities(
            @RequestParam(required = false) Long semesterId) {
        return ApiResponse.success(complaintService.getMyEligibleActivities(semesterId));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ComplaintResponse> submitComplaint(@RequestBody @Valid ComplaintRequest request) {
        return ApiResponse.success(complaintService.submitComplaint(request), "Gửi khiếu nại thành công");
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<ComplaintResponse> approveComplaint(
            @PathVariable Long id,
            @RequestBody @Valid ResolveComplaintRequest request) {
        return ApiResponse.success(complaintService.approveComplaint(id, request), "Duyệt khiếu nại thành công");
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<ComplaintResponse> rejectComplaint(
            @PathVariable Long id,
            @RequestBody @Valid ResolveComplaintRequest request) {
        return ApiResponse.success(complaintService.rejectComplaint(id, request), "Từ chối khiếu nại thành công");
    }
}
