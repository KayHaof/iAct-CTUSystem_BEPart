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
        return ApiResponse.<ProofResponse>builder()
                .result(proofService.submitProof(request))
                .message("Nộp minh chứng thành công! Đang chờ BTC duyệt.")
                .build();
    }
}