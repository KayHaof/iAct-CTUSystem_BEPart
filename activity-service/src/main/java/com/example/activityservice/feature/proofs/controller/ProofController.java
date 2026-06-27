package com.example.activityservice.feature.proofs.controller;

import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
import com.example.activityservice.feature.proofs.service.ProofService;
import com.example.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proofs")
@RequiredArgsConstructor
public class ProofController {
    private final ProofService proofService;

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ProofResponse> submitProof(@RequestBody @Valid ProofSubmissionRequest request) {
        return ApiResponse.success(proofService.submitProof(request), "Nop minh chung thanh cong! Dang cho BTC duyet.");
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<ProofResponse> approveProof(@PathVariable Long id) {
        return ApiResponse.success(proofService.approveProof(id), "Duyet minh chung thanh cong.");
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<ProofResponse> rejectProof(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success(proofService.rejectProof(id, reason), "Tu choi minh chung thanh cong.");
    }
}
