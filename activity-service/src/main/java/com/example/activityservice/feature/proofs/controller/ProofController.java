package com.example.activityservice.feature.proofs.controller;

import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
import com.example.activityservice.feature.proofs.dto.ProofActivitySummaryResponse;
import com.example.activityservice.feature.proofs.dto.ProofStatusResponse;
import com.example.activityservice.feature.proofs.service.ProofService;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proofs")
@RequiredArgsConstructor
public class ProofController {
    private final ProofService proofService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<PageDTO<ProofResponse>> getProofs(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long activityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(proofService.getProofs(status, activityId, pageable));
    }

    @GetMapping("/activity/{activityId}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<PageDTO<ProofResponse>> getSubmittedStudents(
            @PathVariable Long activityId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(proofService.getSubmittedStudents(activityId, status, pageable));
    }

    @GetMapping("/activity/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<ProofActivitySummaryResponse> getActivitySummary(@RequestParam Long activityId) {
        return ApiResponse.success(proofService.getActivitySummary(activityId));
    }

    @GetMapping("/my-status")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ProofStatusResponse> getMyProofStatus(@RequestParam Long activityId) {
        return ApiResponse.success(proofService.getMyProofStatus(activityId));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ProofResponse> submitProof(@RequestBody @Valid ProofSubmissionRequest request) {
        return ApiResponse.success(proofService.submitProof(request), "Nộp minh chứng thành công! Đang chờ BTC duyệt.");
    }

    @PutMapping("/{id}/resubmit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ProofResponse> resubmitProof(
            @PathVariable Long id,
            @RequestBody @Valid ProofSubmissionRequest request) {
        return ApiResponse.success(proofService.resubmitProof(id, request),
                "Nộp lại minh chứng thành công! Đang chờ BTC duyệt.");
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<ProofResponse> approveProof(@PathVariable Long id) {
        return ApiResponse.success(proofService.approveProof(id), "Duyệt minh chứng thành công.");
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<ProofResponse> rejectProof(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success(proofService.rejectProof(id, reason), "Từ chối minh chứng thành công.");
    }
}
