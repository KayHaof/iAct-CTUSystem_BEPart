package com.example.activityservice.feature.certificate_submission_complaints.controller;

import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintApproveRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintRejectRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintResponse;
import com.example.activityservice.feature.certificate_submission_complaints.service.CertificateSubmissionComplaintService;
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

@RestController
@RequestMapping("/api/v1/certificate-submission-complaints")
@RequiredArgsConstructor
public class CertificateSubmissionComplaintController {

    private final CertificateSubmissionComplaintService complaintService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CertificateSubmissionComplaintResponse> submit(
            @RequestBody @Valid CertificateSubmissionComplaintRequest request) {
        return ApiResponse.success(complaintService.submit(request), "Gui khiieu nai thanh cong.");
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageDTO<CertificateSubmissionComplaintResponse>> getMyComplaints(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(complaintService.getMyComplaints(semesterId, status, pageable(page, size)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<PageDTO<CertificateSubmissionComplaintResponse>> getReviewComplaints(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(complaintService.getReviewComplaints(
                status, departmentId, semesterId, keyword, pageable(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'DEPARTMENT')")
    public ApiResponse<CertificateSubmissionComplaintResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(complaintService.getById(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<CertificateSubmissionComplaintResponse> approve(
            @PathVariable Long id,
            @RequestBody @Valid CertificateSubmissionComplaintApproveRequest request) {
        return ApiResponse.success(complaintService.approve(id, request), "Duyet khiieu nai thanh cong.");
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<CertificateSubmissionComplaintResponse> reject(
            @PathVariable Long id,
            @RequestBody @Valid CertificateSubmissionComplaintRejectRequest request) {
        return ApiResponse.success(complaintService.reject(id, request), "Tu choi khiieu nai thanh cong.");
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
