package com.example.activityservice.feature.certificate_submissions.controller;

import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionRejectRequest;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionRequest;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionResponse;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionReviewRequest;
import com.example.activityservice.feature.certificate_submissions.model.CertificateSubmission;
import com.example.activityservice.feature.certificate_submissions.service.CertificateSubmissionService;
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
@RequestMapping("/api/v1/certificate-submissions")
@RequiredArgsConstructor
public class CertificateSubmissionController {

    private final CertificateSubmissionService certificateSubmissionService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CertificateSubmissionResponse> submit(@RequestBody @Valid CertificateSubmissionRequest request) {
        CertificateSubmissionResponse response = certificateSubmissionService.submit(request);
        String message = response.getStatus() != null
                && response.getStatus() == CertificateSubmission.STATUS_REJECTED
                ? "Hồ sơ bị từ chối tự động do minh chứng không khớp. Vui lòng kiểm tra lại."
                : "Nộp giấy khen thành công. Hồ sơ đang chờ Trường duyệt.";
        return ApiResponse.success(response, message);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageDTO<CertificateSubmissionResponse>> getMySubmissions(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(certificateSubmissionService.getMySubmissions(
                semesterId, status, pageable(page, size)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<PageDTO<CertificateSubmissionResponse>> getReviewSubmissions(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(certificateSubmissionService.getReviewSubmissions(
                status, departmentId, semesterId, keyword, pageable(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'DEPARTMENT')")
    public ApiResponse<CertificateSubmissionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(certificateSubmissionService.getById(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<CertificateSubmissionResponse> approve(
            @PathVariable Long id,
            @RequestBody @Valid CertificateSubmissionReviewRequest request) {
        return ApiResponse.success(
                certificateSubmissionService.approve(id, request),
                "Duyệt giấy khen thành công.");
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<CertificateSubmissionResponse> reject(
            @PathVariable Long id,
            @RequestBody @Valid CertificateSubmissionRejectRequest request) {
        return ApiResponse.success(
                certificateSubmissionService.reject(id, request),
                "Từ chối giấy khen thành công.");
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
