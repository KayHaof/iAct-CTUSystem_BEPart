package com.example.activityservice.feature.proofs.service;


import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
import com.example.activityservice.feature.proofs.dto.ProofActivitySummaryResponse;
import com.example.activityservice.feature.proofs.dto.ProofStatusResponse;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface ProofService {
    ProofResponse submitProof(ProofSubmissionRequest request);
    PageDTO<ProofResponse> getProofs(Integer status, Long activityId, Pageable pageable);
    PageDTO<ProofResponse> getSubmittedStudents(Long activityId, Integer status, Pageable pageable);
    ProofActivitySummaryResponse getActivitySummary(Long activityId);
    ProofStatusResponse getMyProofStatus(Long activityId);
    ProofResponse resubmitProof(Long proofId, ProofSubmissionRequest request);
    ProofResponse approveProof(Long proofId);
    ProofResponse rejectProof(Long proofId, String reason);
}
